package com.e4gate.seigoime

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SeigoIME : InputMethodService() {
    private val converter = JapaneseConverter()
    private var keyboardView: View? = null

    private var isShifted = false
    private var isHangulMode = true
    private var isDanmoeum = false
    private var isKatakanaMode = false

    private var currentHiraganaBuffer = ""
    private val candidatesList = mutableListOf<String>()
    private var currentCandidateIndex = -1
    private var lastCommittedLength = 0
    private var lastVowelKey = ""

    private var currentSkin = KeyboardSkin.MOCHI

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var dbHelper: KanjiDatabaseHelper

    override fun onCreate() {
        super.onCreate()
        currentSkin = SkinPreferences.get(this)
        dbHelper = KanjiDatabaseHelper(this)
        serviceScope.launch(Dispatchers.IO) {
            dbHelper.loadCsvToDatabase(this@SeigoIME)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        keyboardView = view
        currentSkin = SkinPreferences.get(this)
        setButtonListeners(view as ViewGroup)
        applySkin()
        return view
    }

    override fun onStartInputView(
        info: android.view.inputmethod.EditorInfo?,
        restarting: Boolean
    ) {
        super.onStartInputView(info, restarting)
        val savedSkin = SkinPreferences.get(this)
        if (savedSkin != currentSkin) {
            currentSkin = savedSkin
            applySkin()
        }
    }

    private fun toKatakana(str: String): String {
        return str.map {
            if (it in '\u3041'..'\u3096') (it + 0x60) else it
        }.joinToString("")
    }

    private fun updateCandidates(query: String) {
        if (query.isEmpty()) {
            keyboardView?.post {
                keyboardView
                    ?.findViewById<LinearLayout>(R.id.candidate_layout)
                    ?.removeAllViews()
                keyboardView
                    ?.findViewById<View>(R.id.candidate_scroll)
                    ?.visibility = View.INVISIBLE
            }
            return
        }

        val searchQuery = query.replace("n", "ん").replace("ｎ", "ん")
        serviceScope.launch(Dispatchers.IO) {
            try {
                val suggestions = dbHelper.getSuggestions(searchQuery)
                withContext(Dispatchers.Main) {
                    val container =
                        keyboardView?.findViewById<LinearLayout>(R.id.candidate_layout)
                    container?.removeAllViews()
                    candidatesList.clear()

                    if (suggestions.isNotEmpty()) {
                        suggestions.forEach { word ->
                            candidatesList.add(word)
                            val candidateButton = Button(this@SeigoIME).apply {
                                text = word
                                tag = "candidate"
                                textSize = 17f
                                isAllCaps = false
                                includeFontPadding = false
                                gravity = Gravity.CENTER
                                minWidth = 0
                                minimumWidth = 0
                                minHeight = 0
                                minimumHeight = 0
                                stateListAnimator = null
                                setPadding(dp(18), 0, dp(18), 0)
                                setOnClickListener {
                                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    commitCandidateFromTouch(word)
                                }
                            }
                            val params = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            ).apply {
                                marginStart = dp(3)
                                marginEnd = dp(3)
                                topMargin = dp(4)
                                bottomMargin = dp(4)
                            }
                            container?.addView(candidateButton, params)
                            styleCandidateButton(candidateButton, selected = false)
                        }
                        keyboardView
                            ?.findViewById<View>(R.id.candidate_scroll)
                            ?.visibility = View.VISIBLE
                    } else {
                        keyboardView
                            ?.findViewById<View>(R.id.candidate_scroll)
                            ?.visibility = View.INVISIBLE
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun commitCandidateFromTouch(word: String) {
        val ic = currentInputConnection
        ic?.deleteSurroundingText(lastCommittedLength, 0)
        ic?.commitText(word, 1)
        clearCandidateBuffer()
    }

    private fun clearCandidateBuffer() {
        currentHiraganaBuffer = ""
        candidatesList.clear()
        currentCandidateIndex = -1
        lastCommittedLength = 0
        lastVowelKey = ""
        converter.clearBuffer()
        updateCandidates("")
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val mappedChar = getKoreanCharFromKeyCode(keyCode)
        when (keyCode) {
            KeyEvent.KEYCODE_DEL -> {
                handleBackspace()
                return true
            }
            KeyEvent.KEYCODE_SPACE -> {
                if (event.isShiftPressed) {
                    showInputMethodPicker()
                } else {
                    handleSpaceAction()
                }
                return true
            }
            KeyEvent.KEYCODE_ENTER -> {
                handleEnterAction()
                return true
            }
        }
        if (mappedChar != null) {
            handleNormalInput(mappedChar)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (
            getKoreanCharFromKeyCode(keyCode) != null ||
            keyCode == KeyEvent.KEYCODE_DEL ||
            keyCode == KeyEvent.KEYCODE_SPACE ||
            keyCode == KeyEvent.KEYCODE_ENTER
        ) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun handleBackspace() {
        if (currentCandidateIndex != -1) {
            clearCandidateBuffer()
            currentInputConnection?.deleteSurroundingText(1, 0)
            return
        }

        currentInputConnection?.deleteSurroundingText(1, 0)
        if (currentHiraganaBuffer.isNotEmpty()) {
            currentHiraganaBuffer = currentHiraganaBuffer.dropLast(1)
            lastCommittedLength = currentHiraganaBuffer.length
            currentCandidateIndex = -1
            updateCandidates(currentHiraganaBuffer)
        }
        lastVowelKey = ""
        converter.clearBuffer()
    }

    private fun handleSpaceAction() {
        if (candidatesList.isNotEmpty()) {
            currentCandidateIndex =
                (currentCandidateIndex + 1) % candidatesList.size
            val selectedWord = candidatesList[currentCandidateIndex]
            val ic = currentInputConnection
            ic?.deleteSurroundingText(lastCommittedLength, 0)
            ic?.commitText(selectedWord, 1)
            lastCommittedLength = selectedWord.length
            highlightCandidateUI(currentCandidateIndex)
        } else {
            handleSpecialInput(" ")
        }
    }

    private fun handleEnterAction() {
        if (currentHiraganaBuffer.isNotEmpty()) {
            clearCandidateBuffer()
        } else {
            handleSpecialInput("\n")
        }
    }

    private fun highlightCandidateUI(index: Int) {
        keyboardView?.post {
            val container =
                keyboardView?.findViewById<LinearLayout>(R.id.candidate_layout)
                    ?: return@post

            for (i in 0 until container.childCount) {
                val button = container.getChildAt(i) as? Button ?: continue
                styleCandidateButton(button, selected = i == index)
            }

            if (index !in 0 until container.childCount) return@post
            val scroll =
                keyboardView?.findViewById<HorizontalScrollView>(R.id.candidate_scroll)
            val selectedButton = container.getChildAt(index)
            if (scroll != null) {
                val scrollX =
                    selectedButton.left - (scroll.width / 2) + (selectedButton.width / 2)
                scroll.smoothScrollTo(scrollX, 0)
            }
        }
    }

    private fun handleNormalInput(text: String) {
        if (currentCandidateIndex != -1) {
            clearCandidateBuffer()
        }

        if (isHangulMode && isShifted) {
            isShifted = false
            updateShiftUI()
        }

        if (
            text == "ㄴ" &&
            (currentHiraganaBuffer.endsWith("n") ||
                currentHiraganaBuffer.endsWith("ｎ"))
        ) {
            currentInputConnection?.deleteSurroundingText(1, 0)
            currentHiraganaBuffer = currentHiraganaBuffer.dropLast(1)

            val finalCommit = if (isKatakanaMode) "ン" else "ん"
            currentInputConnection?.commitText(finalCommit, 1)

            currentHiraganaBuffer += "ん"
            lastCommittedLength = currentHiraganaBuffer.length
            currentCandidateIndex = -1
            converter.clearBuffer()
            updateCandidates(currentHiraganaBuffer)
            return
        }

        if (isDanmoeum && (text == "ㅏ" || text == "ㅗ" || text == "ㅜ")) {
            if (text == lastVowelKey && currentHiraganaBuffer.isNotEmpty()) {
                val lastChar = currentHiraganaBuffer.takeLast(1)

                val mapA = mapOf(
                    "あ" to "や", "か" to "きゃ", "が" to "ぎゃ",
                    "さ" to "しゃ", "ざ" to "じゃ", "た" to "ちゃ",
                    "だ" to "ぢゃ", "な" to "にゃ", "は" to "ひゃ",
                    "ば" to "びゃ", "ぱ" to "ぴゃ", "ま" to "みゃ",
                    "ら" to "りゃ"
                )
                val mapO = mapOf(
                    "お" to "よ", "こ" to "きょ", "ご" to "ぎょ",
                    "そ" to "しょ", "ぞ" to "じょ", "と" to "ちょ",
                    "ど" to "ぢょ", "の" to "にょ", "ほ" to "ひょ",
                    "ぼ" to "びょ", "ぽ" to "ぴょ", "も" to "みょ",
                    "ろ" to "りょ"
                )
                val mapU = mapOf(
                    "う" to "ゆ", "く" to "きゅ", "ぐ" to "ぎゅ",
                    "す" to "しゅ", "ず" to "じゅ", "つ" to "ちゅ",
                    "づ" to "ぢゅ", "ぬ" to "にゅ", "ふ" to "ひゅ",
                    "ぶ" to "びゅ", "ぷ" to "ぴゅ", "む" to "みゅ",
                    "る" to "りゅ"
                )

                val replacement = when (text) {
                    "ㅏ" -> mapA[lastChar]
                    "ㅗ" -> mapO[lastChar]
                    "ㅜ" -> mapU[lastChar]
                    else -> null
                }

                if (replacement != null) {
                    currentInputConnection?.deleteSurroundingText(1, 0)
                    currentHiraganaBuffer = currentHiraganaBuffer.dropLast(1)

                    val finalReplacement =
                        if (isKatakanaMode) toKatakana(replacement) else replacement
                    currentInputConnection?.commitText(finalReplacement, 1)

                    currentHiraganaBuffer += replacement
                    lastCommittedLength = currentHiraganaBuffer.length
                    currentCandidateIndex = -1
                    updateCandidates(currentHiraganaBuffer)
                    lastVowelKey = ""
                    return
                }
            }
        }

        lastVowelKey =
            if (isDanmoeum && "ㅏㅓㅗㅜㅡㅣㅐㅔ".contains(text)) text else ""

        val (deleteCount, textToCommit) = converter.processInput(text)
        if (deleteCount > 0) {
            currentInputConnection?.deleteSurroundingText(deleteCount, 0)
            if (currentHiraganaBuffer.length >= deleteCount) {
                currentHiraganaBuffer =
                    currentHiraganaBuffer.dropLast(deleteCount)
            }
        }

        val finalCommit =
            if (isKatakanaMode) toKatakana(textToCommit) else textToCommit
        currentInputConnection?.commitText(finalCommit, 1)

        currentHiraganaBuffer += textToCommit
        lastCommittedLength = currentHiraganaBuffer.length
        currentCandidateIndex = -1
        updateCandidates(currentHiraganaBuffer)
    }

    private fun handleSpecialInput(text: String) {
        converter.flushPending()?.let {
            currentInputConnection?.deleteSurroundingText(it.first, 0)
            val finalPending = if (isKatakanaMode) toKatakana(it.second) else it.second
            currentInputConnection?.commitText(finalPending, 1)
        }
        currentInputConnection?.commitText(text, 1)
        clearCandidateBuffer()
    }

    private fun setButtonListeners(viewGroup: ViewGroup) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is ViewGroup) {
                setButtonListeners(child)
            } else if (child is Button) {
                if (child.id == R.id.btn_space) {
                    child.setOnLongClickListener {
                        showInputMethodPicker()
                        true
                    }
                }

                child.setOnClickListener {
                    child.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    when (child.id) {
                        R.id.btn_space -> handleSpaceAction()
                        R.id.btn_enter -> handleEnterAction()
                        R.id.btn_skin -> toggleSkinPicker()
                        R.id.btn_skin_mochi -> selectSkin(KeyboardSkin.MOCHI)
                        R.id.btn_skin_sakura -> selectSkin(KeyboardSkin.SAKURA)
                        R.id.btn_skin_blue -> selectSkin(KeyboardSkin.SEIGO_BLUE)
                        R.id.btn_skin_matcha -> selectSkin(KeyboardSkin.MATCHA_NIGHT)
                        R.id.btn_globe -> showInputMethodPicker()
                        R.id.btn_layout_toggle -> toggleLayoutMode()
                        R.id.btn_kana_toggle -> toggleKanaMode()
                        R.id.btn_symbol -> switchLayout(2)
                        R.id.btn_abc -> switchLayout(1)
                        else -> dispatchTextButton(child.text.toString())
                    }
                }
            }
        }
    }

    private fun dispatchTextButton(text: String) {
        when (text) {
            "⌫" -> handleBackspace()
            "⏎", "입력" -> handleEnterAction()
            "⇧", "⇪" -> handleShift()
            "두벌", "단모" -> toggleLayoutMode()
            "あ", "ア" -> toggleKanaMode()
            "!#1", "?123" -> switchLayout(2)
            "=\\<" -> switchLayout(3)
            "한글" -> switchLayout(1)
            "🌐", "언어" -> showInputMethodPicker()
            else -> handleNormalInput(text)
        }
    }

    private fun showInputMethodPicker() {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
            .showInputMethodPicker()
    }

    private fun toggleSkinPicker() {
        val panel = keyboardView?.findViewById<View>(R.id.skin_picker_panel) ?: return
        panel.visibility =
            if (panel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        updateModeStates()
    }

    private fun selectSkin(skin: KeyboardSkin) {
        currentSkin = skin
        SkinPreferences.save(this, skin)
        applySkin()
        keyboardView
            ?.findViewById<View>(R.id.skin_picker_panel)
            ?.visibility = View.GONE
        updateModeStates()
    }

    private fun toggleKanaMode() {
        isKatakanaMode = !isKatakanaMode
        keyboardView
            ?.findViewById<Button>(R.id.btn_kana_toggle)
            ?.text = if (isKatakanaMode) "ア" else "あ"

        if (currentHiraganaBuffer.isNotEmpty()) {
            val ic = currentInputConnection
            ic?.deleteSurroundingText(lastCommittedLength, 0)
            val convertedBuffer =
                if (isKatakanaMode) {
                    toKatakana(currentHiraganaBuffer)
                } else {
                    currentHiraganaBuffer
                }
            ic?.commitText(convertedBuffer, 1)
            lastCommittedLength = convertedBuffer.length
            currentCandidateIndex = -1
            highlightCandidateUI(-1)
        }
        updateModeStates()
    }

    private fun toggleLayoutMode() {
        isDanmoeum = !isDanmoeum
        keyboardView
            ?.findViewById<Button>(R.id.btn_layout_toggle)
            ?.text = if (isDanmoeum) "단모" else "두벌"
        switchLayout(1)
        clearCandidateBuffer()
        updateModeStates()
    }

    private fun handleShift() {
        isShifted = !isShifted
        updateShiftUI()
        converter.clearBuffer()
    }

    private fun updateShiftUI() {
        val view = keyboardView ?: return
        val shift = view.findViewById<Button>(R.id.btn_shift)
        val shiftDan = view.findViewById<Button>(R.id.btn_shift_dan)
        val q = view.findViewById<Button>(R.id.btn_key_q)
        val w = view.findViewById<Button>(R.id.btn_key_w)
        val e = view.findViewById<Button>(R.id.btn_key_e)
        val r = view.findViewById<Button>(R.id.btn_key_r)
        val t = view.findViewById<Button>(R.id.btn_key_t)

        if (isShifted) {
            shift?.text = "⇪"
            shiftDan?.text = "⇪"
            q?.text = "ㅃ"
            w?.text = "ㅉ"
            e?.text = "ㄸ"
            r?.text = "ㄲ"
            t?.text = "ㅆ"
        } else {
            shift?.text = "⇧"
            shiftDan?.text = "⇧"
            q?.text = "ㅂ"
            w?.text = "ㅈ"
            e?.text = "ㄷ"
            r?.text = "ㄱ"
            t?.text = "ㅅ"
        }
        updateModeStates()
    }

    private fun switchLayout(layoutId: Int) {
        val view = keyboardView ?: return
        val hangulDubeol =
            view.findViewById<LinearLayout>(R.id.layout_hangul_dubeol)
        val hangulDanmo =
            view.findViewById<LinearLayout>(R.id.layout_hangul_danmoeum)
        val symbol1 = view.findViewById<LinearLayout>(R.id.layout_symbol_1)
        val symbol2 = view.findViewById<LinearLayout>(R.id.layout_symbol_2)

        hangulDubeol.visibility =
            if (layoutId == 1 && !isDanmoeum) View.VISIBLE else View.GONE
        hangulDanmo.visibility =
            if (layoutId == 1 && isDanmoeum) View.VISIBLE else View.GONE
        symbol1.visibility = if (layoutId == 2) View.VISIBLE else View.GONE
        symbol2.visibility = if (layoutId == 3) View.VISIBLE else View.GONE

        view.findViewById<Button>(R.id.btn_symbol).visibility =
            if (layoutId == 1) View.VISIBLE else View.GONE
        view.findViewById<Button>(R.id.btn_globe).visibility =
            if (layoutId == 1) View.VISIBLE else View.GONE
        view.findViewById<Button>(R.id.btn_layout_toggle).visibility =
            if (layoutId == 1) View.VISIBLE else View.GONE
        view.findViewById<Button>(R.id.btn_kana_toggle).visibility =
            if (layoutId == 1) View.VISIBLE else View.GONE
        view.findViewById<Button>(R.id.btn_abc).visibility =
            if (layoutId != 1) View.VISIBLE else View.GONE

        if (layoutId == 2 || layoutId == 3) {
            view.findViewById<Button>(R.id.btn_abc)?.text = "한글"
        }
        if (layoutId == 1) {
            view.findViewById<Button>(R.id.btn_symbol)?.text = "!#1"
        }

        isHangulMode = layoutId == 1
        converter.clearBuffer()
        updateModeStates()
    }

    private fun applySkin() {
        val view = keyboardView ?: return
        view.setBackgroundColor(currentSkin.keyboardBackground)

        view.findViewById<View>(R.id.toolbar_row)
            ?.setBackgroundColor(currentSkin.keyboardBackground)
        view.findViewById<View>(R.id.candidate_scroll)
            ?.setBackgroundColor(currentSkin.keyboardBackground)
        view.findViewById<View>(R.id.skin_picker_panel)
            ?.setBackgroundColor(currentSkin.keyboardBackground)

        styleButtonsRecursively(view as ViewGroup)

        val candidateContainer =
            view.findViewById<LinearLayout>(R.id.candidate_layout)
        for (i in 0 until candidateContainer.childCount) {
            (candidateContainer.getChildAt(i) as? Button)?.let {
                styleCandidateButton(it, i == currentCandidateIndex)
            }
        }

        updateModeStates()
        updateSkinOptionStates()
    }

    private fun styleButtonsRecursively(group: ViewGroup) {
        for (i in 0 until group.childCount) {
            val child = group.getChildAt(i)
            if (child is ViewGroup) {
                styleButtonsRecursively(child)
            } else if (child is Button) {
                styleButtonByRole(child)
            }
        }
    }

    private fun styleButtonByRole(button: Button) {
        button.stateListAnimator = null
        button.isAllCaps = false
        button.includeFontPadding = false
        button.gravity = Gravity.CENTER
        button.minWidth = 0
        button.minimumWidth = 0
        button.minHeight = 0
        button.minimumHeight = 0

        val role = button.tag?.toString()
        when (role) {
            "accent" -> styleKey(
                button,
                currentSkin.enterColor,
                currentSkin.keyShadow,
                if (currentSkin.isDark) 0xFF142217.toInt() else Color.WHITE
            )
            "space" -> styleKey(
                button,
                currentSkin.spaceColor,
                currentSkin.keyShadow,
                currentSkin.textColor
            )
            "special", "mode", "skin_option" -> styleKey(
                button,
                currentSkin.specialKeyColor,
                currentSkin.keyShadow,
                currentSkin.textColor
            )
            "candidate" -> styleCandidateButton(button, false)
            else -> styleKey(
                button,
                currentSkin.keyColor,
                currentSkin.keyShadow,
                currentSkin.textColor
            )
        }
    }

    private fun updateModeStates() {
        val view = keyboardView ?: return
        styleModeButton(
            view.findViewById<Button>(R.id.btn_layout_toggle),
            active = true
        )
        styleModeButton(
            view.findViewById<Button>(R.id.btn_kana_toggle),
            active = isKatakanaMode
        )
        styleModeButton(
            view.findViewById<Button>(R.id.btn_shift),
            active = isShifted
        )
        styleModeButton(
            view.findViewById<Button>(R.id.btn_shift_dan),
            active = isShifted
        )
        styleModeButton(
            view.findViewById<Button>(R.id.btn_skin),
            active = view.findViewById<View>(R.id.skin_picker_panel)
                ?.visibility == View.VISIBLE
        )
    }

    private fun updateSkinOptionStates() {
        val view = keyboardView ?: return
        val options = listOf(
            R.id.btn_skin_mochi to KeyboardSkin.MOCHI,
            R.id.btn_skin_sakura to KeyboardSkin.SAKURA,
            R.id.btn_skin_blue to KeyboardSkin.SEIGO_BLUE,
            R.id.btn_skin_matcha to KeyboardSkin.MATCHA_NIGHT
        )
        options.forEach { (id, skin) ->
            styleModeButton(view.findViewById<Button>(id), active = skin == currentSkin)
        }
    }

    private fun styleModeButton(button: Button?, active: Boolean) {
        if (button == null) return
        if (active) {
            styleKey(
                button,
                currentSkin.activeColor,
                currentSkin.keyShadow,
                currentSkin.activeTextColor
            )
        } else {
            styleKey(
                button,
                currentSkin.specialKeyColor,
                currentSkin.keyShadow,
                currentSkin.textColor
            )
        }
        button.isSelected = active
    }

    private fun styleCandidateButton(button: Button, selected: Boolean) {
        val fill =
            if (selected) currentSkin.activeColor else currentSkin.candidateColor
        val text =
            if (selected) currentSkin.activeTextColor else currentSkin.textColor
        button.setTextColor(text)
        button.background = statefulRoundedBackground(
            normalColor = fill,
            shadowColor = currentSkin.keyShadow,
            radiusDp = 12f,
            shadowDp = 2
        )
    }

    private fun styleKey(
        button: Button,
        color: Int,
        shadowColor: Int,
        textColor: Int
    ) {
        button.setTextColor(textColor)
        button.background = statefulRoundedBackground(
            normalColor = color,
            shadowColor = shadowColor,
            radiusDp = 12f,
            shadowDp = 3
        )
    }

    private fun statefulRoundedBackground(
        normalColor: Int,
        shadowColor: Int,
        radiusDp: Float,
        shadowDp: Int
    ): StateListDrawable {
        val pressed = roundedDrawable(
            blend(normalColor, if (currentSkin.isDark) Color.WHITE else Color.BLACK, 0.10f),
            radiusDp
        )
        val normal = LayerDrawable(
            arrayOf(
                roundedDrawable(shadowColor, radiusDp),
                roundedDrawable(normalColor, radiusDp)
            )
        ).apply {
            setLayerInset(0, 0, dp(shadowDp), 0, 0)
            setLayerInset(1, 0, 0, 0, dp(shadowDp))
        }

        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), pressed)
            addState(intArrayOf(), normal)
        }
    }

    private fun roundedDrawable(color: Int, radiusDp: Float): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpFloat(radiusDp)
            setColor(color)
        }

    private fun blend(base: Int, overlay: Int, ratio: Float): Int {
        val inverse = 1f - ratio
        return Color.argb(
            Color.alpha(base),
            (Color.red(base) * inverse + Color.red(overlay) * ratio).toInt(),
            (Color.green(base) * inverse + Color.green(overlay) * ratio).toInt(),
            (Color.blue(base) * inverse + Color.blue(overlay) * ratio).toInt()
        )
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun dpFloat(value: Float): Float =
        value * resources.displayMetrics.density

    private fun getKoreanCharFromKeyCode(keyCode: Int): String? {
        return when (keyCode) {
            KeyEvent.KEYCODE_Q -> "ㅂ"
            KeyEvent.KEYCODE_W -> "ㅈ"
            KeyEvent.KEYCODE_E -> "ㄷ"
            KeyEvent.KEYCODE_R -> "ㄱ"
            KeyEvent.KEYCODE_T -> "ㅅ"
            KeyEvent.KEYCODE_Y -> "ㅛ"
            KeyEvent.KEYCODE_U -> "ㅕ"
            KeyEvent.KEYCODE_I -> "ㅑ"
            KeyEvent.KEYCODE_O -> "ㅐ"
            KeyEvent.KEYCODE_P -> "ㅔ"
            KeyEvent.KEYCODE_A -> "ㅁ"
            KeyEvent.KEYCODE_S -> "ㄴ"
            KeyEvent.KEYCODE_D -> "ㅇ"
            KeyEvent.KEYCODE_F -> "ㄹ"
            KeyEvent.KEYCODE_G -> "ㅎ"
            KeyEvent.KEYCODE_H -> "ㅗ"
            KeyEvent.KEYCODE_J -> "ㅓ"
            KeyEvent.KEYCODE_K -> "ㅏ"
            KeyEvent.KEYCODE_L -> "ㅣ"
            KeyEvent.KEYCODE_Z -> "ㅋ"
            KeyEvent.KEYCODE_X -> "ㅌ"
            KeyEvent.KEYCODE_C -> "ㅊ"
            KeyEvent.KEYCODE_V -> "ㅍ"
            KeyEvent.KEYCODE_B -> "ㅠ"
            KeyEvent.KEYCODE_N -> "ㅜ"
            KeyEvent.KEYCODE_M -> "ㅡ"
            else -> null
        }
    }
}
