package com.e4gate.seigoime

class JapaneseConverter {
    // 윈도우 버전에서 사용하신 매핑 로직이 들어갈 자리입니다.
    // 예시: "ㄱㅏ" -> "か"
    
    private var currentBuffer = ""

    fun processInput(input: String): String {
        currentBuffer += input
        
        // 여기에 대표님의 윈도우 변환 알고리즘이 적용됩니다.
        // 현재는 임시로 특정 글자만 변환되도록 세팅해 둡니다.
        return when (currentBuffer) {
            "ㄱㅏ" -> {
                currentBuffer = ""
                "か"
            }
            "ㅅㅏ" -> {
                currentBuffer = ""
                "さ"
            }
            else -> input // 변환 규칙에 없으면 그대로 출력
        }
    }

    fun clearBuffer() {
        currentBuffer = ""
    }
}
