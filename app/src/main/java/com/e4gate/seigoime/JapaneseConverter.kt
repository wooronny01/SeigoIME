package com.e4gate.seigoime

class JapaneseConverter {
    var currentBuffer = ""

    fun processInput(input: String): String {
        currentBuffer += input
        return when (currentBuffer) {
            // 1. 아직 조립 중인 상태 (뒤에 모음이 더 필요한 경우)
            "ㄱ" -> "ㄱ"
            "ㅅ" -> "ㅅ"
            
            // 2. 변환이 완벽하게 끝난 상태!
            "ㄱㅏ" -> {
                currentBuffer = ""
                "か"
            }
            "ㅅㅏ" -> {
                currentBuffer = ""
                "さ"
            }
            
            // 3. 변환 규칙에 없는 글자는 그냥 그대로 확정하고 버퍼 비우기
            else -> {
                val out = currentBuffer
                currentBuffer = ""
                out
            }
        }
    }

    // 이 함수가 핵심! 버퍼에 글자가 남아있으면 "조립 중"이라고 판단합니다.
    fun isComposing(): Boolean {
        return currentBuffer.isNotEmpty()
    }

    fun clearBuffer() {
        currentBuffer = ""
    }
}
