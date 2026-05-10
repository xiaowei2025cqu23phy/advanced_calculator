package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.myapplication.databinding.FragmentMatrixBinding;

import org.ejml.simple.SimpleMatrix;

public class MatrixFragment extends Fragment {

    private FragmentMatrixBinding binding;
    private EditText focusedInput;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMatrixBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Track focus
        binding.etMatrixA.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) focusedInput = binding.etMatrixA; });
        binding.etMatrixB.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) focusedInput = binding.etMatrixB; });

        // Keyboard targets focused matrix input
        View kbView = view.findViewById(R.id.kb_root);
        if (kbView != null) {
            MathKeyboardHelper kb = new MathKeyboardHelper(kbView, text -> {
                if (focusedInput != null) focusedInput.append(text);
            });
            kb.getBackspaceButton().setOnClickListener(v -> {
                if (focusedInput != null) {
                    String t = focusedInput.getText().toString();
                    if (!t.isEmpty()) focusedInput.setText(t.substring(0, t.length() - 1));
                }
            });
            kb.getAcButton().setOnClickListener(v -> {
                if (focusedInput != null) focusedInput.setText("");
            });
            kb.getEqualsButton().setOnClickListener(v -> {
                if (focusedInput != null) focusedInput.append("\n");
            });
        }

        // Prevent system keyboard on matrix inputs
        binding.etMatrixA.setShowSoftInputOnFocus(false);
        binding.etMatrixA.setOnTouchListener((v, event) -> { v.onTouchEvent(event); return true; });
        binding.etMatrixB.setShowSoftInputOnFocus(false);
        binding.etMatrixB.setOnTouchListener((v, event) -> { v.onTouchEvent(event); return true; });

        // Operation buttons
        binding.btnDeterminant.setOnClickListener(v -> calculate(Op.OP_DET));
        binding.btnInverse.setOnClickListener(v -> calculate(Op.OP_INV));
        binding.btnTranspose.setOnClickListener(v -> calculate(Op.OP_TRANS));
        binding.btnTrace.setOnClickListener(v -> calculate(Op.OP_TRACE));
        binding.btnAddMatrix.setOnClickListener(v -> calculate(Op.OP_ADD));
        binding.btnSubMatrix.setOnClickListener(v -> calculate(Op.OP_SUB));
        binding.btnMulMatrix.setOnClickListener(v -> calculate(Op.OP_MUL));
    }

    enum Op { OP_DET, OP_INV, OP_TRANS, OP_TRACE, OP_ADD, OP_SUB, OP_MUL }

    private void calculate(Op op) {
        String a = binding.etMatrixA.getText().toString().trim();
        if (a.isEmpty()) { Toast.makeText(getContext(), "请输入矩阵A", Toast.LENGTH_SHORT).show(); return; }

        try {
            SimpleMatrix mA = parseMatrix(a);
            switch (op) {
                case OP_DET:
                    if (mA.numRows() != mA.numCols()) { binding.tvMatrixResult.setText("Error: 不是方阵"); return; }
                    binding.tvMatrixResult.setText("Det = " + fmt(mA.determinant())); break;
                case OP_INV:
                    if (mA.numRows() != mA.numCols()) { binding.tvMatrixResult.setText("Error: 不是方阵"); return; }
                    if (mA.determinant() == 0) { binding.tvMatrixResult.setText("Error: 奇异矩阵"); return; }
                    binding.tvMatrixResult.setText("Inverse:\n" + matStr(mA.invert())); break;
                case OP_TRANS:
                    binding.tvMatrixResult.setText("Transpose:\n" + matStr(mA.transpose())); break;
                case OP_TRACE:
                    if (mA.numRows() != mA.numCols()) { binding.tvMatrixResult.setText("Error: 不是方阵"); return; }
                    binding.tvMatrixResult.setText("Trace = " + fmt(mA.trace())); break;
                case OP_ADD: case OP_SUB: case OP_MUL: {
                    String b = binding.etMatrixB.getText().toString().trim();
                    if (b.isEmpty()) { Toast.makeText(getContext(), "请输入矩阵B", Toast.LENGTH_SHORT).show(); return; }
                    SimpleMatrix mB = parseMatrix(b);
                    SimpleMatrix r;
                    if (op == Op.OP_ADD) {
                        if (mA.numRows() != mB.numRows() || mA.numCols() != mB.numCols()) {
                            binding.tvMatrixResult.setText("Error: 维度不同"); return;
                        }
                        r = mA.plus(mB);
                        binding.tvMatrixResult.setText("A+B:\n" + matStr(r));
                    } else if (op == Op.OP_SUB) {
                        if (mA.numRows() != mB.numRows() || mA.numCols() != mB.numCols()) {
                            binding.tvMatrixResult.setText("Error: 维度不同"); return;
                        }
                        r = mA.minus(mB);
                        binding.tvMatrixResult.setText("A-B:\n" + matStr(r));
                    } else {
                        if (mA.numCols() != mB.numRows()) {
                            binding.tvMatrixResult.setText("Error: 维度不匹配"); return;
                        }
                        r = mA.mult(mB);
                        binding.tvMatrixResult.setText("A×B:\n" + matStr(r));
                    }
                    break;
                }
            }
        } catch (Exception e) {
            binding.tvMatrixResult.setText("Error: " + e.getMessage());
        }
    }

    private SimpleMatrix parseMatrix(String s) {
        String[] rows = s.split(",");
        double[][] data = null;
        for (int i = 0; i < rows.length; i++) {
            String[] cols = rows[i].trim().split("\\s+");
            if (data == null) data = new double[rows.length][cols.length];
            for (int j = 0; j < cols.length; j++) data[i][j] = Double.parseDouble(cols[j]);
        }
        return new SimpleMatrix(data);
    }

    private String matStr(SimpleMatrix m) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < m.numRows(); i++) {
            for (int j = 0; j < m.numCols(); j++) {
                sb.append(fmt(m.get(i, j)));
                if (j < m.numCols() - 1) sb.append("  ");
            }
            if (i < m.numRows() - 1) sb.append("\n");
        }
        return sb.toString();
    }

    private String fmt(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) return String.valueOf((long) d);
        return String.format("%.4f", d);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
