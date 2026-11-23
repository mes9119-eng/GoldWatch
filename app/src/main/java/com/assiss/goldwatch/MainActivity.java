package com.assiss.goldwatch;

import android.Manifest;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private TextView pMain, pA, pB, lMain, lA, lB, change, time, alertStatus;
    private EditText up, low;
    private RadioGroup karats;
    private Button refreshBtn;
    private final Handler h = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        pMain = findViewById(R.id.pMain);
        pA = findViewById(R.id.pA);
        pB = findViewById(R.id.pB);
        lMain = findViewById(R.id.lMain);
        lA = findViewById(R.id.lA);
        lB = findViewById(R.id.lB);
        change = findViewById(R.id.change);
        time = findViewById(R.id.time);
        alertStatus = findViewById(R.id.alertStatus);
        up = findViewById(R.id.up);
        low = findViewById(R.id.low);
        karats = findViewById(R.id.karats);
        refreshBtn = findViewById(R.id.refresh);

        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        Notifier.ensureChannel(this);
        PriceJob.schedule(this);

        // load saved alert
        int k = Store.karat(this);
        karats.check(k == 24 ? R.id.k24 : (k == 18 ? R.id.k18 : R.id.k21));
        if (Store.upper(this) > 0) up.setText(Updater.fmt(Store.upper(this)));
        if (Store.lower(this) > 0) low.setText(Updater.fmt(Store.lower(this)));

        karats.setOnCheckedChangeListener((g, id) -> {
            int nk = selectedKarat();
            if (nk != Store.karat(this)) {
                Store.saveAlerts(this, nk, Store.upper(this), Store.lower(this));
                render();
                renderAlert();
                GoldWidget.updateAll(this);
            }
        });
        refreshBtn.setOnClickListener(v -> refresh());
        findViewById(R.id.save).setOnClickListener(v -> saveAlert());
        findViewById(R.id.clear).setOnClickListener(v -> {
            up.setText("");
            low.setText("");
            Store.saveAlerts(this, selectedKarat(), 0, 0);
            renderAlert();
            GoldWidget.updateAll(this);
            toast("تم إلغاء التنبيهات");
        });
        findViewById(R.id.testAlert).setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 33 &&
                    checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
                toast("وافق على الإشعارات أولاً، وبعدها اضغط الزر مرة ثانية");
                return;
            }
            double curOz = Store.oz(this);
            int curK = Store.karat(this);
            String price = curOz > 0 ? Updater.fmt(PriceFetcher.gramSar(curOz, curK)) + " ريال" : "--";
            Notifier.show(this, 3, "🔔 تنبيه تجريبي",
                    "هذا شكل وصوت تنبيه مراقب الذهب. سعر عيار " + curK + " الآن " + price);
        });
        findViewById(R.id.addWidget).setOnClickListener(v -> pinWidget());
        findViewById(R.id.battery).setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } catch (Exception e) {
                toast("افتح إعدادات البطارية يدوياً");
            }
        });

        try {
            String ver = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            ((TextView) findViewById(R.id.version)).setText("مراقب الذهب • الإصدار " + ver);
        } catch (Exception ignored) {
        }

        render();
        renderAlert();
        refresh();
    }

    private int selectedKarat() {
        int id = karats.getCheckedRadioButtonId();
        return id == R.id.k24 ? 24 : (id == R.id.k18 ? 18 : 21);
    }

    private void refresh() {
        refreshBtn.setEnabled(false);
        refreshBtn.setText("جاري التحديث...");
        new Thread(() -> {
            boolean ok = Updater.run(getApplicationContext());
            h.post(() -> {
                refreshBtn.setEnabled(true);
                refreshBtn.setText("تحديث الآن");
                render();
                if (!ok) toast("تعذر جلب السعر، تأكد من الاتصال بالإنترنت");
            });
        }).start();
    }

    private void render() {
        double oz = Store.oz(this);
        if (oz <= 0) return;
        int k = Store.karat(this);
        int[] o = Store.others(k);
        lMain.setText("عيار " + k);
        pMain.setText(Updater.fmt(PriceFetcher.gramSar(oz, k)));
        lA.setText("عيار " + o[0]);
        pA.setText(Updater.fmt(PriceFetcher.gramSar(oz, o[0])));
        lB.setText("عيار " + o[1]);
        pB.setText(Updater.fmt(PriceFetcher.gramSar(oz, o[1])));
        double open = Store.dayOpen(this);
        if (open > 0) {
            double pct = (oz - open) / open * 100.0;
            String arrow = pct > 0.005 ? "▲" : (pct < -0.005 ? "▼" : "●");
            change.setText(arrow + " " + String.format(Locale.US, "%.2f%%", Math.abs(pct))
                    + "  منذ أول تحديث اليوم");
            change.setTextColor(pct > 0.005 ? 0xFF4CD964 : (pct < -0.005 ? 0xFFFF5A5F : 0xFFBDBDBD));
        }
        String t = new SimpleDateFormat("HH:mm", Locale.US).format(new Date(Store.time(this)));
        time.setText("الأونصة " + String.format(Locale.US, "%.2f", oz) + "$  •  آخر تحديث " + t);
    }

    private void renderAlert() {
        double u = Store.upper(this), l = Store.lower(this);
        if (u <= 0 && l <= 0) {
            alertStatus.setText("لا يوجد تنبيه مفعّل");
            return;
        }
        StringBuilder sb = new StringBuilder("مفعّل على عيار " + Store.karat(this) + ":");
        if (u > 0) sb.append("\n▲ إذا ارتفع فوق ").append(Updater.fmt(u)).append(" ريال");
        if (l > 0) sb.append("\n▼ إذا نزل تحت ").append(Updater.fmt(l)).append(" ريال");
        alertStatus.setText(sb.toString());
    }

    private void saveAlert() {
        double u = parse(up.getText().toString());
        double l = parse(low.getText().toString());
        if (u < 0 || l < 0) {
            toast("اكتب الرقم بشكل صحيح");
            return;
        }
        if (u > 0 && l > 0 && l >= u) {
            toast("حد النزول لازم يكون أقل من حد الارتفاع");
            return;
        }
        Store.saveAlerts(this, selectedKarat(), u, l);
        renderAlert();
        render();
        GoldWidget.updateAll(this);
        toast("تم حفظ التنبيه ✔");
        final double oz = Store.oz(this);
        new Thread(() -> Updater.checkAlerts(getApplicationContext(), oz)).start();
    }

    /** Accepts Arabic-Indic digits too. Returns 0 for empty, -1 for invalid. */
    private static double parse(String s) {
        s = s.trim();
        if (s.isEmpty()) return 0;
        StringBuilder sb = new StringBuilder();
        for (char ch : s.toCharArray()) {
            if (ch >= '٠' && ch <= '٩') sb.append((char) ('0' + (ch - '٠')));
            else if (ch >= '۰' && ch <= '۹') sb.append((char) ('0' + (ch - '۰')));
            else if (ch == '٫' || ch == ',') sb.append('.');
            else sb.append(ch);
        }
        try {
            double v = Double.parseDouble(sb.toString());
            return v >= 0 ? v : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void pinWidget() {
        AppWidgetManager m = getSystemService(AppWidgetManager.class);
        if (m != null && m.isRequestPinAppWidgetSupported()) {
            m.requestPinAppWidget(new ComponentName(this, GoldWidget.class), null, null);
        } else {
            toast("اضغط مطولاً على الشاشة الرئيسية ← الأدوات (Widgets) ← مراقب الذهب");
        }
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }
}
