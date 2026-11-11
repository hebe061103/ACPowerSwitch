package com.zt.acpowerswitch;

import static com.zt.acpowerswitch.MainActivity.tcpServerAddress;
import static com.zt.acpowerswitch.MainActivity.tcpServerPort;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class TCPClient {

    private static final String TAG = "TCPClient:";
    private static final int CONNECT_TIMEOUT = 3000;
    private static final int READ_TIMEOUT = 1000;

    // ============ 单例 ============
    private static volatile TCPClient instance;

    public static TCPClient getInstance() {
        if (instance == null) {
            synchronized (TCPClient.class) {
                if (instance == null) {
                    instance = new TCPClient();
                }
            }
        }
        return instance;
    }

    private TCPClient() {}

    // ==============================

    private volatile Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;

    // =================================================

    /**
     * ✅ 建立/重建 TCP 连接（系统 DNS 解析）
     */
    @SuppressLint("DefaultLocale")
    public synchronized boolean tcpConnect() {
        if (isConnected()) {
            return true;
        }

        close();

        InetAddress address;
        try {
            about.log(TAG, "开始 DNS 解析: " + tcpServerAddress);
            address = InetAddress.getByName(tcpServerAddress.trim());
            about.log(TAG, "解析结果: " + address.getHostAddress());
        } catch (UnknownHostException e) {
            about.log(TAG, "DNS 解析失败: " + e.getMessage());
            return false;
        }

        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(address, tcpServerPort), CONNECT_TIMEOUT);
            socket.setSoTimeout(READ_TIMEOUT);
            socket.setKeepAlive(true);
            socket.setTcpNoDelay(true);

            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            about.log(TAG, "创建连接成功 -> " + address.getHostAddress());
            return true;

        } catch (IOException e) {
            about.log(TAG, "创建连接异常: " + e.getMessage());
            close();
            return false;
        }
    }

    /**
     * ✅ 发送数据
     */
    public void sendMessage(String message) throws IOException {
        if (!isConnected() || outputStream == null) {
            throw new IOException("Socket未连接");
        }

        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        outputStream.write(data);
        outputStream.flush();
    }

    /**
     * ✅ 接收数据
     */
    public String receiveMessage() {
        if (!isConnected() || inputStream == null) {
            return null;
        }

        StringBuilder responseBuilder = new StringBuilder();
        byte[] buffer = new byte[4096];

        try {
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                String chunk = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                if (!chunk.isEmpty()) {
                    responseBuilder.append(chunk);
                }

                String current = responseBuilder.toString();
                if (current.contains("mark1")
                        || current.contains("mark2")
                        || current.contains("mark3")
                        || current.contains("ACK")) {
                    break;
                }
            }
        } catch (SocketTimeoutException ignored) {
        } catch (IOException e) {
            about.log(TAG, "读取异常: " + e.getMessage());
            close();
        }

        return responseBuilder.toString();
    }

    /**
     * ✅ 对外统一入口（复用 Socket）
     */
    public String sendAndReceive(String message) {
        try {
            if (isConnected()) {
                sendMessage(message);
                return receiveMessage();
            }

            // 未连接 → 尝试重连（最多 3 次）
            int retryCount = 0;
            while (retryCount < 3) {
                about.log(TAG, "尝试连接 (" + (retryCount + 1) + "/3)");
                if (tcpConnect()) {
                    sendMessage(message);
                    return receiveMessage();
                }
                retryCount++;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }

            about.log(TAG, "连接失败，放弃本次请求");
            return null;

        } catch (IOException e) {
            about.log(TAG, "通信异常，关闭连接");
            close();
            return null;
        }
    }

    /**
     * ✅ 连接状态
     */
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    /**
     * ✅ 网络切换时调用
     */
    public synchronized void onNetworkChanged() {
        about.log(TAG, "网络切换，清理连接");
        close();
    }

    /**
     * ✅ 主动关闭
     */
    public synchronized void close() {
        try {
            if (inputStream != null) inputStream.close();
        } catch (IOException ignored) {
        }

        try {
            if (outputStream != null) outputStream.close();
        } catch (IOException ignored) {
        }

        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }

        inputStream = null;
        outputStream = null;
        socket = null;
    }

    // ============ 网络切换广播（内部类） ============
    public static class NetworkChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {

                ConnectivityManager cm =
                        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

                if (activeNetwork != null && activeNetwork.isConnected()) {
                    about.log("NetworkChangeReceiver",
                            "网络已连接: " + activeNetwork.getTypeName());
                } else {
                    about.log("NetworkChangeReceiver", "网络已断开");
                }

                // 无论连接还是断开，都清理旧连接
                TCPClient.getInstance().onNetworkChanged();
            }
        }
    }
}