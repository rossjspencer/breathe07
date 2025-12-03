package com.example.smartair;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class TrendChartView extends View {

    private final Paint axisPaint = new Paint();
    private final Paint linePaint = new Paint();
    private final Paint fillPaint = new Paint();
    private final Paint textPaint = new Paint();
    private List<Integer> points = new ArrayList<>();
    private int rangeDays = 7;

    public TrendChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TrendChartView(Context context) {
        super(context);
        init();
    }

    private void init() {
        axisPaint.setColor(Color.parseColor("#BDBDBD"));
        axisPaint.setStrokeWidth(3f);

        linePaint.setColor(Color.parseColor("#1E88E5"));
        linePaint.setStrokeWidth(6f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);

        fillPaint.setColor(Color.parseColor("#331E88E5"));
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAntiAlias(true);

        textPaint.setColor(Color.parseColor("#616161"));
        textPaint.setTextSize(28f);
    }

    public void setData(List<Integer> points, int rangeDays) {
        this.points = points != null ? points : new ArrayList<>();
        this.rangeDays = rangeDays;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        int padding = 32;

        if (points == null || points.isEmpty()) {
            canvas.drawText("No data yet", padding, h / 2f, textPaint);
            return;
        }

        float maxVal = 1f;
        for (int val : points) {
            if (val > maxVal) maxVal = val;
        }

        float usableW = w - padding * 2f;
        float usableH = h - padding * 2f;
        float stepX = usableW / Math.max(1, points.size() - 1);

        // Draw axes
        canvas.drawLine(padding, padding, padding, padding + usableH, axisPaint);
        canvas.drawLine(padding, padding + usableH, padding + usableW, padding + usableH, axisPaint);

        float prevX = padding;
        float prevY = padding + usableH - (points.get(0) / maxVal) * usableH;

        for (int i = 1; i < points.size(); i++) {
            float x = padding + stepX * i;
            float y = padding + usableH - (points.get(i) / maxVal) * usableH;
            canvas.drawLine(prevX, prevY, x, y, linePaint);
            canvas.drawCircle(x, y, 6f, linePaint);
            prevX = x;
            prevY = y;
        }

}
}
