package com.e4gate.seigoime

class JapaneseConverter {
    private var lastInput = ""

    private val dictionary = mapOf(
        "ㅇㅏ" to "あ", "ㅇㅣ" to "い", "ㅇㅜ" to "う", "ㅇㅔ" to "え", "ㅇㅗ" to "お", 
        "ㅇㅑ" to "や", "ㅇㅠ" to "ゆ", "ㅇㅛ" to "よ", 
        "ㅇㅘ" to "わ", "ㅇㅝ" to "を", 
        "おㅏ" to "わ", "うㅓ" to "を", 
        
        // "ㅅ" -> "っ", "ㄴ" -> "ん" 단독 변환 제거 (일단 ㅅ, ㄴ으로 출력하고 대기합니다!)
        
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

    // 대표님 아이디어: ㅅ 다음에 자음이 오면 무조건 'っ + 자음'으로 변신!
    private val sokuonTriggers = mapOf(
        "ㄱㅋ" to "ㅋ", "ㄱㄱ" to "ㄱ", "ㄱㄲ" to "ㄲ", "ㅂㅍ" to "ㅍ",
        "ㅅㄱ" to "ㄱ", "ㅅㄴ" to "ㄴ", "ㅅㄷ" to "ㄷ", "ㅅㄹ" to "ㄹ", "ㅅㅁ" to "ㅁ", "ㅅㅂ" to "ㅂ", 
        "ㅅㅅ" to "ㅅ", "ㅅㅇ" to "ㅇ", "ㅅㅈ" to "ㅈ", "ㅅㅊ" to "ㅊ", "ㅅㅋ" to "ㅋ", "ㅅㅌ" to "ㅌ", 
        "ㅅㅍ" to "ㅍ", "ㅅㅎ" to "ㅎ"
    )

    // 대표님 아이디어: ㄴ 다음에 자음이 오면 무조건 'ん + 자음'으로 변신!
    private val hatsuonTriggers = mapOf(
        "ㄴㄱ" to "ㄱ", "ㄴㄴ" to "ㄴ", "ㄴㄷ" to "ㄷ", "ㄴㄹ" to "ㄹ", "ㄴㅁ" to "ㅁ", "ㄴㅂ" to "ㅂ", 
        "ㄴㅅ" to "ㅅ", "ㄴㅇ" to "ㅇ", "ㄴㅈ" to "ㅈ", "ㄴㅊ" to "ㅊ", "ㄴㅋ" to "ㅋ", "ㄴㅌ" to "ㅌ", 
        "ㄴㅍ" to "ㅍ", "ㄴㅎ" to "ㅎ"
    )

    fun processInput(input: String): Pair<Int, String> {
        val combined = lastInput + input
        
        if (dictionary.containsKey(combined)) {
            val res = dictionary[combined]!!
            val deleteCount = lastInput.length
            if (res == "お" || res == "う") lastInput = res
            else lastInput = "" 
            return Pair(deleteCount, res)
        } 
        else if (sokuonTriggers.containsKey(combined)) {
            val nextChar = sokuonTriggers[combined]!!
            lastInput = nextChar 
            return Pair(1, "っ" + nextChar)
        }
        else if (hatsuonTriggers.containsKey(combined)) {
            val nextChar = hatsuonTriggers[combined]!!
            lastInput = nextChar 
            return Pair(1, "ん" + nextChar)
        }
        else if (dictionary.containsKey(input)) {
            lastInput = input
            return Pair(0, dictionary[input]!!)
        } 
        else {
            lastInput = input 
            return Pair(0, input)    
        }
    }

    // 띄어쓰기나 엔터를 칠 때 혼자 남은 'ㅅ'이나 'ㄴ'을 'っ', 'ん'으로 마무리해주는 마법 함수
    fun flushPending(): Pair<Int, String>? {
        if (lastInput == "ㅅ") { lastInput = ""; return Pair(1, "っ") }
        if (lastInput == "ㄴ") { lastInput = ""; return Pair(1, "ん") }
        return null
    }

    fun clearBuffer() { lastInput = "" }
}
