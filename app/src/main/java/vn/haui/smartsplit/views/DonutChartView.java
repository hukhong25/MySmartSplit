package vn.haui.smartsplit.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import vn.haui.smartsplit.R;

/**
 * Custom View vẽ Donut Chart bằng Canvas (không dùng thư viện ngoài).
 * Sử dụng: gọi setData() với danh sách segments trước khi hiển thị.
 */
public class DonutChartView extends View {

    public static class Segment {
        public String label;
        public float value;
        public int color;
        public String centerLabel;
        public String centerValue;

        public Segment(String label, float value, int color) {
            this.label = label;
            this.value = value;
            this.color = color;
        }
    }

    private final List<Segment> segments = new ArrayList<>();
    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint subTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rectF = new RectF();

    private String centerLabel = String.valueOf(R.string.donut_center_title);
    private String centerValue = String.valueOf(R.string.currency_short_prefix);
    private float strokeWidth = 0f;  // will be computed

    public DonutChartView(Context context) {
        super(context);
        init();
    }

    public DonutChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DonutChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeCap(Paint.Cap.BUTT);

        centerPaint.setStyle(Paint.Style.FILL);
        centerPaint.setColor(Color.TRANSPARENT);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setFakeBoldText(true);

        subTextPaint.setTextAlign(Paint.Align.CENTER);
        subTextPaint.setStyle(Paint.Style.FILL);
    }

    /**
     * Đặt dữ liệu cho chart.
     * @param data  Danh sách segment
     * @param label Label hiển thị ở giữa donut
     * @param value Giá trị hiển thị ở giữa donut
     */
    public void setData(List<Segment> data, String label, String value) {
        segments.clear();
        if (data != null) segments.addAll(data);
        this.centerLabel = label;
        this.centerValue = value;
        invalidate();
    }

    /** Giải phóng data (empty state) */
    public void clearData() {
        segments.clear();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        float size = Math.min(w, h);
        float cx = w / 2f;
        float cy = h / 2f;

        strokeWidth = size * 0.18f;
        float radius = (size / 2f) - strokeWidth / 2f - 4f;

        rectF.set(cx - radius, cy - radius, cx + radius, cy + radius);

        arcPaint.setStrokeWidth(strokeWidth);

        if (segments.isEmpty()) {
            // Empty state – single grey ring
            arcPaint.setColor(Color.parseColor("#E8EFEC"));
            canvas.drawArc(rectF, 0, 360, false, arcPaint);
        } else {
            float total = 0;
            for (Segment s : segments) total += s.value;

            float startAngle = -90f;
            float gap = 2f; // gap degrees between segments
            for (Segment s : segments) {
                float sweep = (s.value / total) * (360f - gap * segments.size());
                arcPaint.setColor(s.color);
                canvas.drawArc(rectF, startAngle, sweep, false, arcPaint);
                startAngle += sweep + gap;
            }
        }

        // ── Centre text ──
        float density = getResources().getDisplayMetrics().density;

        // Value (large)
        textPaint.setTextSize(size * 0.13f);
        textPaint.setColor(resolveTextColor());
        canvas.drawText(centerValue, cx, cy + size * 0.04f, textPaint);

        // Label (small)
        subTextPaint.setTextSize(size * 0.07f);
        subTextPaint.setColor(resolveSubTextColor());
        canvas.drawText(centerLabel, cx, cy - size * 0.07f, subTextPaint);
    }

    private int resolveTextColor() {
        // Use theme-aware color if available, else fallback
        try {
            int[] attrs = { android.R.attr.textColorPrimary };
            android.content.res.TypedArray ta = getContext().obtainStyledAttributes(attrs);
            int color = ta.getColor(0, Color.parseColor("#1A2420"));
            ta.recycle();
            return color;
        } catch (Exception e) {
            return Color.parseColor("#1A2420");
        }
    }

    private int resolveSubTextColor() {
        try {
            int[] attrs = { android.R.attr.textColorSecondary };
            android.content.res.TypedArray ta = getContext().obtainStyledAttributes(attrs);
            int color = ta.getColor(0, Color.parseColor("#6B7C79"));
            ta.recycle();
            return color;
        } catch (Exception e) {
            return Color.parseColor("#6B7C79");
        }
    }
}
