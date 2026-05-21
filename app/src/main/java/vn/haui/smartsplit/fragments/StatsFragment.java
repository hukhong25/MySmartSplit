package vn.haui.smartsplit.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import vn.haui.smartsplit.R;
import vn.haui.smartsplit.models.Expense;
import vn.haui.smartsplit.views.DonutChartView;

public class StatsFragment extends Fragment {

    private DonutChartView donutChartView;
    private LinearLayout layoutCategories;
    private TextView tvTrendPercent, tvTrendDesc;
    private TextView tvBarLastMonthAmount, tvBarThisMonthAmount;
    private ChipGroup chipGroupPeriod;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Period: 0=week, 1=month, 2=year
    private int selectedPeriod = 1;

    // Category colors
    private static final int[] CAT_COLORS = {
            Color.parseColor("#1CC29F"),  // Food
            Color.parseColor("#3B82F6"),  // Travel
            Color.parseColor("#8B5CF6"),  // Shopping
            Color.parseColor("#F59E0B"),  // Entertainment
            Color.parseColor("#9CA3AF")   // Other
    };

    // Khởi tạo danh sách nhãn danh mục trống, sẽ được gán từ Resources khi Fragment sẵn sàng
    private String[] catLabels;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Đọc mảng nhãn danh mục từ tài nguyên ngôn ngữ hệ thống
        catLabels = new String[]{
                getString(R.string.cat_food),
                getString(R.string.cat_travel),
                getString(R.string.cat_shopping),
                getString(R.string.cat_entertainment),
                getString(R.string.cat_other)
        };

        donutChartView      = view.findViewById(R.id.donutChartView);
        layoutCategories    = view.findViewById(R.id.layoutCategories);
        tvTrendPercent      = view.findViewById(R.id.tvTrendPercent);
        tvTrendDesc         = view.findViewById(R.id.tvTrendDesc);
        tvBarLastMonthAmount = view.findViewById(R.id.tvBarLastMonthAmount);
        tvBarThisMonthAmount = view.findViewById(R.id.tvBarThisMonthAmount);
        chipGroupPeriod     = view.findViewById(R.id.chipGroupPeriod);

