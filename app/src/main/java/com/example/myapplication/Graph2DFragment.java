package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.myapplication.databinding.FragmentGraph2dBinding;

import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Expression;

import java.util.ArrayList;
import java.util.List;

public class Graph2DFragment extends Fragment {

    private FragmentGraph2dBinding binding;
    private boolean parametricMode = false;
    private EditText focusedInput;

    private static final int[] COLORS = {
        0xFF2196F3, 0xFFF44336, 0xFF4CAF50, 0xFFFF9800,
        0xFF9C27B0, 0xFF795548, 0xFF607D8B, 0xFFFFEB3B
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentGraph2dBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupChart();

        // Inflate keyboard programmatically
        View kbView = getLayoutInflater().inflate(R.layout.layout_math_keyboard,
            binding.kbContainer, true);
        MathKeyboardHelper kb = new MathKeyboardHelper(kbView, text -> {
            if (focusedInput != null) focusedInput.append(text);
        });
        if (kbView.findViewById(R.id.kb_7) != null) { // verify keyboard loaded
            kb.getEqualsButton().setOnClickListener(v -> plotAllGraphs());
            kb.getBackspaceButton().setOnClickListener(v -> {
                if (focusedInput != null) {
                    String t = focusedInput.getText().toString();
                    if (!t.isEmpty()) focusedInput.setText(t.substring(0, t.length() - 1));
                }
            });
            kb.getAcButton().setOnClickListener(v -> {
                if (focusedInput != null) focusedInput.setText("");
            });
        }

        binding.btnAddFunction.setOnClickListener(v -> addFunctionInput());
        binding.btnPlot2d.setOnClickListener(v -> plotAllGraphs());
        binding.btnClearAll.setOnClickListener(v -> clearAllFunctions());
        binding.btnModeNormal.setOnClickListener(v -> setMode(false));
        binding.btnModeParam.setOnClickListener(v -> setMode(true));

        updateModeButtons();
        addFunctionInput();
    }

    private void setMode(boolean param) {
        if (parametricMode == param) return;
        parametricMode = param;
        updateModeButtons();
        binding.layoutTRange.setVisibility(param ? View.VISIBLE : View.GONE);
        clearAllFunctions();
    }

    private void updateModeButtons() {
        binding.btnModeNormal.setAlpha(parametricMode ? 0.4f : 1f);
        binding.btnModeParam.setAlpha(parametricMode ? 1f : 0.4f);
    }

    private void setupChart() {
        binding.chart2d.setBounds(-10, 10, -10, 10);
    }

    private void addFunctionInput() {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        row.setPadding(0, 2, 0, 2);

        if (parametricMode) {
            row.addView(makeEditText("x(t)"));
            row.addView(makeEditText("y(t)"));
        } else {
            row.addView(makeEditText("f(x)"));
        }
        Button del = new Button(getContext());
        del.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
        del.setText("×");
        del.setTextColor(0xFFFF453A);
        del.setBackground(null);
        del.setOnClickListener(v -> ((ViewGroup) row.getParent()).removeView(row));
        row.addView(del);
        binding.functionsContainer.addView(row);
    }

