import android.inputmethodservice.InputMethodService
import android.view.inputmethod.InputConnection

class SeigoImeService : InputMethodService() {

    // ==========================================
    // 1. 상태 및 모드 관리
    // ==========================================
    var isKatakanaMode: Boolean = false 

    private var currentConsonant: Char? = null
    private var currentVowel: Char? = null
    private var lastConsonant: Char? = null 
    private val composingBuffer = StringBuilder()

    // ==========================================
    // 2. 한국인 맞춤형 지능형 매핑 테이블 (평/격음 분리)
    // ==========================================
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
        'ㅓ' to "o", 'ㅕ' to "yo", 'ㅡ' to "u"
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
        "wa" to "わ", "wo" to "を", "nn" to "ん"
    )

    // ==========================================
    // 3. UI 연결부
    // ==========================================
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

    // ==========================================
    // 4. 지능형 변환 엔진 (내부 로직)
    // ==========================================
    private fun processInternalInput(inputChar: Char): String {
        if (vowelMap.containsKey(inputChar)) {
            if (currentConsonant == null && currentVowel == 'ㅏ' && inputChar == 'ㅏ' && lastConsonant != null) {
                if (composingBuffer.isNotEmpty()) composingBuffer.deleteCharAt(composingBuffer.length - 1)
                val romaji = consonantMap[lastConsonant] + "ya"
                composingBuffer.append(getKana(romaji))
                currentVowel = 'ㅑ'
            } else if (currentConsonant != null) {
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
                if ((currentConsonant == 'ㄴ' || currentConsonant == 'ㅇ') && inputChar == 'ㄴ') {
                    composingBuffer.append(applyKanaMode("ん"))
                    currentConsonant = null
                } else if (currentConsonant == 'ㅁ' && (inputChar == 'ㅂ' || inputChar == 'ㅍ')) {
                    composingBuffer.append(applyKanaMode("ん"))
                    currentConsonant = inputChar
                } else if (isSameConsonantGroup(currentConsonant!!, inputChar)) {
                    composingBuffer.append(applyKanaMode("っ"))
                    currentConsonant = inputChar
                } else {
                    currentConsonant = inputChar
                }
            } else {
                currentConsonant = inputChar
            }
            currentVowel = null
        }
        return composingBuffer.toString()
    }

    // ==========================================
    // 5. 헬퍼 함수
    // ==========================================
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
