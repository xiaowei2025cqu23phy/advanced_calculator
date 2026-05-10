package com.example.myapplication;

import android.view.View;
import android.widget.Button;

/**
 * Wires up the compact math keyboard.  Two function rows, collapsible, tab-based.
 */
public class MathKeyboardHelper {

    private final View root;
    private final InputTarget target;
    private int currentTab = 0;
    private boolean expanded = false;
    private final Button[] funcBtns = new Button[10];
    private final Button[] tabBtns = new Button[4];

    // {display, inserted text}
    private static final String[][][] FUNCS = {
        { // 三角
            {"sin", "sin("}, {"cos", "cos("}, {"tan", "tan("},
            {"asin", "asin("}, {"acos", "acos("}, {"atan", "atan("},
            {"sinh", "sinh("}, {"cosh", "cosh("}, {"tanh", "tanh("},
            {"deg→rad", "*pi/180"},
        },
        { // 对数
            {"log", "log("}, {"ln", "ln("}, {"lg", "log10("},
            {"log₂", "log2("}, {"exp", "e^"}, {"10^x", "10^"},
            {"√", "sqrt("}, {"∛", "cbrt("}, {"|x|", "abs("},
            {"x²", "^2"},
        },
        { // 幂根
            {"√", "sqrt("}, {"∛", "cbrt("}, {"x²", "^2"},
            {"x³", "^3"}, {"xʸ", "^"}, {"1/x", "1/"},
            {"n!", "!"}, {"%", "%"}, {"mod", "%"},
            {"²√", "sqrt("},
        },
        { // 符号
            {"π", "pi"}, {"e", "e"}, {"i", "i"},
            {"|x|", "abs("}, {"( )", "()"}, {"°", "*pi/180"},
            {"Ans", ""}, {"const", "2.71828"}, {"x", "x"},
            {"y", "y"},
        },
    };

    public interface InputTarget {
        void append(String text);
    }

    public MathKeyboardHelper(View keyboardRoot, InputTarget inputTarget) {
        this.root = keyboardRoot;
        this.target = inputTarget;

        for (int i = 0; i < 10; i++) {
            funcBtns[i] = root.findViewById(
                root.getResources().getIdentifier("kb_fn_" + (i+1), "id", root.getContext().getPackageName()));
        }
        for (int i = 0; i < 4; i++) {
            tabBtns[i] = root.findViewById(
                root.getResources().getIdentifier("kb_tab_" + (i+1), "id", root.getContext().getPackageName()));
        }

        wireTabs();
        wireToggle();
        wireNumbers();
        applyTab(0);
    }

    private void wireTabs() {
        for (int i = 0; i < tabBtns.length; i++) {
            final int t = i;
            tabBtns[i].setOnClickListener(v -> applyTab(t));
        }
    }

    private void applyTab(int idx) {
        currentTab = idx;
        String[][] funcs = FUNCS[idx];
        for (int i = 0; i < funcBtns.length; i++) {
            if (i < funcs.length) {
                funcBtns[i].setVisibility(View.VISIBLE);
                funcBtns[i].setText(funcs[i][0]);
                final String insert = funcs[i][1];
                funcBtns[i].setOnClickListener(v -> target.append(insert));
            } else {
                funcBtns[i].setVisibility(View.GONE);
            }
        }
        for (int i = 0; i < tabBtns.length; i++) {
            tabBtns[i].setAlpha(i == idx ? 1f : 0.5f);
        }
    }

    private void wireToggle() {
        View panel = root.findViewById(
            root.getResources().getIdentifier("kb_func_panel", "id", root.getContext().getPackageName()));
        Button toggle = root.findViewById(
            root.getResources().getIdentifier("kb_toggle", "id", root.getContext().getPackageName()));

        toggle.setOnClickListener(v -> {
            expanded = !expanded;
            panel.setVisibility(expanded ? View.VISIBLE : View.GONE);
            toggle.setText(expanded ? "▼" : "▲");
        });
    }

    public boolean isExpanded() { return expanded; }

    private void wireNumbers() {
        wire("kb_7", "7"); wire("kb_8", "8"); wire("kb_9", "9");
        wire("kb_div", "÷"); wire("kb_4", "4"); wire("kb_5", "5");
        wire("kb_6", "6"); wire("kb_mul", "×"); wire("kb_1", "1");
        wire("kb_2", "2"); wire("kb_3", "3"); wire("kb_sub", "−");
        wire("kb_0", "0"); wire("kb_dot", ".");
        wire("kb_lparen", "("); wire("kb_rparen", ")");
        wire("kb_add", "+");
    }

    private void wire(String idStr, final String text) {
        Button btn = root.findViewById(
            root.getResources().getIdentifier(idStr, "id", root.getContext().getPackageName()));
        if (btn != null) btn.setOnClickListener(v -> target.append(text));
    }

    // ── public overrides ──
    public Button getBackspaceButton() {
        return root.findViewById(
            root.getResources().getIdentifier("kb_backspace", "id", root.getContext().getPackageName()));
    }
    public Button getAcButton() {
        return root.findViewById(
            root.getResources().getIdentifier("kb_ac", "id", root.getContext().getPackageName()));
    }
    public Button getEqualsButton() {
        return root.findViewById(
            root.getResources().getIdentifier("kb_equals", "id", root.getContext().getPackageName()));
    }
}
