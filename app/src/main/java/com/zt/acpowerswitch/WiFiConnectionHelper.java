package com.zt.acpowerswitch;

import static com.zt.acpowerswitch.MainActivity.TAG;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

public class WiFiConnectionHelper {

    private final Activity activity;
    private final WiFiConnectionListener listener;
    private static final int MAX_RETRY_COUNT = 3;
    private int retryCount = 0;

    public interface WiFiConnectionListener {
        void onWiFiConnected(String ssid);
        void onWiFiNotConnected();
        void onTargetWiFiConnected(String ssid);
        void onTargetWiFiNotConnected();
    }

    public WiFiConnectionHelper(Activity activity, WiFiConnectionListener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    /**
     * 开始检查 WiFi 连接状态
     */
    public void startChecking() {
        checkWiFiConnectionWithRetry();
    }

    /**
     * 检查 WiFi 连接（带重试机制）
     */
    public void checkWiFiConnectionWithRetry() {
        if (retryCount >= MAX_RETRY_COUNT) {
            // 重试次数用完，显示最终提示
            showFinalNoWiFiDialog();
            if (listener != null) {
                listener.onWiFiNotConnected();
            }
            return;
        }

        WifiManager wifiManager = (WifiManager)
                activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (wifiManager == null) {
            showWiFiManagerErrorDialog();
            return;
        }

        // 检查 WiFi 是否启用
        if (!wifiManager.isWifiEnabled()) {
            showEnableWiFiDialog();
            return;
        }

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null) {
            retryCount++;
            showRetryDialog();
            return;
        }

        int networkId = wifiInfo.getNetworkId();

        if (networkId == -1) {
            // 没有连接到任何 WiFi
            retryCount++;
            showConnectToWiFiDialog();
        } else {
            // 已连接到某个 WiFi
            retryCount = 0; // 重置重试计数
            String ssid = wifiInfo.getSSID();

            // 清理 SSID 中的引号
            if (ssid != null && ssid.startsWith("\"") && ssid.endsWith("\"")) {
                ssid = ssid.substring(1, ssid.length() - 1);
            }

            if (listener != null) {
                listener.onWiFiConnected(ssid);
            }

            // 检查是否连接到目标 WiFi ESP32-UDP-Server
            if (ssid != null && ssid.equals("ESP32-UDP-Server")) {
                if (listener != null) {
                    listener.onTargetWiFiConnected(ssid);
                }
            } else {
                showConnectToTargetWiFiDialog(ssid);
            }
        }
    }

    /**
     * 提示用户开启 WiFi
     */
    public void showEnableWiFiDialog() {
        activity.runOnUiThread(() -> new AlertDialog.Builder(activity)
                .setTitle("WiFi未开启")
                .setMessage("请开启WiFi以连接设备")
                .setPositiveButton("去开启", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                    activity.startActivity(intent);
                    dialog.dismiss();
                    // 等待一段时间后重新检查
                    new android.os.Handler().postDelayed(
                            this::checkWiFiConnectionWithRetry, 2000);
                })
                .setNegativeButton("取消", (dialog, which) -> {
                    dialog.dismiss();
                    if (listener != null) {
                        listener.onWiFiNotConnected();
                    }
                })
                .setCancelable(false)
                .show());
    }

    /**
     * 提示用户连接 WiFi
     */
    private void showConnectToWiFiDialog() {
        activity.runOnUiThread(() -> new AlertDialog.Builder(activity)
                .setTitle("未连接WiFi")
                .setMessage("请连接到WiFi网络")
                .setPositiveButton("去连接", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                    activity.startActivity(intent);
                    dialog.dismiss();
                    // 等待一段时间后重新检查
                    new android.os.Handler().postDelayed(
                            this::checkWiFiConnectionWithRetry, 3000);
                })
                .setNegativeButton("重试", (dialog, which) -> {
                    dialog.dismiss();
                    // 立即重试
                    new android.os.Handler().postDelayed(
                            this::checkWiFiConnectionWithRetry, 1000);
                })
                .setNeutralButton("取消", (dialog, which) -> {
                    dialog.dismiss();
                    if (listener != null) {
                        listener.onWiFiNotConnected();
                    }
                })
                .setCancelable(false)
                .show());
    }

    /**
     * 提示用户连接到目标 WiFi
     */
    private void showConnectToTargetWiFiDialog(String currentSsid) {
        Log.d(TAG, "showConnectToTargetWiFiDialog() 被调用，调用栈：");
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            Log.d(TAG, "    " + element.toString());
        }
        activity.runOnUiThread(() -> {
            String message = currentSsid == null ?
                    "当前连接到未知WiFi" :
                    "当前WiFi: " + currentSsid + "\n请连接到: ESP32-UDP-Server";

            new AlertDialog.Builder(activity)
                    .setTitle("连接到目标WiFi")
                    .setMessage(message)
                    .setPositiveButton("去连接", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                        activity.startActivity(intent);
                        dialog.dismiss();
                        // 等待一段时间后重新检查
                        new android.os.Handler().postDelayed(
                                this::checkWiFiConnectionWithRetry, 3000);
                    })
                    .setNegativeButton("重试", (dialog, which) -> {
                        dialog.dismiss();
                        // 立即重试
                        new android.os.Handler().postDelayed(
                                this::checkWiFiConnectionWithRetry, 2000);
                    })
                    .setNeutralButton("继续使用", (dialog, which) -> {
                        dialog.dismiss();
                        if (listener != null && currentSsid != null) {
                            listener.onTargetWiFiNotConnected();
                        }
                    })
                    .setCancelable(false)
                    .show();
        });
    }

    /**
     * 重试对话框
     */
    private void showRetryDialog() {
        activity.runOnUiThread(() -> new AlertDialog.Builder(activity)
                .setTitle("无法获取WiFi信息")
                .setMessage("是否重试？")
                .setPositiveButton("重试", (dialog, which) -> {
                    dialog.dismiss();
                    new android.os.Handler().postDelayed(
                            this::checkWiFiConnectionWithRetry, 1000);
                })
                .setNegativeButton("取消", (dialog, which) -> {
                    dialog.dismiss();
                    if (listener != null) {
                        listener.onWiFiNotConnected();
                    }
                })
                .setCancelable(false)
                .show());
    }

    /**
     * WiFiManager 错误对话框
     */
    private void showWiFiManagerErrorDialog() {
        activity.runOnUiThread(() -> {
            Toast.makeText(activity, "WiFi服务不可用", Toast.LENGTH_SHORT).show();
            if (listener != null) {
                listener.onWiFiNotConnected();
            }
        });
    }

    /**
     * 最终无法连接提示
     */
    private void showFinalNoWiFiDialog() {
        activity.runOnUiThread(() -> new AlertDialog.Builder(activity)
                .setTitle("连接失败")
                .setMessage("无法连接到WiFi，请检查网络设置")
                .setPositiveButton("确定", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show());
    }
}