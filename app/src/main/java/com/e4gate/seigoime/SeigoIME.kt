package com.e4gate.seigoime

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.ViewGroup
import android.widget.Button

class SeigoIME : InputMethodService() {
    private val converter = JapaneseConverter()

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
                        "간격" -> { 
                            converter.flushPending()?.let {
                                currentInputConnection?.deleteSurroundingText(it.first, 0)
                                currentInputConnection?.commitText(it.second, 1)
                            }
                            currentInputConnection?.commitText(" ", 1)
                            converter.clearBuffer() 
                        }
                        "⏎" -> { 
                            converter.flushPending()?.let {
                                currentInputConnection?.deleteSurroundingText(it.first, 0)
                                currentInputConnection?.commitText(it.second, 1)
                            }
                            currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
                            converter.clearBuffer() 
                        }
                        "⇧", "!#1", "," , "." -> {
                            // 기능 키 및 기본 기호 처리 (추후 확장 가능)
                            currentInputConnection?.commitText(text, 1)
                            converter.clearBuffer()
                        }
                        else -> {
                            val (deleteCount, textToCommit) = converter.processInput(text)
                            if (deleteCount > 0) {
                                currentInputConnection?.deleteSurroundingText(deleteCount, 0)
                            }
                            currentInputConnection?.commitText(textToCommit, 1)
                        }
                    }
                }
            }
        }
    }
}
