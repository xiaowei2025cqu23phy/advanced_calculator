package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.myapplication.databinding.FragmentComplexBinding;

import org.mariuszgromada.math.mxparser.Expression;

public class ComplexFragment extends Fragment {

    private FragmentComplexBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentComplexBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Prevent system keyboard
        preventSysKb(binding.etComplexA);
        preventSysKb(binding.etComplexB);
        preventSysKb(binding.etComplexSingle);

        // Inflate keyboard
        View kbView = getLayoutInflater().inflate(R.layout.layout_math_keyboard,
            binding.kbContainer, true);
        MathKeyboardHelper kb = new MathKeyboardHelper(kbView, this::appendToActive);
        kb.getBackspaceButton().setOnClickListener(v -> backspace());
        kb.getAcButton().setOnClickListener(v -> clearActive());
        kb.getEqualsButton().setOnClickListener(v -> {});

        // Operation buttons
        binding.btnCadd.setOnClickListener(v -> calc("+"));
        binding.btnCsub.setOnClickListener(v -> calc("-"));
        binding.btnCmul.setOnClickListener(v -> calc("*"));
        binding.btnCdiv.setOnClickListener(v -> calc("/"));
        binding.btnCrect.setOnClickListener(v -> rect2polar(binding.etComplexA, false));
        binding.btnCpolar.setOnClickListener(v -> polar2rect(binding.etComplexA, false));
        binding.btnCrect2.setOnClickListener(v -> rect2polar(binding.etComplexSingle, true));
        binding.btnCpolar2.setOnClickListener(v -> polar2rect(binding.etComplexSingle, true));
    }

    private android.widget.EditText activeInput = null;
    private void appendToActive(String text) {
        if (binding.etComplexA.hasFocus()) activeInput = binding.etComplexA;
        else if (binding.etComplexB.hasFocus()) activeInput = binding.etComplexB;
        else if (binding.etComplexSingle.hasFocus()) activeInput = binding.etComplexSingle;
        else activeInput = binding.etComplexA;
        activeInput.append(text);
    }
    private void backspace() {
        android.widget.EditText et = getFocused();
        if (et == null) return;
        String t = et.getText().toString();
        if (!t.isEmpty()) et.setText(t.substring(0, t.length() - 1));
    }
    private void clearActive() {
        android.widget.EditText et = getFocused();
        if (et != null) et.setText("");
    }
    private android.widget.EditText getFocused() {
        if (binding.etComplexA.hasFocus()) return binding.etComplexA;
        if (binding.etComplexB.hasFocus()) return binding.etComplexB;
        if (binding.etComplexSingle.hasFocus()) return binding.etComplexSingle;
        return binding.etComplexA;
    }

    private void calc(String op) {
        String a = binding.etComplexA.getText().toString().trim();
        String b = binding.etComplexB.getText().toString().trim();
        if (a.isEmpty() || b.isEmpty()) {
            Toast.makeText(getContext(), "请输入两个复数", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Expression e = new Expression("(" + a + ") " + op + " (" + b + ")");
            double r = e.calculate();
            if (Double.isNaN(r) || Double.isInfinite(r))
                binding.tvComplexResult.setText("结果无效");
            else
                binding.tvComplexResult.setText("(" + a + ") " + op + " (" + b + ") = " + fmt(r));
        } catch (Exception ex) {
            binding.tvComplexResult.setText("错误: " + ex.getMessage());
        }
    }

    private void rect2polar(android.widget.EditText et, boolean single) {
        String s = et.getText().toString().trim();
        if (s.isEmpty()) { Toast.makeText(getContext(), "请输入复数", Toast.LENGTH_SHORT).show(); return; }
        try {
            Expression e = new Expression("abs(" + s + ")");
            double mag = e.calculate();
            e = new Expression("arg(" + s + ")");
            double ang = e.calculate();
            if (Double.isNaN(mag) || Double.isNaN(ang))
                binding.tvComplexResult.setText("无法转换");
            else
                binding.tvComplexResult.setText(s + "\n= " + fmt(mag) + " ∠ " + fmt(ang) + " rad\n= " + fmt(mag) + " ∠ " + fmt(ang*180/3.14159) + "°");
        } catch (Exception ex) {
            binding.tvComplexResult.setText("错误: " + ex.getMessage());
        }
    }

    private void polar2rect(android.widget.EditText et, boolean single) {
        String s = et.getText().toString().trim();
        if (s.isEmpty()) { Toast.makeText(getContext(), "请输入模与辐角", Toast.LENGTH_SHORT).show(); return; }
        try {
            String[] parts;
            if (s.contains("∠")) parts = s.split("∠");
            else if (s.contains(",")) parts = s.split(",");
            else { binding.tvComplexResult.setText("格式: 模∠角度 或 模,角度"); return; }
            double mag = Double.parseDouble(parts[0].trim());
            double ang = Double.parseDouble(parts[1].trim());
            double a = mag * Math.cos(ang);
            double b = mag * Math.sin(ang);
            binding.tvComplexResult.setText(fmt(mag) + "∠" + fmt(ang) + "\n= " + fmt(a) + (b >= 0 ? "+" : "") + fmt(b) + "i");
        } catch (Exception ex) {
            binding.tvComplexResult.setText("错误: " + ex.getMessage());
        }
    }

    private String fmt(double d) {
        if (Double.isNaN(d) || Double.isInfinite(d)) return String.valueOf(d);
        if (d == Math.floor(d) && !Double.isInfinite(d)) return String.valueOf((long) d);
        String s = String.format("%.6f", d).replaceAll("0*$", "").replaceAll("\\.$", "");
        return s.isEmpty() ? "0" : s;
    }

    private void preventSysKb(android.widget.EditText et) {
        et.setShowSoftInputOnFocus(false);
        et.setOnTouchListener((v, event) -> { v.onTouchEvent(event); return true; });
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}
