package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.databinding.FragmentComplexBinding;
import com.example.myapplication.viewmodel.ComplexViewModel;

/**
 * UI-only layer. Complex operations delegated to ComplexViewModel + ComplexRepository.
 */
public class ComplexFragment extends Fragment {

    private FragmentComplexBinding binding;
    private ComplexViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try {
            binding = FragmentComplexBinding.inflate(inflater, container, false);
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
            viewModel = new ViewModelProvider(this).get(ComplexViewModel.class);

            // Observe result
            viewModel.getResult().observe(getViewLifecycleOwner(), result -> {
                if (result != null) {
                    if (result.isSuccess()) {
                        binding.tvComplexResult.setText(result.getData());
                    } else {
                        binding.tvComplexResult.setText(result.getError().getUserMessage());
                    }
                }
            });

            viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
                if (loading) binding.tvComplexResult.setText("计算中...");
            });

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

            // Operation buttons → delegate to ViewModel
            binding.btnCadd.setOnClickListener(v -> calc("+"));
            binding.btnCsub.setOnClickListener(v -> calc("-"));
            binding.btnCmul.setOnClickListener(v -> calc("*"));
            binding.btnCdiv.setOnClickListener(v -> calc("/"));
            binding.btnCrect.setOnClickListener(v ->
                viewModel.rectToPolar(binding.etComplexA.getText().toString()));
            binding.btnCpolar.setOnClickListener(v ->
                viewModel.polarToRect(binding.etComplexA.getText().toString()));
            binding.btnCrect2.setOnClickListener(v ->
                viewModel.rectToPolar(binding.etComplexSingle.getText().toString()));
            binding.btnCpolar2.setOnClickListener(v ->
                viewModel.polarToRect(binding.etComplexSingle.getText().toString()));
            binding.btnConj.setOnClickListener(v ->
                viewModel.conjugate(binding.etComplexSingle.getText().toString()));
            binding.btnArg.setOnClickListener(v ->
                viewModel.argument(binding.etComplexSingle.getText().toString()));

        } catch (Exception e) {
            binding.tvComplexResult.setText("初始化错误: " + e.getClass().getSimpleName());
        }
    }

    private void calc(String op) {
        String a = binding.etComplexA.getText().toString().trim();
        String b = binding.etComplexB.getText().toString().trim();
        viewModel.calculate(a, b, op);
    }

    // ── Keyboard input helpers ──

    private EditText activeInput = null;

    private void appendToActive(String text) {
        if (binding.etComplexA.hasFocus()) activeInput = binding.etComplexA;
        else if (binding.etComplexB.hasFocus()) activeInput = binding.etComplexB;
        else if (binding.etComplexSingle.hasFocus()) activeInput = binding.etComplexSingle;
        else activeInput = binding.etComplexA;
        if (activeInput != null) activeInput.append(text);
    }

    private void backspace() {
        EditText et = getFocused();
        if (et == null) return;
        String t = et.getText().toString();
        if (!t.isEmpty()) et.setText(t.substring(0, t.length() - 1));
    }

    private void clearActive() {
        EditText et = getFocused();
        if (et != null) et.setText("");
    }

    private EditText getFocused() {
        if (binding.etComplexA.hasFocus()) return binding.etComplexA;
        if (binding.etComplexB.hasFocus()) return binding.etComplexB;
        if (binding.etComplexSingle.hasFocus()) return binding.etComplexSingle;
        return binding.etComplexA;
    }

    private void preventSysKb(EditText et) {
        et.setShowSoftInputOnFocus(false);
        et.setOnTouchListener((v, event) -> { v.onTouchEvent(event); return true; });
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}
