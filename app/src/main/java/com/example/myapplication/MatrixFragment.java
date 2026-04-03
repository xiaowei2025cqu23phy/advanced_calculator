package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    }

    enum Op { DET, INV, TRANS, TRACE }

    private void calculate(Op op) {
        String input = binding.etMatrixA.getText().toString();
        if (input.isEmpty()) return;
        
        try {
            SimpleMatrix matrix = parseMatrix(input);
            String result = "";
            switch (op) {
                case DET:
                    result = getString(R.string.matrix_det_result, String.valueOf(matrix.determinant()));
                    break;
                case INV:
                    result = getString(R.string.matrix_inv_result, matrix.invert().toString());
                    break;
                case TRANS:
                    result = getString(R.string.matrix_trans_result, matrix.transpose().toString());
                    break;
                case TRACE:
                    result = getString(R.string.matrix_trace_result, String.valueOf(matrix.trace()));
                    break;
            }
            binding.tvMatrixResult.setText(result);
        } catch (Exception e) {
            binding.tvMatrixResult.setText(getString(R.string.error_prefix, e.getMessage()));
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}