package com.e4gate.seigoime

class JapaneseConverter {
    private var lastInput = ""

    fun processInput(input: String): Pair<Int, String> {
        val combined = lastInput + input
        
        return when (combined) {
            "ㄱㅏ" -> {
                lastInput = "" 
                Pair(1, "か")  
            }
            "ㅅㅏ" -> {
                lastInput = ""
                Pair(1, "さ")
            }
            else -> {
                lastInput = input 
                Pair(0, input)    
            }
        }
    }

    fun clearBuffer() {
        lastInput = ""
    }
}
