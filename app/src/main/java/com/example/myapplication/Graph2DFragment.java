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
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.databinding.FragmentGraph2dBinding;
import com.example.myapplication.viewmodel.GraphViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * UI-only layer. 2D plotting delegated to GraphViewModel → PlotEngine (async).
 */
public class Graph2DFragment extends Fragment {

    private FragmentGraph2dBinding binding;
    private GraphViewModel viewModel;
    private int plotMode = 0; // 0=y=f(x), 1=parametric, 2=polar
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

        viewModel = new ViewModelProvider(this).get(GraphViewModel.class);
        setupChart();

        // Observe computed curves
        viewModel.getCurves().observe(getViewLifecycleOwner(), curves -> {
            if (curves == null || curves.isEmpty()) return;
            binding.chart2d.clearCurves();
            for (GraphViewModel.CurveResult c : curves) {
                binding.chart2d.addCurve(c.getPoints(), c.getColor(), c.getLabel());
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            // Could show a progress indicator here
        });

        // Inflate keyboard programmatically
        View kbView = getLayoutInflater().inflate(R.layout.layout_math_keyboard,
            binding.kbContainer, true);
        MathKeyboardHelper kb = new MathKeyboardHelper(kbView, text -> {
            if (focusedInput != null) focusedInput.append(text);
        });
        if (kbView.findViewById(R.id.kb_7) != null) {
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
        binding.btnModeNormal.setOnClickListener(v -> setMode(0));
        binding.btnModeParam.setOnClickListener(v -> setMode(1));
        binding.btnModePolar.setOnClickListener(v -> setMode(2));
        binding.btnVarX.setOnClickListener(v -> { if (focusedInput != null) focusedInput.append("x"); });
        binding.btnVarY.setOnClickListener(v -> { if (focusedInput != null) focusedInput.append("y"); });
        binding.btnVarT.setOnClickListener(v -> { if (focusedInput != null) focusedInput.append("t"); });
        binding.btnVarTheta.setOnClickListener(v -> { if (focusedInput != null) focusedInput.append("θ"); });
        binding.btnVarComma.setOnClickListener(v -> { if (focusedInput != null) focusedInput.append(","); });
        binding.btnResetView.setOnClickListener(v -> binding.chart2d.resetBounds());
        binding.btnPaste.setOnClickListener(v -> pasteFromClipboard());
        binding.btnSaveImg.setOnClickListener(v -> saveChartImage());

        updateModeButtons();
        addFunctionInput();
    }

    private void setMode(int mode) {
        if (plotMode == mode) return;
        plotMode = mode;
        updateModeButtons();
        binding.layoutTRange.setVisibility(mode != 0 ? View.VISIBLE : View.GONE);
        clearAllFunctions();
    }

    private void updateModeButtons() {
        binding.btnModeNormal.setAlpha(plotMode == 0 ? 1f : 0.4f);
        binding.btnModeParam.setAlpha(plotMode == 1 ? 1f : 0.4f);
        binding.btnModePolar.setAlpha(plotMode == 2 ? 1f : 0.4f);
    }

    private void setupChart() {
        binding.chart2d.setBounds(-10, 10, -10, 10);
    }

    private void addFunctionInput() {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        row.setPadding(0, 2, 0, 2);

        if (plotMode == 1) {
            row.addView(makeEditText("x(t)"));
            row.addView(makeEditText("y(t)"));
        } else if (plotMode == 2) {
            row.addView(makeEditText("r(θ)"));
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
        if (plotMode == 1) plotParametric();
        else if (plotMode == 2) plotPolar();
        else plotNormal();
    }

    private void plotNormal() {
        double xMin, xMax, step;
        try {
            xMin = Double.parseDouble(binding.etXMin.getText().toString());
            xMax = Double.parseDouble(binding.etXMax.getText().toString());
            step = Double.parseDouble(binding.etStep.getText().toString());
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "范围无效", Toast.LENGTH_SHORT).show(); return;
        }
        if (xMin >= xMax || step <= 0) {
            Toast.makeText(getContext(), "范围不正确", Toast.LENGTH_SHORT).show(); return;
        }

        List<GraphViewModel.CartesianInput> inputs = new ArrayList<>();
        for (int i = 0; i < binding.functionsContainer.getChildCount(); i++) {
            View c = binding.functionsContainer.getChildAt(i);
            if (c instanceof LinearLayout) {
                String txt = ((EditText) ((LinearLayout) c).getChildAt(0)).getText().toString().trim();
                if (!txt.isEmpty()) {
                    inputs.add(new GraphViewModel.CartesianInput(txt, COLORS[inputs.size() % COLORS.length], "f" + (inputs.size() + 1)));
                }
            }
        }
        if (inputs.isEmpty()) { Toast.makeText(getContext(), "请添加函数", Toast.LENGTH_SHORT).show(); return; }

        viewModel.generateCartesian(inputs, xMin, xMax, step);
    }

    private void plotParametric() {
        double tMin, tMax, tStep;
        try {
            tMin = Double.parseDouble(binding.etTMin.getText().toString());
            tMax = Double.parseDouble(binding.etTMax.getText().toString());
            tStep = Double.parseDouble(binding.etTStep.getText().toString());
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "t范围无效", Toast.LENGTH_SHORT).show(); return;
        }
        if (tMin >= tMax || tStep <= 0) {
            Toast.makeText(getContext(), "范围不正确", Toast.LENGTH_SHORT).show(); return;
        }

        List<GraphViewModel.ParametricInput> inputs = new ArrayList<>();
        for (int i = 0; i < binding.functionsContainer.getChildCount(); i++) {
            View c = binding.functionsContainer.getChildAt(i);
            if (c instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) c;
                if (row.getChildCount() >= 3) {
                    String xs = ((EditText) row.getChildAt(0)).getText().toString().trim();
                    String ys = ((EditText) row.getChildAt(1)).getText().toString().trim();
                    if (!xs.isEmpty() && !ys.isEmpty()) {
                        inputs.add(new GraphViewModel.ParametricInput(xs, ys,
                            COLORS[inputs.size() % COLORS.length], "C" + (inputs.size() + 1)));
                    }
                }
            }
        }
        if (inputs.isEmpty()) { Toast.makeText(getContext(), "请添加参数方程", Toast.LENGTH_SHORT).show(); return; }

