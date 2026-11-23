package com.assiss.goldwatch;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.util.Locale;

public class GoldWidget extends AppWidgetProvider {
    static final String ACTION_REFRESH = "com.assiss.goldwatch.REFRESH";

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        RemoteViews v = build(c);
        for (int id : ids) m.updateAppWidget(id, v);
        PriceJob.schedule(c);
    }

    @Override
    public void onEnabled(Context c) {
        PriceJob.schedule(c);
        final Context a = c.getApplicationContext();
        new Thread(() -> Updater.run(a)).start();
    }

    @Override
    public void onReceive(Context c, Intent i) {
        super.onReceive(c, i);
        if (ACTION_REFRESH.equals(i.getAction())) {
            final PendingResult pr = goAsync();
            final Context a = c.getApplicationContext();
            new Thread(() -> {
                Updater.run(a);
                pr.finish();
            }).start();
        }
    }

    static void updateAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, GoldWidget.class));
        if (ids == null || ids.length == 0) return;
        m.updateAppWidget(ids, build(c));
    }

    static RemoteViews build(Context c) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_gold);
        int k = Store.karat(c);
        double mg = Store.margin(c);
        v.setTextViewText(R.id.wlMain, "عيار " + k);
        boolean triple = mg > 0;
        v.setViewVisibility(R.id.wMain, triple ? android.view.View.GONE : android.view.View.VISIBLE);
        v.setViewVisibility(R.id.wTriple, triple ? android.view.View.VISIBLE : android.view.View.GONE);
        double oz = Store.oz(c);
        if (oz > 0) {
            v.setTextViewText(R.id.wMain, fmt2(Store.gram(c, oz, k)));
            if (triple) {
                v.setTextViewText(R.id.wSell, fmt2(Store.bankSell(c, oz, k)));
                v.setTextViewText(R.id.wWorld, fmt2(PriceFetcher.gramSar(oz, k)));
                v.setTextViewText(R.id.wBuy, fmt2(Store.bankBuy(c, oz, k)));
                boolean sell = Store.sellMode(c);
                // highlight the price the alerts are based on
                v.setTextColor(R.id.wSell, sell ? 0xFFF1D77A : 0xFFFF8A80);
                v.setTextColor(R.id.wBuy, sell ? 0xFF80E0A0 : 0xFFF1D77A);
                v.setTextViewText(R.id.wlSell, sell ? "بيع 🔔" : "بيع");
                v.setTextViewText(R.id.wlBuy, sell ? "شراء" : "شراء 🔔");
            }
            double open = Store.dayOpen(c);
            if (open > 0) {
                double pct = (oz - open) / open * 100.0;
                String arrow = pct > 0.005 ? "▲" : (pct < -0.005 ? "▼" : "●");
                int color = pct > 0.005 ? 0xFF4CD964 : (pct < -0.005 ? 0xFFFF5A5F : 0xFFBDBDBD);
                v.setTextViewText(R.id.wChange,
                        arrow + String.format(Locale.US, "%.2f%%", Math.abs(pct)));
                v.setTextColor(R.id.wChange, color);
            }
        }

        double up = Store.upper(c), low = Store.lower(c);
        String alerts;
        if (up > 0 && low > 0) alerts = "▲ " + fmt0(up) + "   ▼ " + fmt0(low);
        else if (up > 0) alerts = "▲ " + fmt0(up);
        else if (low > 0) alerts = "▼ " + fmt0(low);
        else alerts = "بدون تنبيه";
        v.setTextViewText(R.id.wAlerts, alerts);

        // tap opens the app (which also refreshes the price)
        PendingIntent openApp = PendingIntent.getActivity(c, 0,
                new Intent(c, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        v.setOnClickPendingIntent(R.id.wRoot, openApp);
        return v;
    }

    static String fmt2(double v) {
        return String.format(Locale.US, "%.2f", v);
    }

    static String fmt0(double v) {
        return v == Math.floor(v) ? String.format(Locale.US, "%.0f", v)
                : String.format(Locale.US, "%.1f", v);
    }
}
