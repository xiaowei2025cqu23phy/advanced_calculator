package com.example.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.OverScroller;

import java.util.ArrayList;
import java.util.List;

/**
 * 2D chart with fling-to-pan inertia.
 */
public class SimpleChartView extends View {

    private final Paint gridPaint = new Paint();
    private final Paint axisPaint = new Paint();
    private final Paint labelPaint = new Paint();
    private final List<CurveData> curves = new ArrayList<>();
    private final List<Paint> curvePaints = new ArrayList<>();
    private final OverScroller scroller;
    private final int minFlingVelocity;

    private float xMin = -10, xMax = 10;
    private float yMin = -10, yMax = 10;
    private float baseXMin, baseXMax, baseYMin, baseYMax;

    // Touch
    private VelocityTracker velocityTracker;
    private float lastX, lastY;
    private boolean touched = false;

    public SimpleChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        scroller = new OverScroller(context);
        minFlingVelocity = ViewConfiguration.get(context).getScaledMinimumFlingVelocity();

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
        Paint p = new Paint();
        p.setColor(color); p.setStrokeWidth(3);
        p.setStyle(Paint.Style.STROKE); p.setAntiAlias(true);
        curvePaints.add(p);
        if (!points.isEmpty()) {
            float mn = Float.MAX_VALUE, mx = -Float.MAX_VALUE;
            for (float[] pt2 : points) { mn = Math.min(mn, pt2[1]); mx = Math.max(mx, pt2[1]); }
            float pad = (mx - mn) * 0.1f;
            if (pad < 0.01f) pad = 1;
            yMin = mn - pad; yMax = mx + pad;
            baseYMin = yMin; baseYMax = yMax;
        }
        postInvalidate();
    }

    public void clearCurves() { curves.clear(); curvePaints.clear(); postInvalidate(); }

    // ── Fling via OverScroller ──

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            // The scroller gives us pixel offsets; convert to data-space deltas
            float xRange = xMax - xMin;
            float yRange = yMax - yMin;
            float w = getWidth();
            float h = getHeight();
            if (w <= 0 || h <= 0) return;

            float newXMin = baseXMin - scroller.getCurrX() / (w * 0.5f) * xRange;
            float newXMax = newXMin + xRange;
            float newYMin = baseYMin + scroller.getCurrY() / (h * 0.5f) * yRange;
            float newYMax = newYMin + yRange;

            xMin = newXMin; xMax = newXMax;
            yMin = newYMin; yMax = newYMax;
            postInvalidateOnAnimation();
        }
    }

    // ── Drawing ──

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w = getWidth(), h = getHeight();
        float pad = 50;
        float plotW = w - pad * 2, plotH = h - pad * 2;
        if (plotW <= 0 || plotH <= 0) return;

        float scaleX = plotW / (xMax - xMin);
        float scaleY = plotH / (yMax - yMin);

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

        // Axes
        float ox = pad + (0 - xMin) * scaleX;
        float oy = pad + (yMax - 0) * scaleY;
        if (xMin <= 0 && xMax >= 0) c.drawLine(ox, pad, ox, pad + plotH, axisPaint);
        if (yMin <= 0 && yMax >= 0) c.drawLine(pad, oy, pad + plotW, oy, axisPaint);

        // Labels
        for (float v = (float)(Math.ceil(xMin / gStep) * gStep); v <= xMax; v += gStep) {
            float px = pad + (v - xMin) * scaleX;
            c.drawText(formatNum(v), px - 10, pad + plotH + 18, labelPaint);
        }
        for (float v = (float)(Math.ceil(yMin / gStep) * gStep); v <= yMax; v += gStep) {
            float py = pad + (yMax - v) * scaleY;
            c.drawText(formatNum(v), 4, py + 4, labelPaint);
        }

        // Curves
        for (int ci = 0; ci < curves.size(); ci++) {
            CurveData cd = curves.get(ci);
            Paint lp = ci < curvePaints.size() ? curvePaints.get(ci) : curvePaints.get(0);
            Path path = new Path();
            boolean first = true;
            for (float[] pt : cd.points) {
                float px = pad + (pt[0] - xMin) * scaleX;
                float py2 = pad + (yMax - pt[1]) * scaleY;
                if (first) { path.moveTo(px, py2); first = false; }
                else path.lineTo(px, py2);
            }
            c.drawPath(path, lp);
            if (!cd.points.isEmpty()) {
                float[] last = cd.points.get(cd.points.size() - 1);
                float lx = pad + (last[0] - xMin) * scaleX;
                float ly = pad + (yMax - last[1]) * scaleY;
                c.drawText(cd.label, Math.min(lx + 8, w - 60), ly - 4, lp);
            }
        }
    }

    // ── Touch: drag + fling ──

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (velocityTracker == null) velocityTracker = VelocityTracker.obtain();
        velocityTracker.addMovement(e);

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                scroller.forceFinished(true);
                lastX = e.getX(); lastY = e.getY();
                touched = true;
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = (lastX - e.getX()) / getWidth()  * (xMax - xMin);
                float dy = (lastY - e.getY()) / getHeight() * (yMax - yMin);
                xMin += dx; xMax += dx;
                yMin += dy; yMax += dy;
                lastX = e.getX(); lastY = e.getY();
                postInvalidate();
                return true;

            case MotionEvent.ACTION_UP:
                velocityTracker.computeCurrentVelocity(1000);
                float vx = velocityTracker.getXVelocity();
                float vy = velocityTracker.getYVelocity();

                if (Math.abs(vx) > minFlingVelocity || Math.abs(vy) > minFlingVelocity) {
                    // Convert velocity to scroller units and start fling
                    baseXMin = xMin; baseXMax = xMax;
                    baseYMin = yMin; baseYMax = yMax;
                    scroller.forceFinished(true);
                    scroller.fling(
                        Math.round(getWidth()  * 0.5f * (xMin / (xMax - xMin))),
                        Math.round(getHeight() * 0.5f * (yMin / (yMax - yMin))),
                        (int)-vx * 2, (int)-vy * 2,
                        Integer.MIN_VALUE, Integer.MAX_VALUE,
                        Integer.MIN_VALUE, Integer.MAX_VALUE
                    );
                    postInvalidateOnAnimation();
                } else if (touched && Math.abs(e.getX() - lastX) < 5 && Math.abs(e.getY() - lastY) < 5) {
                    resetBounds();  // tap → reset
                }
                touched = false;
                if (velocityTracker != null) { velocityTracker.recycle(); velocityTracker = null; }
                return true;
        }
        return super.onTouchEvent(e);
    }

    // ── Helpers ──

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

    static class CurveData {
        List<float[]> points; int color; String label;
        CurveData(List<float[]> p, int c, String l) { points = p; color = c; label = l; }
    }
}
