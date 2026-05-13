package com.example.myapplication;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.myapplication.databinding.FragmentGraph3dBinding;

public class Graph3DFragment extends Fragment {

    private FragmentGraph3dBinding binding;
    private Graph3DRenderer renderer;
    private GLSurfaceView glSurfaceView;
    private boolean curveMode = false;
    private float prevX, prevY;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try {
            binding = FragmentGraph3dBinding.inflate(inflater, container, false);
            return binding.getRoot();
        } catch (Exception e) {
            TextView tv = new TextView(inflater.getContext());
            tv.setText("3D加载失败: " + e.getMessage());
            tv.setGravity(Gravity.CENTER); tv.setTextColor(0xFFFFFFFF);
            tv.setBackgroundColor(0xFF1C1C1E);
            return tv;
        }
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (binding == null) return;

        try {
            renderer = new Graph3DRenderer();
            glSurfaceView = binding.glSurface;
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // Touch rotation
        glSurfaceView.setOnTouchListener((v, event) -> {
            float x = event.getX(), y = event.getY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: prevX = x; prevY = y; return true;
                case MotionEvent.ACTION_MOVE:
                    renderer.setRotation(
                        Math.max(-90f, Math.min(90f, renderer.getRotX() + (y - prevY) * 0.5f)),
                        renderer.getRotY() + (x - prevX) * 0.5f);
                    prevX = x; prevY = y; return true;
            }
            return false;
        });

        // Mode toggle
        binding.btnModeSurface.setOnClickListener(v -> setCurveMode(false));
        binding.btnModeCurve.setOnClickListener(v -> setCurveMode(true));

        // Variable buttons
        binding.btnVarX.setOnClickListener(v -> appendToInput("x"));
        binding.btnVarY.setOnClickListener(v -> appendToInput("y"));
        binding.btnVarT.setOnClickListener(v -> appendToInput("t"));
        binding.btnReset3d.setOnClickListener(v -> renderer.setRotation(-25f, 0f));

        // Prevent system keyboard
        preventSystemKeyboard(binding.etFunction3d);
        preventSystemKeyboard(binding.etPx);
        preventSystemKeyboard(binding.etPy);
        preventSystemKeyboard(binding.etPz);

        // Inflate keyboard programmatically
        View kbView = getLayoutInflater().inflate(R.layout.layout_math_keyboard,
            binding.kbContainer, true);
        MathKeyboardHelper kb = new MathKeyboardHelper(kbView, this::appendToInput);
        kb.getBackspaceButton().setOnClickListener(v -> {
            String t = getActiveInputText();
            if (!t.isEmpty()) setActiveInputText(t.substring(0, t.length() - 1));
        });
        kb.getAcButton().setOnClickListener(v -> setActiveInputText(""));
        kb.getEqualsButton().setOnClickListener(v -> plotFunction());

        binding.btnPlot3d.setOnClickListener(v -> plotFunction());
        setCurveMode(false);
        } catch (Exception e) {
            Toast.makeText(getContext(), "3D初始化失败", Toast.LENGTH_LONG).show();
        }
    }

    private void appendToInput(String text) {
        if (curveMode) {
            // Append to x(t) or next empty parametric field
            if (binding.etPx.getText().toString().isEmpty() ||
                !binding.etPx.hasFocus()) {
                if (binding.etPy.hasFocus()) binding.etPy.append(text);
                else if (binding.etPz.hasFocus()) binding.etPz.append(text);
                else binding.etPx.append(text);
            } else binding.etPx.append(text);
        } else {
            binding.etFunction3d.append(text);
        }
    }

    private String getActiveInputText() {
        if (curveMode) {
            if (binding.etPy.hasFocus()) return binding.etPy.getText().toString();
            if (binding.etPz.hasFocus()) return binding.etPz.getText().toString();
            return binding.etPx.getText().toString();
        }
        return binding.etFunction3d.getText().toString();
    }

    private void setActiveInputText(String s) {
        if (curveMode) {
            if (binding.etPy.hasFocus()) binding.etPy.setText(s);
            else if (binding.etPz.hasFocus()) binding.etPz.setText(s);
            else binding.etPx.setText(s);
        } else {
            binding.etFunction3d.setText(s);
        }
    }

    private void setCurveMode(boolean mode) {
        curveMode = mode;
        binding.layoutSurface.setVisibility(mode ? View.GONE : View.VISIBLE);
        binding.layoutParametric.setVisibility(mode ? View.VISIBLE : View.GONE);
        binding.layoutTRange.setVisibility(mode ? View.VISIBLE : View.GONE);
        binding.btnModeSurface.setAlpha(mode ? 0.4f : 1f);
        binding.btnModeCurve.setAlpha(mode ? 1f : 0.4f);
    }

    private void plotFunction() {
        if (curveMode) {
            String xs = binding.etPx.getText().toString().trim();
            String ys = binding.etPy.getText().toString().trim();
            String zs = binding.etPz.getText().toString().trim();
            if (xs.isEmpty() || ys.isEmpty() || zs.isEmpty()) {
                Toast.makeText(getContext(), "请填写 x(t), y(t), z(t)", Toast.LENGTH_SHORT).show();
                return;
            }
            float tMin, tMax, tStep;
            try {
                tMin = Float.parseFloat(binding.etTMin.getText().toString());
                tMax = Float.parseFloat(binding.etTMax.getText().toString());
                tStep = Float.parseFloat(binding.etTStep.getText().toString());
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "t范围无效", Toast.LENGTH_SHORT).show(); return;
            }
            glSurfaceView.queueEvent(() ->
                renderer.setParametricFunction(xs, ys, zs, tMin, tMax, tStep));
        } else {
            String expr = binding.etFunction3d.getText().toString().trim();
            if (expr.isEmpty()) {
                Toast.makeText(getContext(), "请输入函数", Toast.LENGTH_SHORT).show();
                return;
            }
            glSurfaceView.queueEvent(() -> renderer.setFunction(expr));
        }
    }

    private void preventSystemKeyboard(android.widget.EditText et) {
        et.setShowSoftInputOnFocus(false);
        et.setOnTouchListener((v, event) -> { v.onTouchEvent(event); return true; });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (glSurfaceView != null) glSurfaceView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (glSurfaceView != null) glSurfaceView.onResume();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
