package com.example.myapplication;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.databinding.FragmentScientificBinding;
import com.example.myapplication.repository.HistoryRepository;
import com.example.myapplication.viewmodel.ScientificViewModel;

/**
 * UI-only layer. All calculation logic is delegated to ScientificViewModel + CalculationRepository.
 */
public class ScientificFragment extends Fragment {

    private FragmentScientificBinding binding;
    private ScientificViewModel viewModel;
    private boolean degreeMode = false;
    private String lastResult = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try {
            binding = FragmentScientificBinding.inflate(inflater, container, false);
            return binding.getRoot();
        } catch (Exception e) {
            TextView tv = new TextView(inflater.getContext());
            tv.setText("加载失败: " + e.getClass().getSimpleName());
            tv.setTextColor(0xFFFFFFFF);
            tv.setBackgroundColor(0xFF1C1C1E);
            tv.setGravity(17);
            return tv;
        }
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (binding == null) return;

        try {
            viewModel = new ViewModelProvider(this).get(ScientificViewModel.class);

            // Observe results
            viewModel.getResult().observe(getViewLifecycleOwner(), result -> {
                if (result != null && result.isSuccess()) {
                    String s = viewModel.formatResult(result);
                    binding.tvResult.setText("= " + s);
                    lastResult = s;
                }
            });

            viewModel.getHistory().observe(getViewLifecycleOwner(), history -> {
                // history is auto-saved; no UI update needed here
            });

            viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
                if (msg != null) binding.tvResult.setText("错误: " + msg);
            });

            viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
                if (loading) binding.tvResult.setText("计算中...");
            });

            // Prevent system keyboard
            binding.etExpression.setShowSoftInputOnFocus(false);
            binding.etExpression.setOnTouchListener((v, event) -> { v.onTouchEvent(event); return true; });
            // Update LaTeX preview as user types
            binding.etExpression.addTextChangedListener(new android.text.TextWatcher() {
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                public void afterTextChanged(android.text.Editable s) {
                    binding.latexPreview.setExpression(s.toString());
                }
            });

            // Inflate keyboard
            ViewGroup kbc = (ViewGroup) view.findViewById(R.id.kb_container);
            if (kbc != null) {
                View kbv = getLayoutInflater().inflate(R.layout.layout_math_keyboard, kbc, true);
                MathKeyboardHelper kb = new MathKeyboardHelper(kbv, text -> binding.etExpression.append(text));
                kb.getEqualsButton().setOnClickListener(v -> calculate());
                kb.getBackspaceButton().setOnClickListener(v -> {
                    String t = binding.etExpression.getText().toString();
                    if (!t.isEmpty()) binding.etExpression.setText(t.substring(0, t.length() - 1));
                });
                kb.getAcButton().setOnClickListener(v -> {
                    binding.etExpression.setText("");
                    binding.tvResult.setText("");
                });
            }

            // Toolbar
            updateDegDisplay();
            binding.btnDeg.setOnClickListener(v -> {
                degreeMode = !degreeMode;
                updateDegDisplay();
            });
            binding.btnAns.setOnClickListener(v -> {
                if (!lastResult.isEmpty()) binding.etExpression.append(lastResult);
            });
            binding.btnAns.setOnLongClickListener(v -> { showHistory(); return true; });
            binding.btnComplexI.setOnClickListener(v -> binding.etExpression.append("i"));
            binding.btnPi.setOnClickListener(v -> binding.etExpression.append("pi"));
            binding.btnCopy.setOnClickListener(v -> {
                String txt = binding.etExpression.getText().toString();
                if (!txt.isEmpty()) {
                    android.content.ClipboardManager cm = (android.content.ClipboardManager)
                        requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("expr", txt));
                    binding.tvResult.setText("已复制");
                }
            });
            binding.btnPaste.setOnClickListener(v -> {
                android.content.ClipboardManager cm = (android.content.ClipboardManager)
                    requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                if (cm.hasPrimaryClip()) {
                    CharSequence pasted = cm.getPrimaryClip().getItemAt(0).getText();
                    if (pasted != null) binding.etExpression.append(pasted);
                }
            });

        } catch (Exception e) {
            binding.tvResult.setText("初始化错误: " + e.getClass().getSimpleName());
        }
    }

    private void updateDegDisplay() {
        binding.btnDeg.setText(degreeMode ? "DEG" : "RAD");
        binding.tvModeIndicator.setText(degreeMode ? "DEG" : "RAD");
        binding.tvModeIndicator.setTextColor(degreeMode ? 0xFFFF9F0A : 0xFF888888);
    }

    private void calculate() {
        String expr = binding.etExpression.getText().toString();
        if (expr.isEmpty()) return;
        viewModel.calculate(expr, degreeMode);
    }

    private void showHistory() {
        java.util.List<HistoryRepository.Entry> items = viewModel.getHistory().getValue();
        if (items == null || items.isEmpty()) {
            binding.tvResult.setText("暂无历史记录");
            return;
        }
        CharSequence[] lines = new CharSequence[Math.min(items.size(), 50)];
        for (int i = 0; i < lines.length; i++) {
            HistoryRepository.Entry e = items.get(i);
            lines[i] = e.getExpression() + " = " + e.getResult();
        }
        new AlertDialog.Builder(getContext())
            .setTitle("历史记录 (长按清空)")
            .setItems(lines, (dialog, which) -> {
                binding.etExpression.setText(items.get(which).getExpression());
            })
            .setPositiveButton("清空", (d, w) -> viewModel.clearHistory())
            .show();
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}
