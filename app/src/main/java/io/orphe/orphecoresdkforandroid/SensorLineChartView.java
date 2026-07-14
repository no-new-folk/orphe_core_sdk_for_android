package io.orphe.orphecoresdkforandroid;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SensorLineChartView extends View {
    private static final int MAX_POINTS = 120;

    private final Paint mGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mAxisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int[] mSeriesColors = new int[6];
    private final List<double[]> mPoints = new ArrayList<>();
    private final RectF mChartBounds = new RectF();

    private int mBackgroundColor;
    private int mTextColor;
    private String mTitle = "";
    private String[] mLabels = new String[0];
    private String mUnavailableMessage;

    public SensorLineChartView(Context context) {
        super(context);
        init();
    }

    public SensorLineChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SensorLineChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);

        mBackgroundColor = getContext().getColor(R.color.sensor_chart_background);
        mTextColor = getContext().getColor(R.color.sensor_chart_text);
        mSeriesColors[0] = getContext().getColor(R.color.sensor_chart_series_red);
        mSeriesColors[1] = getContext().getColor(R.color.sensor_chart_series_green);
        mSeriesColors[2] = getContext().getColor(R.color.sensor_chart_series_blue);
        mSeriesColors[3] = getContext().getColor(R.color.sensor_chart_series_orange);
        mSeriesColors[4] = getContext().getColor(R.color.sensor_chart_series_purple);
        mSeriesColors[5] = getContext().getColor(R.color.sensor_chart_series_teal);

        mGridPaint.setColor(getContext().getColor(R.color.sensor_chart_grid));
        mGridPaint.setStrokeWidth(1.0f);
        mGridPaint.setStyle(Paint.Style.STROKE);

        mAxisPaint.setColor(getContext().getColor(R.color.sensor_chart_axis));
        mAxisPaint.setStrokeWidth(1.5f);
        mAxisPaint.setStyle(Paint.Style.STROKE);

        mTextPaint.setColor(mTextColor);
        mTextPaint.setTextSize(sp(10));

        mLinePaint.setStrokeWidth(dp(1.5f));
        mLinePaint.setStyle(Paint.Style.STROKE);
    }

    public void configure(String title, String... labels) {
        mTitle = title;
        mLabels = labels;
        invalidate();
    }

    public void addValues(double... values) {
        if (mUnavailableMessage != null || values == null || values.length == 0) {
            return;
        }
        mPoints.add(values.clone());
        while (mPoints.size() > MAX_POINTS) {
            mPoints.remove(0);
        }
        invalidate();
    }

    public void clear() {
        mPoints.clear();
        invalidate();
    }

    public void setUnavailableMessage(@Nullable String unavailableMessage) {
        mUnavailableMessage = unavailableMessage;
        mPoints.clear();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final int width = getWidth();
        final int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        canvas.drawColor(mBackgroundColor);

        final float paddingLeft = dp(8);
        final float paddingTop = dp(20);
        final float paddingRight = dp(8);
        final float paddingBottom = dp(14);
        mChartBounds.set(paddingLeft, paddingTop, width - paddingRight, height - paddingBottom);

        drawHeader(canvas);
        drawGrid(canvas);
        if (mUnavailableMessage != null) {
            drawUnavailableMessage(canvas);
            return;
        }
        drawSeries(canvas);
    }

    private void drawUnavailableMessage(Canvas canvas) {
        final Paint.Align previousAlign = mTextPaint.getTextAlign();
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(
                mUnavailableMessage,
                mChartBounds.centerX(),
                mChartBounds.centerY() - (mTextPaint.ascent() + mTextPaint.descent()) / 2.0f,
                mTextPaint
        );
        mTextPaint.setTextAlign(previousAlign);
    }

    private void drawHeader(Canvas canvas) {
        mTextPaint.setFakeBoldText(true);
        canvas.drawText(mTitle, dp(8), dp(14), mTextPaint);
        mTextPaint.setFakeBoldText(false);

        float x = getWidth() - dp(8);
        for (int i = mLabels.length - 1; i >= 0; i--) {
            mTextPaint.setColor(mSeriesColors[i % mSeriesColors.length]);
            final String label = mLabels[i];
            final float labelWidth = mTextPaint.measureText(label);
            x -= labelWidth;
            canvas.drawText(label, x, dp(14), mTextPaint);
            x -= dp(8);
        }
        mTextPaint.setColor(mTextColor);
    }

    private void drawGrid(Canvas canvas) {
        for (int i = 0; i <= 4; i++) {
            final float y = mChartBounds.top + (mChartBounds.height() * i / 4.0f);
            canvas.drawLine(mChartBounds.left, y, mChartBounds.right, y, mGridPaint);
        }
        for (int i = 0; i <= 4; i++) {
            final float x = mChartBounds.left + (mChartBounds.width() * i / 4.0f);
            canvas.drawLine(x, mChartBounds.top, x, mChartBounds.bottom, mGridPaint);
        }
        canvas.drawRect(mChartBounds, mAxisPaint);
    }

    private void drawSeries(Canvas canvas) {
        if (mPoints.size() < 2) {
            return;
        }

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (double[] point : mPoints) {
            for (double value : point) {
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
        }

        if (min == Double.MAX_VALUE || max == -Double.MAX_VALUE) {
            return;
        }

        if (min > 0) {
            min = 0;
        }
        if (max < 0) {
            max = 0;
        }
        if (Math.abs(max - min) < 0.000001) {
            max += 1.0;
            min -= 1.0;
        }

        if (min <= 0 && max >= 0) {
            final float zeroY = valueToY(0, min, max);
            canvas.drawLine(mChartBounds.left, zeroY, mChartBounds.right, zeroY, mAxisPaint);
        }

        final int seriesCount = mPoints.get(0).length;
        for (int series = 0; series < seriesCount; series++) {
            mLinePaint.setColor(mSeriesColors[series % mSeriesColors.length]);
            final Path path = new Path();
            for (int i = 0; i < mPoints.size(); i++) {
                final double[] point = mPoints.get(i);
                if (series >= point.length) {
                    continue;
                }
                final float x = mChartBounds.left + (mChartBounds.width() * i / (mPoints.size() - 1));
                final float y = valueToY(point[series], min, max);
                if (i == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }
            canvas.drawPath(path, mLinePaint);
        }
    }

    private float valueToY(double value, double min, double max) {
        final double ratio = (value - min) / (max - min);
        return (float) (mChartBounds.bottom - (mChartBounds.height() * ratio));
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}
