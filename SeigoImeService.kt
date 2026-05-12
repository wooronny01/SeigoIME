import android.inputmethodservice.InputMethodService
import android.view.inputmethod.InputConnection

class SeigoImeService : InputMethodService() {

    // ==========================================
    // 1. 상태 관리 (직전 출력 길이 추적 로직 추가)
    // ==========================================
    var isKatakanaMode: Boolean = false 

    private var pendingConsonant: Char? = null  
    private var lastVowel: Char? = null         
    private var lastConsonantForVowel: Char? = null 
    private var lastPrintLength: Int = 0 // [핵심] 화면에 직전에 찍힌 글자의 길이 (삭제용)

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
    // 3. UI 입력 처리 로직
    // ==========================================
    fun onCharInput(inputChar: Char) {
        val ic = currentInputConnection ?: return

        // [A] 모음 입력 처리
        if (vowelMap.containsKey(inputChar)) {
            
            // 1) 복모음 지능형 조합 (ㅇ+ㅗ+ㅏ -> わ, ㅇ+ㅗ+ㅣ -> おぃ 등)
            if (pendingConsonant == null && lastVowel != null) {
                val combinedKana = getCombinedVowel(lastVowel!!, inputChar, lastConsonantForVowel)
                if (combinedKana != null) {
                    ic.deleteSurroundingText(lastPrintLength, 0) // 직전 모음(예: お) 완벽 삭제
                    val kana = applyKanaMode(combinedKana)
                    ic.commitText(kana, 1)
                    lastPrintLength = kana.length
                    lastVowel = inputChar 
                    return
                }
            }

            // 2) 단모음 요음 변환 (ㄱ+ㅏ+ㅏ -> ぎゃ)
            if (pendingConsonant == null && lastVowel == 'ㅏ' && inputChar == 'ㅏ' && lastConsonantForVowel != null) {
                ic.deleteSurroundingText(lastPrintLength, 0)
                val romaji = consonantMap[lastConsonantForVowel] + "ya"
                val kana = getKana(romaji)
                ic.commitText(kana, 1)
                lastPrintLength = kana.length
                lastVowel = 'ㅑ'
                return
            }

            // 3) 일반 자음 + 모음 결합
            if (pendingConsonant != null) {
                ic.deleteSurroundingText(lastPrintLength, 0) // 허공에 떠있던 로마자 자음 삭제
                val specialCombo = checkSpecialRules(pendingConsonant!!, inputChar)
                val kana = if (specialCombo != null) {
                    applyKanaMode(specialCombo)
                } else {
                    getKana(consonantMap[pendingConsonant] + vowelMap[inputChar])
                }
                ic.commitText(kana, 1)
                lastPrintLength = kana.length

                lastConsonantForVowel = pendingConsonant
                pendingConsonant = null
                lastVowel = inputChar
            } else {
                // 모음 단독 입력 (ㅇ 뒤에 오거나 처음 칠 때)
                val kana = getKana(vowelMap[inputChar] ?: "")
                ic.commitText(kana, 1)
                lastPrintLength = kana.length

                lastConsonantForVowel = null
                lastVowel = inputChar
            }

        // [B] 자음 입력 처리 (받침 및 촉음 규칙)
        } else if (consonantMap.containsKey(inputChar)) {
            if (pendingConsonant != null) {
                val c1 = pendingConsonant!!
                val c2 = inputChar
                
                // ㄴ,ㅁ,ㅇ 받침 -> ん 변환
                if (c1 == 'ㄴ' || c1 == 'ㅁ' || c1 == 'ㅇ') {
                    ic.deleteSurroundingText(lastPrintLength, 0) 
                    ic.commitText(applyKanaMode("ん"), 1)
                    
                    val nextRomaji = consonantMap[c2] ?: ""
                    ic.commitText(nextRomaji, 1)
                    lastPrintLength = nextRomaji.length
                    pendingConsonant = c2

                // ㅅ,ㅆ 받침 및 동일 발음군 연타 -> っ 변환 (っㅅて 찌꺼기 완벽 해결)
                } else if (c1 == 'ㅅ' || c1 == 'ㅆ' || isSameConsonantGroup(c1, c2)) {
                    ic.deleteSurroundingText(lastPrintLength, 0) 
                    ic.commitText(applyKanaMode("っ"), 1)
                    
                    val nextRomaji = consonantMap[c2] ?: ""
                    ic.commitText(nextRomaji, 1)
                    lastPrintLength = nextRomaji.length
                    pendingConsonant = c2
                } else {
                    // 일반 자음 연속 (그대로 다음 글자 초성으로)
                    val romaji = consonantMap[c2] ?: ""
                    ic.commitText(romaji, 1)
                    lastPrintLength = romaji.length
                    pendingConsonant = c2
                }
            } else {
                // 새로운 초성 시작
                val romaji = consonantMap[inputChar] ?: ""
                ic.commitText(romaji, 1)
                lastPrintLength = romaji.length
                pendingConsonant = inputChar
            }
            lastVowel = null
        }
    }

    fun onBackspace() {
        val ic = currentInputConnection ?: return
        pendingConsonant = null
        lastVowel = null
        lastConsonantForVowel = null
        ic.deleteSurroundingText(1, 0)
    }

    // ==========================================
    // 4. 헬퍼 함수
    // ==========================================
    
    // 대표님께서 요청하신 복모음 전용 매핑 함수
    private fun getCombinedVowel(v1: Char, v2: Char, consonant: Char?): String? {
        val combo = "$v1$v2"
        val isNoConsonant = (consonant == null || consonant == 'ㅇ')
        
        if (isNoConsonant) {
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
        return null // 자음이 섞인 복모음은 추후 필요시 확장
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
}