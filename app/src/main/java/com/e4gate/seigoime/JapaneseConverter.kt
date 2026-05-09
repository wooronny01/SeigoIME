package com.e4gate.seigoime

class JapaneseConverter {
    private var lastInput = ""

    // 대표님의 디테일이 살아있는 마스터 사전
    private val dictionary = mapOf(
        // 기본 모음
        "ㅇㅏ" to "あ", "ㅇㅣ" to "い", "ㅇㅜ" to "う", "ㅇㅔ" to "え", "ㅇㅗ" to "お", 
        "ㅇㅑ" to "や", "ㅇㅠ" to "ゆ", "ㅇㅛ" to "よ", 
        "ㅇㅘ" to "わ", "ㅇㅝ" to "を", 
        
        "ㄴ" to "ん", "ㅅ" to "っ", 

        // 청음 (대체 발음 완벽 적용)
        "ㅋㅏ" to "か", "ㅋㅣ" to "き", "ㅋㅜ" to "く", "ㅋㅔ" to "け", "ㅋㅗ" to "こ", 
        "ㅅㅏ" to "さ", "ㅅㅣ" to "し", "ㅅㅜ" to "す", "ㅅㅡ" to "す", "ㅅㅔ" to "せ", "ㅅㅗ" to "そ", 
        "ㅌㅏ" to "た", "ㅌㅣ" to "ち", "ㅊㅣ" to "ち", "ㅌㅜ" to "つ", "ㅌㅡ" to "つ", "ㅊㅡ" to "つ", "ㅌㅔ" to "て", "ㅌㅗ" to "と", 
        "ㅎㅏ" to "は", "ㅎㅣ" to "ひ", "ㅎㅜ" to "ふ", "ㅎㅔ" to "へ", "ㅎㅗ" to "ほ", 
        "ㅁㅏ" to "ま", "ㅁㅣ" to "み", "ㅁㅜ" to "む", "ㅁㅔ" to "め", "ㅁㅗ" to "も", 
        "ㄴㅏ" to "な", "ㄴㅣ" to "に", "ㄴㅜ" to "ぬ", "ㄴㅔ" to "ね", "ㄴㅗ" to "の", 
        "ㄹㅏ" to "ら", "ㄹㅣ" to "り", "ㄹㅜ" to "る", "ㄹㅔ" to "れ", "ㄹㅗ" to "ろ", 

        // 탁음 (대체 발음 완벽 적용)
        "ㄱㅏ" to "が", "ㄱㅣ" to "ぎ", "ㄱㅜ" to "ぐ", "ㄱㅔ" to "げ", "ㄱㅗ" to "ご", 
        "ㅈㅏ" to "ざ", "ㅈㅣ" to "じ", "ㅈㅜ" to "ず", "ㅈㅡ" to "ず", "ㅈㅔ" to "ぜ", "ㅈㅗ" to "ぞ", 
        "ㄷㅏ" to "だ", "ㄷㅣ" to "ぢ", "ㄷㅜ" to "づ", "ㄷㅡ" to "づ", "ㄷㅔ" to "で", "ㄷㅗ" to "ど", 
        "ㅂㅏ" to "ば", "ㅂㅣ" to "び", "ㅂㅜ" to "ぶ", "ㅂㅔ" to "べ", "ㅂㅗ" to "ぼ", 

        // 반탁음
        "ㅍㅏ" to "ぱ", "ㅍㅣ" to "ぴ", "ㅍㅜ" to "ぷ", "ㅍㅔ" to "ぺ", "ㅍㅗ" to "ぽ", 

        // 요음 (청음)
        "ㅋㅑ" to "きゃ", "ㅋㅠ" to "きゅ", "ㅋㅛ" to "きょ", 
        "ㅅㅑ" to "しゃ", "ㅅㅠ" to "しゅ", "ㅅㅛ" to "しょ", 
        "ㅊㅏ" to "ちゃ", "ㅊㅜ" to "ちゅ", "ㅊㅗ" to "ちょ", 
        "ㅎㅑ" to "ひゃ", "ㅎㅠ" to "ひゅ", "ㅎㅛ" to "ひょ", 
        "ㅁㅑ" to "みゃ", "ㅁㅠ" to "みゅ", "ㅁㅛ" to "みょ", 
        "ㄴㅑ" to "にゃ", "ㄴㅠ" to "にゅ", "ㄴㅛ" to "にょ", 
        "ㄹㅑ" to "りゃ", "ㄹㅠ" to "りゅ", "ㄹㅛ" to "りょ", 

        // 요음 (탁음/반탁음)
        "ㄱㅑ" to "ぎゃ", "ㄱㅠ" to "ぎゅ", "ㄱㅛ" to "ぎょ", 
        "ㅈㅑ" to "じゃ", "ㅈㅠ" to "じゅ", "ㅈㅛ" to "じょ", 
        "ㅂㅑ" to "びゃ", "ㅂㅠ" to "びゅ", "ㅂㅛ" to "びょ", 
        "ㅍㅑ" to "ぴゃ", "ㅍㅠ" to "ぴゅ", "ㅍㅛ" to "ぴょ"
    )

    // ㄱ, ㅂ을 받침으로 썼을 때 촉음(っ)을 만들어주는 특수 콤보 트리거
    private val sokuonTriggers = mapOf(
        "ㄱㅋ" to "ㅋ", "ㄱㄱ" to "ㄱ", "ㄱㄲ" to "ㄲ", // ㄱ 받침
        "ㅂㅍ" to "ㅍ"                               // ㅂ 받침
    )

    fun processInput(input: String): Pair<Int, String> {
        val combined = lastInput + input
        
        // 1. 일반 조합 (예: ㄱ + ㅏ = が)
        if (dictionary.containsKey(combined)) {
            val res = dictionary[combined]!!
            lastInput = "" 
            return Pair(1, res)
        } 
        // 2. 받침 촉음 특수 콤보 (예: ㄱ + ㅋ = っ + ㅋ)
        else if (sokuonTriggers.containsKey(combined)) {
            val nextChar = sokuonTriggers[combined]!!
            lastInput = nextChar // 다음 모음을 위해 자음을 버퍼에 살려둡니다.
            return Pair(1, "っ" + nextChar) // 앞글자 지우고 'っ자음' 출력
        }
        // 3. 단일 입력 (예: ㅅ = っ, ㄴ = ん)
        else if (dictionary.containsKey(input)) {
            val res = dictionary[input]!!
            lastInput = input
            return Pair(0, res)
        } 
        // 4. 조합 중
        else {
            lastInput = input 
            return Pair(0, input)    
        }
    }

    fun clearBuffer() {
        lastInput = ""
    }
}
