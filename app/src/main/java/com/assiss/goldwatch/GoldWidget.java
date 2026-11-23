package com.assiss.goldwatch;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
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
        double oz = Store.oz(c);
        if (oz > 0) {
            int k = Store.karat(c);
            int[] o = Store.others(k);
            v.setTextViewText(R.id.wlMain, "عيار " + k);
            v.setTextViewText(R.id.wMain, fmt0(PriceFetcher.gramSar(oz, k)));
            v.setTextViewText(R.id.wlA, "عيار " + o[0]);
            v.setTextViewText(R.id.wA, fmt0(PriceFetcher.gramSar(oz, o[0])));
            v.setTextViewText(R.id.wlB, "عيار " + o[1]);
            v.setTextViewText(R.id.wB, fmt0(PriceFetcher.gramSar(oz, o[1])));
            double open = Store.dayOpen(c);
            if (open > 0) {
                double pct = (oz - open) / open * 100.0;
                String arrow = pct > 0.005 ? "▲" : (pct < -0.005 ? "▼" : "●");
                int color = pct > 0.005 ? 0xFF4CD964 : (pct < -0.005 ? 0xFFFF5A5F : 0xFFBDBDBD);
                v.setTextViewText(R.id.wChange,
                        arrow + " " + String.format(Locale.US, "%.2f%%", Math.abs(pct)));
                v.setTextColor(R.id.wChange, color);
            }
            String t = new SimpleDateFormat("HH:mm", Locale.US).format(new Date(Store.time(c)));
            v.setTextViewText(R.id.wTime, "آخر تحديث " + t);
        }

        PendingIntent open = PendingIntent.getActivity(c, 0,
                new Intent(c, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        v.setOnClickPendingIntent(R.id.wRoot, open);

        Intent r = new Intent(c, GoldWidget.class).setAction(ACTION_REFRESH);
        PendingIntent refresh = PendingIntent.getBroadcast(c, 1, r,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        v.setOnClickPendingIntent(R.id.wRefresh, refresh);
        return v;
    }

    static String fmt0(double v) {
        return String.format(Locale.US, "%.1f", v);
    }
}
