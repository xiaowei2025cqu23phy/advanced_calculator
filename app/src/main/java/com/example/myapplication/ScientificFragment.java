package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.myapplication.databinding.FragmentScientificBinding;
import org.mariuszgromada.math.mxparser.Expression;

public class ScientificFragment extends Fragment {

    private FragmentScientificBinding binding;
    private boolean degreeMode = false; // default RAD
    private String lastResult = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try {
            binding = FragmentScientificBinding.inflate(inflater, container, false);
            return binding.getRoot();
        } catch (Exception e) {
            TextView tv = new TextView(inflater.getContext());
            tv.setText("inflate: " + e.getClass().getSimpleName() + ": " + e.getMessage());
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
            binding.etExpression.setShowSoftInputOnFocus(false);
            binding.etExpression.setOnTouchListener((v, event) -> { v.onTouchEvent(event); return true; });

            // Inflate keyboard manually
            ViewGroup kbc = (ViewGroup) view.findViewById(R.id.kb_container);
            if (kbc != null) {
                View kbv = getLayoutInflater().inflate(R.layout.layout_math_keyboard, kbc, true);
                MathKeyboardHelper kb = new MathKeyboardHelper(kbv, text -> binding.etExpression.append(text));
                kb.getEqualsButton().setOnClickListener(v -> calc());
                kb.getBackspaceButton().setOnClickListener(v -> {
                    String t = binding.etExpression.getText().toString();
                    if (!t.isEmpty()) binding.etExpression.setText(t.substring(0, t.length() - 1));
                });
                kb.getAcButton().setOnClickListener(v -> {
                    binding.etExpression.setText("");
                    binding.tvResult.setText("");
                });
            }

            updateDegDisplay();
            binding.btnDeg.setOnClickListener(v -> {
                degreeMode = !degreeMode;
                updateDegDisplay();
            });
            binding.btnAns.setOnClickListener(v -> { if (!lastResult.isEmpty()) binding.etExpression.append(lastResult); });
            binding.btnComplexI.setOnClickListener(v -> binding.etExpression.append("i"));
            binding.btnPi.setOnClickListener(v -> binding.etExpression.append("pi"));
            binding.btnE.setOnClickListener(v -> binding.etExpression.append("e"));

        } catch (Exception e) {
            binding.tvResult.setText("err: " + e.getClass().getSimpleName());
        }
    }

    private void updateDegDisplay() {
        binding.btnDeg.setText(degreeMode ? "DEG" : "RAD");
        binding.tvModeIndicator.setText(degreeMode ? "DEG" : "RAD");
        binding.tvModeIndicator.setTextColor(degreeMode ? 0xFFFF9F0A : 0xFF888888);
    }

    private void calc() {
        String expr = binding.etExpression.getText().toString();
        if (expr.isEmpty()) return;
        expr = expr.replace("×", "*").replace("÷", "/").replace("−", "-");
        if (degreeMode) {
            expr = expr.replaceAll("\\bsin\\(", "sin(pi/180*");
            expr = expr.replaceAll("\\bcos\\(", "cos(pi/180*");
            expr = expr.replaceAll("\\btan\\(", "tan(pi/180*");
            expr = expr.replaceAll("\\basin\\(", "180/pi*asin(");
            expr = expr.replaceAll("\\bacos\\(", "180/pi*acos(");
            expr = expr.replaceAll("\\batan\\(", "180/pi*atan(");
        }
        try {
            double r = new Expression(expr).calculate();
            if (Double.isNaN(r)) {
                binding.tvResult.setText("无效");
            } else if (Double.isInfinite(r)) {
                binding.tvResult.setText("无穷");
            } else {
                String s = (r == Math.floor(r) && !Double.isInfinite(r)) ? String.valueOf((long) r) : String.valueOf(r);
                binding.tvResult.setText("= " + s);
                lastResult = s;
            }
        } catch (Exception ex) {
            binding.tvResult.setText("错误");
        }
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}
