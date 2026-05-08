package com.e4gate.seigoime

class JapaneseConverter {
    private var currentBuffer = ""

    fun processInput(input: String): String {
        currentBuffer += input
        return when (currentBuffer) {
            "ㄱㅏ" -> { currentBuffer = ""; "か" }
            "ㅅㅏ" -> { currentBuffer = ""; "さ" }
            else -> input
        }
    }

    fun clearBuffer() {
        currentBuffer = ""
    }
}
