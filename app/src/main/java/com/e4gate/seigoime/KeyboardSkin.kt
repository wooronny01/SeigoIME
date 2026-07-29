package com.e4gate.seigoime

import android.content.Context

enum class KeyboardSkin(
    val id: String,
    val displayName: String,
    val shortName: String,
    val description: String,
    val windowBackground: Int,
    val keyboardBackground: Int,
    val keyColor: Int,
    val keyShadow: Int,
    val specialKeyColor: Int,
    val textColor: Int,
    val mutedTextColor: Int,
    val candidateColor: Int,
    val activeColor: Int,
    val activeTextColor: Int,
    val accentColor: Int,
    val enterColor: Int,
    val spaceColor: Int,
    val isDark: Boolean
) {
    MOCHI(
        id = "mochi",
        displayName = "모찌 파스텔",
        shortName = "모찌",
        description = "크림색과 분홍색의 귀엽고 따뜻한 자판",
        windowBackground = 0xFFFFF8EE.toInt(),
        keyboardBackground = 0xFFF7E7E1.toInt(),
        keyColor = 0xFFFFFDFC.toInt(),
        keyShadow = 0xFFD9C5C0.toInt(),
        specialKeyColor = 0xFFE8D3E6.toInt(),
        textColor = 0xFF4C3F46.toInt(),
        mutedTextColor = 0xFF8E7480.toInt(),
        candidateColor = 0xFFFFF7F3.toInt(),
        activeColor = 0xFFF8CAD6.toInt(),
        activeTextColor = 0xFF8E3452.toInt(),
        accentColor = 0xFFE97898.toInt(),
        enterColor = 0xFFEC88A5.toInt(),
        spaceColor = 0xFFFFF4CC.toInt(),
        isDark = false
    ),
    SAKURA(
        id = "sakura",
        displayName = "사쿠라 글라스",
        shortName = "사쿠라",
        description = "벚꽃빛과 보라색의 감성적인 프리미엄 자판",
        windowBackground = 0xFFF6D9E7.toInt(),
        keyboardBackground = 0xFFEDE3F2.toInt(),
        keyColor = 0xFFFFFFFF.toInt(),
        keyShadow = 0xFFC8B6D2.toInt(),
        specialKeyColor = 0xFFD9C9E8.toInt(),
        textColor = 0xFF43354A.toInt(),
        mutedTextColor = 0xFF7B6784.toInt(),
        candidateColor = 0xFFF8F2FA.toInt(),
        activeColor = 0xFF8F6AD5.toInt(),
        activeTextColor = 0xFFFFFFFF.toInt(),
        accentColor = 0xFF9368D8.toInt(),
        enterColor = 0xFF8E6AD5.toInt(),
        spaceColor = 0xFFFFF9FE.toInt(),
        isDark = false
    ),
    SEIGO_BLUE(
        id = "seigo_blue",
        displayName = "세이고 블루",
        shortName = "블루",
        description = "글자가 선명하고 기능 구분이 쉬운 실용적인 자판",
        windowBackground = 0xFFE6EEF8.toInt(),
        keyboardBackground = 0xFFDCE5EE.toInt(),
        keyColor = 0xFFFFFFFF.toInt(),
        keyShadow = 0xFFB8C5D1.toInt(),
        specialKeyColor = 0xFFC3D0DC.toInt(),
        textColor = 0xFF1A2A3A.toInt(),
        mutedTextColor = 0xFF61758A.toInt(),
        candidateColor = 0xFFF2F6FA.toInt(),
        activeColor = 0xFFCFEAFF.toInt(),
        activeTextColor = 0xFF075D98.toInt(),
        accentColor = 0xFF1787D4.toInt(),
        enterColor = 0xFF1688D4.toInt(),
        spaceColor = 0xFFF7FBFF.toInt(),
        isDark = false
    ),
    MATCHA_NIGHT(
        id = "matcha_night",
        displayName = "말차 나이트",
        shortName = "말차",
        description = "눈이 편하고 집중하기 좋은 말차색 다크 자판",
        windowBackground = 0xFF15201C.toInt(),
        keyboardBackground = 0xFF202B25.toInt(),
        keyColor = 0xFF35423A.toInt(),
        keyShadow = 0xFF0E1511.toInt(),
        specialKeyColor = 0xFF45554B.toInt(),
        textColor = 0xFFF1F6F2.toInt(),
        mutedTextColor = 0xFFA8B7AC.toInt(),
        candidateColor = 0xFF2A372F.toInt(),
        activeColor = 0xFF92C97E.toInt(),
        activeTextColor = 0xFF132117.toInt(),
        accentColor = 0xFF9DD789.toInt(),
        enterColor = 0xFF8BC477.toInt(),
        spaceColor = 0xFF29362F.toInt(),
        isDark = true
    );

    companion object {
        fun fromId(id: String?): KeyboardSkin =
            entries.firstOrNull { it.id == id } ?: MOCHI
    }
}

object SkinPreferences {
    private const val PREFS_NAME = "seigo_ime_preferences"
    private const val KEY_SKIN = "keyboard_skin"

    fun get(context: Context): KeyboardSkin {
        val id = context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SKIN, KeyboardSkin.MOCHI.id)
        return KeyboardSkin.fromId(id)
    }

    fun save(context: Context, skin: KeyboardSkin) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SKIN, skin.id)
            .apply()
    }
}
