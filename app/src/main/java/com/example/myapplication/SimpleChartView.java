package com.example.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple 2D chart view that draws function curves using Canvas.
 * Replaces MPAndroidChart to avoid library compatibility issues.
 */
public class SimpleChartView extends View {

    private final Paint gridPaint = new Paint();
    private final Paint axisPaint = new Paint();
    private final Paint labelPaint = new Paint();
    private final List<CurveData> curves = new ArrayList<>();

    private float xMin = -10, xMax = 10;
    private float yMin = -10, yMax = 10;
    private float scaleX, scaleY;

    // Touch pan/zoom
    private float lastX, lastY;
    private boolean touched = false;
    private float baseXMin, baseXMax, baseYMin, baseYMax;

    public SimpleChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        gridPaint.setColor(0x33FFFFFF);
        gridPaint.setStrokeWidth(1);
        gridPaint.setStyle(Paint.Style.STROKE);

        axisPaint.setColor(0xAAFFFFFF);
        axisPaint.setStrokeWidth(2);

        labelPaint.setColor(0x88FFFFFF);
        labelPaint.setTextSize(24f);
        labelPaint.setAntiAlias(true);

        setBackgroundColor(0xFF121214);
    }

    public void setBounds(float xMn, float xMx, float yMn, float yMx) {
        xMin = xMn; xMax = xMx; yMin = yMn; yMax = yMx;
        baseXMin = xMn; baseXMax = xMx; baseYMin = yMn; baseYMax = yMx;
        postInvalidate();
    }

    public void resetBounds() {
        xMin = baseXMin; xMax = baseXMax;
        yMin = baseYMin; yMax = baseYMax;
        postInvalidate();
    }

    public void addCurve(List<float[]> points, int color, String label) {
        curves.add(new CurveData(points, color, label));
        // Auto-scale Y to data
        if (!points.isEmpty()) {
            float mn = Float.MAX_VALUE, mx = -Float.MAX_VALUE;
            for (float[] p : points) {
                mn = Math.min(mn, p[1]);
                mx = Math.max(mx, p[1]);
            }
            float pad = (mx - mn) * 0.1f;
            if (pad < 0.01f) pad = 1;
            yMin = mn - pad; yMax = mx + pad;
            baseYMin = yMin; baseYMax = yMax;
        }
        postInvalidate();
    }

    public void clearCurves() {
        curves.clear();
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w = getWidth(), h = getHeight();
        float pad = 50;
        float plotW = w - pad * 2, plotH = h - pad * 2;

        if (plotW <= 0 || plotH <= 0) return;

        scaleX = plotW / (xMax - xMin);
        scaleY = plotH / (yMax - yMin);

        // Grid
        float gStep = niceStep(xMax - xMin);
        for (float v = (float)(Math.ceil(xMin / gStep) * gStep); v <= xMax; v += gStep) {
            float px = pad + (v - xMin) * scaleX;
            c.drawLine(px, pad, px, pad + plotH, gridPaint);
        }
        gStep = niceStep(yMax - yMin);
        for (float v = (float)(Math.ceil(yMin / gStep) * gStep); v <= yMax; v += gStep) {
            float py = pad + (yMax - v) * scaleY;
            c.drawLine(pad, py, pad + plotW, py, gridPaint);
        }

        // Axes (thicker)
        float ox = pad + (0 - xMin) * scaleX;
        float oy = pad + (yMax - 0) * scaleY;
        if (xMin <= 0 && xMax >= 0) c.drawLine(ox, pad, ox, pad + plotH, axisPaint);
        if (yMin <= 0 && yMax >= 0) c.drawLine(pad, oy, pad + plotW, oy, axisPaint);

        // Axis labels
        for (float v = (float)(Math.ceil(xMin / gStep) * gStep); v <= xMax; v += gStep) {
            float px = pad + (v - xMin) * scaleX;
            c.drawText(formatNum(v), px - 10, pad + plotH + 18, labelPaint);
        }
        for (float v = (float)(Math.ceil(yMin / gStep) * gStep); v <= yMax; v += gStep) {
            float py = pad + (yMax - v) * scaleY;
            c.drawText(formatNum(v), 4, py + 4, labelPaint);
        }

        // Curves
        for (CurveData cd : curves) {
            Paint linePaint = new Paint();
            linePaint.setColor(cd.color);
            linePaint.setStrokeWidth(3);
            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setAntiAlias(true);

            Path path = new Path();
            boolean first = true;
            for (float[] pt : cd.points) {
                float px = pad + (pt[0] - xMin) * scaleX;
                float py2 = pad + (yMax - pt[1]) * scaleY;
                if (first) { path.moveTo(px, py2); first = false; }
                else path.lineTo(px, py2);
            }
            c.drawPath(path, linePaint);

            // Label
            if (!cd.points.isEmpty()) {
                float[] last = cd.points.get(cd.points.size() - 1);
                float lx = pad + (last[0] - xMin) * scaleX;
                float ly = pad + (yMax - last[1]) * scaleY;
                labelPaint.setColor(cd.color);
                c.drawText(cd.label, Math.min(lx + 8, w - 60), ly - 4, labelPaint);
                labelPaint.setColor(0x88FFFFFF);
            }
        }
    }

    private float niceStep(float range) {
        float rough = range / 5;
        float mag = (float) Math.pow(10, Math.floor(Math.log10(rough)));
        float norm = rough / mag;
        if (norm < 1.5f) return mag;
        if (norm < 3.5f) return mag * 2;
        if (norm < 7.5f) return mag * 5;
        return mag * 10;
    }

    private String formatNum(float v) {
        if (Math.abs(v) < 0.0001f) return "0";
        if (Math.abs(v) >= 100) return String.format("%.0f", v);
        if (Math.abs(v) >= 1) return String.format("%.1f", v);
        return String.format("%.2f", v);
    }

    // Touch pan/zoom
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastX = e.getX(); lastY = e.getY();
                touched = true;
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = (lastX - e.getX()) / getWidth() * (xMax - xMin);
                float dy = (lastY - e.getY()) / getHeight() * (yMax - yMin);
                xMin += dx; xMax += dx;
                yMin += dy; yMax += dy;
                lastX = e.getX(); lastY = e.getY();
                postInvalidate();
                return true;
            case MotionEvent.ACTION_UP:
                if (touched && Math.abs(e.getX() - lastX) < 5 && Math.abs(e.getY() - lastY) < 5) {
                    // Double-tap: reset
                    resetBounds();
                }
                touched = false;
                return true;
        }
        return super.onTouchEvent(e);
    }

    static class CurveData {
        List<float[]> points;
        int color;
        String label;
        CurveData(List<float[]> p, int c, String l) { points = p; color = c; label = l; }
    }
}
