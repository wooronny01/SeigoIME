package com.e4gate.seigoime

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val mappedChar = getKoreanCharFromKeyCode(keyCode)
        
        when (keyCode) {
            KeyEvent.KEYCODE_DEL -> {
                currentInputConnection?.deleteSurroundingText(1, 0)
                converter.clearBuffer()
                return true
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
                        "=\\<" -> { switchLayout(3) } 
                        
                        // 🌟 "ABC"를 "한글"로 변경
                        "한글" -> { switchLayout(1) } 
                        
                        "🌐" -> {
                            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.showInputMethodPicker()
                        }
                        
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
        keyboardView?.findViewById<Button>(R.id.btn_globe)?.visibility = if (layoutId == 1) View.VISIBLE else View.GONE
        keyboardView?.findViewById<Button>(R.id.btn_abc)?.visibility = if (layoutId == 2 || layoutId == 3) View.VISIBLE else View.GONE
        
        // 🌟 "ABC"를 "한글"로 변경
        val abcButton = keyboardView?.findViewById<Button>(R.id.btn_abc)
        if (layoutId == 2 || layoutId == 3) abcButton?.text = "한글"

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
