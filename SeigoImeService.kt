package com.e4gate.seigoime

import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import kotlinx.coroutines.*

class SeigoImeService : InputMethodService() {

    // ==========================================
    // 1. 상태 관리 (Composing 버퍼 및 비동기 통신 추가)
    // ==========================================
    var isKatakanaMode: Boolean = false 

    private var pendingConsonant: Char? = null  
    private var lastVowel: Char? = null         
    private var lastConsonantForVowel: Char? = null 
    private var lastPrintLength: Int = 0 

    // [핵심 추가] 밑줄 쳐진 조합 중인 글자를 모아두는 바구니
    private val composingBuffer = StringBuilder() 
    private var fetchJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var candidateContainer: LinearLayout

    // ==========================================
    // 2. 완벽 매핑 테이블 (기존과 동일)
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
    // 3. UI 및 API 통신 로직 (추천 한자 띄우기)
    // ==========================================
    
    // 키보드 상단에 추천 한자를 보여줄 띠(후보군 뷰) 생성
    override fun onCreateCandidatesView(): View {
        val scrollView = HorizontalScrollView(this)
        scrollView.isHorizontalScrollBarEnabled = false
        candidateContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#F5F5F5")) // 깔끔한 연회색 배경
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 130)
        }
        scrollView.addView(candidateContainer)
        return scrollView
    }

    // 구글 서버에 히라가나를 보내서 한자 후보군을 받아오는 함수
    private fun fetchKanjiCandidates(hiragana: String) {
        if (hiragana.isBlank()) {
            setCandidatesViewShown(false)
            return
        }

        fetchJob?.cancel()
        fetchJob = serviceScope.launch {
            val suggestions = KanjiAPIClient.getKanjiSuggestions(hiragana)
            updateCandidateUI(hiragana, suggestions)
        }
    }

    // 받아온 한자들을 버튼으로 만들어서 화면에 뿌려주는 함수
    private fun updateCandidateUI(original: String, suggestions: List<String>) {
        candidateContainer.removeAllViews()
        val allCandidates = mutableListOf(original) + suggestions

        for (word in allCandidates) {
            val btn = Button(this).apply {
                text = word
                textSize = 18f
                setBackgroundColor(Color.TRANSPARENT)
                setPadding(40, 0, 40, 0)
                
                // [핵심] 원하는 한자를 터치하면 앱에 '확정'되고 바구니가 비워짐
                setOnClickListener {
                    currentInputConnection?.commitText(word, 1)
                    resetComposingState()
                }
            }
            candidateContainer.addView(btn)
        }
        setCandidatesViewShown(true)
    }

    private fun resetComposingState() {
        composingBuffer.clear()
        setCandidatesViewShown(false)
        pendingConsonant = null
        lastVowel = null
        lastConsonantForVowel = null
        lastPrintLength = 0
    }

    // ==========================================
    // 4. 입력 처리 로직 (Direct Commit -> Composing 변환)
    // ==========================================
    fun onCharInput(inputChar: Char) {
        val ic = currentInputConnection ?: return

        // 허공에 커밋하지 않고 바구니(버퍼)에서 지우고 더하도록 헬퍼 함수 생성
        fun deleteFromBuffer(len: Int) {
            if (composingBuffer.length >= len) {
                composingBuffer.setLength(composingBuffer.length - len)
            }
        }

        fun appendToBuffer(text: String) {
            composingBuffer.append(text)
        }

        if (vowelMap.containsKey(inputChar)) {
            if (pendingConsonant == null && lastVowel != null) {
                val combinedKana = getCombinedVowel(lastVowel!!, inputChar, lastConsonantForVowel)
                if (combinedKana != null) {
                    deleteFromBuffer(lastPrintLength) 
                    val kana = applyKanaMode(combinedKana)
                    appendToBuffer(kana)
                    lastPrintLength = kana.length
                    lastVowel = inputChar 
                } else {
                    processNormalVowel(inputChar, ::deleteFromBuffer, ::appendToBuffer)
                }
            } else {
                processNormalVowel(inputChar, ::deleteFromBuffer, ::appendToBuffer)
            }
        } else if (consonantMap.containsKey(inputChar)) {
            if (pendingConsonant != null) {
                val c1 = pendingConsonant!!
                val c2 = inputChar
                
                if (c1 == 'ㄴ' || c1 == 'ㅁ' || c1 == 'ㅇ') {
                    deleteFromBuffer(lastPrintLength) 
                    appendToBuffer(applyKanaMode("ん"))
                    
                    val nextRomaji = consonantMap[c2] ?: ""
                    appendToBuffer(nextRomaji)
                    lastPrintLength = nextRomaji.length
                    pendingConsonant = c2

                } else if (c1 == 'ㅅ' || c1 == 'ㅆ' || isSameConsonantGroup(c1, c2)) {
                    deleteFromBuffer(lastPrintLength) 
                    appendToBuffer(applyKanaMode("っ"))
                    
                    val nextRomaji = consonantMap[c2] ?: ""
                    appendToBuffer(nextRomaji)
                    lastPrintLength = nextRomaji.length
                    pendingConsonant = c2
                } else {
                    val romaji = consonantMap[c2] ?: ""
                    appendToBuffer(romaji)
                    lastPrintLength = romaji.length
                    pendingConsonant = c2
                }
            } else {
                val romaji = consonantMap[inputChar] ?: ""
                appendToBuffer(romaji)
                lastPrintLength = romaji.length
                pendingConsonant = inputChar
            }
            lastVowel = null
        }

        // [최종 마무리의 핵심] 조합된 글자가 있으면 밑줄 친 상태로 띄우고 한자 검색!
        if (composingBuffer.isNotEmpty()) {
            ic.setComposingText(composingBuffer.toString(), 1)
            fetchKanjiCandidates(composingBuffer.toString())
        }
    }

    private fun processNormalVowel(inputChar: Char, deleteFromBuffer: (Int) -> Unit, appendToBuffer: (String) -> Unit) {
        if (pendingConsonant == null && lastVowel == 'ㅏ' && inputChar == 'ㅏ' && lastConsonantForVowel != null) {
            deleteFromBuffer(lastPrintLength)
            val romaji = consonantMap[lastConsonantForVowel] + "ya"
            val kana = getKana(romaji)
            appendToBuffer(kana)
            lastPrintLength = kana.length
            lastVowel = 'ㅑ'
            return
        }

        if (pendingConsonant != null) {
            deleteFromBuffer(lastPrintLength) 
            val specialCombo = checkSpecialRules(pendingConsonant!!, inputChar)
            val kana = if (specialCombo != null) {
                applyKanaMode(specialCombo)
            } else {
                getKana(consonantMap[pendingConsonant] + vowelMap[inputChar])
            }
            appendToBuffer(kana)
            lastPrintLength = kana.length
            lastConsonantForVowel = pendingConsonant
            pendingConsonant = null
            lastVowel = inputChar
        } else {
            val kana = getKana(vowelMap[inputChar] ?: "")
            appendToBuffer(kana)
            lastPrintLength = kana.length
            lastConsonantForVowel = null
            lastVowel = inputChar
        }
    }

    fun onBackspace() {
        val ic = currentInputConnection ?: return
        
        // 지울 때도 버퍼 상태를 먼저 확인
        if (composingBuffer.isNotEmpty()) {
            composingBuffer.setLength(composingBuffer.length - 1)
            ic.setComposingText(composingBuffer.toString(), 1)
            fetchKanjiCandidates(composingBuffer.toString())
            
            if (composingBuffer.isEmpty()) {
                setCandidatesViewShown(false)
            }
        } else {
            ic.deleteSurroundingText(1, 0)
        }
        
        pendingConsonant = null
        lastVowel = null
        lastConsonantForVowel = null
        lastPrintLength = 0
    }

    // 사용자가 스페이스바나 엔터를 눌렀을 때 강제로 확정 짓는 기능
    fun confirmComposing() {
        if (composingBuffer.isNotEmpty()) {
            currentInputConnection?.commitText(composingBuffer.toString(), 1)
            resetComposingState()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel() // 앱 종료 시 통신도 깔끔하게 종료
    }

    // ==========================================
    // 5. 헬퍼 함수 (기존과 동일)
    // ==========================================
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
        return null 
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