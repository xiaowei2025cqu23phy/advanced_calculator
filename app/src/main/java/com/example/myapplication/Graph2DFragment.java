package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.myapplication.databinding.FragmentGraph2dBinding;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Expression;

import java.util.ArrayList;
import java.util.List;

public class Graph2DFragment extends Fragment {

    private FragmentGraph2dBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentGraph2dBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnPlot2d.setOnClickListener(v -> {
            String funcStr = binding.etFunction2d.getText().toString();
            if (!funcStr.isEmpty()) {
                plotGraph(funcStr);
            }
        });
    }

    private void plotGraph(String expression) {
        List<Entry> entries = new ArrayList<>();
        Argument x = new Argument("x");
        Expression e = new Expression(expression, x);

        for (double val = -10; val <= 10; val += 0.1) {
            x.setArgumentValue(val);
            float y = (float) e.calculate();
            if (!Double.isNaN(y)) {
                entries.add(new Entry((float) val, y));
            }
        }

        LineDataSet dataSet = new LineDataSet(entries, "f(x)");
        LineData lineData = new LineData(dataSet);
        binding.chart2d.setData(lineData);
        binding.chart2d.invalidate(); // refresh
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}