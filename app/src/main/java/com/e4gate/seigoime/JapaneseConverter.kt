package com.e4gate.seigoime

class JapaneseConverter {

    private var pendingConsonant: Char? = null
    private var lastVowel: Char? = null
    private var isKatakanaMode: Boolean = false // 추후 가타카나 토글 연결용

    // 1. 완벽 분리된 자음 매핑 (평음: 탁음, 격음: 청음)
    private val consonantMap = mapOf(
        'ㄱ' to "g", 'ㅋ' to "k", 'ㄲ' to "k",
        'ㄷ' to "d", 'ㅌ' to "t", 'ㄸ' to "t",
        'ㅂ' to "b", 'ㅍ' to "p", 'ㅃ' to "p",
        'ㅅ' to "s", 'ㅆ' to "s",
        'ㅈ' to "z", 'ㅊ' to "c", 'ㅉ' to "z",
        'ㅇ' to "", 'ㅎ' to "h", 'ㅁ' to "m", 'ㄴ' to "n", 'ㄹ' to "r"
    )

    private val vowelMap = mapOf(
        'ㅏ' to "a", 'ㅣ' to "i", 'ㅜ' to "u", 'ㅔ' to "e", 'ㅗ' to "o",
        'ㅑ' to "ya", 'ㅠ' to "yu", 'ㅛ' to "yo",
        'ㅓ' to "o", 'ㅕ' to "yo", 'ㅡ' to "u", 'ㅐ' to "e"
    )

    // 확장 요음까지 꽉 채운 매핑 사전
    private val romajiToHiragana = mapOf(
        "a" to "あ", "i" to "い", "u" to "う", "e" to "え", "o" to "お",
        "ka" to "か", "ki" to "き", "ku" to "く", "ke" to "け", "ko" to "こ",
        "sa" to "さ", "shi" to "し", "si" to "し", "su" to "す", "se" to "せ", "so" to "そ",
        "ta" to "た", "chi" to "ち", "ci" to "ち", "tsu" to "つ", "te" to "て", "to" to "と",
        "na" to "な", "ni" to "に", "nu" to "ぬ", "ne" to "ね", "no" to "の",
        "ha" to "は", "hi" to "ひ", "fu" to "ふ", "hu" to "ふ", "he" to "へ", "ho" to "ほ",
        "ma" to "ま", "mi" to "み", "mu" to "む", "me" to "め", "mo" to "も",
        "ya" to "や", "yu" to "ゆ", "yo" to "よ",
        "ra" to "ら", "ri" to "り", "ru" to "る", "re" to "れ", "ro" to "ろ",
        "wa" to "わ", "wo" to "を", "nn" to "ん",
        "ga" to "が", "gi" to "ぎ", "gu" to "ぐ", "ge" to "げ", "go" to "ご",
        "za" to "ざ", "ji" to "じ", "zi" to "じ", "zu" to "ず", "ze" to "ぜ", "zo" to "ぞ",
        "da" to "だ", "di" to "ぢ", "du" to "づ", "de" to "で", "do" to "ど",
        "ba" to "ば", "bi" to "び", "bu" to "ぶ", "be" to "べ", "bo" to "ぼ",
        "pa" to "ぱ", "pi" to "ぴ", "pu" to "ぷ", "pe" to "ぺ", "po" to "ぽ",
        "kya" to "きゃ", "kyu" to "きゅ", "kyo" to "きょ",
        "gya" to "ぎゃ", "gyu" to "ぎゅ", "gyo" to "ぎょ",
        "sya" to "しゃ", "sha" to "しゃ", "syu" to "しゅ", "shu" to "しゅ", "syo" to "しょ", "sho" to "しょ",
        "zya" to "じゃ", "ja" to "じゃ", "jya" to "じゃ", "zyu" to "じゅ", "ju" to "じゅ", "jyu" to "じゅ", "zyo" to "じょ", "jo" to "じょ", "jyo" to "じょ",
        "tya" to "ちゃ", "cha" to "ちゃ", "cya" to "ちゃ", "tyu" to "ちゅ", "chu" to "ちゅ", "cyu" to "ちゅ", "tyo" to "ちょ", "cho" to "ちょ", "cyo" to "ちょ",
        "nya" to "にゃ", "nyu" to "にゅ", "nyo" to "にょ",
        "hya" to "ひゃ", "hyu" to "ひゅ", "hyo" to "ひょ",
        "bya" to "びゃ", "byu" to "びゅ", "byo" to "びょ",
        "pya" to "ぴゃ", "pyu" to "ぴゅ", "pyo" to "ぴょ",
        "mya" to "みゃ", "myu" to "みゅ", "myo" to "みょ",
        "rya" to "りゃ", "ryu" to "りゅ", "ryo" to "りょ"
    )

