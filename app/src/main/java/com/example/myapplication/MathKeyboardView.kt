package com.example.myapplication

import android.content.Context
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout

class MathKeyboardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var onInput: ((String) -> Unit)? = null
    var onBackspace: (() -> Unit)? = null
    var onClear: (() -> Unit)? = null
    var onEquals: (() -> Unit)? = null

    private var expanded = true
    private val funcBtns = arrayOfNulls<Button>(10)
    private val tabBtns = arrayOfNulls<Button>(4)
    private val toggleBtn: Button
    private val funcPanel: View

    companion object {
        private val FUNCS = arrayOf(
            arrayOf( // 三角
                "sin" to "sin(", "cos" to "cos(", "tan" to "tan(",
                "asin" to "asin(", "acos" to "acos(", "atan" to "atan(",
                "sinh" to "sinh(", "cosh" to "cosh(", "tanh" to "tanh(",
                "deg→rad" to "*pi/180"
            ),
            arrayOf( // 对数
                "log" to "log(", "ln" to "ln(", "lg" to "log10(",
                "log₂" to "log2(", "exp" to "e^", "10^x" to "10^",
                "√" to "sqrt(", "∛" to "cbrt(", "|x|" to "abs(",
                "x²" to "^2"
            ),
            arrayOf( // 幂根
                "√" to "sqrt(", "∛" to "cbrt(", "x²" to "^2",
                "x³" to "^3", "xʸ" to "^", "1/x" to "1/",
                "n!" to "!", "%" to "%", "mod" to "%",
                "²√" to "sqrt("
            ),
            arrayOf( // 符号
                "π" to "pi", "e" to "e", "i" to "i",
                "|x|" to "abs(", "( )" to "()", "°" to "*pi/180",
                "Ans" to "", "const" to "2.71828", "x" to "x",
                "y" to "y"
            )
        )
    }

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.layout_math_keyboard, this, true)

        toggleBtn = findViewById(R.id.kb_toggle)
        funcPanel = findViewById(R.id.kb_func_panel)

        (1..10).forEach { i ->
            val id = resources.getIdentifier("kb_fn_$i", "id", context.packageName)
            funcBtns[i - 1] = findViewById(id)
        }
        (1..4).forEach { i ->
            val id = resources.getIdentifier("kb_tab_$i", "id", context.packageName)
            tabBtns[i - 1] = findViewById(id)
        }

        wireTabs()
        wireToggle()
        wireNumbers()
        wireSpecial()
        applyTab(0)
    }

    private fun wireTabs() {
        tabBtns.forEachIndexed { index, button ->
            button?.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                applyTab(index)
            }
        }
    }

    private fun applyTab(idx: Int) {
        val funcs = FUNCS[idx]
        funcBtns.forEachIndexed { i, button ->
            if (button == null) return@forEachIndexed
            if (i < funcs.size) {
                button.visibility = View.VISIBLE
                button.text = funcs[i].first
                val insert = funcs[i].second
                button.setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onInput?.invoke(insert)
                }
            } else {
                button.visibility = View.GONE
            }
        }
        tabBtns.forEachIndexed { i, button ->
            button?.alpha = if (i == idx) 1f else 0.5f
        }
    }

    private fun wireToggle() {
        toggleBtn.setOnClickListener {
            expanded = !expanded
            funcPanel.visibility = if (expanded) View.VISIBLE else View.GONE
            toggleBtn.text = if (expanded) "▲" else "▼"
        }
    }

    private fun wireNumbers() {
        mapOf(
            R.id.kb_7 to "7", R.id.kb_8 to "8", R.id.kb_9 to "9",
            R.id.kb_div to "÷", R.id.kb_4 to "4", R.id.kb_5 to "5",
            R.id.kb_6 to "6", R.id.kb_mul to "×", R.id.kb_1 to "1",
            R.id.kb_2 to "2", R.id.kb_3 to "3", R.id.kb_sub to "−",
            R.id.kb_0 to "0", R.id.kb_dot to ".",
            R.id.kb_lparen to "(", R.id.kb_rparen to ")",
            R.id.kb_add to "+"
        ).forEach { (id, text) ->
            findViewById<Button>(id)?.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onInput?.invoke(text)
            }
        }
    }

    private fun wireSpecial() {
        findViewById<Button>(R.id.kb_backspace)?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onBackspace?.invoke()
        }
        findViewById<Button>(R.id.kb_ac)?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onClear?.invoke()
        }
        findViewById<Button>(R.id.kb_equals)?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onEquals?.invoke()
        }
    }
}
