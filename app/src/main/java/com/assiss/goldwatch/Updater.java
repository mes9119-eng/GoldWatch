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
        double gSell = Store.bankSell(c, oz, k); // what the bank pays you
        double gBuy = Store.bankBuy(c, oz, k);   // what you pay the bank
        boolean bank = Store.margin(c) > 0;
        double up = Store.upper(c);
        double low = Store.lower(c);

        if (up > 0) {
            if (gSell >= up && !Store.fired(c, "upFired")) {
                Notifier.show(c, 1, "📈 وقت البيع",
                        (bank ? "سعر البيع للبنك" : "سعر") + " عيار " + k + " وصل " + fmt(gSell)
                                + " ريال للجرام (حدك " + fmt(up) + ")");
                Store.setFired(c, "upFired", true);
            } else if (gSell < up * 0.998) {
                Store.setFired(c, "upFired", false); // re-arm after it drops back
            }
        }
        if (low > 0) {
            if (gBuy <= low && !Store.fired(c, "lowFired")) {
                Notifier.show(c, 2, "📉 وقت الشراء",
                        (bank ? "سعر الشراء من البنك" : "سعر") + " عيار " + k + " نزل إلى " + fmt(gBuy)
                                + " ريال للجرام (حدك " + fmt(low) + ")");
                Store.setFired(c, "lowFired", true);
            } else if (gBuy > low * 1.002) {
                Store.setFired(c, "lowFired", false);
            }
        }
    }

    static String fmt(double v) { return String.format(Locale.US, "%.2f", v); }
}
