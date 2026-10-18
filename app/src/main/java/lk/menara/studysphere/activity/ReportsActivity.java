package lk.menara.studysphere.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import lk.menara.studysphere.R;
import lk.menara.studysphere.model.Order;

public class ReportsActivity extends AppCompatActivity {

    private static final int TEAL_DARK = Color.parseColor("#0E6B63");
    private static final int TEAL_MID = Color.parseColor("#2FA595");
    private static final int TEAL_LIGHT = Color.parseColor("#7FCFC1");
    private static final int AMBER = Color.parseColor("#F2A93B");
    private static final int RED = Color.parseColor("#E5675B");
    private static final int GREEN = Color.parseColor("#4CAF7A");
    private static final int GREY_TEXT = Color.parseColor("#5B6B68");
    private static final int GRID = Color.parseColor("#E1EEEB");

    // FIX: orders now store the list of sellers in "sellerIds"
    private static final String SELLER_FIELD = "sellerIds";
    private static final int MAX_DAYS = 30;

    private BarChart revenueChart;
    private PieChart statusChart;
    private LineChart dailyRevenueChart;
    private TextView tvTotalRevenue, tvTotalOrders, tvAvgOrder, tvDailySubtitle, tvRevenueSubtitle;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private View loadingLayout;

    private List<Order> allOrders = new ArrayList<>();
    private int periodMode = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        revenueChart = findViewById(R.id.revenueChart);
        statusChart = findViewById(R.id.statusChart);
        dailyRevenueChart = findViewById(R.id.dailyRevenueChart);
        loadingLayout = findViewById(R.id.loadingLayout);
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        tvTotalOrders = findViewById(R.id.tvTotalOrders);
        tvAvgOrder = findViewById(R.id.tvAvgOrder);
        tvDailySubtitle = findViewById(R.id.tvDailySubtitle);
        tvRevenueSubtitle = findViewById(R.id.tvRevenueSubtitle);
        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        Spinner filterSpinner = findViewById(R.id.filterSpinner);
        ArrayAdapter<String> periodAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, new String[]{"Day", "Week", "Month"});
        periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(periodAdapter);
        filterSpinner.setSelection(periodMode);
        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                periodMode = position;
                if (!allOrders.isEmpty()) bindReports(allOrders);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        setupBarChartStyle();
        setupPieChartStyle();
        setupLineChartStyle();

        loadReportsData();
    }

    private void loadReportsData() {
        String sellerId = firebaseAuth.getUid();
        if (sellerId == null) {
            Toast.makeText(this, "Please sign in to view reports", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showLoading(true);

        // FIX: whereArrayContains, because sellerIds is a list
        firestore.collection("orders")
                .whereArrayContains(SELLER_FIELD, sellerId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Order> orders = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Order order = doc.toObject(Order.class);
                        if (order != null) orders.add(order);
                    }
                    allOrders = orders;
                    bindReports(orders);
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Couldn't load reports. Check your connection and try again.",
                            Toast.LENGTH_LONG).show();
                    showLoading(false);
                });
    }

    private void bindReports(List<Order> orders) {
        if (orders.isEmpty()) {
            tvTotalRevenue.setText("0");
            tvTotalOrders.setText("0");
            tvAvgOrder.setText("0");
            return;
        }

        Date anchor = null;
        for (Order o : orders) {
            if (o.getOrderDate() == null) continue;
            Date d = o.getOrderDate().toDate();
            if (anchor == null || d.after(anchor)) anchor = d;
        }
        if (anchor == null) return;

        List<Calendar> starts = bucketStarts(anchor);
        Calendar endCal = (Calendar) starts.get(starts.size() - 1).clone();
        stepPeriod(endCal, 1);
        Date windowStart = starts.get(0).getTime();
        Date windowEnd = endCal.getTime();

        List<Order> windowOrders = new ArrayList<>();
        for (Order o : orders) {
            if (o.getOrderDate() == null) continue;
            Date d = o.getOrderDate().toDate();
            if (!d.before(windowStart) && d.before(windowEnd)) windowOrders.add(o);
        }

        String[] subtitles = {"Revenue per day (last 7 days)",
                "Revenue per week (last 8 weeks)",
                "Revenue per month (last 12 months)"};
        tvRevenueSubtitle.setText(subtitles[periodMode]);

        if (windowOrders.isEmpty()) {
            tvTotalRevenue.setText("0");
            tvTotalOrders.setText("0");
            tvAvgOrder.setText("0");
            revenueChart.clear();
            statusChart.clear();
        } else {
            bindSummary(windowOrders);
            populateRevenueChart(windowOrders, starts);
            populateStatusChart(windowOrders);
        }
        populateDailyRevenueChart(orders);
    }

    private void stepPeriod(Calendar c, int dir) {
        if (periodMode == 0) c.add(Calendar.DAY_OF_MONTH, dir);
        else if (periodMode == 1) c.add(Calendar.WEEK_OF_YEAR, dir);
        else c.add(Calendar.MONTH, dir);
    }

    private List<Calendar> bucketStarts(Date anchor) {
        int n = periodMode == 0 ? 7 : periodMode == 1 ? 8 : 12;
        Calendar c = Calendar.getInstance();
        c.setFirstDayOfWeek(Calendar.MONDAY);
        c.setTime(anchor);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        if (periodMode == 1) c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        if (periodMode == 2) c.set(Calendar.DAY_OF_MONTH, 1);

        List<Calendar> starts = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            starts.add(0, (Calendar) c.clone());
            stepPeriod(c, -1);
        }
        return starts;
    }

    private boolean countsAsRevenue(Order o) {
        String s = o.getStatus() == null ? "" : o.getStatus().trim().toUpperCase(Locale.ROOT);
        return !(s.equals("CANCELLED") || s.equals("CANCELED") || s.equals("FAILED") || s.equals("REFUNDED"));
    }

    private void bindSummary(List<Order> orders) {
        double revenue = 0;
        int revenueOrders = 0;
        for (Order o : orders) {
            if (countsAsRevenue(o)) {
                revenue += o.getTotalAmount();
                revenueOrders++;
            }
        }
        tvTotalRevenue.setText(compact(revenue));
        tvTotalOrders.setText(String.valueOf(orders.size()));
        tvAvgOrder.setText(revenueOrders == 0 ? "0" : compact(revenue / revenueOrders));
    }

    private void populateRevenueChart(List<Order> orders, List<Calendar> starts) {
        SimpleDateFormat labelFmt = new SimpleDateFormat(
                periodMode == 2 ? "MMM yy" : "d MMM", Locale.getDefault());

        float[] totals = new float[starts.size()];
        for (Order o : orders) {
            if (o.getOrderDate() == null || !countsAsRevenue(o)) continue;
            Date d = o.getOrderDate().toDate();
            for (int i = starts.size() - 1; i >= 0; i--) {
                if (!d.before(starts.get(i).getTime())) {
                    totals[i] += (float) o.getTotalAmount();
                    break;
                }
            }
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < starts.size(); i++) {
            entries.add(new BarEntry(i, totals[i]));
            labels.add(labelFmt.format(starts.get(i).getTime()));
        }

        BarDataSet set = new BarDataSet(entries, "Revenue (LKR)");
        set.setColor(TEAL_MID);
        set.setHighLightColor(TEAL_DARK);
        set.setValueTextColor(GREY_TEXT);
        set.setValueTextSize(10f);
        set.setValueFormatter(new ValueFormatter() {
            @Override
            public String getBarLabel(BarEntry e) {
                return e.getY() == 0f ? "" : compact(e.getY());
            }
        });

        BarData data = new BarData(set);
        data.setBarWidth(0.55f);
        revenueChart.setData(data);

        XAxis x = revenueChart.getXAxis();
        x.setValueFormatter(new IndexAxisValueFormatter(labels));
        x.setLabelCount(Math.min(labels.size(), 6));
        revenueChart.animateY(600);
        revenueChart.invalidate();
    }

    private void setupBarChartStyle() {
        revenueChart.getDescription().setEnabled(false);
        revenueChart.setNoDataText("No revenue yet");
        revenueChart.setNoDataTextColor(GREY_TEXT);
        revenueChart.setDrawGridBackground(false);
        revenueChart.setScaleEnabled(false);
        revenueChart.setExtraBottomOffset(4f);
        revenueChart.getLegend().setEnabled(false);

        XAxis x = revenueChart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setDrawGridLines(false);
        x.setDrawAxisLine(false);
        x.setGranularity(1f);
        x.setGranularityEnabled(true);
        x.setTextColor(GREY_TEXT);

        YAxis left = revenueChart.getAxisLeft();
        left.setAxisMinimum(0f);
        left.setDrawAxisLine(false);
        left.setGridColor(GRID);
        left.setTextColor(GREY_TEXT);
        left.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                return compact(value);
            }
        });
        revenueChart.getAxisRight().setEnabled(false);
    }

    private void populateStatusChart(List<Order> orders) {
        TreeMap<String, Integer> counts = new TreeMap<>();
        for (Order o : orders) {
            String s = o.getStatus() == null || o.getStatus().trim().isEmpty()
                    ? "UNKNOWN" : o.getStatus().trim().toUpperCase(Locale.ROOT);
            counts.put(s, counts.containsKey(s) ? counts.get(s) + 1 : 1);
        }

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        int[] fallback = {TEAL_LIGHT, TEAL_MID, AMBER, GREEN, RED};
        int f = 0;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            entries.add(new PieEntry(e.getValue(), toTitleCase(e.getKey())));
            colors.add(statusColor(e.getKey(), fallback[f++ % fallback.length]));
        }

        PieDataSet set = new PieDataSet(entries, "");
        set.setColors(colors);
        set.setSliceSpace(3f);
        set.setSelectionShift(6f);
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(12f);

        PieData data = new PieData(set);
        data.setValueFormatter(new PercentFormatter(statusChart));
        statusChart.setData(data);
        statusChart.setCenterText(orders.size() + "\norders");
        statusChart.animateY(600);
        statusChart.invalidate();
    }

    private void setupPieChartStyle() {
        statusChart.getDescription().setEnabled(false);
        statusChart.setNoDataText("No orders yet");
        statusChart.setNoDataTextColor(GREY_TEXT);
        statusChart.setUsePercentValues(true);
        statusChart.setDrawEntryLabels(false);
        statusChart.setDrawHoleEnabled(true);
        statusChart.setHoleRadius(62f);
        statusChart.setTransparentCircleRadius(66f);
        statusChart.setHoleColor(Color.WHITE);
        statusChart.setCenterTextSize(16f);
        statusChart.setCenterTextColor(TEAL_DARK);
        statusChart.setRotationEnabled(false);

        Legend l = statusChart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setForm(Legend.LegendForm.CIRCLE);
        l.setWordWrapEnabled(true);
        l.setTextColor(GREY_TEXT);
        l.setTextSize(12f);
    }

    private int statusColor(String status, int fallback) {
        switch (status) {
            case "PAID":
            case "COMPLETED":
            case "DELIVERED": return TEAL_MID;
            case "PENDING":
            case "PROCESSING": return AMBER;
            case "CANCELLED":
            case "CANCELED":
            case "FAILED":
            case "REFUNDED": return RED;
            case "SHIPPED": return GREEN;
            default: return fallback;
        }
    }

    private void populateDailyRevenueChart(List<Order> orders) {
        SimpleDateFormat keyFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat labelFmt = new SimpleDateFormat("d MMM", Locale.getDefault());

        TreeMap<String, Double> byDay = new TreeMap<>();
        Date max = null;
        for (Order o : orders) {
            if (o.getOrderDate() == null || !countsAsRevenue(o)) continue;
            Date d = o.getOrderDate().toDate();
            String key = keyFmt.format(d);
            byDay.put(key, byDay.containsKey(key) ? byDay.get(key) + o.getTotalAmount() : o.getTotalAmount());
            if (max == null || d.after(max)) max = d;
        }
        if (max == null) return;

        Calendar cursor = Calendar.getInstance();
        cursor.setTime(max);
        List<String> labels = new ArrayList<>();
        List<Entry> entries = new ArrayList<>();
        for (int i = MAX_DAYS - 1; i >= 0; i--) {
            Calendar c = (Calendar) cursor.clone();
            c.add(Calendar.DAY_OF_MONTH, -i);
            Double v = byDay.get(keyFmt.format(c.getTime()));
            labels.add(labelFmt.format(c.getTime()));
            entries.add(new Entry(MAX_DAYS - 1 - i, v == null ? 0f : v.floatValue()));
        }
        tvDailySubtitle.setText("30 days up to " + labels.get(labels.size() - 1));

        LineDataSet set = new LineDataSet(entries, "Daily revenue (LKR)");
        set.setColor(TEAL_MID);
        set.setLineWidth(2.5f);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawCircles(true);
        set.setCircleRadius(3.5f);
        set.setCircleColor(TEAL_DARK);
        set.setCircleHoleColor(Color.WHITE);
        set.setDrawCircleHole(true);
        set.setDrawValues(false);
        set.setDrawFilled(true);
        set.setFillColor(TEAL_LIGHT);
        set.setFillAlpha(70);
        set.setHighLightColor(TEAL_DARK);

        dailyRevenueChart.setData(new LineData(set));
        XAxis x = dailyRevenueChart.getXAxis();
        x.setValueFormatter(new IndexAxisValueFormatter(labels));
        x.setLabelCount(5);
        dailyRevenueChart.animateX(600);
        dailyRevenueChart.invalidate();
    }

    private void setupLineChartStyle() {
        dailyRevenueChart.getDescription().setEnabled(false);
        dailyRevenueChart.setNoDataText("No daily revenue yet");
        dailyRevenueChart.setNoDataTextColor(GREY_TEXT);
        dailyRevenueChart.setScaleEnabled(false);
        dailyRevenueChart.setExtraBottomOffset(4f);
        dailyRevenueChart.getLegend().setEnabled(false);

        XAxis x = dailyRevenueChart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setDrawGridLines(false);
        x.setDrawAxisLine(false);
        x.setGranularity(1f);
        x.setTextColor(GREY_TEXT);

        YAxis left = dailyRevenueChart.getAxisLeft();
        left.setAxisMinimum(0f);
        left.setDrawAxisLine(false);
        left.setGridColor(GRID);
        left.setTextColor(GREY_TEXT);
        left.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                return compact(value);
            }
        });
        dailyRevenueChart.getAxisRight().setEnabled(false);
    }

    private String compact(double v) {
        if (v >= 1_000_000) return String.format(Locale.US, "%.1fM", v / 1_000_000);
        if (v >= 10_000) return String.format(Locale.US, "%.1fk", v / 1_000);
        return NumberFormat.getNumberInstance(Locale.US).format(Math.round(v));
    }

    private String toTitleCase(String s) {
        if (s.isEmpty()) return s;
        return s.substring(0, 1) + s.substring(1).toLowerCase(Locale.ROOT);
    }

    private void showLoading(boolean visible) {
        if (loadingLayout != null) {
            loadingLayout.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
}