    /**
     * @return Pair<지울_글자_수, 출력할_글자>
     */
    fun processInput(inputStr: String): Pair<Int, String> {
        val inputChar = inputStr.firstOrNull() ?: return Pair(0, "")
        var deleteCount = 0
        var textToCommit = ""

        if (vowelMap.containsKey(inputChar)) {
            // [복모음 처리] ㅗ+ㅏ -> わ, ㅜ+ㅓ -> を 등
            if (pendingConsonant == null && lastVowel != null) {
                val combined = getCombinedVowel(lastVowel!!, inputChar)
                if (combined != null) {
                    lastVowel = inputChar
                    return Pair(1, applyKanaMode(combined)) // 직전 모음(1글자) 지우고 복모음 리턴
                }
            }

            if (pendingConsonant != null) {
                deleteCount = 1 // 화면에 허공에 찍힌 로마자(예: 'g') 1개 지움
                val specialCombo = checkSpecialRules(pendingConsonant!!, inputChar)
                
                textToCommit = if (specialCombo != null) {
                    applyKanaMode(specialCombo)
                } else {
                    val romaji = consonantMap[pendingConsonant] + vowelMap[inputChar]
                    getKana(romaji)
                }
                pendingConsonant = null
                lastVowel = inputChar
            } else {
                // 자음 없이 모음만 들어온 경우 (ㅇ 받침 후 또는 처음)
                textToCommit = getKana(vowelMap[inputChar] ?: "")
                lastVowel = inputChar
            }
        } else if (consonantMap.containsKey(inputChar)) {
            if (pendingConsonant != null) {
                val c1 = pendingConsonant!!
                val c2 = inputChar
                
                if (c1 == 'ㄴ' || c1 == 'ㅁ' || c1 == 'ㅇ') {
                    deleteCount = 1 // 앞의 로마자 1개 지움
                    textToCommit = applyKanaMode("ん") + (consonantMap[c2] ?: "")
                    pendingConsonant = c2
                } else if (c1 == 'ㅅ' || c1 == 'ㅆ' || isSameConsonantGroup(c1, c2)) {
                    deleteCount = 1 // 앞의 로마자 1개 지움
                    textToCommit = applyKanaMode("っ") + (consonantMap[c2] ?: "")
                    pendingConsonant = c2
                } else {
                    deleteCount = 0
                    textToCommit = consonantMap[c2] ?: ""
                    pendingConsonant = c2
                }
            } else {
                deleteCount = 0
                textToCommit = consonantMap[inputChar] ?: ""
                pendingConsonant = inputChar
            }
            lastVowel = null
        }

        return Pair(deleteCount, textToCommit)
    }

    private fun getCombinedVowel(v1: Char, v2: Char): String? {
        val combo = "$v1$v2"
        return when (combo) {
            "ㅗㅏ" -> "わ"
            "ㅜㅓ" -> "を"
            "ㅗㅣ" -> "おぃ"
            "ㅗㅐ" -> "おぇ"
            "ㅜㅣ" -> "うぃ"
            "ㅜㅔ" -> "うぇ"
            else -> null
        }
    }

    private fun checkSpecialRules(consonant: Char, vowel: Char): String? {
        if (vowel == 'ㅡ') {
            return when (consonant) {
                'ㅅ', 'ㅆ' -> "す"
                'ㅈ', 'ㅉ' -> "ず"
                'ㅊ', 'ㅌ' -> "つ"
                else -> null
            }
        }
        return null
    }

    private fun isSameConsonantGroup(c1: Char, c2: Char): Boolean {
        val g1 = getConsonantGroup(c1)
        val g2 = getConsonantGroup(c2)
        return g1 == g2 && g1 != -1
    }

    private fun getConsonantGroup(c: Char): Int {
        return when (c) {
            'ㄱ', 'ㅋ', 'ㄲ' -> 1
            'ㄷ', 'ㅌ', 'ㄸ' -> 2
            'ㅂ', 'ㅍ', 'ㅃ' -> 3
            'ㅅ', 'ㅆ' -> 4
            'ㅈ', 'ㅉ', 'ㅊ' -> 5
            else -> -1
        }
    }

    private fun getKana(romaji: String): String {
        val hiragana = romajiToHiragana[romaji] ?: romaji
        return applyKanaMode(hiragana)
    }

    private fun applyKanaMode(text: String): String {
        if (!isKatakanaMode) return text
        val katakana = java.lang.StringBuilder()
        for (c in text) {
            if (c in '\u3041'..'\u3096') katakana.append(c + 0x60)
            else katakana.append(c)
        }
        return katakana.toString()
    }

    fun clearBuffer() {
        pendingConsonant = null
        lastVowel = null
    }

    fun flushPending(): Pair<Int, String>? {
        if (pendingConsonant != null) {
            val romaji = consonantMap[pendingConsonant] ?: ""
            pendingConsonant = null
            return Pair(1, romaji) // 버퍼에 남은 자음을 지우고 출력 확정
        }
        return null
    }
}