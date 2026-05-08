package com.e4gate.seigoime

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.ViewGroup
import android.widget.Button

class SeigoIME : InputMethodService() {
    
    // 대표님의 변환 엔진 장착!
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
                            // 일반 자모음이 입력되면 변환 엔진을 거쳐서 출력합니다!
                            val convertedText = converter.processInput(text)
                            // 기존 글자를 덮어쓰거나 새로 입력하는 로직 (윈도우의 이벤트 처리 방식과 동일)
                            currentInputConnection?.commitText(convertedText, 1)
                        }
                    }
                }
            }
        }
    }
}
