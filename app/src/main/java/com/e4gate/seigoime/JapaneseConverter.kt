package com.e4gate.seigoime

class JapaneseConverter {
    private var pendingConsonant = ""

    fun clearBuffer() {
        pendingConsonant = ""
    }

    fun flushPending(): Pair<Int, String>? {
        if (pendingConsonant.isNotEmpty()) {
            val res = pendingConsonant
            pendingConsonant = ""
            return Pair(0, res)
        }
        return null
    }

    fun processInput(text: String): Pair<Int, String> {
        val consonants = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"
        val vowels = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ"

        // 1. 자음이 들어온 경우
        if (consonants.contains(text)) {
            val prev = pendingConsonant
            
            // 🌟 1) 발음 'ん' 규칙 (ㄴ 연속, 또는 ㅇ+ㄴ)
            if ((prev == "ㄴ" && text == "ㄴ") || (prev == "ㅇ" && text == "ㄴ")) {
                pendingConsonant = "" 
                return Pair(1, "ん") 
            }
            
            // 🌟 2) 발음 'ん' 특수 규칙 (ㅁ 치고 ㅂ/ㅍ 칠 때 -> 신문 shimbun)
            if (prev == "ㅁ" && "ㅂㅍㅃㅁ".contains(text)) {
                pendingConsonant = text
                return Pair(1, "ん" + text)
            }

            // 🌟 3) 촉음 'っ' 규칙 (같은 발음군 연속 입력)
            val isK = "ㄱㅋㄲ".contains(prev) && "ㄱㅋㄲ".contains(text)
            val isT = "ㄷㅌㄸ".contains(prev) && "ㄷㅌㄸ".contains(text)
            val isS = "ㅅㅆ".contains(prev) && "ㅅㅆ".contains(text)
            val isP = "ㅂㅍㅃ".contains(prev) && "ㅂㅍㅃ".contains(text)
            val isJ = "ㅈㅉㅊ".contains(prev) && "ㅈㅉㅊ".contains(text)
            
            if (isK || isT || isS || isP || isJ) {
                pendingConsonant = text
                return Pair(1, "っ" + text) // 이전 자음을 지우고 촉음+현재자음 출력
            }
            
            pendingConsonant = text
            return Pair(0, text)
        }

        // 2. 모음이 들어온 경우
        if (vowels.contains(text)) {
            if (pendingConsonant.isNotEmpty()) {
                val combined = getKana(pendingConsonant, text)
                pendingConsonant = ""
                return Pair(1, combined)
            } else {
                val kana = getKana("ㅇ", text)
                return Pair(0, kana)
            }
        }
        return Pair(0, text)
    }

    private fun getKana(c: String, v: String): String {
        val key = c + v
        return kanaMap[key] ?: (c + v) 
    }