        chipGroupPeriod.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipWeek)        selectedPeriod = 0;
            else if (id == R.id.chipMonth)  selectedPeriod = 1;
            else if (id == R.id.chipYear)   selectedPeriod = 2;
            loadStats();
        });

        loadStats();
    }

    private void loadStats() {
        if (mAuth.getCurrentUser() == null || !isAdded()) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collectionGroup("expenses")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null || !isAdded()) return;

                    double[] catTotals = new double[catLabels.length];
                    double totalThisMonth = 0;
                    double totalLastMonth = 0;

                    Calendar now = Calendar.getInstance();
                    int thisMonth = now.get(Calendar.MONTH);
                    int thisYear  = now.get(Calendar.YEAR);

                    for (QueryDocumentSnapshot doc : value) {
                        String status = doc.getString("status");
                        if (!Expense.STATUS_COMPLETED.equals(status)) continue;

                        Boolean isSettlement = doc.getBoolean("settlement");
                        if (isSettlement != null && isSettlement) continue;

                        @SuppressWarnings("unchecked")
                        Map<String, Object> splitDetails = (Map<String, Object>) doc.get("splitDetails");
                        if (splitDetails == null || !splitDetails.containsKey(uid)) continue;

                        double userShare = 0;
                        try {
                            userShare = Double.parseDouble(splitDetails.get(uid).toString());
                        } catch (Exception ignored) {}

                        if (userShare <= 0) continue;

                        Long timestamp = doc.getLong("timestamp");
                        if (timestamp == null) continue;

                        Calendar expCal = Calendar.getInstance();
                        expCal.setTimeInMillis(timestamp);

                        int expMonth = expCal.get(Calendar.MONTH);
                        int expYear  = expCal.get(Calendar.YEAR);

                        boolean inPeriod = false;
                        if (selectedPeriod == 0) { // week
                            long diffMs = now.getTimeInMillis() - expCal.getTimeInMillis();
                            inPeriod = diffMs >= 0 && diffMs <= 7L * 24 * 3600 * 1000;
                        } else if (selectedPeriod == 1) { // month
                            inPeriod = (expMonth == thisMonth && expYear == thisYear);
                        } else { // year
                            inPeriod = (expYear == thisYear);
                        }

                        if (expMonth == thisMonth && expYear == thisYear) {
                            totalThisMonth += userShare;
                        }
                        int lastMonthVal = thisMonth == 0 ? 11 : thisMonth - 1;
                        int lastMonthYear = thisMonth == 0 ? thisYear - 1 : thisYear;
                        if (expMonth == lastMonthVal && expYear == lastMonthYear) {
                            totalLastMonth += userShare;
                        }

                        if (!inPeriod) continue;

                        String desc = doc.getString("description");
                        if (desc == null) desc = "";
                        desc = desc.toLowerCase();

                        int catIdx = 4; // Other
                        if (desc.contains("ăn") || desc.contains("uống") ||
                                desc.contains("cà phê") || desc.contains("cafe") ||
                                desc.contains("bữa") || desc.contains("food")) {
                            catIdx = 0;
                        } else if (desc.contains("xe") || desc.contains("grab") ||
                                desc.contains("taxi") || desc.contains("vé") ||
                                desc.contains("du lịch") || desc.contains("travel") || desc.contains("xăng")) {
                            catIdx = 1;
                        } else if (desc.contains("mua") || desc.contains("shop") ||
                                desc.contains("quần") || desc.contains("áo") || desc.contains("mỹ phẩm")) {
                            catIdx = 2;
                        } else if (desc.contains("phim") || desc.contains("game") ||
                                desc.contains("giải trí") || desc.contains("karaoke") || desc.contains("vé xem")) {
                            catIdx = 3;
                        }
                        catTotals[catIdx] += userShare;
                    }

                    updateUI(catTotals, totalThisMonth, totalLastMonth);
                });
    }

    private void updateUI(double[] catTotals, double totalThisMonth, double totalLastMonth) {
        if (!isAdded()) return;

        DecimalFormat fmt = new DecimalFormat("#,###");
        double total = 0;
        for (double v : catTotals) total += v;

        // Build donut segments
        List<DonutChartView.Segment> segments = new ArrayList<>();
        for (int i = 0; i < catLabels.length; i++) {
            if (catTotals[i] > 0) {
                segments.add(new DonutChartView.Segment(catLabels[i], (float) catTotals[i], CAT_COLORS[i]));
            }
        }

        String centerValue = getString(R.string.currency_short_prefix, formatShort(total));
        donutChartView.setData(segments, getString(R.string.donut_center_title), centerValue);

        // Category rows
        layoutCategories.removeAllViews();
        for (int i = 0; i < catLabels.length; i++) {
            if (catTotals[i] == 0) continue;
            int pct = total > 0 ? (int) Math.round(catTotals[i] / total * 100) : 0;
            layoutCategories.addView(buildCategoryRow(catLabels[i], pct, catTotals[i], CAT_COLORS[i], fmt));
        }

        // Monthly trend
        double diff = totalLastMonth > 0
                ? ((totalThisMonth - totalLastMonth) / totalLastMonth) * 100 : 0;
        String arrow = diff <= 0 ? "↓" : "↑";
        int trendColor = diff <= 0
                ? Color.parseColor("#1CC29F") : Color.parseColor("#E53935");

        tvTrendPercent.setText(getString(R.string.trend_percent_format, arrow, (int) Math.abs(diff)));
        tvTrendPercent.setTextColor(trendColor);

        if (diff <= 0) {
            tvTrendDesc.setText(getString(R.string.trend_desc_less_format, (int) Math.abs(diff)));
        } else {
            tvTrendDesc.setText(getString(R.string.trend_desc_more_format, (int) Math.abs(diff)));
        }

        tvBarLastMonthAmount.setText(getString(R.string.currency_short_prefix, formatShort(totalLastMonth)));
        tvBarThisMonthAmount.setText(getString(R.string.currency_short_prefix, formatShort(totalThisMonth)));
    }

    private View buildCategoryRow(String label, int pct, double amount, int color, DecimalFormat fmt) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 12);
        row.setLayoutParams(lp);

        // Color dot
        View dot = new View(requireContext());
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(24, 24);
        dot.setLayoutParams(dotLp);
        dot.setBackground(createCircleDrawable(color));
        row.addView(dot);

        // Label + pct
        LinearLayout textCol = new LinearLayout(requireContext());
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textLp.setMarginStart(12);
        textCol.setLayoutParams(textLp);

        TextView tvLabel = new TextView(requireContext());
        tvLabel.setText(label);
        tvLabel.setTextSize(14);
        tvLabel.setTextColor(resolveColor(android.R.attr.textColorPrimary));
        tvLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        textCol.addView(tvLabel);

        TextView tvPct = new TextView(requireContext());
        tvPct.setText(getString(R.string.percent_format, pct));
        tvPct.setTextSize(12);
        tvPct.setTextColor(resolveColor(android.R.attr.textColorSecondary));
        textCol.addView(tvPct);

        row.addView(textCol);

        // Amount
        TextView tvAmt = new TextView(requireContext());
        tvAmt.setText(getString(R.string.currency_prefix_format, fmt.format(amount)));
        tvAmt.setTextSize(14);
        tvAmt.setTypeface(null, android.graphics.Typeface.BOLD);
        tvAmt.setTextColor(resolveColor(android.R.attr.textColorPrimary));
        row.addView(tvAmt);

        return row;
    }

    private android.graphics.drawable.ShapeDrawable createCircleDrawable(int color) {
        android.graphics.drawable.ShapeDrawable d =
                new android.graphics.drawable.ShapeDrawable(
                        new android.graphics.drawable.shapes.OvalShape());
        d.getPaint().setColor(color);
        return d;
    }

    private int resolveColor(int attr) {
        int[] attrs = { attr };
        android.content.res.TypedArray ta =
                requireContext().obtainStyledAttributes(attrs);
        int c = ta.getColor(0, Color.BLACK);
        ta.recycle();
        return c;
    }

    private String formatShort(double value) {
        if (value >= 1_000_000) return String.format(Locale.US, "%.1fM", value / 1_000_000);
        if (value >= 1_000)     return String.format(Locale.US, "%.0fk", value / 1_000);
        return String.valueOf((long) value);
    }
}