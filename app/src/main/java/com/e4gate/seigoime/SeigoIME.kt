package com.e4gate.seigoime

import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import org.json.JSONArray
import java.net.URL
import java.net.URLEncoder

class SeigoIME : InputMethodService() {
    private val converter = JapaneseConverter()
    private var keyboardView: View? = null
    
    private var isShifted = false
    private var isHangulMode = true
    private var isDanmoeum = false 
    private var isKatakanaMode = false 
    
    private var currentHiraganaBuffer = "" 
    private var candidatesList = mutableListOf<String>()
    private var currentCandidateIndex = -1
    private var lastCommittedLength = 0 
    private var lastVowelKey = "" 

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        keyboardView = view
        setButtonListeners(view as ViewGroup)
        return view
    }

    private fun toKatakana(str: String): String {
        return str.map {
            if (it in '\u3041'..'\u3096') (it + 0x60) else it
        }.joinToString("")
    }

    private fun updateCandidates(query: String) {
        if (query.isEmpty()) {
            keyboardView?.post {
                keyboardView?.findViewById<LinearLayout>(R.id.candidate_layout)?.removeAllViews()
                keyboardView?.findViewById<View>(R.id.candidate_scroll)?.visibility = View.GONE
            }
            return
        }

        Thread {
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val url = "https://www.google.com/transliterate?langpair=ja-Hira|ja&text=$encoded"
                val response = URL(url).readText()
                val jsonArray = JSONArray(response)
                val fetchedCandidates = jsonArray.getJSONArray(0).getJSONArray(1)

                keyboardView?.post {
                    val container = keyboardView?.findViewById<LinearLayout>(R.id.candidate_layout)
                    container?.removeAllViews()
                    candidatesList.clear()
                    
                    if (fetchedCandidates.length() > 0) {
                        for (i in 0 until fetchedCandidates.length()) {
                            val word = fetchedCandidates.getString(i)
                            candidatesList.add(word)
                            val btn = Button(this).apply {
                                text = word
                                setTextColor(Color.BLACK)
                                setBackgroundColor(Color.TRANSPARENT)
                                setPadding(30, 0, 30, 0)
                                textSize = 18f
                                isAllCaps = false
                                setOnClickListener { commitCandidateFromTouch(word) }
                            }
                            container?.addView(btn)
                        }
                        keyboardView?.findViewById<View>(R.id.candidate_scroll)?.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }.start()
    }

    private fun commitCandidateFromTouch(word: String) {
        val ic = currentInputConnection
        ic?.deleteSurroundingText(lastCommittedLength, 0)
        ic?.commitText(word, 1)
        clearCandidateBuffer()
    }

    private fun clearCandidateBuffer() {
        currentHiraganaBuffer = ""
        candidatesList.clear()
        currentCandidateIndex = -1
        lastCommittedLength = 0
        lastVowelKey = ""
        converter.clearBuffer()
        updateCandidates("")
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val mappedChar = getKoreanCharFromKeyCode(keyCode)
        when (keyCode) {
            KeyEvent.KEYCODE_DEL -> { handleBackspace(); return true }
            KeyEvent.KEYCODE_SPACE -> {
                if (event.isShiftPressed) {
                    (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
                } else {
                    handleSpaceAction() 
                }
                return true
            }
            KeyEvent.KEYCODE_ENTER -> { handleEnterAction(); return true }
        }
        if (mappedChar != null) { handleNormalInput(mappedChar); return true }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (getKoreanCharFromKeyCode(keyCode) != null || keyCode == KeyEvent.KEYCODE_DEL || keyCode == KeyEvent.KEYCODE_SPACE || keyCode == KeyEvent.KEYCODE_ENTER) return true
        return super.onKeyUp(keyCode, event)
    }

    private fun handleBackspace() {
        currentInputConnection?.deleteSurroundingText(1, 0)
        if (currentHiraganaBuffer.isNotEmpty()) {
            currentHiraganaBuffer = currentHiraganaBuffer.dropLast(1)
            lastCommittedLength = currentHiraganaBuffer.length
            currentCandidateIndex = -1
            updateCandidates(currentHiraganaBuffer)
        }
        lastVowelKey = ""
        converter.clearBuffer()
    }

    private fun handleSpaceAction() {
        if (candidatesList.isNotEmpty()) {
            currentCandidateIndex = (currentCandidateIndex + 1) % candidatesList.size
            val selectedWord = candidatesList[currentCandidateIndex]
            val ic = currentInputConnection
            ic?.deleteSurroundingText(lastCommittedLength, 0)
            ic?.commitText(selectedWord, 1)
            lastCommittedLength = selectedWord.length 
            highlightCandidateUI(currentCandidateIndex)
        } else {
            handleSpecialInput(" ")
        }
    }

    private fun handleEnterAction() {
        if (currentHiraganaBuffer.isNotEmpty()) clearCandidateBuffer()
        else handleSpecialInput("\n")
    }

    private fun highlightCandidateUI(index: Int) {
        keyboardView?.post {
            val container = keyboardView?.findViewById<LinearLayout>(R.id.candidate_layout) ?: return@post
            for (i in 0 until container.childCount) {
                val btn = container.getChildAt(i) as? Button
                btn?.setBackgroundColor(if (i == index) Color.parseColor("#BBDEFB") else Color.TRANSPARENT)
            }
            val scroll = keyboardView?.findViewById<HorizontalScrollView>(R.id.candidate_scroll)
            val selectedBtn = container.getChildAt(index)
            if (selectedBtn != null && scroll != null) {
                val scrollX = selectedBtn.left - (scroll.width / 2) + (selectedBtn.width / 2)
                scroll.smoothScrollTo(scrollX, 0)
            }
        }
    }

    private fun handleNormalInput(text: String) {
        if (isHangulMode && isShifted) { isShifted = false; updateShiftUI() }

        if (isDanmoeum && (text == "ㅏ" || text == "ㅗ" || text == "ㅜ")) {
            if (text == lastVowelKey && currentHiraganaBuffer.isNotEmpty()) {
                val lastChar = currentHiraganaBuffer.takeLast(1)
                
                val mapA = mapOf("あ" to "や", "か" to "きゃ", "が" to "ぎゃ", "さ" to "しゃ", "ざ" to "じゃ", "た" to "ちゃ", "だ" to "ぢゃ", "な" to "にゃ", "は" to "ひゃ", "ば" to "びゃ", "ぱ" to "ぴゃ", "ま" to "みゃ", "ら" to "りゃ")
                val mapO = mapOf("お" to "よ", "こ" to "きょ", "ご" to "ぎょ", "そ" to "しょ", "ぞ" to "じょ", "と" to "ちょ", "ど" to "ぢょ", "の" to "にょ", "ほ" to "ひょ", "ぼ" to "びょ", "ぽ" to "ぴょ", "も" to "みょ", "ろ" to "りょ")
                val mapU = mapOf("う" to "ゆ", "く" to "きゅ", "ぐ" to "ぎゅ", "す" to "しゅ", "ず" to "じゅ", "つ" to "ちゅ", "づ" to "ぢゅ", "ぬ" to "にゅ", "ふ" to "ひゅ", "ぶ" to "びゅ", "ぷ" to "ぴゅ", "む" to "みゅ", "る" to "りゅ")
                
                var replacement: String? = null
                if (text == "ㅏ") replacement = mapA[lastChar]
                else if (text == "ㅗ") replacement = mapO[lastChar]
                else if (text == "ㅜ") replacement = mapU[lastChar]

                if (replacement != null) {
                    currentInputConnection?.deleteSurroundingText(1, 0)
                    currentHiraganaBuffer = currentHiraganaBuffer.dropLast(1)
                    
                    val finalReplacement = if (isKatakanaMode) toKatakana(replacement) else replacement
                    currentInputConnection?.commitText(finalReplacement, 1)
                    
                    currentHiraganaBuffer += replacement
                    lastCommittedLength = currentHiraganaBuffer.length
                    currentCandidateIndex = -1
                    updateCandidates(currentHiraganaBuffer)
                    lastVowelKey = "" 
                    return
                }
            }
        }
        
        lastVowelKey = if (isDanmoeum && "ㅏㅓㅗㅜㅡㅣㅐㅔ".contains(text)) text else ""

        val (deleteCount, textToCommit) = converter.processInput(text)
        if (deleteCount > 0) {
            currentInputConnection?.deleteSurroundingText(deleteCount, 0)
            if (currentHiraganaBuffer.length >= deleteCount) {
                currentHiraganaBuffer = currentHiraganaBuffer.dropLast(deleteCount)
            }
        }
        
        val finalCommit = if (isKatakanaMode) toKatakana(textToCommit) else textToCommit
        currentInputConnection?.commitText(finalCommit, 1)
        
        currentHiraganaBuffer += textToCommit
        lastCommittedLength = currentHiraganaBuffer.length
        currentCandidateIndex = -1 
        updateCandidates(currentHiraganaBuffer)
    }

    private fun handleSpecialInput(text: String) {
        converter.flushPending()?.let {
            currentInputConnection?.deleteSurroundingText(it.first, 0)
            val finalPending = if (isKatakanaMode) toKatakana(it.second) else it.second
            currentInputConnection?.commitText(finalPending, 1)
        }
        currentInputConnection?.commitText(text, 1)
        clearCandidateBuffer()
    }

    private fun setButtonListeners(viewGroup: ViewGroup) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is ViewGroup) setButtonListeners(child)
            else if (child is Button) {
                if (child.id == R.id.btn_space) {
                    child.setOnLongClickListener {
                        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
                        true
                    }
                }
                child.setOnClickListener {
                    val text = child.text.toString()
                    when (text) {
                        "⌫" -> { handleBackspace() }
                        "セイゴ입력" -> { handleSpaceAction() }
                        "⏎" -> { handleEnterAction() }
                        "⇧", "⇪" -> { handleShift() }
                        "두벌", "단모" -> { toggleLayoutMode() }
                        "あ", "ア" -> { toggleKanaMode() } 
                        "!#1", "?123" -> { switchLayout(2) } 
                        "=\\<" -> { switchLayout(3) } 
                        "한글" -> { switchLayout(1) } 
                        "🌐" -> { (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker() }
                        else -> { handleNormalInput(text) }
                    }
                }
            }
        }
    }

    private fun toggleKanaMode() {
        isKatakanaMode = !isKatakanaMode
        val btn = keyboardView?.findViewById<Button>(R.id.btn_kana_toggle)
        btn?.text = if (isKatakanaMode) "ア" else "あ"
        
        if (currentHiraganaBuffer.isNotEmpty()) {
            val ic = currentInputConnection
            ic?.deleteSurroundingText(lastCommittedLength, 0)
            val convertedBuffer = if (isKatakanaMode) toKatakana(currentHiraganaBuffer) else currentHiraganaBuffer
            ic?.commitText(convertedBuffer, 1)
            lastCommittedLength = convertedBuffer.length
            
            currentCandidateIndex = -1
            highlightCandidateUI(-1)
        }
    }

    private fun toggleLayoutMode() {
        isDanmoeum = !isDanmoeum
        val btn = keyboardView?.findViewById<Button>(R.id.btn_layout_toggle)
        btn?.text = if (isDanmoeum) "단모" else "두벌"
        switchLayout(1) 
        clearCandidateBuffer()
    }

    private fun handleShift() { isShifted = !isShifted; updateShiftUI(); converter.clearBuffer() }
    private fun updateShiftUI() {
        val v = keyboardView ?: return
        val s = v.findViewById<Button>(R.id.btn_shift)
        val sd = v.findViewById<Button>(R.id.btn_shift_dan)
        val q = v.findViewById<Button>(R.id.btn_key_q); val w = v.findViewById<Button>(R.id.btn_key_w)
        val e = v.findViewById<Button>(R.id.btn_key_e); val r = v.findViewById<Button>(R.id.btn_key_r); val t = v.findViewById<Button>(R.id.btn_key_t)
        
        if (isShifted) { 
            s?.text = "⇪"; sd?.text = "⇪"
            q?.text = "ㅃ"; w?.text = "ㅉ"; e?.text = "ㄸ"; r?.text = "ㄲ"; t?.text = "ㅆ"
        } else { 
            s?.text = "⇧"; sd?.text = "⇧"
            q?.text = "ㅂ"; w?.text = "ㅈ"; e?.text = "ㄷ"; r?.text = "ㄱ"; t?.text = "ㅅ" 
        }
    }

    private fun switchLayout(layoutId: Int) {
        val v = keyboardView ?: return
        val hangulDubeol = v.findViewById<LinearLayout>(R.id.layout_hangul_dubeol)
        val hangulDanmo = v.findViewById<LinearLayout>(R.id.layout_hangul_danmoeum)
        val symbol1 = v.findViewById<LinearLayout>(R.id.layout_symbol_1)
        val symbol2 = v.findViewById<LinearLayout>(R.id.layout_symbol_2)
        
        hangulDubeol.visibility = if (layoutId == 1 && !isDanmoeum) View.VISIBLE else View.GONE
        hangulDanmo.visibility = if (layoutId == 1 && isDanmoeum) View.VISIBLE else View.GONE
        symbol1.visibility = if (layoutId == 2) View.VISIBLE else View.GONE
        symbol2.visibility = if (layoutId == 3) View.VISIBLE else View.GONE
        
        v.findViewById<Button>(R.id.btn_symbol).visibility = if (layoutId == 1) View.VISIBLE else View.GONE
        v.findViewById<Button>(R.id.btn_globe).visibility = if (layoutId == 1) View.VISIBLE else View.GONE
        v.findViewById<Button>(R.id.btn_layout_toggle).visibility = if (layoutId == 1) View.VISIBLE else View.GONE
        v.findViewById<Button>(R.id.btn_kana_toggle).visibility = if (layoutId == 1) View.VISIBLE else View.GONE
        v.findViewById<Button>(R.id.btn_abc).visibility = if (layoutId != 1) View.VISIBLE else View.GONE
        
        val abcButton = v.findViewById<Button>(R.id.btn_abc)
        if (layoutId == 2 || layoutId == 3) abcButton?.text = "한글"
        val symbolButton = v.findViewById<Button>(R.id.btn_symbol)
        if (layoutId == 1) symbolButton?.text = "!#1"
        
        isHangulMode = (layoutId == 1)
        converter.clearBuffer()
    }

    private fun getKoreanCharFromKeyCode(k: Int): String? {
        return when (k) {
            KeyEvent.KEYCODE_Q->"ㅂ"; KeyEvent.KEYCODE_W->"ㅈ"; KeyEvent.KEYCODE_E->"ㄷ"; KeyEvent.KEYCODE_R->"ㄱ"; KeyEvent.KEYCODE_T->"ㅅ"
            KeyEvent.KEYCODE_Y->"ㅛ"; KeyEvent.KEYCODE_U->"ㅕ"; KeyEvent.KEYCODE_I->"ㅑ"; KeyEvent.KEYCODE_O->"ㅐ"; KeyEvent.KEYCODE_P->"ㅔ"
            KeyEvent.KEYCODE_A->"ㅁ"; KeyEvent.KEYCODE_S->"ㄴ"; KeyEvent.KEYCODE_D->"ㅇ"; KeyEvent.KEYCODE_F->"ㄹ"; KeyEvent.KEYCODE_G->"ㅎ"
            KeyEvent.KEYCODE_H->"ㅗ"; KeyEvent.KEYCODE_J->"ㅓ"; KeyEvent.KEYCODE_K->"ㅏ"; KeyEvent.KEYCODE_L->"ㅣ"
            KeyEvent.KEYCODE_Z->"ㅋ"; KeyEvent.KEYCODE_X->"ㅌ"; KeyEvent.KEYCODE_C->"ㅊ"; KeyEvent.KEYCODE_V->"ㅍ"; KeyEvent.KEYCODE_B->"ㅠ"
            KeyEvent.KEYCODE_N->"ㅜ"; KeyEvent.KEYCODE_M->"ㅡ"; else -> null
        }
    }
}
