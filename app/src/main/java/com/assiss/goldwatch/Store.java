package com.assiss.goldwatch;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Small wrapper around SharedPreferences. */
final class Store {
    private Store() {}

    static SharedPreferences p(Context c) {
        return c.getSharedPreferences("gold", Context.MODE_PRIVATE);
    }

    private static double d(Context c, String k) {
        SharedPreferences sp = p(c);
        return sp.contains(k) ? Double.longBitsToDouble(sp.getLong(k, 0)) : 0;
    }

    private static void putD(SharedPreferences.Editor e, String k, double v) {
        e.putLong(k, Double.doubleToRawLongBits(v));
    }

    // ---- price ----
    static double oz(Context c) { return d(c, "oz"); }
    static double dayOpen(Context c) { return d(c, "dayOpen"); }
    static long time(Context c) { return p(c).getLong("time", 0); }

    static void savePrice(Context c, double oz) {
        SharedPreferences sp = p(c);
        String today = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
        SharedPreferences.Editor e = sp.edit();
        if (!today.equals(sp.getString("day", ""))) {
            e.putString("day", today);
            putD(e, "dayOpen", oz);
        }
        putD(e, "oz", oz);
        e.putLong("time", System.currentTimeMillis());
        e.commit();
    }

    // ---- bank margin (% added on top of world spot price) ----
    static double margin(Context c) { return d(c, "margin"); }
    /** true = watch the bank's SELL (buy-back) price, false = its BUY price. */
    static boolean sellMode(Context c) { return p(c).getBoolean("sellMode", true); }
    static void setMargin(Context c, double m, boolean sell) {
        SharedPreferences.Editor e = p(c).edit();
        putD(e, "margin", m);
        e.putBoolean("sellMode", sell);
        e.commit();
    }

    /** Gram price in SAR for a karat, including the user's bank margin. */
    static double gram(Context c, double oz, int karat) {
        double m = margin(c) / 100.0;
        return PriceFetcher.gramSar(oz, karat) * (sellMode(c) ? 1 - m : 1 + m);
    }

    // ---- alerts ----
    static int karat(Context c) { return p(c).getInt("karat", 24); }
    /** The two karats shown in small text, given the main one. */
    static int[] others(int main) {
        if (main == 24) return new int[]{21, 18};
        if (main == 21) return new int[]{24, 18};
        return new int[]{24, 21};
    }
    static double upper(Context c) { return d(c, "upper"); }
    static double lower(Context c) { return d(c, "lower"); }

    static void saveAlerts(Context c, int karat, double up, double low) {
        SharedPreferences.Editor e = p(c).edit();
        e.putInt("karat", karat);
        putD(e, "upper", up);
        putD(e, "lower", low);
        e.putBoolean("upFired", false);
        e.putBoolean("lowFired", false);
        e.commit();
    }

    static boolean fired(Context c, String k) { return p(c).getBoolean(k, false); }
    static void setFired(Context c, String k, boolean v) { p(c).edit().putBoolean(k, v).commit(); }
}
