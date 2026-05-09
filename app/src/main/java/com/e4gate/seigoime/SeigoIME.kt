package com.e4gate.seigoime

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout

class SeigoIME : InputMethodService() {
    private val converter = JapaneseConverter()
    
    private var isShifted = false
    private var isHangulMode = true
    
    // 화면 뷰를 명시적으로 담아둘 변수 추가
    private var keyboardView: View? = null

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        keyboardView = view
        setButtonListeners(view as ViewGroup)
        return view
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
