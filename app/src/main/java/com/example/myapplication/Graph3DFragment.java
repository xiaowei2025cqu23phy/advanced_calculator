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

        // Math keyboard targets the function input
        View kbView = view.findViewById(R.id.kb_root);
        if (kbView != null) {
            MathKeyboardHelper kb = new MathKeyboardHelper(kbView, text -> {
                binding.etFunction3d.append(text);
            });
            kb.getBackspaceButton().setOnClickListener(v -> {
                String t = binding.etFunction3d.getText().toString();
                if (!t.isEmpty()) binding.etFunction3d.setText(t.substring(0, t.length() - 1));
            });
            kb.getAcButton().setOnClickListener(v -> binding.etFunction3d.setText(""));
            kb.getEqualsButton().setOnClickListener(v -> plotFunction());
        }

        // Prevent system keyboard on the edit text
        binding.etFunction3d.setShowSoftInputOnFocus(false);
        binding.etFunction3d.setOnTouchListener((v, event) -> {
            v.onTouchEvent(event);
            return true;
        });

        binding.btnPlot3d.setOnClickListener(v -> plotFunction());
    }

    private void plotFunction() {
        String expr = binding.etFunction3d.getText().toString().trim();
        if (expr.isEmpty()) {
            Toast.makeText(getContext(), "请输入函数", Toast.LENGTH_SHORT).show();
            return;
        }
        glSurfaceView.queueEvent(() -> renderer.setFunction(expr));
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