    private val kanaMap = mapOf(
        // ㅇ (Vowels)
        "ㅇㅏ" to "あ", "ㅇㅣ" to "い", "ㅇㅜ" to "う", "ㅇㅡ" to "う", "ㅇㅔ" to "え", "ㅇㅗ" to "お",
        "ㅇㅑ" to "や", "ㅇㅠ" to "ゆ", "ㅇㅛ" to "よ",

        // ㄱ/ㅋ (K / G)
        "ㄱㅏ" to "か", "ㄱㅣ" to "き", "ㄱㅜ" to "く", "ㄱㅡ" to "く", "ㄱㅔ" to "け", "ㄱㅗ" to "こ",
        "ㅋㅏ" to "か", "ㅋㅣ" to "き", "ㅋㅜ" to "く", "ㅋㅡ" to "く", "ㅋㅔ" to "け", "ㅋㅗ" to "こ",
        "ㄲㅏ" to "が", "ㄲㅣ" to "ぎ", "ㄲㅜ" to "ぐ", "ㄲㅡ" to "ぐ", "ㄲㅔ" to "げ", "ㄲㅗ" to "ご",

        // ㅅ/ㅆ (S / Z) - ㅡ 완벽 지원
        "ㅅㅏ" to "さ", "ㅅㅣ" to "し", "ㅅㅜ" to "す", "ㅅㅡ" to "す", "ㅅㅔ" to "せ", "ㅅㅗ" to "そ",
        "ㅆㅏ" to "ざ", "ㅆㅣ" to "じ", "ㅆㅜ" to "ず", "ㅆㅡ" to "ず", "ㅆㅔ" to "ぜ", "ㅆㅗ" to "ぞ",

        // ㄷ/ㅌ (T / D) - ㅡ 완벽 지원
        "ㄷㅏ" to "た", "ㄷㅣ" to "ち", "ㄷㅜ" to "つ", "ㄷㅡ" to "つ", "ㄷㅔ" to "て", "ㄷㅗ" to "と",
        "ㅌㅏ" to "た", "ㅌㅣ" to "ち", "ㅌㅜ" to "つ", "ㅌㅡ" to "つ", "ㅌㅔ" to "て", "ㅌㅗ" to "と",
        "ㄸㅏ" to "だ", "ㄸㅣ" to "ぢ", "ㄸㅜ" to "づ", "ㄸㅡ" to "づ", "ㄸㅔ" to "で", "ㄸㅗ" to "ど",

        // ㄴ (N)
        "ㄴㅏ" to "な", "ㄴㅣ" to "に", "ㄴㅜ" to "ぬ", "ㄴㅡ" to "ぬ", "ㄴㅔ" to "ね", "ㄴㅗ" to "の",

        // ㅎ/ㅂ/ㅍ (H / B / P)
        "ㅎㅏ" to "は", "ㅎㅣ" to "ひ", "ㅎㅜ" to "ふ", "ㅎㅡ" to "ふ", "ㅎㅔ" to "へ", "ㅎㅗ" to "ほ",
        "ㅂㅏ" to "ば", "ㅂㅣ" to "び", "ㅂㅜ" to "ぶ", "ㅂㅡ" to "ぶ", "ㅂㅔ" to "べ", "ㅂㅗ" to "ぼ",
        "ㅍㅏ" to "ぱ", "ㅍㅣ" to "ぴ", "ㅍㅜ" to "ぷ", "ㅍㅡ" to "ぷ", "ㅍㅔ" to "ぺ", "ㅍㅗ" to "ぽ",

        // ㅁ (M)
        "ㅁㅏ" to "ま", "ㅁㅣ" to "み", "ㅁㅜ" to "む", "ㅁㅡ" to "む", "ㅁㅔ" to "め", "ㅁㅗ" to "も",

        // ㄹ (R)
        "ㄹㅏ" to "ら", "ㄹㅣ" to "り", "ㄹㅜ" to "る", "ㄹㅡ" to "る", "ㄹㅔ" to "れ", "ㄹㅗ" to "ろ",

        // ㅈ/ㅊ (Z / J / CH / TS) - ㅡ 완벽 지원
        "ㅈㅏ" to "ざ", "ㅈㅣ" to "じ", "ㅈㅜ" to "ず", "ㅈㅡ" to "ず", "ㅈㅔ" to "ぜ", "ㅈㅗ" to "ぞ",
        "ㅊㅏ" to "ちゃ", "ㅊㅣ" to "ち", "ㅊㅜ" to "つ", "ㅊㅡ" to "つ", "ㅊㅔ" to "て", "ㅊㅗ" to "ちょ",

        // 요음 (Y-sounds)
        "ㄱㅑ" to "きゃ", "ㄱㅠ" to "きゅ", "ㄱㅛ" to "きょ",
        "ㄴㅑ" to "にゃ", "ㄴㅠ" to "にゅ", "ㄴㅛ" to "にょ",
        "ㅎㅑ" to "ひゃ", "ㅎㅠ" to "ひゅ", "ㅎㅛ" to "ひょ"
    )
}
