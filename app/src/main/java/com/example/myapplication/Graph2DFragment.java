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
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

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

        // Keyboard (use findViewById for reliability with included layouts)
        View kbView = view.findViewById(R.id.kb_root);
        MathKeyboardHelper kb = null;
        if (kbView != null) {
            kb = new MathKeyboardHelper(kbView, text -> {
                if (focusedInput != null) focusedInput.append(text);
            });
            final MathKeyboardHelper kbRef = kb;
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

        // Mode toggle
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
        binding.chart2d.getDescription().setEnabled(false);
        binding.chart2d.setTouchEnabled(true);
        binding.chart2d.setDragEnabled(true);
        binding.chart2d.setScaleEnabled(true);
        binding.chart2d.setPinchZoom(true);
        binding.chart2d.setDrawGridBackground(false);
        binding.chart2d.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        binding.chart2d.getAxisRight().setEnabled(false);
    }

    private void addFunctionInput() {
        LinearLayout container = binding.functionsContainer;
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        row.setPadding(0, 2, 0, 2);

        if (parametricMode) {
            EditText xt = makeEditText("x(t)");
            EditText yt = makeEditText("y(t)");
            Button del = makeDeleteButton(row);
            row.addView(xt); row.addView(yt); row.addView(del);
        } else {
            EditText et = makeEditText("f(x)");
            Button del = makeDeleteButton(row);
            row.addView(et); row.addView(del);
        }
        container.addView(row);
    }

    private EditText makeEditText(String hint) {
        EditText et = new EditText(getContext());
        et.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        et.setHint(hint);
        et.setTextSize(13f);
        et.setTextColor(0xFFFFFFFF);
        et.setHintTextColor(0x88FFFFFF);
        et.setBackground(null);
        et.setSingleLine(true);
        et.setId(View.generateViewId());
        et.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) focusedInput = (EditText) v;
        });
        return et;
    }

    private Button makeDeleteButton(ViewGroup row) {
        Button b = new Button(getContext());
        b.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
        b.setText("×");
        b.setTextColor(0xFFFF453A);
        b.setBackground(null);
        b.setOnClickListener(v -> ((ViewGroup) row.getParent()).removeView(row));
        return b;
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
        if (xMin >= xMax || step <= 0) { Toast.makeText(getContext(), "范围设置不正确", Toast.LENGTH_SHORT).show(); return; }

        List<String> funcs = new ArrayList<>();
        for (int i = 0; i < binding.functionsContainer.getChildCount(); i++) {
            View c = binding.functionsContainer.getChildAt(i);
            if (c instanceof LinearLayout) {
                String txt = ((EditText) ((LinearLayout) c).getChildAt(0)).getText().toString().trim();
                if (!txt.isEmpty()) funcs.add(txt);
            }
        }
        if (funcs.isEmpty()) { Toast.makeText(getContext(), "请添加函数", Toast.LENGTH_SHORT).show(); return; }

        List<ILineDataSet> sets = new ArrayList<>();
        int ci = 0;
        for (String f : funcs) {
            List<Entry> entries = generate(f, xMin, xMax, step);
            if (!entries.isEmpty()) {
                LineDataSet ds = new LineDataSet(entries, "f" + (ci+1));
                ds.setColor(COLORS[ci % COLORS.length]); ds.setLineWidth(2f);
                ds.setCircleRadius(0f); ds.setDrawValues(false); ds.setDrawCircles(false);
                sets.add(ds); ci++;
            }
        }
        if (sets.isEmpty()) { Toast.makeText(getContext(), "无法绘制", Toast.LENGTH_SHORT).show(); return; }

        binding.chart2d.getXAxis().setAxisMinimum(xMin);
        binding.chart2d.getXAxis().setAxisMaximum(xMax);
        binding.chart2d.getAxisLeft().resetAxisMinimum();
        binding.chart2d.getAxisLeft().resetAxisMaximum();
        binding.chart2d.setData(new LineData(sets));
        binding.chart2d.invalidate();
    }

    private List<Entry> generate(String expr, float min, float max, float step) {
        List<Entry> entries = new ArrayList<>();
        try {
            Argument x = new Argument("x");
            Expression e = new Expression(expr, x);
            if (!e.checkSyntax()) return entries;
            for (double v = min; v <= max + step*0.5; v += step) {
                x.setArgumentValue(v);
                double y = e.calculate();
                if (!Double.isNaN(y) && !Double.isInfinite(y))
                    entries.add(new Entry((float)v, (float)y));
            }
        } catch (Exception ignored) {}
        return entries;
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
        if (tMin >= tMax || tStep <= 0) { Toast.makeText(getContext(), "范围不正确", Toast.LENGTH_SHORT).show(); return; }

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

        List<ILineDataSet> sets = new ArrayList<>();
        int ci = 0;
        for (String[] p : funcs) {
            List<Entry> entries = generateParam(p[0], p[1], tMin, tMax, tStep);
            if (!entries.isEmpty()) {
                LineDataSet ds = new LineDataSet(entries, "C" + (ci+1));
                ds.setColor(COLORS[ci % COLORS.length]); ds.setLineWidth(2f);
                ds.setCircleRadius(0f); ds.setDrawValues(false); ds.setDrawCircles(false);
                sets.add(ds); ci++;
            }
        }
        if (sets.isEmpty()) { Toast.makeText(getContext(), "无法绘制", Toast.LENGTH_SHORT).show(); return; }

        binding.chart2d.getXAxis().resetAxisMinimum();
        binding.chart2d.getXAxis().resetAxisMaximum();
        binding.chart2d.getAxisLeft().resetAxisMinimum();
        binding.chart2d.getAxisLeft().resetAxisMaximum();
        binding.chart2d.setData(new LineData(sets));
        binding.chart2d.invalidate();
    }

    private List<Entry> generateParam(String xs, String ys, float tMin, float tMax, float tStep) {
        List<Entry> entries = new ArrayList<>();
        try {
            Argument t = new Argument("t");
            Expression ex = new Expression(xs, t);
            Expression ey = new Expression(ys, t);
            for (double v = tMin; v <= tMax + tStep*0.5; v += tStep) {
                t.setArgumentValue(v);
                double x = ex.calculate(), y = ey.calculate();
                if (!Double.isNaN(x) && !Double.isInfinite(x) && !Double.isNaN(y) && !Double.isInfinite(y))
                    entries.add(new Entry((float)x, (float)y));
            }
        } catch (Exception ignored) {}
        return entries;
    }

    private void clearAllFunctions() {
        binding.functionsContainer.removeAllViews();
        binding.chart2d.clear(); binding.chart2d.invalidate();
        addFunctionInput();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
