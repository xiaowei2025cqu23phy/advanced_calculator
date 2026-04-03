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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentScientificBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnCalculate.setOnClickListener(v -> {
            String exprString = binding.etExpression.getText().toString();
            if (!exprString.isEmpty()) {
                Expression e = new Expression(exprString);
                double result = e.calculate();
                binding.tvResult.setText(String.valueOf(result));
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}