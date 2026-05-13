package com.example.myapplication;

import android.view.View;
import android.widget.Button;

/**
 * Wires up the compact math keyboard (layout_math_keyboard.xml).
 * Uses R.id references (compile-time safe) to find buttons.
 */
public class MathKeyboardHelper {

    private final View root;
    private final InputTarget target;
    private boolean expanded = true;
    private final Button[] funcBtns = new Button[10];
    private final Button[] tabBtns = new Button[4];
    private final Button toggleBtn;
    private final View funcPanel;

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

    public MathKeyboardHelper(View root, InputTarget inputTarget) {
        this.root = root;
        this.target = inputTarget;

        funcBtns[0] = root.findViewById(R.id.kb_fn_1);
        funcBtns[1] = root.findViewById(R.id.kb_fn_2);
        funcBtns[2] = root.findViewById(R.id.kb_fn_3);
        funcBtns[3] = root.findViewById(R.id.kb_fn_4);
        funcBtns[4] = root.findViewById(R.id.kb_fn_5);
        funcBtns[5] = root.findViewById(R.id.kb_fn_6);
        funcBtns[6] = root.findViewById(R.id.kb_fn_7);
        funcBtns[7] = root.findViewById(R.id.kb_fn_8);
        funcBtns[8] = root.findViewById(R.id.kb_fn_9);
        funcBtns[9] = root.findViewById(R.id.kb_fn_10);

        tabBtns[0] = root.findViewById(R.id.kb_tab_1);
        tabBtns[1] = root.findViewById(R.id.kb_tab_2);
        tabBtns[2] = root.findViewById(R.id.kb_tab_3);
        tabBtns[3] = root.findViewById(R.id.kb_tab_4);

        toggleBtn = root.findViewById(R.id.kb_toggle);
        funcPanel = root.findViewById(R.id.kb_func_panel);

        // Only set up if all critical views exist
        if (funcBtns[0] == null) return; // keyboard not found in layout

        wireTabs();
        wireToggle();
        wireNumbers();
        applyTab(0);
    }

    private void wireTabs() {
        for (int i = 0; i < tabBtns.length; i++) {
            final int t = i;
            if (tabBtns[i] != null) tabBtns[i].setOnClickListener(v -> {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                applyTab(t);
            });
        }
    }

    private void applyTab(int idx) {
        String[][] funcs = FUNCS[idx];
        for (int i = 0; i < funcBtns.length; i++) {
            if (funcBtns[i] == null) continue;
            if (i < funcs.length) {
                funcBtns[i].setVisibility(View.VISIBLE);
                funcBtns[i].setText(funcs[i][0]);
                final String insert = funcs[i][1];
                funcBtns[i].setOnClickListener(v -> {
                    v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                    target.append(insert);
                });
            } else {
                funcBtns[i].setVisibility(View.GONE);
            }
        }
        for (int i = 0; i < tabBtns.length; i++) {
            if (tabBtns[i] != null) tabBtns[i].setAlpha(i == idx ? 1f : 0.5f);
        }
    }

    private void wireToggle() {
        if (toggleBtn == null || funcPanel == null) return;
        toggleBtn.setOnClickListener(v -> {
            expanded = !expanded;
            funcPanel.setVisibility(expanded ? View.VISIBLE : View.GONE);
            toggleBtn.setText(expanded ? "▲" : "▼");
        });
    }

    private void wireNumbers() {
        wire(R.id.kb_7, "7"); wire(R.id.kb_8, "8"); wire(R.id.kb_9, "9");
        wire(R.id.kb_div, "÷"); wire(R.id.kb_4, "4"); wire(R.id.kb_5, "5");
        wire(R.id.kb_6, "6"); wire(R.id.kb_mul, "×"); wire(R.id.kb_1, "1");
        wire(R.id.kb_2, "2"); wire(R.id.kb_3, "3"); wire(R.id.kb_sub, "−");
        wire(R.id.kb_0, "0"); wire(R.id.kb_dot, ".");
        wire(R.id.kb_lparen, "("); wire(R.id.kb_rparen, ")");
        wire(R.id.kb_add, "+");
    }

    private void wire(int id, final String text) {
        Button btn = root.findViewById(id);
        if (btn != null) btn.setOnClickListener(v -> {
            btn.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
            target.append(text);
        });
    }

    public Button getBackspaceButton() { return root.findViewById(R.id.kb_backspace); }
    public Button getAcButton()        { return root.findViewById(R.id.kb_ac); }
    public Button getEqualsButton()    { return root.findViewById(R.id.kb_equals); }
}
