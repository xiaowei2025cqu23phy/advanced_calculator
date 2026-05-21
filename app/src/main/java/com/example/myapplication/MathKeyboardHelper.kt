package com.example.myapplication

import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Button

/**
 * Functional interface for input callbacks, better for Java interop.
 */
fun interface OnInputListener {
    fun onInput(text: String)
}

/**
 * Modernized Kotlin MathKeyboardHelper with haptic feedback.
 */
class MathKeyboardHelper(
    private val root: View,
    private val onInput: OnInputListener
) {
    private var expanded = true
    private val funcBtns = arrayOfNulls<Button>(10)
    private val tabBtns = arrayOfNulls<Button>(4)
    private val toggleBtn: Button? = root.findViewById(R.id.kb_toggle)
    private val funcPanel: View? = root.findViewById(R.id.kb_func_panel)

    val backspaceButton: Button? get() = root.findViewById(R.id.kb_backspace)
    val acButton: Button? get() = root.findViewById(R.id.kb_ac)
    val equalsButton: Button? get() = root.findViewById(R.id.kb_equals)

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
        (1..10).forEach { i ->
            val id = root.resources.getIdentifier("kb_fn_$i", "id", root.context.packageName)
            funcBtns[i - 1] = root.findViewById(id)
        }
        (1..4).forEach { i ->
            val id = root.resources.getIdentifier("kb_tab_$i", "id", root.context.packageName)
            tabBtns[i - 1] = root.findViewById(id)
        }

        wireTabs()
        wireToggle()
        wireNumbers()
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
                    onInput.onInput(insert)
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
        toggleBtn?.setOnClickListener {
            expanded = !expanded
            funcPanel?.visibility = if (expanded) View.VISIBLE else View.GONE
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
            root.findViewById<Button>(id)?.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onInput.onInput(text)
            }
        }
    }
}
