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
        
        val cursor = db.rawQuery(
            "SELECT $COLUMN_KANJI FROM $TABLE_NAME WHERE $COLUMN_HIRAGANA = ? LIMIT 100", 
            arrayOf(hiragana)
        )
        
        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.getString(0))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }
}
