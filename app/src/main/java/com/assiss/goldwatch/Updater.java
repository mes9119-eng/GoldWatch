package com.assiss.goldwatch;

import android.content.Context;

import java.util.Locale;

/** Fetch -> save -> check alerts -> refresh widgets. Call off the main thread. */
final class Updater {
    private Updater() {}

    static synchronized boolean run(Context c) {
        try {
            double oz = PriceFetcher.fetchOzUsd();
            Store.savePrice(c, oz);
            checkAlerts(c, oz);
            GoldWidget.updateAll(c);
            return true;
        } catch (Exception e) {
            GoldWidget.updateAll(c);
            return false;
        }
    }

    static void checkAlerts(Context c, double oz) {
        if (oz <= 0) return;
        int k = Store.karat(c);
        double g = Store.gram(c, oz, k);
        double up = Store.upper(c);
        double low = Store.lower(c);

        if (up > 0) {
            if (g >= up && !Store.fired(c, "upFired")) {
                Notifier.show(c, 1, "📈 الذهب ارتفع",
                        "عيار " + k + " وصل " + fmt(g) + " ريال للجرام (حدك " + fmt(up) + ")");
                Store.setFired(c, "upFired", true);
            } else if (g < up * 0.998) {
                Store.setFired(c, "upFired", false); // re-arm after it drops back
            }
        }
        if (low > 0) {
            if (g <= low && !Store.fired(c, "lowFired")) {
                Notifier.show(c, 2, "📉 الذهب نزل",
                        "عيار " + k + " نزل إلى " + fmt(g) + " ريال للجرام (حدك " + fmt(low) + ")");
                Store.setFired(c, "lowFired", true);
            } else if (g > low * 1.002) {
                Store.setFired(c, "lowFired", false);
            }
        }
    }

    static String fmt(double v) { return String.format(Locale.US, "%.2f", v); }
}
