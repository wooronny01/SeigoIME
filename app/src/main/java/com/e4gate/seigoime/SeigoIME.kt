package com.e4gate.seigoime

import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONArray
import java.net.URL
import java.net.URLEncoder

class SeigoIME : InputMethodService() {
    private val converter = JapaneseConverter()
    private var keyboardView: View? = null
    
    // 상태 관리
    private var isShifted = false
    private var isHangulMode = true
    private var currentHiraganaBuffer = "" // 추천 API에 보낼 히라가나 누적분

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        keyboardView = view
        setButtonListeners(view as ViewGroup)
        return view
    }

    // 🌟 [핵심] 구글 일본어 변환 API 호출 및 UI 업데이트
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
                val candidates = jsonArray.getJSONArray(0).getJSONArray(1)

                keyboardView?.post {
                    val container = keyboardView?.findViewById<LinearLayout>(R.id.candidate_layout)
                    container?.removeAllViews()
                    
                    for (i in 0 until candidates.length()) {
                        val word = candidates.getString(i)
                        val btn = Button(this).apply {
                            text = word
                            setTextColor(Color.BLACK)
                            setBackgroundColor(Color.TRANSPARENT)
                            setPadding(30, 0, 30, 0)
                            textSize = 18f
                            isAllCaps = false
                            setOnClickListener { commitCandidate(word) }
                        }
                        container?.addView(btn)
                    }
                    keyboardView?.findViewById<View>(R.id.candidate_scroll)?.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    // 추천 단어 클릭 시 기존 히라가나를 지우고 한자로 교체
    private fun commitCandidate(word: String) {
        val ic = currentInputConnection
        // 현재 버퍼에 쌓인 히라가나 길이만큼 지우기
        ic?.deleteSurroundingText(currentHiraganaBuffer.length, 0)
        // 선택한 한자/단어로 교체
        ic?.commitText(word, 1)
        clearCandidateBuffer()
    }

    private fun clearCandidateBuffer() {
        currentHiraganaBuffer = ""
        updateCandidates("")
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val mappedChar = getKoreanCharFromKeyCode(keyCode)
        when (keyCode) {
            KeyEvent.KEYCODE_DEL -> {
                currentInputConnection?.deleteSurroundingText(1, 0)
                if (currentHiraganaBuffer.isNotEmpty()) {
                    currentHiraganaBuffer = currentHiraganaBuffer.dropLast(1)
                    updateCandidates(currentHiraganaBuffer)
                }
                converter.clearBuffer()
                return true
            }
            KeyEvent.KEYCODE_SPACE -> {
                if (event.isShiftPressed) {
                    (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
                } else {
                    handleSpecialInput(" ")
                }
                return true
            }
            KeyEvent.KEYCODE_ENTER -> {
                handleSpecialInput("\n")
                return true
            }
        }
        if (mappedChar != null) {
            handleNormalInput(mappedChar)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun handleNormalInput(text: String) {
        if (isHangulMode && isShifted) {
            isShifted = false
            updateShiftUI()
        }
        val (deleteCount, textToCommit) = converter.processInput(text)
        if (deleteCount > 0) {
            currentInputConnection?.deleteSurroundingText(deleteCount, 0)
            // 지워진 만큼 버퍼에서도 제거 (오 お -> わ 로 변하는 경우 등 대응)
            if (currentHiraganaBuffer.length >= deleteCount) {
                currentHiraganaBuffer = currentHiraganaBuffer.dropLast(deleteCount)
            }
        }
        currentInputConnection?.commitText(textToCommit, 1)
        
        // 🌟 히라가나 누적 및 추천 업데이트
        currentHiraganaBuffer += textToCommit
        updateCandidates(currentHiraganaBuffer)
    }

    private fun handleSpecialInput(text: String) {
        converter.flushPending()?.let {
            currentInputConnection?.deleteSurroundingText(it.first, 0)
            currentInputConnection?.commitText(it.second, 1)
        }
        currentInputConnection?.commitText(text, 1)
        converter.clearBuffer()
        clearCandidateBuffer() // 스페이스나 엔터 치면 추천바 초기화
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
                        "⌫" -> { 
                            currentInputConnection?.deleteSurroundingText(1, 0)
                            if (currentHiraganaBuffer.isNotEmpty()) {
                                currentHiraganaBuffer = currentHiraganaBuffer.dropLast(1)
                                updateCandidates(currentHiraganaBuffer)
                            }
                            converter.clearBuffer() 
                        }
                        "セイ고입력" -> { handleSpecialInput(" ") }
                        "⏎" -> { handleSpecialInput("\n") }
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
        val shiftButton = keyboardView?.findViewById<Button>(R.id.btn_shift)
        val qButton = keyboardView?.findViewById<Button>(R.id.btn_key_q)
        val wButton = keyboardView?.findViewById<Button>(R.id.btn_key_w)
        val eButton = keyboardView?.findViewById<Button>(R.id.btn_key_e)
        val rButton = keyboardView?.findViewById<Button>(R.id.btn_key_r)
        val tButton = keyboardView?.findViewById<Button>(R.id.btn_key_t)
        if (isShifted) {
            shiftButton?.text = "⇪"; qButton?.text = "ㅃ"; wButton?.text = "ㅉ"; eButton?.text = "ㄸ"; rButton?.text = "ㄲ"; tButton?.text = "ㅆ"
        } else {
            shiftButton?.text = "⇧"; qButton?.text = "ㅂ"; wButton?.text = "ㅈ"; eButton?.text = "ㄷ"; rButton?.text = "ㄱ"; tButton?.text = "ㅅ"
        }
    }

    private fun switchLayout(layoutId: Int) {
        keyboardView?.findViewById<LinearLayout>(R.id.layout_hangul)?.visibility = if (layoutId == 1) View.VISIBLE else View.GONE
        keyboardView?.findViewById<LinearLayout>(R.id.layout_symbol_1)?.visibility = if (layoutId == 2) View.VISIBLE else View.GONE
        keyboardView?.findViewById<LinearLayout>(R.id.layout_symbol_2)?.visibility = if (layoutId == 3) View.VISIBLE else View.GONE
        keyboardView?.findViewById<Button>(R.id.btn_symbol)?.visibility = if (layoutId == 1) View.VISIBLE else View.GONE
        keyboardView?.findViewById<Button>(R.id.btn_globe)?.visibility = if (layoutId == 1) View.VISIBLE else View.GONE
        keyboardView?.findViewById<Button>(R.id.btn_abc)?.visibility = if (layoutId == 2 || layoutId == 3) View.VISIBLE else View.GONE
        val abcButton = keyboardView?.findViewById<Button>(R.id.btn_abc)
        if (layoutId == 2 || layoutId == 3) abcButton?.text = "한글"
        val symbolButton = keyboardView?.findViewById<Button>(R.id.btn_symbol)
        if (layoutId == 1) symbolButton?.text = "!#1"
        isHangulMode = (layoutId == 1)
        converter.clearBuffer()
    }

    private fun getKoreanCharFromKeyCode(keyCode: Int): String? {
        return when (keyCode) {
            KeyEvent.KEYCODE_Q -> "ㅂ"; KeyEvent.KEYCODE_W -> "ㅈ"; KeyEvent.KEYCODE_E -> "ㄷ"; KeyEvent.KEYCODE_R -> "ㄱ"; KeyEvent.KEYCODE_T -> "ㅅ"
            KeyEvent.KEYCODE_Y -> "ㅛ"; KeyEvent.KEYCODE_U -> "ㅕ"; KeyEvent.KEYCODE_I -> "ㅑ"; KeyEvent.KEYCODE_O -> "ㅐ"; KeyEvent.KEYCODE_P -> "ㅔ"
            KeyEvent.KEYCODE_A -> "ㅁ"; KeyEvent.KEYCODE_S -> "ㄴ"; KeyEvent.KEYCODE_D -> "ㅇ"; KeyEvent.KEYCODE_F -> "ㄹ"; KeyEvent.KEYCODE_G -> "ㅎ"
            KeyEvent.KEYCODE_H -> "ㅗ"; KeyEvent.KEYCODE_J -> "ㅓ"; KeyEvent.KEYCODE_K -> "ㅏ"; KeyEvent.KEYCODE_L -> "ㅣ"
            KeyEvent.KEYCODE_Z -> "ㅋ"; KeyEvent.KEYCODE_X -> "ㅌ"; KeyEvent.KEYCODE_C -> "ㅊ"; KeyEvent.KEYCODE_V -> "ㅍ"; KeyEvent.KEYCODE_B -> "ㅠ"
            KeyEvent.KEYCODE_N -> "ㅜ"; KeyEvent.KEYCODE_M -> "ㅡ"; else -> null
        }
    }
}
