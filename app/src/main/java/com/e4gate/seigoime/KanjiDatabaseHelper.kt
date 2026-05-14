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

    private fun toKatakana(str: String): String {
        return str.map {
            if (it in '\u3041'..'\u3096') (it + 0x60) else it
        }.joinToString("")
    }

    // ==========================================
    // [완성형 엔진] 정밀 검색 + 조사 분리 하이브리드
    // ==========================================
    fun getSuggestions(hiragana: String): List<String> {
        val resultSet = LinkedHashSet<String>()
        val db = this.readableDatabase
        
        // 1. [정밀 타격] 정확히 일치하는 단어 검색
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

        // 2. [문장형 처리] 조사 분리 로직 (Gboard 지능형 흉내내기)
        // 일본어 필수 조사 리스트
        val commonParticles = listOf("は", "が", "를", "を", "に", "で", "と", "も", "へ", "から", "まで")
        for (particle in commonParticles) {
            // 입력값이 조사로 끝나고, 몸통이 존재할 때만 작동 (안전장치)
            if (hiragana.length > particle.length && hiragana.endsWith(particle)) {
                val baseWord = hiragana.substring(0, hiragana.length - particle.length)
                
                // 몸통(명사)에 해당하는 한자를 찾아봄
                val cursorBase = db.rawQuery(
                    "SELECT $COLUMN_KANJI FROM $TABLE_NAME WHERE $COLUMN_HIRAGANA = ? ORDER BY LENGTH($COLUMN_KANJI) ASC LIMIT 5", 
                    arrayOf(baseWord)
                )
                if (cursorBase.moveToFirst()) {
                    do {
                        // 찾은 한자 몸통 + 조사 결합 (예: 私 + は)
                        resultSet.add(cursorBase.getString(0) + particle)
                    } while (cursorBase.moveToNext())
                }
                cursorBase.close()
            }
        }

        // 3. [자동완성] 입력값으로 시작하는 긴 단어들 덧붙임
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

        // 4. 입력값 원본 (히라가나/가타가나)
        resultSet.add(hiragana)
        resultSet.add(toKatakana(hiragana))

        return resultSet.toList()
    }
}
