package com.e4gate.seigoime

import android.inputmethodservice.InputMethodService
import android.view.View

class SeigoIME : InputMethodService() {
    override fun onCreateInputView(): View {
        // 방금 만든 자판 디자인을 화면에 불러옵니다
        return layoutInflater.inflate(R.layout.keyboard_view, null)
    }
}
