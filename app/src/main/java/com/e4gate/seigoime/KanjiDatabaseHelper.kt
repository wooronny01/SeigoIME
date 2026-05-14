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
        private const val DATABASE_NAME = "kanji_dictionary.db"
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
        
        // 108만 개 데이터 검색을 위한 초고속 인덱스(Index) 생성
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

            // 이미 데이터가 로딩되어 있다면 건너뜁니다
            if (count > 0) return@withContext

            db.beginTransaction()
            try {
                // assets 폴더에 있는 파일 읽기 (파일명이 다르면 여기를 수정해야 합니다)
                val inputStream = context.assets.open("kanji_data.csv") 
                val reader = BufferedReader(InputStreamReader(inputStream))
                var line: String?
                
                val insertStmt = db.compileStatement("INSERT INTO $TABLE_NAME ($COLUMN_HIRAGANA, $COLUMN_KANJI) VALUES (?, ?)")

                while (reader.readLine().also { line = it } != null) {
                    val tokens = line!!.split(",")
                    if (tokens.size >= 2) {
                        insertStmt.bindString(1, tokens[0].trim()) // 히라가나
                        insertStmt.bindString(2, tokens[1].trim()) // 한자
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

    fun getSuggestions(hiragana: String): List<String> {
        val list = mutableListOf<String>()
        val db = this.readableDatabase
        
        // 1. 정확히 일치하는 단어 먼저 최우선으로 찾기 (예: "えい" -> 英, 影)
        // 제한을 100개에서 150개로 늘렸습니다.
        val cursorExact = db.rawQuery(
            "SELECT $COLUMN_KANJI FROM $TABLE_NAME WHERE $COLUMN_HIRAGANA = ? LIMIT 150", 
            arrayOf(hiragana)
        )
        
        if (cursorExact.moveToFirst()) {
            do {
                val kanji = cursorExact.getString(0)
                if (!list.contains(kanji)) list.add(kanji) // 중복 방지
            } while (cursorExact.moveToNext())
        }
        cursorExact.close()

        // 2. Gboard처럼 입력한 글자로 '시작하는(자동완성)' 단어들 추가로 찾기 (예: "えい" -> 映画, 英語)
        // 최대 300개까지 시원하게 끌어옵니다!
        val cursorPrefix = db.rawQuery(
            "SELECT $COLUMN_KANJI FROM $TABLE_NAME WHERE $COLUMN_HIRAGANA LIKE ? AND $COLUMN_HIRAGANA != ? LIMIT 300", 
            arrayOf("$hiragana%", hiragana)
        )
        
        if (cursorPrefix.moveToFirst()) {
            do {
                val kanji = cursorPrefix.getString(0)
                if (!list.contains(kanji)) list.add(kanji) // 중복 방지
            } while (cursorPrefix.moveToNext())
        }
        cursorPrefix.close()

        return list
    }
}