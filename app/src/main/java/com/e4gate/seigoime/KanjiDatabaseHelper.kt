package com.e4gate.seigoime

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class KanjiDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        // 이름을 v3로 바꿔서 다이어트된 DB를 새로 설치하게 만듭니다.
        private const val DATABASE_NAME = "seigo_kanji_v3.db" 
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "kanji_table"
        private const val COLUMN_HIRAGANA = "hiragana"
        private const val COLUMN_KANJI = "kanji"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = ("CREATE TABLE " + TABLE_NAME + "("
                + COLUMN_HIRAGANA + " TEXT,"
                + COLUMN_KANJI + " TEXT" + ")")
        db.execSQL(createTable)
        db.execSQL("CREATE INDEX idx_hiragana ON $TABLE_NAME($COLUMN_HIRAGANA)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    suspend fun loadCsvToDatabase(context: Context) {
        withContext(Dispatchers.IO) {
            val db = this@KanjiDatabaseHelper.writableDatabase
            val cursor = db.rawQuery("SELECT count(*) FROM $TABLE_NAME", null)
            cursor.moveToFirst()
            val count = cursor.getInt(0)
            cursor.close()

            if (count > 0) return@withContext

            db.beginTransaction()
            try {
                val inputStream = context.assets.open("kanji_data.csv") 
                val reader = BufferedReader(InputStreamReader(inputStream))
                var line: String?
                
                val insertStmt = db.compileStatement("INSERT INTO $TABLE_NAME ($COLUMN_HIRAGANA, $COLUMN_KANJI) VALUES (?, ?)")

                while (reader.readLine().also { line = it } != null) {
                    val tokens = line!!.split(",")
                    if (tokens.size >= 2) {
                        insertStmt.bindString(1, tokens[0].trim())
                        insertStmt.bindString(2, tokens[1].trim())
                        insertStmt.executeInsert()
                    }
                }
                db.setTransactionSuccessful()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                db.endTransaction()
            }
        }
    }

    // [추가됨] 가타가나 자동 변환 도우미 함수
    private fun toKatakana(str: String): String {
        return str.map {
            if (it in '\u3041'..'\u3096') (it + 0x60) else it
        }.joinToString("")
    }

    // ==========================================
    // [UX 최적화] 완벽한 정렬이 적용된 검색 엔진
    // ==========================================
    fun getSuggestions(hiragana: String): List<String> {
        val resultSet = LinkedHashSet<String>()
        val db = this.readableDatabase
        
        // 1순위: 사용자가 입력한 원본 히라가나 무조건 첫 번째에 추가
        resultSet.add(hiragana)
        
        // 2순위: 가타가나 변환 결과를 무조건 두 번째에 추가
        resultSet.add(toKatakana(hiragana))
        
        // 3순위: 정확히 일치하는 한자들을 "글자 수(LENGTH)"가 짧은 순서대로 가져옴 (1글자 -> 2글자...)
        val cursorExact = db.rawQuery(
            "SELECT $COLUMN_KANJI FROM $TABLE_NAME WHERE $COLUMN_HIRAGANA = ? ORDER BY LENGTH($COLUMN_KANJI) ASC", 
            arrayOf(hiragana)
        )
        
        if (cursorExact.moveToFirst()) {
            do {
                resultSet.add(cursorExact.getString(0))
            } while (cursorExact.moveToNext())
        }
        cursorExact.close()

        // 4순위: 자동완성 단어(시작하는 단어)들 역시 "글자 수"가 짧은 순서대로 덧붙임
        val cursorPrefix = db.rawQuery(
            "SELECT $COLUMN_KANJI FROM $TABLE_NAME WHERE $COLUMN_HIRAGANA LIKE ? AND $COLUMN_HIRAGANA != ? ORDER BY LENGTH($COLUMN_KANJI) ASC LIMIT 1000", 
            arrayOf("$hiragana%", hiragana)
        )
        
        if (cursorPrefix.moveToFirst()) {
            do {
                resultSet.add(cursorPrefix.getString(0))
            } while (cursorPrefix.moveToNext())
        }
        cursorPrefix.close()

        return resultSet.toList()
    }
}