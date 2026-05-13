package com.e4gate.seigoime

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object KanjiAPIClient {
    // [핵심 원인 해결] 구글 API 엔드포인트에 &num=20 을 추가하여 무조건 5개만 오던 제한을 풀었습니다.
    private const val API_URL = "https://www.google.com/transliterate?langpair=ja-Hira|ja&num=100&text="

    suspend fun getKanjiSuggestions(hiragana: String): List<String> {
        return withContext(Dispatchers.IO) {
            val suggestions = mutableListOf<String>()
            if (hiragana.isBlank()) return@withContext suggestions

            try {
                val encodedText = URLEncoder.encode(hiragana, "UTF-8")
                val url = URL(API_URL + encodedText)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 3000
                connection.readTimeout = 3000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    
                    val jsonArray = JSONArray(response)
                    if (jsonArray.length() > 0) {
                        val firstWordBlock = jsonArray.getJSONArray(0)
                        val candidatesArray = firstWordBlock.getJSONArray(1)
                        
                        // 이제 구글이 5개가 아닌 20개(또는 해당 단어의 최대치)의 한자를 보내줍니다!
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
