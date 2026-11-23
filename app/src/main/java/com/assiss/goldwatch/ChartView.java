package com.assiss.goldwatch;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Simple touchable line chart, drawn left (old) to right (new). */
public class ChartView extends View {
    private long[] t = new long[0];
    private double[] v = new double[0];
    private boolean intraday;
    private int sel = -1;
    private String emptyText = "جاري التحميل...";

    private final float dp;
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tip = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tipBg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);

    public ChartView(Context c, AttributeSet a) {
        super(c, a);
        dp = getResources().getDisplayMetrics().density;
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(2.2f * dp);
        line.setStrokeJoin(Paint.Join.ROUND);
        line.setStrokeCap(Paint.Cap.ROUND);
        fill.setStyle(Paint.Style.FILL);
        grid.setColor(0x33D4AF37);
        grid.setStrokeWidth(1 * dp);
        grid.setPathEffect(new DashPathEffect(new float[]{6 * dp, 5 * dp}, 0));
        text.setColor(0xFF8C8676);
        text.setTextSize(10.5f * dp);
        tip.setColor(0xFFF5F1E6);
        tip.setTextSize(12 * dp);
        tip.setFakeBoldText(true);
        tipBg.setColor(0xEE2B2414);
        dot.setStyle(Paint.Style.FILL);
    }

    void setEmpty(String msg) {
        t = new long[0];
        v = new double[0];
        sel = -1;
        emptyText = msg;
        invalidate();
    }

    void setData(long[] times, double[] values, boolean intraday) {
        this.t = times;
        this.v = values;
        this.intraday = intraday;
        this.sel = -1;
        invalidate();
    }

    private int color() {
        if (v.length < 2) return 0xFFD4AF37;
        return v[v.length - 1] >= v[0] ? 0xFF4CD964 : 0xFFFF5A5F;
    }

    private float left() { return 6 * dp; }
    private float right() { return getWidth() - 6 * dp; }
    private float top() { return 30 * dp; }
    private float bottom() { return getHeight() - 20 * dp; }

    private float xAt(int i) {
        if (v.length < 2) return left();
        return left() + (right() - left()) * i / (float) (v.length - 1);
    }

    private double min, max;

    private float yAt(double val) {
        double span = max - min;
        if (span <= 0) span = 1;
        return (float) (bottom() - (val - min) / span * (bottom() - top()));
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (v.length < 2) {
            text.setTextAlign(Paint.Align.CENTER);
            c.drawText(emptyText, getWidth() / 2f, getHeight() / 2f, text);
            return;
        }
        min = Double.MAX_VALUE;
        max = -Double.MAX_VALUE;
        int iMin = 0, iMax = 0;
        for (int i = 0; i < v.length; i++) {
            if (v[i] < min) { min = v[i]; iMin = i; }
            if (v[i] > max) { max = v[i]; iMax = i; }
        }
        double pad = (max - min) * 0.08;
        double realMin = min, realMax = max;
        min -= pad;
        max += pad;

        int col = color();
        // grid lines at real high / low
        float yHi = yAt(realMax), yLo = yAt(realMin);
        c.drawLine(left(), yHi, right(), yHi, grid);
        c.drawLine(left(), yLo, right(), yLo, grid);
        text.setTextAlign(Paint.Align.LEFT);
        c.drawText("أعلى " + Updater.fmt(realMax), left(), yHi - 4 * dp, text);
        c.drawText("أقل " + Updater.fmt(realMin), left(), yLo + 13 * dp, text);

        Path p = new Path();
        for (int i = 0; i < v.length; i++) {
            float x = xAt(i), y = yAt(v[i]);
            if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        Path f = new Path(p);
        f.lineTo(xAt(v.length - 1), bottom());
        f.lineTo(xAt(0), bottom());
        f.close();
        fill.setShader(new LinearGradient(0, top(), 0, bottom(),
                (col & 0x00FFFFFF) | 0x55000000, (col & 0x00FFFFFF), Shader.TileMode.CLAMP));
        c.drawPath(f, fill);
        line.setColor(col);
        c.drawPath(p, line);

        // end dot
        dot.setColor(col);
        c.drawCircle(xAt(v.length - 1), yAt(v[v.length - 1]), 3.5f * dp, dot);

        // x labels: first and last
        text.setTextAlign(Paint.Align.LEFT);
        c.drawText(fmtTime(t[0]), left(), getHeight() - 5 * dp, text);
        text.setTextAlign(Paint.Align.RIGHT);
        c.drawText(fmtTime(t[t.length - 1]), right(), getHeight() - 5 * dp, text);

        // selection tooltip
        if (sel >= 0 && sel < v.length) {
            float x = xAt(sel), y = yAt(v[sel]);
            Paint vl = new Paint(grid);
            vl.setColor(0x88F1D77A);
            c.drawLine(x, top() - 4 * dp, x, bottom(), vl);
            dot.setColor(0xFFF1D77A);
            c.drawCircle(x, y, 5 * dp, dot);
            String s = Updater.fmt(v[sel]) + "  •  " + fmtTime(t[sel]);
            float w = tip.measureText(s) + 16 * dp;
            float bx = Math.max(left(), Math.min(x - w / 2, right() - w));
            c.drawRoundRect(bx, 2 * dp, bx + w, 24 * dp, 8 * dp, 8 * dp, tipBg);
            tip.setTextAlign(Paint.Align.CENTER);
            c.drawText(s, bx + w / 2, 17 * dp, tip);
        }
    }

    private String fmtTime(long ms) {
        String pat = intraday ? "HH:mm" : "dd/MM";
        return new SimpleDateFormat(pat, Locale.US).format(new Date(ms));
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (v.length < 2) return false;
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                getParent().requestDisallowInterceptTouchEvent(true);
                float fx = (e.getX() - left()) / (right() - left());
                int i = Math.round(fx * (v.length - 1));
                sel = Math.max(0, Math.min(v.length - 1, i));
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;
        }
        return super.onTouchEvent(e);
    }
}
