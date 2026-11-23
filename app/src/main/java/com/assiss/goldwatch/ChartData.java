package com.assiss.goldwatch;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** Price history for the chart (USD per troy ounce). */
final class ChartData {
    static final int DAY = 0, WEEK = 1, MONTH = 2, YEAR = 3;

    static final class Series {
        final long[] t;     // epoch millis
        final double[] oz;  // USD / oz
        Series(long[] t, double[] oz) { this.t = t; this.oz = oz; }
        int size() { return t.length; }
    }

    private static String historyCache;
    private static long historyCacheTime;

    private ChartData() {}

    static Series load(android.content.Context c, int range, double liveOz) throws Exception {
        List<long[]> tl = new ArrayList<>();
        List<Double> vl = new ArrayList<>();
        long now = System.currentTimeMillis();

        if (range == DAY) {
            long from = now - 24 * 3600 * 1000L;
            java.util.TreeMap<Long, Double> pts = new java.util.TreeMap<>();
            try {
                JSONObject o = new JSONObject(PriceFetcher.get(
                        "https://xaus.com/api/v1/intraday?symbol=xau&hours=24"));
                JSONArray a = o.getJSONArray("points");
                for (int i = 0; i < a.length(); i++) {
                    JSONObject p = a.getJSONObject(i);
                    long t = p.getLong("t");
                    if (t < 100000000000L) t *= 1000L; // seconds -> millis
                    double v = p.getDouble("p");
                    if (v > 100 && t >= from && t <= now + 60000) pts.put(t, v);
                }
            } catch (Exception ignored) {
                // fall back to our own readings only
            }
            for (double[] r : Store.log(c)) {
                long t = (long) r[0];
                if (t >= from && r[1] > 100) pts.put(t, r[1]);
            }
            for (java.util.Map.Entry<Long, Double> en : pts.entrySet()) {
                tl.add(new long[]{en.getKey()});
                vl.add(en.getValue());
            }
        } else {
            String json;
            synchronized (ChartData.class) {
                if (historyCache == null || now - historyCacheTime > 3 * 3600 * 1000L) {
                    historyCache = PriceFetcher.get("https://xaus.com/api/v1/history");
                    historyCacheTime = now;
                }
                json = historyCache;
            }
            int days = range == WEEK ? 7 : (range == MONTH ? 30 : 365);
            long from = now - days * 86400000L;
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            f.setTimeZone(TimeZone.getTimeZone("UTC"));
            JSONArray a = new JSONObject(json).getJSONArray("points");
            for (int i = 0; i < a.length(); i++) {
                JSONObject p = a.getJSONObject(i);
                Date d = f.parse(p.getString("d"));
                if (d == null) continue;
                long t = d.getTime() + 12 * 3600 * 1000L;
                if (t < from) continue;
                double v = p.getDouble("c");
                if (v > 100) {
                    tl.add(new long[]{t});
                    vl.add(v);
                }
            }
        }

        // append the live price so the line ends "now"
        if (liveOz > 100 && (tl.isEmpty() || now - tl.get(tl.size() - 1)[0] > 5 * 60 * 1000L)) {
            tl.add(new long[]{now});
            vl.add(liveOz);
        }

        long[] t = new long[tl.size()];
        double[] v = new double[vl.size()];
        for (int i = 0; i < t.length; i++) {
            t[i] = tl.get(i)[0];
            v[i] = vl.get(i);
        }
        return new Series(t, v);
    }
}
