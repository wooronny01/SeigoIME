package com.e4gate.seigoime

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var content: LinearLayout
    private var selectedSkin: KeyboardSkin = KeyboardSkin.MOCHI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedSkin = SkinPreferences.get(this)
        render()
    }

    private fun render() {
        selectedSkin = SkinPreferences.get(this)
        applySystemBarColors(selectedSkin)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(selectedSkin.windowBackground)
        }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(32))
        }
        scroll.addView(
            content,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        addHeader()
        addSetupButtons()

        val sectionTitle = TextView(this).apply {
            text = "자판 스킨"
            textSize = 22f
            setTextColor(selectedSkin.textColor)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, dp(26), 0, dp(5))
        }
        content.addView(sectionTitle)

        val sectionDescription = TextView(this).apply {
            text = "원하는 디자인을 누르면 바로 저장됩니다. 자판 상단의 ‘스킨’ 버튼에서도 언제든 바꿀 수 있습니다."
            textSize = 14f
            setTextColor(selectedSkin.mutedTextColor)
            setLineSpacing(0f, 1.2f)
            setPadding(0, 0, 0, dp(14))
        }
        content.addView(sectionDescription)

        KeyboardSkin.entries.forEach { content.addView(createSkinCard(it)) }
        setContentView(scroll)
    }

    private fun addHeader() {
        val badge = TextView(this).apply {
            text = "Seigo"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(selectedSkin.activeTextColor)
            background = rounded(selectedSkin.activeColor, 18f)
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        content.addView(
            badge,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        content.addView(TextView(this).apply {
            text = "Seigo IME"
            textSize = 32f
            setTextColor(selectedSkin.textColor)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, dp(12), 0, dp(4))
        })

        content.addView(TextView(this).apply {
            text = "한글 자판으로 일본어를 편하게 입력하세요."
            textSize = 16f
            setTextColor(selectedSkin.mutedTextColor)
            setPadding(0, 0, 0, dp(18))
        })
    }

    private fun addSetupButtons() {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val enableButton = actionButton("1. 자판 활성화", selectedSkin.specialKeyColor).apply {
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }
        }
        val chooseButton = actionButton("2. 자판 선택", selectedSkin.accentColor).apply {
            setTextColor(if (selectedSkin.isDark) Color.rgb(20, 34, 23) else Color.WHITE)
            setOnClickListener {
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
            }
        }

        row.addView(enableButton, weightedParams())
        row.addView(chooseButton, weightedParams(leftMargin = dp(8)))
        content.addView(
            row,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52)
            )
        )
    }

    private fun createSkinCard(skin: KeyboardSkin): View {
        val selected = skin == selectedSkin
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            isClickable = true
            isFocusable = true
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = cardBackground(skin, selected)
            setOnClickListener {
                SkinPreferences.save(this@MainActivity, skin)
                Toast.makeText(
                    this@MainActivity,
                    "${skin.displayName} 스킨을 적용했습니다.",
                    Toast.LENGTH_SHORT
                ).show()
                render()
            }
        }

        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        titleRow.addView(TextView(this).apply {
            text = skin.displayName
            textSize = 19f
            setTextColor(skin.textColor)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }, weightedParams())

        titleRow.addView(TextView(this).apply {
            text = if (selected) "사용 중" else "선택"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(if (selected) skin.activeTextColor else skin.mutedTextColor)
            background = rounded(
                if (selected) skin.activeColor else skin.candidateColor,
                15f
            )
            setPadding(dp(12), dp(6), dp(12), dp(6))
        })
        card.addView(titleRow)

        card.addView(TextView(this).apply {
            text = skin.description
            textSize = 13f
            setTextColor(skin.mutedTextColor)
            setPadding(0, dp(4), 0, dp(12))
        })

        card.addView(createKeyboardPreview(skin))

        card.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(12)
        }
        return card
    }

    private fun createKeyboardPreview(skin: KeyboardSkin): View {
        val preview = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = rounded(skin.keyboardBackground, 14f)
        }

        val candidates = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        listOf("今日", "京都", "きょう").forEachIndexed { index, text ->
            candidates.addView(TextView(this).apply {
                this.text = text
                textSize = 11f
                gravity = Gravity.CENTER
                setTextColor(if (index == 0) skin.activeTextColor else skin.textColor)
                background = rounded(
                    if (index == 0) skin.activeColor else skin.candidateColor,
                    10f
                )
                setPadding(dp(11), dp(5), dp(11), dp(5))
            }, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dp(5) })
        }
        preview.addView(candidates)

        val rows = listOf(
            listOf("ㅂ", "ㅈ", "ㄷ", "ㄱ", "ㅅ"),
            listOf("ㅁ", "ㄴ", "ㅇ", "ㄹ", "ㅎ"),
            listOf("⇧", "ㅋ", "ㅌ", "ㅊ", "←")
        )
        rows.forEachIndexed { rowIndex, labels ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(5), 0, 0)
            }
            labels.forEachIndexed { index, label ->
                val special = rowIndex == 2 && (index == 0 || index == labels.lastIndex)
                row.addView(TextView(this).apply {
                    text = label
                    textSize = 14f
                    gravity = Gravity.CENTER
                    setTextColor(skin.textColor)
                    background = rounded(
                        if (special) skin.specialKeyColor else skin.keyColor,
                        8f
                    )
                }, LinearLayout.LayoutParams(0, dp(34), 1f).apply {
                    if (index > 0) marginStart = dp(5)
                })
            }
            preview.addView(row)
        }

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(5), 0, 0)
        }
        bottom.addView(previewKey("あ", skin.specialKeyColor, skin.textColor), previewWeight(1f))
        bottom.addView(previewKey("세이고 입력", skin.spaceColor, skin.textColor), previewWeight(3f, dp(5)))
        bottom.addView(
            previewKey(
                "입력",
                skin.enterColor,
                if (skin.isDark) 0xFF142217.toInt() else Color.WHITE
            ),
            previewWeight(1.2f, dp(5))
        )
        preview.addView(bottom)

        return preview
    }

    private fun previewKey(text: String, color: Int, textColor: Int): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(textColor)
            background = rounded(color, 8f)
        }

    private fun actionButton(label: String, color: Int): Button =
        Button(this).apply {
            text = label
            textSize = 14f
            isAllCaps = false
            setTextColor(selectedSkin.textColor)
            background = rounded(color, 14f)
            stateListAnimator = null
            minWidth = 0
            minimumWidth = 0
        }

    private fun cardBackground(skin: KeyboardSkin, selected: Boolean): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(20).toFloat()
            setColor(skin.candidateColor)
            setStroke(
                dp(if (selected) 3 else 1),
                if (selected) skin.accentColor else skin.keyShadow
            )
        }

    private fun rounded(color: Int, radiusDp: Float): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp.toInt()).toFloat()
            setColor(color)
        }

    private fun weightedParams(leftMargin: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
            marginStart = leftMargin
        }

    private fun previewWeight(weight: Float, leftMargin: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, dp(38), weight).apply {
            marginStart = leftMargin
        }

    private fun applySystemBarColors(skin: KeyboardSkin) {
        window.statusBarColor = skin.windowBackground
        window.navigationBarColor = skin.windowBackground
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility =
                if (skin.isDark) 0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
