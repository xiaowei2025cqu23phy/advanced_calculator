package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.databinding.FragmentMatrixBinding;
import com.example.myapplication.viewmodel.MatrixViewModel;

/**
 * UI-only layer. Matrix operations delegated to MatrixViewModel + MatrixRepository.
 */
public class MatrixFragment extends Fragment {

    private FragmentMatrixBinding binding;
    private MatrixViewModel viewModel;
    private EditText focusedInput;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMatrixBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(MatrixViewModel.class);

        // Observe result
        viewModel.getResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                if (result.isSuccess()) {
                    binding.tvMatrixResult.setText(result.getData());
                } else {
                    binding.tvMatrixResult.setText(result.getError().getUserMessage());
                }
            }
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading) binding.tvMatrixResult.setText("计算中...");
        });

        // Track focus for keyboard input
        binding.etMatrixA.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) focusedInput = binding.etMatrixA; });
        binding.etMatrixB.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) focusedInput = binding.etMatrixB; });

        // Inflate keyboard
        View kbView = getLayoutInflater().inflate(R.layout.layout_math_keyboard,
            view.findViewById(R.id.kb_container), true);
        MathKeyboardHelper kb = new MathKeyboardHelper(kbView, text -> {
            if (focusedInput != null) focusedInput.append(text);
        });
        kb.getBackspaceButton().setOnClickListener(v -> {
            if (focusedInput != null) {
                String t = focusedInput.getText().toString();
                if (!t.isEmpty()) focusedInput.setText(t.substring(0, t.length() - 1));
            }
        });
        kb.getAcButton().setOnClickListener(v -> { if (focusedInput != null) focusedInput.setText(""); });
        kb.getEqualsButton().setOnClickListener(v -> { if (focusedInput != null) focusedInput.append("\n"); });

        // Prevent system keyboard
        binding.etMatrixA.setShowSoftInputOnFocus(false);
        binding.etMatrixA.setOnTouchListener((v, event) -> { v.onTouchEvent(event); return true; });
        binding.etMatrixB.setShowSoftInputOnFocus(false);
        binding.etMatrixB.setOnTouchListener((v, event) -> { v.onTouchEvent(event); return true; });

        // Operation buttons → delegate to ViewModel
        binding.btnDeterminant.setOnClickListener(v -> calc(MatrixViewModel.Op.DET));
        binding.btnInverse.setOnClickListener(v -> calc(MatrixViewModel.Op.INV));
        binding.btnTranspose.setOnClickListener(v -> calc(MatrixViewModel.Op.TRANS));
        binding.btnTrace.setOnClickListener(v -> calc(MatrixViewModel.Op.TRACE));
        binding.btnAddMatrix.setOnClickListener(v -> calc(MatrixViewModel.Op.ADD));
        binding.btnSubMatrix.setOnClickListener(v -> calc(MatrixViewModel.Op.SUB));
        binding.btnMulMatrix.setOnClickListener(v -> calc(MatrixViewModel.Op.MUL));
    }

    private void calc(MatrixViewModel.Op op) {
        String a = binding.etMatrixA.getText().toString().trim();
        String b = binding.etMatrixB.getText().toString().trim();
        viewModel.calculate(op, a, b);
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}
