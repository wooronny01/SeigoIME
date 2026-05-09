package com.e4gate.seigoime

class JapaneseConverter {
    private var lastInput = ""

    // 출력값에 한글이 단 한 글자도 남지 않도록 완벽히 정제된 사전입니다.
    private val dictionary = mapOf(
        "ㅇㅏ" to "あ", "ㅇㅣ" to "い", "ㅇㅜ" to "う", "ㅇㅔ" to "え", "ㅇㅗ" to "お", 
        "ㅇㅑ" to "や", "ㅇㅠ" to "ゆ", "ㅇㅛ" to "よ", 
        "ㅇㅗㅏ" to "わ", "ㅇㅜㅓ" to "を", "おㅏ" to "わ", "うㅓ" to "を", 
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

    private val sokuonTriggers = listOf("ㅌ", "ㅅ", "ㄲ", "ㅂ", "ㄱ") // っ 로 변환될 받침 후보
    private val hatsuonTriggers = listOf("ㄴ", "ㅁ") // ん 으로 변환될 받침 후보

    fun processInput(input: String): Pair<Int, String> {
        val combined = lastInput + input
        
        // [1순위] 완벽한 조합이 사전에 있는지 먼저 확인 (ㅇ+ㅏ, ㅋ+ㅏ 등)
        if (dictionary.containsKey(combined)) {
            val res = dictionary[combined]!!
            val deleteCount = lastInput.length
            // わ, を 결합을 위해 お, う는 버퍼에 남겨둠
            lastInput = if (res == "お" || res == "う") res else ""
            return Pair(deleteCount, res)
        } 
        
        // [2순위] 사전 조합에 실패했을 때, 앞글자가 받침이고 다음 글자가 모음이 아닐 경우 촉음/발음 융합!
        if (sokuonTriggers.contains(lastInput) && !isVowel(input)) {
            val nextChar = input
            lastInput = nextChar 
            return Pair(1, "っ" + nextChar)
        }
        if (hatsuonTriggers.contains(lastInput) && !isVowel(input)) {
            val nextChar = input
            lastInput = nextChar 
            return Pair(1, "ん" + nextChar)
        }

        // [3순위] 합치지 못하는 단일 입력은 일단 출력하고 대기
        lastInput = input
        return Pair(0, dictionary[input] ?: input)
    }

    private fun isVowel(c: String): Boolean {
        return "ㅏㅑㅓㅕㅗㅛㅜㅠㅡㅣㅐㅔㅘㅝ".contains(c)
    }

    // 띄어쓰기나 엔터를 칠 때, 남아있는 받침(ㅅ, ㄴ 등)을 알아서 털어주는 함수
    fun flushPending(): Pair<Int, String>? {
        if (sokuonTriggers.contains(lastInput)) { val res = "っ"; lastInput = ""; return Pair(1, res) }
        if (hatsuonTriggers.contains(lastInput)) { val res = "ん"; lastInput = ""; return Pair(1, res) }
        return null
    }

    fun clearBuffer() { lastInput = "" }
}
