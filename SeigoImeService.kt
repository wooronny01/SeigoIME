import android.inputmethodservice.InputMethodService
import android.view.inputmethod.InputConnection

class SeigoImeService : InputMethodService() {

    // ==========================================
    // 1. 상태 관리 (내부 버퍼 제거, 직전 상태만 추적)
    // ==========================================
    var isKatakanaMode: Boolean = false 

    private var pendingConsonant: Char? = null  // 초성 대기용
    private var lastInputChar: Char? = null     // 직전에 입력된 키 (복모음 판단용)
    private var lastConsonantForYa: Char? = null // 단모음 요음(갸) 판단용

    // ==========================================
    // 2. 완벽 매핑 테이블
    // ==========================================
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
    // 3. 다이렉트 커밋 UI 연결부
    // ==========================================
    fun onCharInput(inputChar: Char) {
        val ic = currentInputConnection ?: return

        if (vowelMap.containsKey(inputChar)) {
            // [A] 복모음 지능형 치환 (화면에 찍힌 글자를 지우고 합쳐서 다시 씀)
            if (inputChar == 'ㅏ' && lastInputChar == 'ㅗ') {
                ic.deleteSurroundingText(1, 0)
                ic.commitText(applyKanaMode("わ"), 1)
                lastInputChar = 'ㅏ'
                pendingConsonant = null
                return
            }
            if (inputChar == 'ㅓ' && lastInputChar == 'ㅜ') {
                ic.deleteSurroundingText(1, 0)
                ic.commitText(applyKanaMode("を"), 1)
                lastInputChar = 'ㅓ'
                pendingConsonant = null
                return
            }
            
            // [B] 단모음 요음 변환 (ㄱ+ㅏ+ㅏ -> ぎゃ)
            if (inputChar == 'ㅏ' && lastInputChar == 'ㅏ' && lastConsonantForYa != null) {
                ic.deleteSurroundingText(1, 0)
                val romaji = consonantMap[lastConsonantForYa] + "ya"
                ic.commitText(getKana(romaji), 1)
                lastInputChar = 'ㅑ'
                pendingConsonant = null
                return
            }

            // [C] 일반 초성 + 모음 출력
            if (pendingConsonant != null) {
                val specialCombo = checkSpecialRules(pendingConsonant!!, inputChar)
                if (specialCombo != null) {
                    ic.commitText(applyKanaMode(specialCombo), 1)
                } else {
                    val romaji = consonantMap[pendingConsonant] + vowelMap[inputChar]
                    ic.commitText(getKana(romaji), 1)
                }
                lastConsonantForYa = pendingConsonant
                pendingConsonant = null
            } else {
                ic.commitText(getKana(vowelMap[inputChar] ?: ""), 1)
                lastConsonantForYa = null
            }
            lastInputChar = inputChar

        } else if (consonantMap.containsKey(inputChar)) {
            // [D] 자음 처리 (발음 및 촉음 규칙)
            if (pendingConsonant != null) {
                val c1 = pendingConsonant!!
                val c2 = inputChar
                
                if (c1 == 'ㄴ' || c1 == 'ㅁ' || c1 == 'ㅇ') {
                    ic.commitText(applyKanaMode("ん"), 1)
                } else if (c1 == 'ㅅ' || c1 == 'ㅆ' || isSameConsonantGroup(c1, c2)) {
                    ic.commitText(applyKanaMode("っ"), 1)
                }
                pendingConsonant = c2
            } else {
                pendingConsonant = inputChar
            }
            lastInputChar = inputChar
        }
    }

    fun onBackspace() {
        val ic = currentInputConnection ?: return
        pendingConsonant = null
        lastInputChar = null
        lastConsonantForYa = null
        ic.deleteSurroundingText(1, 0)
    }

    // ==========================================
    // 4. 헬퍼 함수
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
}