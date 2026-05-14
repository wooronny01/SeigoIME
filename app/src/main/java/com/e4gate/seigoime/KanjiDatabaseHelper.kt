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
        // [완성판] 안드로이드 캐시 무력화를 위해 v3 사용
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
        // 초고속 검색을 위한 인덱스 생성
        db.execSQL("CREATE INDEX idx_hiragana ON $TABLE_NAME($COLUMN_HIRAGANA)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    // 앱 설치 후 최초 1회, CSV 데이터를 내장 DB로 빌드
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

    private fun toKatakana(str: String): String {
        return str.map {
            if (it in '\u3041'..'\u3096') (it + 0x60) else it
        }.joinToString("")
    }

    // ==========================================
    // [최종 마스터] 일본어 정석 변환 엔진
    // ==========================================
    fun getSuggestions(hiragana: String): List<String> {
        val resultSet = LinkedHashSet<String>()
        val db = this.readableDatabase
        
        // 1. 정확히 일치하는 단어(관용어/숙어/한자) 우선 검색
        // ORDER BY LENGTH로 단일 한자(1글자)를 항상 우선 배치
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

        // 2. 입력값으로 시작하는 긴 단어들 추가 (관용구 추천)
        val cursorPrefix = db.rawQuery(
            "SELECT $COLUMN_KANJI FROM $TABLE_NAME WHERE $COLUMN_HIRAGANA LIKE ? AND $COLUMN_HIRAGANA != ? ORDER BY LENGTH($COLUMN_KANJI) ASC LIMIT 100", 
            arrayOf("$hiragana%", hiragana)
        )
        if (cursorPrefix.moveToFirst()) {
            do {
                resultSet.add(cursorPrefix.getString(0))
            } while (cursorPrefix.moveToNext())
        }
        cursorPrefix.close()

        // 3. 마지막에 입력값 자체(히라가나/가타가나)를 넣어 선택권 부여
        resultSet.add(hiragana)
        resultSet.add(toKatakana(hiragana))

        return resultSet.toList()
    }
}