    private EditText makeEditText(String hint) {
        EditText et = new EditText(getContext());
        et.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        et.setHint(hint); et.setTextSize(13f);
        et.setTextColor(0xFFFFFFFF); et.setHintTextColor(0x88FFFFFF);
        et.setBackground(null); et.setSingleLine(true);
        et.setId(View.generateViewId());
        et.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) focusedInput = (EditText) v; });
        return et;
    }

    private void plotAllGraphs() {
        if (parametricMode) plotParametric();
        else plotNormal();
    }

    private void plotNormal() {
        float xMin, xMax, step;
        try {
            xMin = Float.parseFloat(binding.etXMin.getText().toString());
            xMax = Float.parseFloat(binding.etXMax.getText().toString());
            step = Float.parseFloat(binding.etStep.getText().toString());
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "范围无效", Toast.LENGTH_SHORT).show(); return;
        }
        if (xMin >= xMax || step <= 0) {
            Toast.makeText(getContext(), "范围不正确", Toast.LENGTH_SHORT).show(); return;
        }

        List<String> funcs = new ArrayList<>();
        for (int i = 0; i < binding.functionsContainer.getChildCount(); i++) {
            View c = binding.functionsContainer.getChildAt(i);
            if (c instanceof LinearLayout) {
                String txt = ((EditText) ((LinearLayout) c).getChildAt(0)).getText().toString().trim();
                if (!txt.isEmpty()) funcs.add(txt);
            }
        }
        if (funcs.isEmpty()) { Toast.makeText(getContext(), "请添加函数", Toast.LENGTH_SHORT).show(); return; }

        binding.chart2d.clearCurves();
        int ci = 0;
        for (String f : funcs) {
            List<float[]> pts = eval(f, xMin, xMax, step);
            if (!pts.isEmpty()) {
                binding.chart2d.addCurve(pts, COLORS[ci % COLORS.length], "f" + (ci+1));
                ci++;
            }
        }
        if (ci == 0) { Toast.makeText(getContext(), "无法绘制", Toast.LENGTH_SHORT).show(); return; }
    }

    private List<float[]> eval(String expr, float min, float max, float step) {
        List<float[]> pts = new ArrayList<>();
        try {
            Argument x = new Argument("x");
            Expression e = new Expression(expr, x);
            if (!e.checkSyntax()) return pts;
            for (double v = min; v <= max + step * 0.5; v += step) {
                x.setArgumentValue(v);
                double y = e.calculate();
                if (!Double.isNaN(y) && !Double.isInfinite(y))
                    pts.add(new float[]{(float)v, (float)y});
            }
        } catch (Exception ignored) {}
        return pts;
    }

    private void plotParametric() {
        float tMin, tMax, tStep;
        try {
            tMin = Float.parseFloat(binding.etTMin.getText().toString());
            tMax = Float.parseFloat(binding.etTMax.getText().toString());
            tStep = Float.parseFloat(binding.etTStep.getText().toString());
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "t范围无效", Toast.LENGTH_SHORT).show(); return;
        }
        if (tMin >= tMax || tStep <= 0) {
            Toast.makeText(getContext(), "范围不正确", Toast.LENGTH_SHORT).show(); return;
        }

        List<String[]> funcs = new ArrayList<>();
        for (int i = 0; i < binding.functionsContainer.getChildCount(); i++) {
            View c = binding.functionsContainer.getChildAt(i);
            if (c instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) c;
                if (row.getChildCount() >= 3) {
                    String xs = ((EditText) row.getChildAt(0)).getText().toString().trim();
                    String ys = ((EditText) row.getChildAt(1)).getText().toString().trim();
                    if (!xs.isEmpty() && !ys.isEmpty()) funcs.add(new String[]{xs, ys});
                }
            }
        }
        if (funcs.isEmpty()) { Toast.makeText(getContext(), "请添加参数方程", Toast.LENGTH_SHORT).show(); return; }

        binding.chart2d.clearCurves();
        int ci = 0;
        for (String[] p : funcs) {
            List<float[]> pts = evalParam(p[0], p[1], tMin, tMax, tStep);
            if (!pts.isEmpty()) {
                binding.chart2d.addCurve(pts, COLORS[ci % COLORS.length], "C" + (ci+1));
                ci++;
            }
        }
        if (ci == 0) { Toast.makeText(getContext(), "无法绘制", Toast.LENGTH_SHORT).show(); }
    }

    private List<float[]> evalParam(String xs, String ys, float tMin, float tMax, float tStep) {
        List<float[]> pts = new ArrayList<>();
        try {
            Argument t = new Argument("t");
            Expression ex = new Expression(xs, t);
            Expression ey = new Expression(ys, t);
            for (double v = tMin; v <= tMax + tStep * 0.5; v += tStep) {
                t.setArgumentValue(v);
                double x = ex.calculate(), y = ey.calculate();
                if (!Double.isNaN(x) && !Double.isInfinite(x) && !Double.isNaN(y) && !Double.isInfinite(y))
                    pts.add(new float[]{(float)x, (float)y});
            }
        } catch (Exception ignored) {}
        return pts;
    }

    private void clearAllFunctions() {
        binding.functionsContainer.removeAllViews();
        binding.chart2d.clearCurves();
        addFunctionInput();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
