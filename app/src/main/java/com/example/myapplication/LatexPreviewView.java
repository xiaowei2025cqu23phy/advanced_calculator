package com.example.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders a pretty-printed math expression using Canvas.
 * Converts calculator syntax (sin, sqrt, pi, ^) into display notation.
 */
public class LatexPreviewView extends View {

    private final TextPaint textPaint = new TextPaint();
    private String expression = "";
    private List<Segment> segments = new ArrayList<>();

    public LatexPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        textPaint.setColor(0xCCFFFFFF);
        textPaint.setTextSize(32f);
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(Typeface.MONOSPACE);
    }

    public void setExpression(String expr) {
        this.expression = expr != null ? expr : "";
        this.segments = tokenize(this.expression);
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        float x = getPaddingLeft() + 8;
        float y = getPaddingTop() + 36f;

        for (Segment seg : segments) {
            textPaint.setTextSize(seg.isLarge ? 36f : 28f);
            textPaint.setColor(seg.color);
            textPaint.setFakeBoldText(seg.bold);
            c.drawText(seg.text, x, y, textPaint);
            x += textPaint.measureText(seg.text);

            // Handle superscript
            if (seg.isSuperscript && seg.childText != null) {
                float oldSize = textPaint.getTextSize();
                textPaint.setTextSize(20f);
                c.drawText(seg.childText, x, y - 12, textPaint);
                x += textPaint.measureText(seg.childText);
                textPaint.setTextSize(oldSize);
            }

            // Handle radical bar
            if (seg.isRadical && seg.childText != null) {
                float barY = y - 28f;
                float barW = textPaint.measureText(seg.childText) + 12;
                c.drawLine(x, barY, x + barW, barY, textPaint);
                float innerY = y - 22f;
                c.drawLine(x, innerY, x, barY, textPaint);
                c.drawText(seg.childText, x + 6, y, textPaint);
                x += barW;
            }
        }
    }

    private List<Segment> tokenize(String expr) {
        List<Segment> result = new ArrayList<>();
        int i = 0;
        while (i < expr.length()) {
            char ch = expr.charAt(i);

            // Numbers
            if (Character.isDigit(ch) || ch == '.') {
                int end = i;
                while (end < expr.length() && (Character.isDigit(expr.charAt(end)) || expr.charAt(end) == '.'))
                    end++;
                result.add(new Segment(expr.substring(i, end), 0xCCFFFFFF));
                i = end;
                continue;
            }

            // Variables
            if (ch == 'x' || ch == 'y' || ch == 't') {
                result.add(new Segment(String.valueOf(ch), 0xFFFF9F0A, true, false));
                i++;
                continue;
            }

            // Greek letters
            if (expr.startsWith("pi", i)) {
                result.add(new Segment("π", 0xCCFFFFFF, false, true));
                i += 2;
                continue;
            }
            if (expr.startsWith("θ", i)) {
                result.add(new Segment("θ", 0xFFFF9F0A, true, true));
                i++;
                continue;
            }

            // Functions
            String[] funcs = {"sin", "cos", "tan", "log", "ln", "sqrt", "abs"};
            boolean matched = false;
            for (String func : funcs) {
                if (expr.startsWith(func + "(", i)) {
                    result.add(new Segment(func, 0xCC6CB4FF, false, false));
                    result.add(new Segment("(", 0xCCFFFFFF));
                    i += func.length();
                    matched = true;
                    break;
                }
            }
            if (matched) continue;

            // Operators
            switch (ch) {
                case '+': case '−': case '×': case '÷':
                    result.add(new Segment(String.valueOf(ch), 0xCCFFFFFF, false, true));
                    i++;
                    continue;
                case '^':
                    // Superscript: collect next token
                    int next = i + 1;
                    StringBuilder sup = new StringBuilder();
                    while (next < expr.length() && (Character.isDigit(expr.charAt(next)) || expr.charAt(next) == '(' || expr.charAt(next) == ')'))
                        sup.append(expr.charAt(next++));
                    Segment seg = new Segment("", 0xCCFFFFFF);
                    seg.isSuperscript = true;
                    seg.childText = sup.toString().replace("(", "").replace(")", "");
                    result.add(seg);
                    i = next;
                    continue;
                case '(': case ')':
                    result.add(new Segment(String.valueOf(ch), 0xAAFFFFFF));
                    i++;
                    continue;
                case ',':
                    result.add(new Segment(", ", 0xAAFFFFFF));
                    i++;
                    continue;
                default:
                    result.add(new Segment(String.valueOf(ch), 0x88FFFFFF));
                    i++;
                    break;
            }
        }
        return result;
    }

    static class Segment {
        String text;
        int color;
        boolean bold;
        boolean isLarge;
        boolean isSuperscript;
        boolean isRadical;
        String childText;

        Segment(String t, int c) { text = t; color = c; }
        Segment(String t, int c, boolean b, boolean l) { text = t; color = c; bold = b; isLarge = l; }
    }
}
