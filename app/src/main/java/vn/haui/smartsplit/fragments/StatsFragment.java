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
import java.util.Map;

import vn.haui.smartsplit.R;
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
    private static final String[] CAT_LABELS = {
            "Ăn uống", "Di chuyển", "Mua sắm", "Giải trí", "Khác"
    };

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

                    double[] catTotals = new double[CAT_LABELS.length];
                    double totalThisMonth = 0;
                    double totalLastMonth = 0;

                    Calendar now = Calendar.getInstance();
                    int thisMonth = now.get(Calendar.MONTH);
                    int thisYear  = now.get(Calendar.YEAR);

                    for (QueryDocumentSnapshot doc : value) {
                        // Only expenses involving this user
                        String payerId = doc.getString("payerId");
                        @SuppressWarnings("unchecked")
                        Map<String, Object> splitDetails =
                                (Map<String, Object>) doc.get("splitDetails");
                        if (splitDetails == null) continue;

                        double userShare = 0;
                        if (uid.equals(payerId)) {
                            // User paid – count total
                            Object amtObj = doc.get("amount");
                            if (amtObj != null) {
                                try { userShare = Double.parseDouble(amtObj.toString()); }
                                catch (NumberFormatException ignored) {}
                            }
                        } else if (splitDetails.containsKey(uid)) {
                            try {
                                userShare = Double.parseDouble(
                                        splitDetails.get(uid).toString());
                            } catch (NumberFormatException ignored) {}
                        }

                        if (userShare == 0) continue;

                        // Get timestamp
                        com.google.firebase.Timestamp ts = doc.getTimestamp("createdAt");
                        if (ts == null) continue;
                        Calendar expCal = Calendar.getInstance();
                        expCal.setTime(ts.toDate());

                        int expMonth = expCal.get(Calendar.MONTH);
                        int expYear  = expCal.get(Calendar.YEAR);

                        // Filter by period
                        boolean inPeriod = false;
                        if (selectedPeriod == 0) { // week
                            long diffMs = now.getTimeInMillis() - expCal.getTimeInMillis();
                            inPeriod = diffMs >= 0 && diffMs <= 7L * 24 * 3600 * 1000;
                        } else if (selectedPeriod == 1) { // month
                            inPeriod = (expMonth == thisMonth && expYear == thisYear);
                        } else { // year
                            inPeriod = (expYear == thisYear);
                        }

                        // Monthly comparison (always compute)
                        if (expMonth == thisMonth && expYear == thisYear) {
                            totalThisMonth += userShare;
                        }
                        int lastMonthVal = thisMonth == 0 ? 11 : thisMonth - 1;
                        int lastMonthYear = thisMonth == 0 ? thisYear - 1 : thisYear;
                        if (expMonth == lastMonthVal && expYear == lastMonthYear) {
                            totalLastMonth += userShare;
                        }

                        if (!inPeriod) continue;

                        // Categorize by description keywords
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
                                desc.contains("du lịch") || desc.contains("travel")) {
                            catIdx = 1;
                        } else if (desc.contains("mua") || desc.contains("shop") ||
                                desc.contains("quần") || desc.contains("áo")) {
                            catIdx = 2;
                        } else if (desc.contains("phim") || desc.contains("game") ||
                                desc.contains("giải trí") || desc.contains("karaoke")) {
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
        for (int i = 0; i < CAT_LABELS.length; i++) {
            if (catTotals[i] > 0) {
                segments.add(new DonutChartView.Segment(CAT_LABELS[i], (float) catTotals[i], CAT_COLORS[i]));
            }
        }

        String centerValue = "₫" + formatShort(total);
        donutChartView.setData(segments, "Tổng chi tiêu", centerValue);

        // Category rows
        layoutCategories.removeAllViews();
        for (int i = 0; i < CAT_LABELS.length; i++) {
            if (catTotals[i] == 0) continue;
            int pct = total > 0 ? (int) Math.round(catTotals[i] / total * 100) : 0;
            layoutCategories.addView(buildCategoryRow(CAT_LABELS[i], pct, catTotals[i], CAT_COLORS[i], fmt));
        }

        // Monthly trend
        double diff = totalLastMonth > 0
                ? ((totalThisMonth - totalLastMonth) / totalLastMonth) * 100 : 0;
        String arrow = diff <= 0 ? "↓" : "↑";
        int trendColor = diff <= 0
                ? Color.parseColor("#1CC29F") : Color.parseColor("#E53935");
        String pctStr = arrow + " " + (int) Math.abs(diff) + "%";
        tvTrendPercent.setText(pctStr);
        tvTrendPercent.setTextColor(trendColor);

        if (diff <= 0) {
            tvTrendDesc.setText("Bạn đã chi tiêu ít hơn " + (int) Math.abs(diff) + "% so với tháng trước.");
        } else {
            tvTrendDesc.setText("Bạn đã chi tiêu nhiều hơn " + (int) Math.abs(diff) + "% so với tháng trước.");
        }

        tvBarLastMonthAmount.setText("₫" + formatShort(totalLastMonth));
        tvBarThisMonthAmount.setText("₫" + formatShort(totalThisMonth));
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
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(12, 12);
        dot.setLayoutParams(dotLp);
        dot.setBackgroundColor(color);
        // Make circular
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
        tvPct.setText(pct + "%");
        tvPct.setTextSize(12);
        tvPct.setTextColor(resolveColor(android.R.attr.textColorSecondary));
        textCol.addView(tvPct);

        row.addView(textCol);

        // Amount
        TextView tvAmt = new TextView(requireContext());
        tvAmt.setText("₫" + fmt.format(amount / 1000) + "k");
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
        int c = ta.getColor(0, Color.parseColor("#1A2420"));
        ta.recycle();
        return c;
    }

    private String formatShort(double value) {
        if (value >= 1_000_000) return String.format("%.1fM", value / 1_000_000);
        if (value >= 1_000)     return String.format("%.0fk", value / 1_000);
        return String.valueOf((long) value);
    }
}
