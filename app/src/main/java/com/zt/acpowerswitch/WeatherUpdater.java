package com.zt.acpowerswitch;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WeatherUpdater {
    private static final String TAG = "WeatherUpdater";
    private static final String API_URL = "https://api.open-meteo.com/v1/forecast?current=cloud_cover,uv_index,weather_code&daily=sunrise,sunset&timezone=auto";
    private static final String PREFS = "weather_cache";
    private static final String KEY_CLOUD = "cloud";
    private static final String KEY_UV = "uv";
    private static final String KEY_CODE = "code";
    private static final String KEY_TIME = "time";
    private static final String KEY_SUNRISE = "sunrise";
    private static final String KEY_SUNSET = "sunset";
    private static final long CACHE_VALID_MS = 2 * 60 * 60 * 1000L;

    // 固定坐标：光伏安装位置坐标
    private static final double FIXED_LAT = 27.12;
    private static final double FIXED_LON = 111.05;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final SharedPreferences cache;

    public WeatherUpdater(Context context) {
        Context context1 = context.getApplicationContext();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.cache = context1.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static class WeatherData {
        public float cloudCover;
        public float uvIndex;
        public int weatherCode;
        public long updateTime;
        public String sunrise;
        public String sunset;
    }

    public interface Callback {
        void onResult(WeatherData data, boolean fromCache);
    }

    public void check(boolean force, Callback callback) {
        long now = System.currentTimeMillis();
        long last = cache.getLong(KEY_TIME, 0);
        if (!force && now - last < CACHE_VALID_MS) {
            Log.d(TAG, "使用缓存天气数据，直接返回");
            if (callback != null) callback.onResult(readCache(), true);
            return;
        }

        executor.execute(() -> {
            Log.d(TAG, "使用固定坐标 lat=" + FIXED_LAT + " lon=" + FIXED_LON);
            WeatherData data = fetchFromApi();
            if (data != null) {
                saveCache(data);
                mainHandler.post(() -> { if (callback != null) callback.onResult(data, false); });
            } else {
                Log.w(TAG, "API请求失败，返回旧缓存");
                mainHandler.post(() -> { if (callback != null) callback.onResult(readCache(), true); });
            }
        });
    }

    private WeatherData fetchFromApi() {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(API_URL + "&latitude=" + WeatherUpdater.FIXED_LAT + "&longitude=" + WeatherUpdater.FIXED_LON);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            int httpCode = conn.getResponseCode();
            Log.d(TAG, "HTTP " + httpCode);

            if (httpCode != 200) {
                BufferedReader err = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                StringBuilder esb = new StringBuilder();
                String el;
                while ((el = err.readLine()) != null) esb.append(el);
                Log.e(TAG, "Open-Meteo error: " + esb);
                return null;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);

            JSONObject root = new JSONObject(sb.toString());
            WeatherData d = new WeatherData();

            JSONObject cur = root.getJSONObject("current");
            d.cloudCover = (float) cur.optDouble("cloud_cover", 0);
            d.uvIndex = (float) cur.optDouble("uv_index", 0);
            d.weatherCode = cur.optInt("weather_code", 0);

            JSONObject daily = root.optJSONObject("daily");
            if (daily != null) {
                JSONArray sr = daily.optJSONArray("sunrise");
                JSONArray ss = daily.optJSONArray("sunset");
                String rawSr = sr != null && sr.length() > 0 ? sr.optString(0, "") : "";
                String rawSs = ss != null && ss.length() > 0 ? ss.optString(0, "") : "";

                // 从 "2026-11-24T06:42" 提取 "06:42"
                d.sunrise = extractTime(rawSr);
                d.sunset = extractTime(rawSs);
            }

            d.updateTime = System.currentTimeMillis();
            return d;
        } catch (Exception e) {
            Log.e(TAG, "API请求失败: " + e.getClass().getSimpleName() + " | " + e.getMessage(), e);
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
    private static String extractTime(String iso) {
        if (iso == null || iso.isEmpty()) return "";
        // "2026-11-24T06:42" → 取 T 后面部分
        int tIdx = iso.indexOf('T');
        if (tIdx >= 0 && tIdx + 1 < iso.length()) {
            return iso.substring(tIdx + 1);  // "06:42"
        }
        return iso; // 已经是 "HH:mm" 格式就原样返回
    }
    static int parseTimeToMinute(String time) {
        if (time == null || time.isEmpty()) return -1;
        try {
            // 先提 HH:mm（兼容 ISO 格式）
            String t = time.contains("T") ? time.substring(time.indexOf('T') + 1) : time;
            String[] p = t.split(":");
            return Integer.parseInt(p[0].trim()) * 60 + Integer.parseInt(p[1].trim());
        } catch (Exception e) {
            return -1;
        }
    }
    private void saveCache(WeatherData d) {
        cache.edit()
                .putFloat(KEY_CLOUD, d.cloudCover)
                .putFloat(KEY_UV, d.uvIndex)
                .putInt(KEY_CODE, d.weatherCode)
                .putString(KEY_SUNRISE, d.sunrise)
                .putString(KEY_SUNSET, d.sunset)
                .putLong(KEY_TIME, d.updateTime)
                .apply();
    }

    private WeatherData readCache() {
        WeatherData d = new WeatherData();
        d.cloudCover = cache.getFloat(KEY_CLOUD, 0);
        d.uvIndex = cache.getFloat(KEY_UV, 0);
        d.weatherCode = cache.getInt(KEY_CODE, 0);
        d.sunrise = cache.getString(KEY_SUNRISE, "");
        d.sunset = cache.getString(KEY_SUNSET, "");
        d.updateTime = cache.getLong(KEY_TIME, 0);
        return d;
    }

    public void clearCache() {
        cache.edit().clear().apply();
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}