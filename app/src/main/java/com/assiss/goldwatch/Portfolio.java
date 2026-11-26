package com.assiss.goldwatch;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The user's gold purchases (24K grams) stored locally. */
final class Portfolio {
    static final class Buy {
        final long date;      // epoch millis
        final double grams;
        final double price;   // SAR per gram paid
        Buy(long date, double grams, double price) {
            this.date = date;
            this.grams = grams;
            this.price = price;
        }
        double cost() { return grams * price; }
    }

    private Portfolio() {}

    static List<Buy> all(Context c) {
        List<Buy> out = new ArrayList<>();
        for (String item : Store.p(c).getString("portfolio", "").split(";")) {
            String[] f = item.split(":");
            if (f.length != 3) continue;
            try {
                out.add(new Buy(Long.parseLong(f[0]), Double.parseDouble(f[1]), Double.parseDouble(f[2])));
            } catch (NumberFormatException ignored) {
            }
        }
        Collections.sort(out, (a, b) -> Long.compare(a.date, b.date));
        return out;
    }

    private static void save(Context c, List<Buy> list) {
        StringBuilder sb = new StringBuilder();
        for (Buy b : list) sb.append(b.date).append(':').append(b.grams).append(':').append(b.price).append(';');
        Store.p(c).edit().putString("portfolio", sb.toString()).commit();
    }

    static void add(Context c, long date, double grams, double price) {
        List<Buy> list = all(c);
        list.add(new Buy(date, grams, price));
        save(c, list);
    }

    static void remove(Context c, int index) {
        List<Buy> list = all(c);
        if (index >= 0 && index < list.size()) {
            list.remove(index);
            save(c, list);
        }
    }

    static double totalGrams(List<Buy> l) {
        double g = 0;
        for (Buy b : l) g += b.grams;
        return g;
    }

    static double totalCost(List<Buy> l) {
        double s = 0;
        for (Buy b : l) s += b.cost();
        return s;
    }
}
