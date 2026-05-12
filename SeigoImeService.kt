import android.inputmethodservice.InputMethodService
import android.view.inputmethod.InputConnection

class SeigoImeService : InputMethodService() {

    var isKatakanaMode: Boolean = false 

    private var currentConsonant: Char? = null
    private var currentVowel: Char? = null
    private var lastConsonant: Char? = null 
    private val composingBuffer = StringBuilder()

    private val consonantMap = mapOf(
        'ㄱ' to "g", 'ㅋ' to "k", 'ㄲ' to "k",
        'ㄷ' to "d", 'ㅌ' to "t", 'ㄸ' to "t",
        'ㅂ' to "b", 'ㅍ' to "p", 'ㅃ' to "p",
        'ㅅ' to "s", 'ㅆ' to "s",
        'ㅈ' to "z", 'ㅊ' to "c", 'ㅉ' to "z",
        'ㅇ' to "",  
        'ㅎ' to "h", 'ㅁ' to "m", 'ㄴ' to "n", 'ㄹ' to "r"
    )

    private val vowelMap = mapOf(
        'ㅏ' to "a", 'ㅣ' to "i", 'ㅜ' to "u", 'ㅔ' to "e", 'ㅗ' to "o",
        'ㅑ' to "ya", 'ㅠ' to "yu", 'ㅛ' to "yo",
        'ㅓ' to "o", 'ㅕ' to "yo", 'ㅡ' to "u", 'ㅐ' to "e"
    )

    private val romajiToHiragana = mapOf(
        "a" to "あ", "i" to "い", "u" to "う", "e" to "え", "o" to "お",
        "ka" to "か", "ki" to "き", "ku" to "く", "ke" to "け", "ko" to "こ",
        "kya" to "きゃ", "kyu" to "きゅ", "kyo" to "きょ",
        "ga" to "が", "gi" to "ぎ", "gu" to "ぐ", "ge" to "げ", "go" to "ご",
        "gya" to "ぎゃ", "gyu" to "ぎゅ", "gyo" to "ぎょ",
        "sa" to "さ", "shi" to "し", "si" to "し", "su" to "す", "se" to "せ", "so" to "そ",
        "za" to "ざ", "ji" to "じ", "zi" to "じ", "zu" to "ず", "ze" to "ぜ", "zo" to "ぞ",
        "ta" to "た", "chi" to "ち", "ci" to "ち", "tsu" to "つ", "te" to "て", "to" to "と",
        "da" to "だ", "di" to "ぢ", "du" to "づ", "de" to "で", "do" to "ど",
        "na" to "な", "ni" to "に", "nu" to "ぬ", "ne" to "ね", "no" to "の",
        "ha" to "は", "hi" to "ひ", "fu" to "ふ", "hu" to "ふ", "he" to "へ", "ho" to "ほ",
        "ba" to "ば", "bi" to "び", "bu" to "ぶ", "be" to "べ", "bo" to "ぼ",
        "pa" to "ぱ", "pi" to "ぴ", "pu" to "ぷ", "pe" to "ぺ", "po" to "ぽ",
        "ma" to "ま", "mi" to "み", "mu" to "む", "me" to "め", "mo" to "も",
        "ya" to "や", "yu" to "ゆ", "yo" to "よ",
        "ra" to "ら", "ri" to "り", "ru" to "る", "re" to "れ", "ro" to "ろ",
        "wa" to "わ", "wo" to "を", "we" to "うぇ", "wi" to "うぃ", "ui" to "うぃ", "ye" to "いぇ",
        "nn" to "ん", "gwa" to "ぐぁ", "kwa" to "くぁ"
    )

    fun onCharInput(charInput: Char) {
        val ic: InputConnection = currentInputConnection ?: return
        val resultString = processInternalInput(charInput)
        if (resultString.isNotEmpty()) {
            ic.commitText(resultString, 1)
            clearBuffer()
        }
    }

    fun onBackspace() {
        val ic: InputConnection = currentInputConnection ?: return
        clearBuffer()
        ic.deleteSurroundingText(1, 0)
    }

    // [핵심 1] 복모음 지능형 조합 로직 (ㅗ+ㅏ -> wa)
    private fun combineVowels(v1: Char, v2: Char): String? {
        return when ("$v1$v2") {
            "ㅗㅏ" -> "wa"
            "ㅜㅓ" -> "wo" 
            "ㅗㅣ" -> "we"
            "ㅜㅣ" -> "wi"
            "ㅡㅣ" -> "ui"
            "ㅑㅣ" -> "ye"
            "ㅕㅣ" -> "ye"
            "ㅏㅣ" -> "e"
            "ㅓㅣ" -> "e"
            else -> null
        }
    }

    private fun processInternalInput(inputChar: Char): String {
        if (vowelMap.containsKey(inputChar)) {
            // 1) 복모음(와, 워 등) 연속 타건 처리
            if (currentConsonant == null && currentVowel != null) {
                val combinedRomaji = combineVowels(currentVowel!!, inputChar)
                if (combinedRomaji != null) {
                    if (composingBuffer.isNotEmpty()) composingBuffer.deleteCharAt(composingBuffer.length - 1)
                    val romaji = (if (lastConsonant != null) consonantMap[lastConsonant] else "") + combinedRomaji
                    composingBuffer.append(getKana(romaji))
                    currentVowel = inputChar 
                    return composingBuffer.toString()
                }
            }
            
            // 2) 갸(ㄱ+ㅏ+ㅏ) 단모음 요음 변환 규칙
            if (currentConsonant == null && currentVowel == 'ㅏ' && inputChar == 'ㅏ' && lastConsonant != null) {
                if (composingBuffer.isNotEmpty()) composingBuffer.deleteCharAt(composingBuffer.length - 1)
                val romaji = consonantMap[lastConsonant] + "ya"
                composingBuffer.append(getKana(romaji))
                currentVowel = 'ㅑ'
            } 
            else if (currentConsonant != null) {
                val specialCombo = checkSpecialRules(currentConsonant!!, inputChar)
                if (specialCombo != null) {
                    composingBuffer.append(applyKanaMode(specialCombo))
                } else {
                    val romaji = consonantMap[currentConsonant] + vowelMap[inputChar]
                    composingBuffer.append(getKana(romaji))
                }
                lastConsonant = currentConsonant
                currentVowel = inputChar
                currentConsonant = null
            } else {
                composingBuffer.append(getKana(vowelMap[inputChar] ?: ""))
                lastConsonant = null
                currentVowel = inputChar
            }
        } else if (consonantMap.containsKey(inputChar)) {
            if (currentConsonant != null) {
                val c1 = currentConsonant!!
                val c2 = inputChar
                
                // [핵심 2] 한국인 맞춤 받침 발음(ん) 규칙
                if (c1 == 'ㄴ' || c1 == 'ㅁ' || c1 == 'ㅇ') {
                    composingBuffer.append(applyKanaMode("ん"))
                    currentConsonant = c2
                } 
                // [핵심 3] 한국인 맞춤 촉음(っ) 규칙 (ㅅ 받침을 보편적인 촉음으로 사용)
                else if (c1 == 'ㅅ' || c1 == 'ㅆ' || isSameConsonantGroup(c1, c2)) {
                    composingBuffer.append(applyKanaMode("っ"))
                    currentConsonant = c2
                } else {
                    currentConsonant = c2
                }
            } else {
                currentConsonant = inputChar
            }
            currentVowel = null
        }
        return composingBuffer.toString()
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

    private fun clearBuffer() {
        currentConsonant = null
        currentVowel = null
        lastConsonant = null
        composingBuffer.clear()
    }
}
