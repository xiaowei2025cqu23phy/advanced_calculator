package com.example.myapplication

import android.content.Context
import android.graphics.Canvas
import android.graphics.Typeface
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View

/**
 * Renders a pretty-printed math expression using Canvas.
 * Converts calculator syntax (sin, sqrt, pi, ^) into display notation.
 */
class LatexPreviewView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val textPaint = TextPaint().apply {
        color = 0xCCFFFFFF.toInt()
        textSize = 32f
        isAntiAlias = true
        typeface = Typeface.MONOSPACE
    }
    private var expression = ""
    private var segments = mutableListOf<Segment>()

    fun setExpression(expr: String?) {
        this.expression = expr ?: ""
        this.segments = tokenize(this.expression)
        postInvalidate()
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        var x = paddingLeft + 8f
        val y = paddingTop + 36f

        for (seg in segments) {
            textPaint.textSize = if (seg.isLarge) 36f else 28f
            textPaint.color = seg.color
            textPaint.isFakeBoldText = seg.bold
            c.drawText(seg.text, x, y, textPaint)
            x += textPaint.measureText(seg.text)

            // Handle superscript
            if (seg.isSuperscript && seg.childText != null) {
                val oldSize = textPaint.textSize
                textPaint.textSize = 20f
                c.drawText(seg.childText!!, x, y - 12f, textPaint)
                x += textPaint.measureText(seg.childText!!)
                textPaint.textSize = oldSize
            }

            // Handle radical bar
            if (seg.isRadical && seg.childText != null) {
                val barY = y - 28f
                val barW = textPaint.measureText(seg.childText!!) + 12f
                c.drawLine(x, barY, x + barW, barY, textPaint)
                val innerY = y - 22f
                c.drawLine(x, innerY, x, barY, textPaint)
                c.drawText(seg.childText!!, x + 6f, y, textPaint)
                x += barW
            }
        }
    }

    private fun tokenize(expr: String): MutableList<Segment> {
        val result = mutableListOf<Segment>()
        var i = 0
        while (i < expr.length) {
            val ch = expr[i]

            // Numbers
            if (ch.isDigit() || ch == '.') {
                var end = i
                while (end < expr.length && (expr[end].isDigit() || expr[end] == '.'))
                    end++
                result.add(Segment(expr.substring(i, end), 0xCCFFFFFF.toInt()))
                i = end
                continue
            }

            // Variables
            if (ch == 'x' || ch == 'y' || ch == 't') {
                result.add(Segment(ch.toString(), 0xFFFF9F0A.toInt(), bold = true, isLarge = false))
                i++
                continue
            }

            // Greek letters
            if (expr.startsWith("pi", i)) {
                result.add(Segment("π", 0xCCFFFFFF.toInt(), bold = false, isLarge = true))
                i += 2
                continue
            }
            if (expr.startsWith("θ", i)) {
                result.add(Segment("θ", 0xFFFF9F0A.toInt(), bold = true, isLarge = true))
                i++
                continue
            }

            // Functions
            val funcs = arrayOf("sin", "cos", "tan", "log", "ln", "sqrt", "abs")
            var matched = false
            for (func in funcs) {
                if (expr.startsWith("$func(", i)) {
                    result.add(Segment(func, 0xCC6CB4FF.toInt(), bold = false, isLarge = false))
                    result.add(Segment("(", 0xCCFFFFFF.toInt()))
                    i += func.length
                    matched = true
                    break
                }
            }
            if (matched) continue

            // Operators
            when (ch) {
                '+', '−', '×', '÷' -> {
                    result.add(Segment(ch.toString(), 0xCCFFFFFF.toInt(), bold = false, isLarge = true))
                    i++
                }
                '^' -> {
                    // Superscript: collect next token
                    var next = i + 1
                    val sup = StringBuilder()
                    while (next < expr.length && (expr[next].isDigit() || expr[next] == '(' || expr[next] == ')'))
                        sup.append(expr[next++])
                    val seg = Segment("", 0xCCFFFFFF.toInt())
                    seg.isSuperscript = true
                    seg.childText = sup.toString().replace("(", "").replace(")", "")
                    result.add(seg)
                    i = next
                }
                '(', ')' -> {
                    result.add(Segment(ch.toString(), 0xAAFFFFFF.toInt()))
                    i++
                }
                ',' -> {
                    result.add(Segment(", ", 0xAAFFFFFF.toInt()))
                    i++
                }
                else -> {
                    result.add(Segment(ch.toString(), 0x88FFFFFF.toInt()))
                    i++
                }
            }
        }
        return result
    }

    private class Segment(
        var text: String,
        var color: Int,
        var bold: Boolean = false,
        var isLarge: Boolean = false
    ) {
        var isSuperscript: Boolean = false
        var isRadical: Boolean = false
        var childText: String? = null
    }
}
