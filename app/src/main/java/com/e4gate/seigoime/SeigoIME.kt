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
                        "지우기" -> { 
                            currentInputConnection?.deleteSurroundingText(1, 0)
                            converter.clearBuffer() 
                        }
                        "띄어쓰기" -> { 
                            // 띄어쓰기 누르기 직전에 혼자 남은 'ㅅ'이나 'ㄴ'이 있다면 っ/ん으로 깔끔하게 변환
                            converter.flushPending()?.let {
                                currentInputConnection?.deleteSurroundingText(it.first, 0)
                                currentInputConnection?.commitText(it.second, 1)
                            }
                            currentInputConnection?.commitText(" ", 1)
                            converter.clearBuffer() 
                        }
                        "엔터" -> { 
                            converter.flushPending()?.let {
                                currentInputConnection?.deleteSurroundingText(it.first, 0)
                                currentInputConnection?.commitText(it.second, 1)
                            }
                            currentInputConnection?.commitText("\n", 1)
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
