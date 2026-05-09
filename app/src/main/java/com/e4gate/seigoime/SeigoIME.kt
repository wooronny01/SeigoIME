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
                            currentInputConnection?.commitText(" ", 1)
                            converter.clearBuffer() 
                        }
                        "엔터" -> { 
                            currentInputConnection?.commitText("\n", 1)
                            converter.clearBuffer() 
                        }
                        else -> {
                            val result = converter.processInput(text)
                            
                            // [안드로이드 변환의 핵심 로직]
                            if (converter.isComposing()) {
                                // 조립 중이면 밑줄 친 상태로 임시 대기 (setComposingText)
                                currentInputConnection?.setComposingText(result, 1)
                            } else {
                                // 완성이면 글자를 완전히 확정 (commitText)
                                currentInputConnection?.commitText(result, 1)
                            }
                        }
                    }
                }
            }
        }
    }
}
