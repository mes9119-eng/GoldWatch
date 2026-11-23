package com.assiss.goldwatch;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/** Fetches the world spot price of gold and converts it to SAR per gram. */
final class PriceFetcher {
    static final double SAR_PER_USD = 3.75;          // official SAR peg
    static final double GRAMS_PER_OZ = 31.1034768;   // troy ounce

    private PriceFetcher() {}

    /** @return USD per troy ounce */
    static double fetchOzUsd() throws Exception {
        try {
            JSONObject o = new JSONObject(get("https://api.gold-api.com/price/XAU"));
            double v = o.getDouble("price");
            if (v > 100) return v;
        } catch (Exception ignored) {
            // fall through to backup source
        }
        JSONObject o = new JSONObject(get("https://data-asg.goldprice.org/dbXRates/USD"));
        return o.getJSONArray("items").getJSONObject(0).getDouble("xauPrice");
    }

    static double gramSar(double ozUsd, int karat) {
        return ozUsd / GRAMS_PER_OZ * SAR_PER_USD * karat / 24.0;
    }

    static String get(String u) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(15000);
        c.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) GoldWatch/1.0");
        c.setRequestProperty("Accept", "application/json");
        try {
            int code = c.getResponseCode();
            if (code != 200) throw new IOException("HTTP " + code);
            BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            r.close();
            return sb.toString();
        } finally {
            c.disconnect();
        }
    }
}