        viewModel.generateParametric(inputs, tMin, tMax, tStep);
    }

    private void plotPolar() {
        double tMin, tMax, tStep;
        try {
            tMin = Double.parseDouble(binding.etTMin.getText().toString());
            tMax = Double.parseDouble(binding.etTMax.getText().toString());
            tStep = Double.parseDouble(binding.etTStep.getText().toString());
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "θ范围无效", Toast.LENGTH_SHORT).show(); return;
        }
        if (tMin >= tMax || tStep <= 0) {
            Toast.makeText(getContext(), "范围不正确", Toast.LENGTH_SHORT).show(); return;
        }

        List<GraphViewModel.PolarInput> inputs = new ArrayList<>();
        for (int i = 0; i < binding.functionsContainer.getChildCount(); i++) {
            View c = binding.functionsContainer.getChildAt(i);
            if (c instanceof LinearLayout) {
                String txt = ((EditText) ((LinearLayout) c).getChildAt(0)).getText().toString().trim();
                if (!txt.isEmpty()) {
                    inputs.add(new GraphViewModel.PolarInput(txt, COLORS[inputs.size() % COLORS.length], "r" + (inputs.size() + 1)));
                }
            }
        }
        if (inputs.isEmpty()) { Toast.makeText(getContext(), "请添加函数", Toast.LENGTH_SHORT).show(); return; }

        viewModel.generatePolar(inputs, tMin, tMax, tStep);
    }

    private void pasteFromClipboard() {
        android.content.ClipboardManager cm = (android.content.ClipboardManager)
            requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        if (cm.hasPrimaryClip() && focusedInput != null) {
            CharSequence s = cm.getPrimaryClip().getItemAt(0).getText();
            if (s != null) focusedInput.append(s);
        }
    }

    private void saveChartImage() {
        try {
            android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(
                binding.chart2d.getWidth(), binding.chart2d.getHeight(),
                android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas c = new android.graphics.Canvas(bmp);
            binding.chart2d.draw(c);

            java.io.File dir = new java.io.File(
                android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_PICTURES), "Calculator");
            if (!dir.exists()) dir.mkdirs();
            java.io.File file = new java.io.File(dir, "graph_" + System.currentTimeMillis() + ".png");
            java.io.FileOutputStream out = new java.io.FileOutputStream(file);
            bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
            out.flush(); out.close();
            bmp.recycle();

            Toast.makeText(getContext(), "已保存: " + file.getName(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearAllFunctions() {
        binding.functionsContainer.removeAllViews();
        binding.chart2d.clearCurves();
        viewModel.clear();
        addFunctionInput();
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}
