package com.example.myapplication;

import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Expression;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure computation — generates (x,y) point lists for all 2D graph modes.
 * No Android dependencies, testable in JUnit.
 */
public class PlotEngine {

    public static List<float[]> evalCartesian(String expr, double xMin, double xMax, double step) {
        List<float[]> pts = new ArrayList<>();
        try {
            Argument x = new Argument("x");
            Expression e = new Expression(expr, x);
            if (!e.checkSyntax()) return pts;
            for (double v = xMin; v <= xMax + step * 0.5; v += step) {
                x.setArgumentValue(v);
                double y = e.calculate();
                if (!Double.isNaN(y) && !Double.isInfinite(y))
                    pts.add(new float[]{(float) v, (float) y});
            }
        } catch (Exception ignored) {}
        return pts;
    }

    public static List<float[]> evalParametric(String xExpr, String yExpr,
                                                double tMin, double tMax, double tStep) {
        List<float[]> pts = new ArrayList<>();
        try {
            Argument t = new Argument("t");
            Expression ex = new Expression(xExpr, t);
            Expression ey = new Expression(yExpr, t);
            if (!ex.checkSyntax() || !ey.checkSyntax()) return pts;
            for (double v = tMin; v <= tMax + tStep * 0.5; v += tStep) {
                t.setArgumentValue(v);
                double x = ex.calculate(), y = ey.calculate();
                if (!Double.isNaN(x) && !Double.isInfinite(x) && !Double.isNaN(y) && !Double.isInfinite(y))
                    pts.add(new float[]{(float) x, (float) y});
            }
        } catch (Exception ignored) {}
        return pts;
    }

    public static List<float[]> evalPolar(String rExpr, double tMin, double tMax, double tStep) {
        List<float[]> pts = new ArrayList<>();
        try {
            Argument theta = new Argument("θ");
            Expression e = new Expression(rExpr, theta);
            if (!e.checkSyntax()) return pts;
            for (double v = tMin; v <= tMax + tStep * 0.5; v += tStep) {
                theta.setArgumentValue(v);
                double r = e.calculate();
                if (!Double.isNaN(r) && !Double.isInfinite(r))
                    pts.add(new float[]{(float)(r * Math.cos(v)), (float)(r * Math.sin(v))});
            }
        } catch (Exception ignored) {}
        return pts;
    }
}
