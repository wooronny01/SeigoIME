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
    
    // 🌟 변환 상태 관리를 위한 변수들
    private var currentHiraganaBuffer = "" 
    private var candidatesList = mutableListOf<String>()
    private var currentCandidateIndex = -1
    private var lastCommittedLength = 0 // 에디터에서 지워야 할 글자 수 추적

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        keyboardView = view
        setButtonListeners(view as ViewGroup)
        return view
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

    // 🌟 화면에서 직접 터치하여 확정할 때
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
        converter.clearBuffer()
        updateCandidates("")
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val mappedChar = getKoreanCharFromKeyCode(keyCode)
        when (keyCode) {
            KeyEvent.KEYCODE_DEL -> {
                handleBackspace()
                return true
            }
            KeyEvent.KEYCODE_SPACE -> {
                if (event.isShiftPressed) {
                    (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
                } else {
                    handleSpaceAction() 
                }
                return true
            }
            KeyEvent.KEYCODE_ENTER -> {
                handleEnterAction()
                return true
            }
        }
        if (mappedChar != null) {
            handleNormalInput(mappedChar)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (getKoreanCharFromKeyCode(keyCode) != null || 
            keyCode == KeyEvent.KEYCODE_DEL || 
            keyCode == KeyEvent.KEYCODE_SPACE || 
            keyCode == KeyEvent.KEYCODE_ENTER) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun handleBackspace() {
        currentInputConnection?.deleteSurroundingText(1, 0)
        if (currentHiraganaBuffer.isNotEmpty()) {
            currentHiraganaBuffer = currentHiraganaBuffer.dropLast(1)
            lastCommittedLength = currentHiraganaBuffer.length
            currentCandidateIndex = -1 // 추천 사이클 초기화
            updateCandidates(currentHiraganaBuffer)
        }
        converter.clearBuffer()
    }

    // 🌟 스페이스바: 추천어 목록 순환 이동
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

    // 🌟 엔터키: 현재 선택된 추천어 확정 또는 줄바꿈
    private fun handleEnterAction() {
        if (currentHiraganaBuffer.isNotEmpty()) {
            clearCandidateBuffer() // 글자를 그대로 확정하고 버퍼 비움
        } else {
            handleSpecialInput("\n")
        }
    }

    // 추천바 UI 하이라이트 (파란색 배경)
    private fun highlightCandidateUI(index: Int) {
        keyboardView?.post {
            val container = keyboardView?.findViewById<LinearLayout>(R.id.candidate_layout) ?: return@post
            for (i in 0 until container.childCount) {
                val btn = container.getChildAt(i) as? Button
                if (i == index) {
                    btn?.setBackgroundColor(Color.parseColor("#BBDEFB")) // 연한 파란색 하이라이트
                } else {
                    btn?.setBackgroundColor(Color.TRANSPARENT)
                }
            }
            // 스크롤도 선택된 항목으로 이동
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
        
        val (deleteCount, textToCommit) = converter.processInput(text)
        if (deleteCount > 0) {
            currentInputConnection?.deleteSurroundingText(deleteCount, 0)
            if (currentHiraganaBuffer.length >= deleteCount) {
                currentHiraganaBuffer = currentHiraganaBuffer.dropLast(deleteCount)
            }
        }
        currentInputConnection?.commitText(textToCommit, 1)
        
        currentHiraganaBuffer += textToCommit
        lastCommittedLength = currentHiraganaBuffer.length
        currentCandidateIndex = -1 // 타이핑하면 선택 초기화
        updateCandidates(currentHiraganaBuffer)
    }

    private fun handleSpecialInput(text: String) {
        converter.flushPending()?.let {
            currentInputConnection?.deleteSurroundingText(it.first, 0)
            currentInputConnection?.commitText(it.second, 1)
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
                    // 스페이스바 길게 누르기 (입력기 선택)
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

    private fun handleShift() { isShifted = !isShifted; updateShiftUI(); converter.clearBuffer() }
    
    private fun updateShiftUI() {
        val v = keyboardView ?: return
        val s = v.findViewById<Button>(R.id.btn_shift)
        val q = v.findViewById<Button>(R.id.btn_key_q); val w = v.findViewById<Button>(R.id.btn_key_w)
        val e = v.findViewById<Button>(R.id.btn_key_e); val r = v.findViewById<Button>(R.id.btn_key_r); val t = v.findViewById<Button>(R.id.btn_key_t)
        if (isShifted) { s.text = "⇪"; q.text = "ㅃ"; w.text = "ㅉ"; e.text = "ㄸ"; r.text = "ㄲ"; t.text = "ㅆ" } 
        else { s.text = "⇧"; q.text = "ㅂ"; w.text = "ㅈ"; e.text = "ㄷ"; r.text = "ㄱ"; t.text = "ㅅ" }
    }

    private fun switchLayout(layoutId: Int) {
        val v = keyboardView ?: return
        v.findViewById<LinearLayout>(R.id.layout_hangul).visibility = if (layoutId == 1) View.VISIBLE else View.GONE
        v.findViewById<LinearLayout>(R.id.layout_symbol_1).visibility = if (layoutId == 2) View.VISIBLE else View.GONE
        v.findViewById<LinearLayout>(R.id.layout_symbol_2).visibility = if (layoutId == 3) View.VISIBLE else View.GONE
        v.findViewById<Button>(R.id.btn_symbol).visibility = if (layoutId == 1) View.VISIBLE else View.GONE
        v.findViewById<Button>(R.id.btn_globe).visibility = if (layoutId == 1) View.VISIBLE else View.GONE
        v.findViewById<Button>(R.id.btn_abc).visibility = if (layoutId != 1) View.VISIBLE else View.GONE
        val abcButton = v.findViewById<Button>(R.id.btn_abc)
        if (layoutId == 2 || layoutId == 3) abcButton?.text = "한글"
        val symbolButton = v.findViewById<Button>(R.id.btn_symbol)
        if (layoutId == 1) symbolButton?.text = "!#1"
        isHangulMode = (layoutId == 1)
        clearCandidateBuffer() // 레이아웃 전환 시 버퍼 초기화
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
