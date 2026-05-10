package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.myapplication.databinding.FragmentMatrixBinding;

import org.ejml.simple.SimpleMatrix;

public class MatrixFragment extends Fragment {

    private FragmentMatrixBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMatrixBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnDeterminant.setOnClickListener(v -> calculate(Op.DET));
        binding.btnInverse.setOnClickListener(v -> calculate(Op.INV));
        binding.btnTranspose.setOnClickListener(v -> calculate(Op.TRANS));
        binding.btnTrace.setOnClickListener(v -> calculate(Op.TRACE));
        binding.btnAddMatrix.setOnClickListener(v -> calculate(Op.ADD));
        binding.btnSubMatrix.setOnClickListener(v -> calculate(Op.SUB));
        binding.btnMulMatrix.setOnClickListener(v -> calculate(Op.MUL));
    }

    enum Op { DET, INV, TRANS, TRACE, ADD, SUB, MUL }

    private void calculate(Op op) {
        String inputA = binding.etMatrixA.getText().toString().trim();
        if (inputA.isEmpty()) {
            Toast.makeText(getContext(), "请输入矩阵A", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            SimpleMatrix matrixA = parseMatrix(inputA);

            switch (op) {
                case DET: {
                    if (matrixA.numRows() != matrixA.numCols()) {
                        binding.tvMatrixResult.setText("Error: 矩阵不是方阵，无法求行列式");
                        return;
                    }
                    String result = formatNumber(matrixA.determinant());
                    binding.tvMatrixResult.setText(getString(R.string.matrix_det_result, result));
                    break;
                }
                case INV: {
                    if (matrixA.numRows() != matrixA.numCols()) {
                        binding.tvMatrixResult.setText("Error: 矩阵不是方阵，无法求逆");
                        return;
                    }
                    if (matrixA.determinant() == 0) {
                        binding.tvMatrixResult.setText("Error: 矩阵奇异，无法求逆（行列式为0）");
                        return;
                    }
                    SimpleMatrix inv = matrixA.invert();
                    binding.tvMatrixResult.setText(getString(R.string.matrix_inv_result, formatMatrix(inv)));
                    break;
                }
                case TRANS: {
                    SimpleMatrix trans = matrixA.transpose();
                    binding.tvMatrixResult.setText(getString(R.string.matrix_trans_result, formatMatrix(trans)));
                    break;
                }
                case TRACE: {
                    if (matrixA.numRows() != matrixA.numCols()) {
                        binding.tvMatrixResult.setText("Error: 矩阵不是方阵，无法求迹");
                        return;
                    }
                    String result = formatNumber(matrixA.trace());
                    binding.tvMatrixResult.setText(getString(R.string.matrix_trace_result, result));
                    break;
                }
                case ADD:
                case SUB:
                case MUL: {
                    String inputB = binding.etMatrixB.getText().toString().trim();
                    if (inputB.isEmpty()) {
                        Toast.makeText(getContext(), "请先输入矩阵B", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    SimpleMatrix matrixB = parseMatrix(inputB);

                    SimpleMatrix result;
                    String opName;
                    switch (op) {
                        case ADD:
                            if (matrixA.numRows() != matrixB.numRows() || matrixA.numCols() != matrixB.numCols()) {
                                binding.tvMatrixResult.setText("Error: 矩阵维度不同，无法相加");
                                return;
                            }
                            result = matrixA.plus(matrixB);
                            opName = "A + B";
                            break;
                        case SUB:
                            if (matrixA.numRows() != matrixB.numRows() || matrixA.numCols() != matrixB.numCols()) {
                                binding.tvMatrixResult.setText("Error: 矩阵维度不同，无法相减");
                                return;
                            }
                            result = matrixA.minus(matrixB);
                            opName = "A - B";
                            break;
                        case MUL:
                            if (matrixA.numCols() != matrixB.numRows()) {
                                binding.tvMatrixResult.setText("Error: 矩阵维度不匹配 (" +
                                        matrixA.numRows() + "x" + matrixA.numCols() +
                                        " × " + matrixB.numRows() + "x" + matrixB.numCols() + ")，无法相乘");
                                return;
                            }
                            result = matrixA.mult(matrixB);
                            opName = "A × B";
                            break;
                        default:
                            return;
                    }
                    binding.tvMatrixResult.setText(opName + " =\n" + formatMatrix(result));
                    break;
                }
            }
        } catch (Exception e) {
            binding.tvMatrixResult.setText("Error: " + e.getMessage());
        }
    }

    private SimpleMatrix parseMatrix(String input) {
        String[] rows = input.split(",");
        int numRows = rows.length;
        double[][] data = null;
        for (int i = 0; i < numRows; i++) {
            String[] cols = rows[i].trim().split("\\s+");
            if (data == null) {
                data = new double[numRows][cols.length];
            }
            for (int j = 0; j < cols.length; j++) {
                data[i][j] = Double.parseDouble(cols[j]);
            }
        }
        return new SimpleMatrix(data);
    }

    private String formatMatrix(SimpleMatrix m) {
        StringBuilder sb = new StringBuilder();
        int rows = m.numRows();
        int cols = m.numCols();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                sb.append(formatNumber(m.get(i, j)));
                if (j < cols - 1) sb.append("  ");
            }
            if (i < rows - 1) sb.append("\n");
        }
        return sb.toString();
    }

    private String formatNumber(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) {
            return String.valueOf((long) d);
        }
        return String.format("%.4f", d);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
