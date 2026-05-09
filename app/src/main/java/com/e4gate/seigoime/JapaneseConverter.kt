package com.e4gate.seigoime

class JapaneseConverter {
    private var lastInput = ""

    private val dictionary = mapOf(
        "ㅇㅏ" to "あ", "ㅇㅣ" to "이", "ㅇㅜ" to "う", "ㅇㅔ" to "え", "ㅇㅗ" to "お", 
        "ㅇㅑ" to "や", "ㅇㅠ" to "ゆ", "ㅇㅛ" to "よ", 
        "ㅇㅗㅏ" to "わ", "ㅇㅜㅓ" to "를", "おㅏ" to "わ", "うㅓ" to "を", 
        
        "ㅋㅏ" to "か", "ㅋㅣ" to "き", "ㅋㅜ" to "く", "ㅋㅔ" to "け", "ㅋㅗ" to "こ", 
        "ㅅㅏ" to "さ", "ㅅㅣ" to "し", "ㅅㅜ" to "す", "ㅅㅡ" to "す", "ㅅㅔ" to "せ", "ㅅㅗ" to "そ", 
        "ㅌㅏ" to "た", "ㅌㅣ" to "ち", "ㅊㅣ" to "ち", "ㅌㅜ" to "つ", "ㅌㅡ" to "つ", "ㅊㅡ" to "つ", "ㅌㅔ" to "て", "ㅌㅗ" to "と", 
        "ㅎㅏ" to "은", "ㅎㅣ" to "ひ", "ㅎㅜ" to "ふ", "ㅎㅔ" to "へ", "ㅎㅗ" to "ほ", 
        "ㅁㅏ" to "ま", "ㅁㅣ" to "み", "ㅁㅜ" to "む", "ㅁㅔ" to "め", "ㅁㅗ" to "も", 
        "ㄴㅏ" to "な", "ㄴㅣ" to "に", "ㄴㅜ" to "ぬ", "ㄴㅔ" to "ね", "ㄴㅗ" to "の", 
        "ㄹㅏ" to "ら", "ㄹㅣ" to "り", "ㄹㅜ" to "る", "ㄹㅔ" to "れ", "ㄹㅗ" to "ろ", 
        "ㄱㅏ" to "が", "ㄱㅣ" to "ぎ", "ㄱㅜ" to "ぐ", "ㄱㅔ" to "게", "ㄱㅗ" to "ご", 
        "ㅈㅏ" to "ざ", "ㅈㅣ" to "じ", "ㅈㅜ" to "ず", "ㅈㅡ" to "ず", "ㅈㅔ" to "ぜ", "ㅈㅗ" to "ぞ", 
        "ㄷㅏ" to "だ", "ㄷㅣ" to "ぢ", "ㄷㅜ" to "づ", "ㄷㅡ" to "づ", "ㄷㅔ" to "で", "ㄷㅗ" to "ど", 
        "ㅂㅏ" to "ば", "ㅂㅣ" to "び", "ㅂㅜ" to "ぶ", "ㅂㅔ" to "べ", "ㅂㅗ" to "ぼ", 
        "ㅍㅏ" to "ぱ", "ㅍㅣ" to "ぴ", "ㅍㅜ" to "ぷ", "ㅍㅔ" to "ぺ", "ㅍㅗ" to "ぽ", 
        "ㅋㅑ" to "きゃ", "ㅋㅠ" to "きゅ", "ㅋㅛ" to "きょ", "ㅅㅑ" to "しゃ", "ㅅㅠ" to "しゅ", "ㅅㅛ" to "しょ", 
        "ㅊㅏ" to "ちゃ", "ㅊㅜ" to "치ゅ", "ㅊㅗ" to "ちょ", "ㅎㅑ" to "ひゃ", "ㅎㅠ" to "히ゅ", "ㅎㅛ" to "히ょ", 
        "ㅁㅑ" to "みゃ", "ㅁㅠ" to "みゅ", "ㅁㅛ" to "みょ", "ㄴㅑ" to "にゃ", "ㄴㅠ" to "에", "ㄴㅛ" to "에", 
        "ㄹㅑ" to "りゃ", "ㄹㅠ" to "りゅ", "ㄹㅛ" to "りょ", "ㄱㅑ" to "ぎゃ", "ㄱㅠ" to "ぎゅ", "ㄱㅛ" to "ぎょ", 
        "ㅈㅑ" to "じゃ", "ㅈㅠ" to "じゅ", "ㅈㅛ" to "じょ", "ㅂㅑ" to "びゃ", "ㅂㅠ" to "비ゅ", "ㅂㅛ" to "비ょ", 
        "ㅍㅑ" to "ぴゃ", "ㅍㅠ" to "ぴゅ", "ㅍㅛ" to "ぴょ"
    )

    // 🌟 대표님께서 말씀하신 '받침 촉음' 대응 리스트
    // 밭테(ㅌ+ㅌ), 깟테(ㅅ+ㅌ), 깝파(ㅂ+ㅍ) 등 모든 조합 수용
    private val sokuonTriggers = listOf("ㅌ", "ㅅ", "ㄲ", "ㅂ", "ㄱ")

    fun processInput(input: String): Pair<Int, String> {
        val combined = lastInput + input
        
        // 1. 사전 조합 성공 (가, 카, 와, 워 등)
        if (dictionary.containsKey(combined)) {
            val res = dictionary[combined]!!
            val deleteCount = lastInput.length
            lastInput = if (res == "お" || res == "う") res else ""
            return Pair(deleteCount, res)
        } 
        // 2. 🌟 받침 촉음 로직: "lastInput이 받침 후보군"이고 "다음에 자음이 오면" 촉음 변신!
        else if (sokuonTriggers.contains(lastInput) && !isVowel(input)) {
            val nextChar = input
            lastInput = nextChar 
            return Pair(1, "っ" + nextChar)
        }
        // 3. 단일 입력 처리 (ㄴ=ん 등)
        else if (dictionary.containsKey(input)) {
            lastInput = input
            return Pair(0, dictionary[input]!!)
        } 
        else {
            lastInput = input 
            return Pair(0, input)    
        }
    }

    private fun isVowel(c: String): Boolean {
        return "ㅏㅑㅓㅕㅗㅛㅜㅠㅡㅣㅐㅔㅘㅝ".contains(c)
    }

    fun flushPending(): Pair<Int, String>? {
        if (lastInput == "ㅅ" || lastInput == "ㅌ") { lastInput = ""; return Pair(1, "っ") }
        if (lastInput == "ㄴ") { lastInput = ""; return Pair(1, "ん") }
        return null
    }

    fun clearBuffer() { lastInput = "" }
}
