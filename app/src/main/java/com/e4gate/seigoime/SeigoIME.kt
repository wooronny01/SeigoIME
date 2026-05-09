package com.e4gate.seigoime

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout

class SeigoIME : InputMethodService() {
    private val converter = JapaneseConverter()
    
    private var isShifted = false
    private var isHangulMode = true
    
    private var keyboardView: View? = null

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        keyboardView = view
        setButtonListeners(view as ViewGroup)
        return view
    }

    // -------------------------------------------------------------
    // 🌟 1. 하드웨어 키보드 입력 가로채기 (물리 키보드 연동 로직)
    // -------------------------------------------------------------
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // 영문 키코드를 한글 자모로 변환
        val mappedChar = getKoreanCharFromKeyCode(keyCode)
        
        when (keyCode) {
            KeyEvent.KEYCODE_DEL -> {
                currentInputConnection?.deleteSurroundingText(1, 0)
                converter.clearBuffer()
                return true // 입력 소비(완료) 처리
            }
            KeyEvent.KEYCODE_SPACE -> {
                handleSpecialInput(" ")
                return true
            }
            KeyEvent.KEYCODE_ENTER -> {
                handleSpecialInput("\n")
                return true
            }
        }

        // 변환된 한글이 있다면 우리 엔진으로 전송
        if (mappedChar != null) {
            handleNormalInput(mappedChar)
            return true
        }

        // 우리가 처리하지 않는 키(숫자, 기호, 방향키 등)는 안드로이드 기본 동작에 맡김
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        // onKeyDown에서 우리가 가로챈 키가 onKeyUp에서 다시 시스템으로 넘어가지 않게 방어
        if (getKoreanCharFromKeyCode(keyCode) != null || 
            keyCode == KeyEvent.KEYCODE_DEL || 
            keyCode == KeyEvent.KEYCODE_SPACE || 
            keyCode == KeyEvent.KEYCODE_ENTER) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    // 영문 QWERTY 레이아웃을 한글 두벌식/단모음으로 매핑
    private fun getKoreanCharFromKeyCode(keyCode: Int): String? {
        return when (keyCode) {
            KeyEvent.KEYCODE_Q -> "ㅂ"
            KeyEvent.KEYCODE_W -> "ㅈ"
            KeyEvent.KEYCODE_E -> "ㄷ"
            KeyEvent.KEYCODE_R -> "ㄱ"
            KeyEvent.KEYCODE_T -> "ㅅ"
            KeyEvent.KEYCODE_Y -> "ㅛ"
            KeyEvent.KEYCODE_U -> "ㅕ"
            KeyEvent.KEYCODE_I -> "ㅑ"
            KeyEvent.KEYCODE_O -> "ㅐ"
            KeyEvent.KEYCODE_P -> "ㅔ"
            KeyEvent.KEYCODE_A -> "ㅁ"
            KeyEvent.KEYCODE_S -> "ㄴ"
            KeyEvent.KEYCODE_D -> "ㅇ"
            KeyEvent.KEYCODE_F -> "ㄹ"
            KeyEvent.KEYCODE_G -> "ㅎ"
            KeyEvent.KEYCODE_H -> "ㅗ"
            KeyEvent.KEYCODE_J -> "ㅓ"
            KeyEvent.KEYCODE_K -> "ㅏ"
            KeyEvent.KEYCODE_L -> "ㅣ"
            KeyEvent.KEYCODE_Z -> "ㅋ"
            KeyEvent.KEYCODE_X -> "ㅌ"
            KeyEvent.KEYCODE_C -> "ㅊ"
            KeyEvent.KEYCODE_V -> "ㅍ"
            KeyEvent.KEYCODE_B -> "ㅠ"
            KeyEvent.KEYCODE_N -> "ㅜ"
            KeyEvent.KEYCODE_M -> "ㅡ"
            else -> null
        }
    }

    // -------------------------------------------------------------
    // 2. 화면(소프트) 키보드 터치 로직 (기존과 동일)
    // -------------------------------------------------------------
    private fun setButtonListeners(viewGroup: ViewGroup) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is ViewGroup) {
                setButtonListeners(child)
            } else if (child is Button) {
                child.setOnClickListener {
                    val text = child.text.toString()
                    when (text) {
                        "⌫" -> { 
                            currentInputConnection?.deleteSurroundingText(1, 0)
                            converter.clearBuffer() 
                        }
                        "간격" -> { handleSpecialInput(" ") }
                        "⏎" -> { handleSpecialInput("\n") }
                        "⇧", "⇪" -> { handleShift() }
                        "!#1", "?123" -> { switchLayout(2) } 
                        "ABC" -> { switchLayout(1) } 
                        "=\\<" -> { switchLayout(3) } 
                        else -> { handleNormalInput(text) }
                    }
                }
            }
        }
    }

    private fun handleShift() {
        isShifted = !isShifted
        updateShiftUI()
        converter.clearBuffer()
    }

    private fun updateShiftUI() {
        val shiftButton = keyboardView?.findViewById<Button>(R.id.btn_shift)
        val qButton = keyboardView?.findViewById<Button>(R.id.btn_key_q)
        val wButton = keyboardView?.findViewById<Button>(R.id.btn_key_w)
        val eButton = keyboardView?.findViewById<Button>(R.id.btn_key_e)
        val rButton = keyboardView?.findViewById<Button>(R.id.btn_key_r)
        val tButton = keyboardView?.findViewById<Button>(R.id.btn_key_t)

        if (isShifted) {
            shiftButton?.text = "⇪" 
            qButton?.text = "ㅃ"
            wButton?.text = "ㅉ"
            eButton?.text = "ㄸ"
            rButton?.text = "ㄲ"
            tButton?.text = "ㅆ"
        } else {
            shiftButton?.text = "⇧"
            qButton?.text = "ㅂ"
            wButton?.text = "ㅈ"
            eButton?.text = "ㄷ"
            rButton?.text = "ㄱ"
            tButton?.text = "ㅅ"
        }
    }

    private fun switchLayout(layoutId: Int) {
        keyboardView?.findViewById<LinearLayout>(R.id.layout_hangul)?.visibility = if (layoutId == 1) View.VISIBLE else View.GONE
        keyboardView?.findViewById<LinearLayout>(R.id.layout_symbol_1)?.visibility = if (layoutId == 2) View.VISIBLE else View.GONE
        keyboardView?.findViewById<LinearLayout>(R.id.layout_symbol_2)?.visibility = if (layoutId == 3) View.VISIBLE else View.GONE
        
        keyboardView?.findViewById<Button>(R.id.btn_symbol)?.visibility = if (layoutId == 1) View.VISIBLE else View.GONE
        keyboardView?.findViewById<Button>(R.id.btn_abc)?.visibility = if (layoutId == 2 || layoutId == 3) View.VISIBLE else View.GONE
        
        val abcButton = keyboardView?.findViewById<Button>(R.id.btn_abc)
        if (layoutId == 2 || layoutId == 3) abcButton?.text = "ABC"

        val symbolButton = keyboardView?.findViewById<Button>(R.id.btn_symbol)
        if (layoutId == 1) symbolButton?.text = "!#1"

        isHangulMode = (layoutId == 1)
        converter.clearBuffer()
    }

    private fun handleNormalInput(text: String) {
        if (isHangulMode && isShifted) {
            isShifted = false
            updateShiftUI()
        }

        val (deleteCount, textToCommit) = converter.processInput(text)
        if (deleteCount > 0) {
            currentInputConnection?.deleteSurroundingText(deleteCount, 0)
        }
        currentInputConnection?.commitText(textToCommit, 1)
    }

    private fun handleSpecialInput(text: String) {
        converter.flushPending()?.let {
            currentInputConnection?.deleteSurroundingText(it.first, 0)
            currentInputConnection?.commitText(it.second, 1)
        }
        currentInputConnection?.commitText(text, 1)
        converter.clearBuffer()
    }
}
