package com.e4gate.seigoime

/**
 * 일본어 IME에서 사용하는 문자 폭 변환 도우미.
 *
 * 내부 조합 버퍼는 항상 히라가나를 유지하고, 화면에 표시하거나 확정할 때만
 * 전각 가타카나/반각 가타카나로 변환한다.
 */
object CharacterWidthConverter {
    fun hiraganaToKatakana(text: String): String = buildString(text.length) {
        text.forEach { char ->
            append(if (char in '\u3041'..'\u3096') char + 0x60 else char)
        }
    }

    fun hiraganaToHalfWidthKatakana(text: String): String =
        katakanaToHalfWidth(hiraganaToKatakana(text))

    fun katakanaToHalfWidth(text: String): String = buildString(text.length * 2) {
        text.forEach { char -> append(FULL_TO_HALF_KATAKANA[char] ?: char.toString()) }
    }

    fun asciiToFullWidth(text: String): String = buildString(text.length) {
        text.forEach { char ->
            append(
                when (char) {
                    ' ' -> '\u3000'
                    in '!'..'~' -> char + 0xFEE0
                    else -> char
                }
            )
        }
    }

    fun fullWidthAsciiToHalfWidth(text: String): String = buildString(text.length) {
        text.forEach { char ->
            append(
                when (char) {
                    '\u3000' -> ' '
                    in '\uFF01'..'\uFF5E' -> char - 0xFEE0
                    else -> char
                }
            )
        }
    }

    private val FULL_TO_HALF_KATAKANA: Map<Char, String> = mapOf(
        'ァ' to "ｧ", 'ア' to "ｱ", 'ィ' to "ｨ", 'イ' to "ｲ",
        'ゥ' to "ｩ", 'ウ' to "ｳ", 'ェ' to "ｪ", 'エ' to "ｴ",
        'ォ' to "ｫ", 'オ' to "ｵ",
        'カ' to "ｶ", 'ガ' to "ｶﾞ", 'キ' to "ｷ", 'ギ' to "ｷﾞ",
        'ク' to "ｸ", 'グ' to "ｸﾞ", 'ケ' to "ｹ", 'ゲ' to "ｹﾞ",
        'コ' to "ｺ", 'ゴ' to "ｺﾞ",
        'サ' to "ｻ", 'ザ' to "ｻﾞ", 'シ' to "ｼ", 'ジ' to "ｼﾞ",
        'ス' to "ｽ", 'ズ' to "ｽﾞ", 'セ' to "ｾ", 'ゼ' to "ｾﾞ",
        'ソ' to "ｿ", 'ゾ' to "ｿﾞ",
        'タ' to "ﾀ", 'ダ' to "ﾀﾞ", 'チ' to "ﾁ", 'ヂ' to "ﾁﾞ",
        'ッ' to "ｯ", 'ツ' to "ﾂ", 'ヅ' to "ﾂﾞ", 'テ' to "ﾃ",
        'デ' to "ﾃﾞ", 'ト' to "ﾄ", 'ド' to "ﾄﾞ",
        'ナ' to "ﾅ", 'ニ' to "ﾆ", 'ヌ' to "ﾇ", 'ネ' to "ﾈ", 'ノ' to "ﾉ",
        'ハ' to "ﾊ", 'バ' to "ﾊﾞ", 'パ' to "ﾊﾟ", 'ヒ' to "ﾋ",
        'ビ' to "ﾋﾞ", 'ピ' to "ﾋﾟ", 'フ' to "ﾌ", 'ブ' to "ﾌﾞ",
        'プ' to "ﾌﾟ", 'ヘ' to "ﾍ", 'ベ' to "ﾍﾞ", 'ペ' to "ﾍﾟ",
        'ホ' to "ﾎ", 'ボ' to "ﾎﾞ", 'ポ' to "ﾎﾟ",
        'マ' to "ﾏ", 'ミ' to "ﾐ", 'ム' to "ﾑ", 'メ' to "ﾒ", 'モ' to "ﾓ",
        'ャ' to "ｬ", 'ヤ' to "ﾔ", 'ュ' to "ｭ", 'ユ' to "ﾕ",
        'ョ' to "ｮ", 'ヨ' to "ﾖ",
        'ラ' to "ﾗ", 'リ' to "ﾘ", 'ル' to "ﾙ", 'レ' to "ﾚ", 'ロ' to "ﾛ",
        'ヮ' to "ﾜ", 'ワ' to "ﾜ", 'ヰ' to "ｲ", 'ヱ' to "ｴ",
        'ヲ' to "ｦ", 'ン' to "ﾝ", 'ヴ' to "ｳﾞ",
        'ヵ' to "ｶ", 'ヶ' to "ｹ", 'ヷ' to "ﾜﾞ", 'ヸ' to "ｲﾞ",
        'ヹ' to "ｴﾞ", 'ヺ' to "ｦﾞ",
        'ー' to "ｰ", '・' to "･", '「' to "｢", '」' to "｣",
        '、' to "､", '。' to "｡", '゛' to "ﾞ", '゜' to "ﾟ"
    )
}
