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

        binding.btnAddFunction.setOnClickListener(v -> addFunctionInput());
        binding.btnPlot2d.setOnClickListener(v -> plotAllGraphs());
        binding.btnClearAll.setOnClickListener(v -> clearAllFunctions());

        addFunctionInput();
    }

    private void setupChart() {
        LineChart chart = binding.chart2d;

        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDrawGridBackground(false);
        chart.setExtraOffsets(8, 8, 8, 8);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGranularity(1f);
        leftAxis.setGranularityEnabled(true);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);

        chart.getLegend().setEnabled(true);
    }

    private void addFunctionInput() {
        LinearLayout container = binding.functionsContainer;

        LinearLayout functionLayout = new LinearLayout(getContext());
        functionLayout.setOrientation(LinearLayout.HORIZONTAL);
        functionLayout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        EditText editText = new EditText(getContext());
        editText.setLayoutParams(new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
        ));
        editText.setHint("f(x) = x^2");
        editText.setId(View.generateViewId());

        Button removeBtn = new Button(getContext());
        removeBtn.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        removeBtn.setText("删除");
        removeBtn.setOnClickListener(v -> container.removeView(functionLayout));

        functionLayout.addView(editText);
        functionLayout.addView(removeBtn);
        container.addView(functionLayout);
    }

    private void plotAllGraphs() {
        // Parse range inputs
        float xMin, xMax, step;
        try {
            xMin = Float.parseFloat(binding.etXMin.getText().toString());
            xMax = Float.parseFloat(binding.etXMax.getText().toString());
            step = Float.parseFloat(binding.etStep.getText().toString());
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "请输入有效的数字范围", Toast.LENGTH_SHORT).show();
            return;
        }

        if (xMin >= xMax) {
            Toast.makeText(getContext(), "X最小值必须小于最大值", Toast.LENGTH_SHORT).show();
            return;
        }
        if (step <= 0) {
            Toast.makeText(getContext(), "步长必须大于0", Toast.LENGTH_SHORT).show();
            return;
        }

        // Collect functions
        List<String> functions = new ArrayList<>();
        LinearLayout container = binding.functionsContainer;

        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child instanceof LinearLayout) {
                EditText editText = (EditText) ((LinearLayout) child).getChildAt(0);
                String func = editText.getText().toString().trim();
                if (!func.isEmpty()) {
                    String cleaned = func.replaceAll("(?i)^f\\d*\\(x\\)\\s*=\\s*", "");
                    functions.add(cleaned);
                }
            }
        }

        if (functions.isEmpty()) {
            Toast.makeText(getContext(), "请输入至少一个函数", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate data for each function
        List<ILineDataSet> dataSets = new ArrayList<>();
        int colorIdx = 0;

        for (String func : functions) {
            List<Entry> entries = generateFunctionData(func, xMin, xMax, step);
            if (!entries.isEmpty()) {
                LineDataSet ds = new LineDataSet(entries, "f" + (colorIdx + 1) + "(x)");
                ds.setColor(COLORS[colorIdx % COLORS.length]);
                ds.setLineWidth(2f);
                ds.setCircleRadius(0f);
                ds.setDrawValues(false);
                ds.setDrawCircles(false);
                ds.setMode(LineDataSet.Mode.LINEAR);
                dataSets.add(ds);
                colorIdx++;
            }
        }

        if (dataSets.isEmpty()) {
            Toast.makeText(getContext(), "无法绘制函数图形，请检查表达式", Toast.LENGTH_SHORT).show();
            return;
        }

        LineChart chart = binding.chart2d;

        // Configure axis to match range
        XAxis xAxis = chart.getXAxis();
        xAxis.setAxisMinimum(xMin);
        xAxis.setAxisMaximum(xMax);

        // Auto-scale Y axis to data
        chart.getAxisLeft().resetAxisMinimum();
        chart.getAxisLeft().resetAxisMaximum();

        chart.setData(new LineData(dataSets));
        chart.invalidate();
    }

    private List<Entry> generateFunctionData(String expression, float xMin, float xMax, float step) {
        List<Entry> entries = new ArrayList<>();
        try {
            Argument x = new Argument("x");
            Expression e = new Expression(expression, x);
            if (!e.checkSyntax()) return entries;

            for (double val = xMin; val <= xMax + step * 0.5; val += step) {
                x.setArgumentValue(val);
                double y = e.calculate();
                if (!Double.isNaN(y) && !Double.isInfinite(y)) {
                    entries.add(new Entry((float) val, (float) y));
                }
            }
        } catch (Exception ignored) {}
        return entries;
    }

    private void clearAllFunctions() {
        binding.functionsContainer.removeAllViews();
        binding.chart2d.clear();
        binding.chart2d.invalidate();
        addFunctionInput();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
