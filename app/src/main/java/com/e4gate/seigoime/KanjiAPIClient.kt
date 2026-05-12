package com.e4gate.seigoime

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object KanjiAPIClient {
    // 구글 변환 API 엔드포인트
    private const val API_URL = "https://www.google.com/transliterate?langpair=ja-Hira|ja&text="

    // 네트워크 작업은 비동기로 처리해야 하므로 suspend 함수 사용
    suspend fun getKanjiSuggestions(hiragana: String): List<String> {
        return withContext(Dispatchers.IO) {
            val suggestions = mutableListOf<String>()
            // 빈 문자열이 들어오면 바로 리턴
            if (hiragana.isBlank()) return@withContext suggestions

            try {
                // 한글/일본어 URL 인코딩
                val encodedText = URLEncoder.encode(hiragana, "UTF-8")
                val url = URL(API_URL + encodedText)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 3000 // 3초 타임아웃
                connection.readTimeout = 3000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    
                    // 구글 API 응답 형태: [["ひらがな", ["漢字1", "漢字2", ...]]]
                    val jsonArray = JSONArray(response)
                    if (jsonArray.length() > 0) {
                        val firstWordBlock = jsonArray.getJSONArray(0)
                        val candidatesArray = firstWordBlock.getJSONArray(1)
                        
                        // 서버에서 준 추천 단어들을 리스트에 담기
                        for (i in 0 until candidatesArray.length()) {
                            suggestions.add(candidatesArray.getString(i))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("KanjiAPI", "구글 API 호출 실패: ${e.message}")
            }
            suggestions
        }
    }
}
