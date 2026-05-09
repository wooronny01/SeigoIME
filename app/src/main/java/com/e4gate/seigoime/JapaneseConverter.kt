package com.e4gate.seigoime

class JapaneseConverter {
    private var lastInput = ""

    private val dictionary = mapOf(
        "ㅇㅏ" to "あ", "ㅇㅣ" to "い", "ㅇㅜ" to "う", "ㅇㅔ" to "え", "ㅇㅗ" to "お", 
        "ㅇㅑ" to "や", "ㅇㅠ" to "ゆ", "ㅇㅛ" to "よ", 
        "ㅇㅘ" to "わ", "ㅇㅝ" to "を", 
        
        // 🌟 핵심 추가: 3단계 결합을 위한 '마법의 징검다리'
        "おㅏ" to "わ", "うㅓ" to "を", 
        
        "ㄴ" to "ん", "ㅅ" to "っ", 
        "ㅋㅏ" to "か", "ㅋㅣ" to "き", "ㅋㅜ" to "く", "ㅋㅔ" to "け", "ㅋㅗ" to "こ", 
        "ㅅㅏ" to "さ", "ㅅㅣ" to "し", "ㅅㅜ" to "す", "ㅅㅡ" to "す", "ㅅㅔ" to "せ", "ㅅㅗ" to "そ", 
        "ㅌㅏ" to "た", "ㅌㅣ" to "ち", "ㅊㅣ" to "ち", "ㅌㅜ" to "つ", "ㅌㅡ" to "つ", "ㅊㅡ" to "つ", "ㅌㅔ" to "て", "ㅌㅗ" to "と", 
        "ㅎㅏ" to "は", "ㅎㅣ" to "ひ", "ㅎㅜ" to "ふ", "ㅎㅔ" to "へ", "ㅎㅗ" to "ほ", 
        "ㅁㅏ" to "ま", "ㅁㅣ" to "み", "ㅁㅜ" to "む", "ㅁㅔ" to "め", "ㅁㅗ" to "も", 
        "ㄴㅏ" to "な", "ㄴㅣ" to "に", "ㄴㅜ" to "ぬ", "ㄴㅔ" to "ね", "ㄴㅗ" to "の", 
        "ㄹㅏ" to "ら", "ㄹㅣ" to "り", "ㄹㅜ" to "る", "ㄹㅔ" to "れ", "ㄹㅗ" to "ろ", 
        "ㄱㅏ" to "が", "ㄱㅣ" to "ぎ", "ㄱㅜ" to "ぐ", "ㄱㅔ" to "げ", "ㄱㅗ" to "ご", 
        "ㅈㅏ" to "ざ", "ㅈㅣ" to "じ", "ㅈㅜ" to "ず", "ㅈㅡ" to "ず", "ㅈㅔ" to "ぜ", "ㅈㅗ" to "ぞ", 
        "ㄷㅏ" to "だ", "ㄷㅣ" to "ぢ", "ㄷㅜ" to "づ", "ㄷㅡ" to "づ", "ㄷㅔ" to "で", "ㄷㅗ" to "ど", 
        "ㅂㅏ" to "ば", "ㅂㅣ" to "び", "ㅂㅜ" to "ぶ", "ㅂㅔ" to "べ", "ㅂㅗ" to "ぼ", 
        "ㅍㅏ" to "ぱ", "ㅍㅣ" to "ぴ", "ㅍㅜ" to "ぷ", "ㅍㅔ" to "ぺ", "ㅍㅗ" to "ぽ", 
        "ㅋㅑ" to "きゃ", "ㅋㅠ" to "きゅ", "ㅋㅛ" to "きょ", 
        "ㅅㅑ" to "しゃ", "ㅅㅠ" to "しゅ", "ㅅㅛ" to "しょ", 
        "ㅊㅏ" to "ちゃ", "ㅊㅜ" to "ちゅ", "ㅊㅗ" to "ちょ", 
        "ㅎㅑ" to "ひゃ", "ㅎㅠ" to "ひゅ", "ㅎㅛ" to "ひょ", 
        "ㅁㅑ" to "みゃ", "ㅁㅠ" to "みゅ", "ㅁㅛ" to "みょ", 
        "ㄴㅑ" to "にゃ", "ㄴㅠ" to "にゅ", "ㄴㅛ" to "にょ", 
        "ㄹㅑ" to "りゃ", "ㄹㅠ" to "りゅ", "ㄹㅛ" to "りょ", 
        "ㄱㅑ" to "ぎゃ", "ㄱㅠ" to "ぎゅ", "ㄱㅛ" to "ぎょ", 
        "ㅈㅑ" to "じゃ", "ㅈㅠ" to "じゅ", "ㅈㅛ" to "じょ", 
        "ㅂㅑ" to "びゃ", "ㅂㅠ" to "びゅ", "ㅂㅛ" to "びょ", 
        "ㅍㅑ" to "ぴゃ", "ㅍㅠ" to "ぴゅ", "ㅍㅛ" to "ぴょ"
    )

    private val sokuonTriggers = mapOf("ㄱㅋ" to "ㅋ", "ㄱㄱ" to "ㄱ", "ㄱㄲ" to "ㄲ", "ㅂㅍ" to "ㅍ")

    fun processInput(input: String): Pair<Int, String> {
        val combined = lastInput + input
        
        if (dictionary.containsKey(combined)) {
            val res = dictionary[combined]!!
            val deleteCount = lastInput.length
            
            // 🌟 핵심 로직: わ, を 조합을 위해 'お', 'う'는 버퍼에 살려둡니다.
            if (res == "お" || res == "う") {
                lastInput = res
            } else {
                lastInput = "" 
            }
            return Pair(deleteCount, res)
        } 
        else if (sokuonTriggers.containsKey(combined)) {
            val nextChar = sokuonTriggers[combined]!!
            lastInput = nextChar 
            return Pair(1, "っ" + nextChar)
        }
        else if (dictionary.containsKey(input)) {
            lastInput = input
            return Pair(0, dictionary[input]!!)
        } 
        else {
            // 오타를 쳐도 다음 글자 입력이 꼬이지 않도록 버퍼를 새 글자로 갱신합니다.
            lastInput = input 
            return Pair(0, input)    
        }
    }

    fun clearBuffer() { lastInput = "" }
}
