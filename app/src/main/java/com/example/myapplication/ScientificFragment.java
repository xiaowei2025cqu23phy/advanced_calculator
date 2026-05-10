package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.myapplication.databinding.FragmentScientificBinding;
import org.mariuszgromada.math.mxparser.*;

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

        setupKeyboard();

        binding.btnAc.setOnClickListener(v -> {
            binding.etExpression.setText("");
            binding.tvResult.setText("");
        });

        binding.btnDel.setOnClickListener(v -> {
            String text = binding.etExpression.getText().toString();
            if (!text.isEmpty()) {
                binding.etExpression.setText(text.substring(0, text.length() - 1));
            }
        });

        binding.btnEquals.setOnClickListener(v -> calculateResult());

        binding.btnDeg.setOnClickListener(v -> {
            degreeMode = !degreeMode;
            binding.btnDeg.setText(degreeMode ? "DEG" : "RAD");
        });
    }

    private void setupKeyboard() {
        View.OnClickListener digitListener = v -> {
            Button b = (Button) v;
            binding.etExpression.append(b.getText());
        };

        View.OnClickListener funcListener = v -> {
            Button b = (Button) v;
            binding.etExpression.append(b.getText() + "(");
        };

        View.OnClickListener constantListener = v -> {
            Button b = (Button) v;
            String constText = b.getText().toString();
            switch (constText) {
                case "π":
                    binding.etExpression.append("pi");
                    break;
                case "e":
                    binding.etExpression.append("e");
                    break;
                case "i":
                    binding.etExpression.append("i");
                    break;
            }
        };

        View.OnClickListener specialFuncListener = v -> {
            Button b = (Button) v;
            String func = b.getText().toString();
            switch (func) {
                case "√":
                    binding.etExpression.append("sqrt(");
                    break;
                case "x^y":
                    binding.etExpression.append("^");
                    break;
                case "|x|":
                    binding.etExpression.append("abs(");
                    break;
                case "n!":
                    binding.etExpression.append("!");
                    break;
                case "1/x":
                    binding.etExpression.append("1/");
                    break;
                case "%":
                    binding.etExpression.append("%");
                    break;
            }
        };

        binding.btnSin.setOnClickListener(funcListener);
        binding.btnCos.setOnClickListener(funcListener);
        binding.btnTan.setOnClickListener(funcListener);
        binding.btnAsin.setOnClickListener(funcListener);
        binding.btnAcos.setOnClickListener(funcListener);
        binding.btnAtan.setOnClickListener(funcListener);
        binding.btnLog.setOnClickListener(funcListener);
        binding.btnLn.setOnClickListener(funcListener);

        binding.btnPi.setOnClickListener(constantListener);
        binding.btnExp.setOnClickListener(constantListener);
        binding.btnComplexI.setOnClickListener(constantListener);

        binding.btnSqrt.setOnClickListener(specialFuncListener);
        binding.btnPow.setOnClickListener(specialFuncListener);
        binding.btnAbs.setOnClickListener(specialFuncListener);
        binding.btnFact.setOnClickListener(specialFuncListener);
        binding.btnFrac.setOnClickListener(specialFuncListener);
        binding.btnMod.setOnClickListener(specialFuncListener);

        binding.btnConst.setOnClickListener(v -> {
            if (!lastResult.isEmpty()) {
                binding.etExpression.append(lastResult);
            }
        });

        binding.btnNeg.setOnClickListener(v -> {
            binding.etExpression.append("-");
        });

        binding.btn0.setOnClickListener(digitListener);
        binding.btn1.setOnClickListener(digitListener);
        binding.btn2.setOnClickListener(digitListener);
        binding.btn3.setOnClickListener(digitListener);
        binding.btn4.setOnClickListener(digitListener);
        binding.btn5.setOnClickListener(digitListener);
        binding.btn6.setOnClickListener(digitListener);
        binding.btn7.setOnClickListener(digitListener);
        binding.btn8.setOnClickListener(digitListener);
        binding.btn9.setOnClickListener(digitListener);
        binding.btnDot.setOnClickListener(digitListener);

        binding.btnLeftParen.setOnClickListener(digitListener);
        binding.btnRightParen.setOnClickListener(digitListener);
        binding.btnAdd.setOnClickListener(digitListener);
        binding.btnSub.setOnClickListener(digitListener);
        binding.btnMul.setOnClickListener(digitListener);
        binding.btnDiv.setOnClickListener(digitListener);
    }

    private void calculateResult() {
        String exprString = binding.etExpression.getText().toString();
        if (exprString.isEmpty()) return;

        // Replace display operators with mXparser operators
        exprString = exprString.replace("×", "*");
        exprString = exprString.replace("÷", "/");
        exprString = exprString.replace("−", "-");

        // Handle degree/radian mode using regex with word boundaries.
        // This avoids corrupting nested trig (e.g. asin(sin(30))):
        //   \b ensures "sin(" won't match inside "asin(".
        if (degreeMode) {
            exprString = exprString.replaceAll("\\bsin\\(", "sin(pi/180*");
            exprString = exprString.replaceAll("\\bcos\\(", "cos(pi/180*");
            exprString = exprString.replaceAll("\\btan\\(", "tan(pi/180*");
            exprString = exprString.replaceAll("\\basin\\(", "180/pi*asin(");
            exprString = exprString.replaceAll("\\bacos\\(", "180/pi*acos(");
            exprString = exprString.replaceAll("\\batan\\(", "180/pi*atan(");
        }

        try {
            Expression e = new Expression(exprString);
            double result = e.calculate();

            if (Double.isNaN(result)) {
                binding.tvResult.setText(getString(R.string.error_prefix, "Invalid expression"));
            } else if (Double.isInfinite(result)) {
                binding.tvResult.setText(getString(R.string.error_prefix, "Infinity"));
            } else {
                String resultStr;
                if (result == Math.floor(result) && !Double.isInfinite(result)) {
                    resultStr = String.valueOf((long) result);
                } else {
                    resultStr = String.valueOf(result);
                }
                binding.tvResult.setText("= " + resultStr);
                lastResult = resultStr;
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
