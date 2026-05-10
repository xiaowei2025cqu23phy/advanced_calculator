package com.example.myapplication;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.myapplication.databinding.FragmentGraph3dBinding;

public class Graph3DFragment extends Fragment {

    private FragmentGraph3dBinding binding;
    private Graph3DRenderer renderer;
    private GLSurfaceView glSurfaceView;

    // Touch tracking
    private float prevX, prevY;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentGraph3dBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        renderer = new Graph3DRenderer();
        glSurfaceView = binding.glSurface;
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // Touch rotation
        glSurfaceView.setOnTouchListener((v, event) -> {
            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    prevX = x;
                    prevY = y;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx = x - prevX;
                    float dy = y - prevY;
                    float newRotY = renderer.getRotY() + dx * 0.5f;
                    float newRotX = renderer.getRotX() + dy * 0.5f;
                    newRotX = Math.max(-90f, Math.min(90f, newRotX));
                    renderer.setRotation(newRotX, newRotY);
                    prevX = x;
                    prevY = y;
                    return true;
            }
            return false;
        });

        // Default function
        binding.etFunction3d.setText("sin(x)*cos(y)");

        binding.btnPlot3d.setOnClickListener(v -> plotFunction());
    }

    private void plotFunction() {
        String expr = binding.etFunction3d.getText().toString().trim();
        if (expr.isEmpty()) {
            Toast.makeText(getContext(), "请输入函数表达式", Toast.LENGTH_SHORT).show();
            return;
        }
        glSurfaceView.queueEvent(() -> renderer.setFunction(expr));
        Toast.makeText(getContext(), "绘制中: f(x,y) = " + expr, Toast.LENGTH_SHORT).show();
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
