package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.myapplication.databinding.FragmentScientificBinding;

import org.mariuszgromada.math.mxparser.Expression;

public class ScientificFragment extends Fragment {

    private FragmentScientificBinding binding;
    private boolean degreeMode = true;
    private String lastResult = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentScientificBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Prevent system keyboard
        binding.etExpression.setShowSoftInputOnFocus(false);
        binding.etExpression.setOnTouchListener((v, event) -> {
            v.onTouchEvent(event);
            return true;
        });

        // Inflate keyboard programmatically
        View kbView = getLayoutInflater().inflate(R.layout.layout_math_keyboard,
            view.findViewById(R.id.kb_container), true);
        MathKeyboardHelper kb = new MathKeyboardHelper(kbView, text -> {
            binding.etExpression.append(text);
        });
        kb.getEqualsButton().setOnClickListener(v -> calculateResult());
        kb.getAcButton().setOnClickListener(v -> {
            binding.etExpression.setText("");
            binding.tvResult.setText("");
        });
        kb.getBackspaceButton().setOnClickListener(v -> {
            String t = binding.etExpression.getText().toString();
            if (!t.isEmpty()) {
                binding.etExpression.setText(t.substring(0, t.length() - 1));
            }
        });

        // Toolbar buttons
        binding.btnDeg.setOnClickListener(v -> {
            degreeMode = !degreeMode;
            binding.btnDeg.setText(degreeMode ? "DEG" : "RAD");
        });

        binding.btnAns.setOnClickListener(v -> {
            if (!lastResult.isEmpty()) {
                binding.etExpression.append(lastResult);
            }
        });

        binding.btnComplexI.setOnClickListener(v -> binding.etExpression.append("i"));
        binding.btnPi.setOnClickListener(v -> binding.etExpression.append("pi"));
        binding.btnE.setOnClickListener(v -> binding.etExpression.append("e"));
    }

    private void calculateResult() {
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
            Expression e = new Expression(expr);
            double result = e.calculate();

            if (Double.isNaN(result)) {
                binding.tvResult.setText(getString(R.string.error_prefix, "表达式无效"));
            } else if (Double.isInfinite(result)) {
                binding.tvResult.setText(getString(R.string.error_prefix, "结果为无穷大"));
            } else {
                String s;
                if (result == Math.floor(result) && !Double.isInfinite(result)) {
                    s = String.valueOf((long) result);
                } else {
                    s = String.valueOf(result);
                }
                binding.tvResult.setText("= " + s);
                lastResult = s;
            }
        } catch (Exception ex) {
            binding.tvResult.setText(getString(R.string.error_prefix, ex.getMessage()));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
