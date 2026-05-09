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

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
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
        val shiftButton = currentInputView?.findViewById<Button>(R.id.btn_shift)
        val ㅂButton = currentInputView?.findViewById<Button>(R.id.btn_key_ㅂ)
        val ㅈButton = currentInputView?.findViewById<Button>(R.id.btn_key_ㅈ)
        val ㄷButton = currentInputView?.findViewById<Button>(R.id.btn_key_ㄷ)
        val ㄱButton = currentInputView?.findViewById<Button>(R.id.btn_key_ㄱ)
        val ㅅButton = currentInputView?.findViewById<Button>(R.id.btn_key_ㅅ)

        if (isShifted) {
            shiftButton?.text = "⇪" 
            ㅂButton?.text = "ㅃ"
            ㅈButton?.text = "ㅉ"
            ㄷButton?.text = "ㄸ"
            ㄱButton?.text = "ㄲ"
            ㅅButton?.text = "ㅆ"
        } else {
            shiftButton?.text = "⇧"
            ㅂButton?.text = "ㅂ"
            ㅈButton?.text = "ㅈ"
            ㄷButton?.text = "ㄷ"
            ㄱButton?.text = "ㄱ"
            ㅅButton?.text = "ㅅ"
        }
    }

    private fun switchLayout(layoutId: Int) {
        currentInputView?.findViewById<LinearLayout>(R.id.layout_hangul)?.visibility = if (layoutId == 1) View.VISIBLE else View.GONE
        currentInputView?.findViewById<LinearLayout>(R.id.layout_symbol_1)?.visibility = if (layoutId == 2) View.VISIBLE else View.GONE
        currentInputView?.findViewById<LinearLayout>(R.id.layout_symbol_2)?.visibility = if (layoutId == 3) View.VISIBLE else View.GONE
        
        currentInputView?.findViewById<Button>(R.id.btn_symbol)?.visibility = if (layoutId == 1) View.VISIBLE else View.GONE
        currentInputView?.findViewById<Button>(R.id.btn_abc)?.visibility = if (layoutId == 2 || layoutId == 3) View.VISIBLE else View.GONE
        
        val abcButton = currentInputView?.findViewById<Button>(R.id.btn_abc)
        if (layoutId == 2 || layoutId == 3) abcButton?.text = "ABC"

        val symbolButton = currentInputView?.findViewById<Button>(R.id.btn_symbol)
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
