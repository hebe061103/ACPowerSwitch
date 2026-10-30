package com.zt.acpowerswitch;

import static com.zt.acpowerswitch.MainActivity.tcpServerAddress;
import static com.zt.acpowerswitch.MainActivity.tcpServerPort;

import android.annotation.SuppressLint;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TCPClient {

    private static final String TAG = "TCPClient:";
    private static final int CONNECT_TIMEOUT = 3000;
    private static final int READ_TIMEOUT = 1000;

    private volatile Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;

    // ============ DNS over HTTPS（AAAA，国内双备援） ============
    private static final String[] DOH_URLS = {
            "https://dns.alidns.com/resolve?name=%s&type=28",  // 阿里 JSON API
            "https://doh.pub/dns-query?name=%s&type=28"         // 腾讯 DoH
    };

    private static final OkHttpClient DOH_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .writeTimeout(3, TimeUnit.SECONDS)
            .build();

    // ============ DNS 缓存（TTL 驱动） ============
    private static volatile InetAddress dnsCache = null;
    private static volatile long dnsCacheExpireTime = 0; // 过期时刻（毫秒）

    private static boolean isDnsCacheValid() {
        if (dnsCache == null) return false;
        return System.currentTimeMillis() < dnsCacheExpireTime;
    }

    /**
     * 双备援 DoH 解析 IPv6（AAAA）
     * 顺序：阿里 → 腾讯 → 失败抛 IOException
     * 成功后更新 dnsCache + dnsCacheExpireTime
     */
    private static InetAddress resolveViaDoh(String host) throws IOException {
        Exception lastException = null;

        for (String dohTemplate : DOH_URLS) {
            String url = String.format(dohTemplate, host);
            Request request = new Request.Builder()
                    .url(url)
                    .header("Accept", "application/dns-json")
                    .build();

            try (Response response = DOH_CLIENT.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    about.log(TAG, "DoH 服务返回异常 HTTP " + response.code() + "，尝试下一个...");
                    lastException = new IOException("DoH HTTP " + response.code());
                    continue;
                }

                JSONObject json = new JSONObject(response.body().string());
                JSONArray answers = json.optJSONArray("Answer");
                if (answers == null) {
                    about.log(TAG, "DoH 无 Answer 段，尝试下一个...");
                    lastException = new UnknownHostException("No AAAA record: " + host);
                    continue;
                }

                for (int i = 0; i < answers.length(); i++) {
                    JSONObject ans = answers.getJSONObject(i);
                    if (ans.optInt("type") == 28) {          // 28 = AAAA
                        String ipv6 = ans.getString("data").trim();
                        long ttl = ans.optLong("TTL", 300);  // 默认 300s 兜底

                        dnsCache = InetAddress.getByAddress(
                                host,
                                InetAddress.getByName(ipv6).getAddress()
                        );
                        dnsCacheExpireTime = System.currentTimeMillis() + ttl * 1000;

                        about.log(TAG, "DoH 解析到 IPv6: " + ipv6 + "，TTL=" + ttl + "s");
                        return dnsCache;
                    }
                }
                about.log(TAG, "DoH 无 AAAA 记录，尝试下一个...");
                lastException = new UnknownHostException("AAAA empty: " + host);

            } catch (Exception e) {
                about.log(TAG, "DoH 请求异常: " + e.getMessage() + "，尝试下一个...");
                lastException = e;
            }
        }

        // 两个都失败了
        if (lastException instanceof IOException) {
            throw (IOException) lastException;
        }
        throw new IOException("所有 DoH 服务均失败", lastException);
    }

    // =================================================

    /**
     * ✅ 只负责建立 TCP 连接
     * ✅ synchronized 防止并发重连
     * ✅ DoH 解析 → 成功写缓存（TTL 驱动）；失败 → 用有效缓存；过期 → 立即失败
     */
    @SuppressLint("DefaultLocale")
    public synchronized boolean tcpConnect() {
        if (isConnected()) {
            return true;
        }

        close();

        InetAddress address;

        // ===== DNS（DoH AAAA）=====
        try {
            about.log(TAG, "开始 DoH 解析 IPv6...");
            address = resolveViaDoh(tcpServerAddress.trim());
            about.log(TAG, "DoH 解析成功并缓存: " + address.getHostAddress());

        } catch (Exception e) {
            about.log(TAG, "DoH 解析失败: " + e.getMessage());

            if (isDnsCacheValid()) {
                address = dnsCache;
                about.log(TAG, "使用有效缓存 DNS: " + address.getHostAddress());
            } else {
                about.log(TAG, "缓存已过期且无可用解析，连接失败");
                dnsCache = null;
                dnsCacheExpireTime = 0;
                return false;
            }
        }

        // ===== TCP 连接 =====
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(address, tcpServerPort), CONNECT_TIMEOUT);
            socket.setSoTimeout(READ_TIMEOUT);

            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            about.log(TAG, "创建连接成功");
            return true;

        } catch (IOException e) {
            about.log(TAG, "创建连接异常: " + e.getMessage());
            close();
            return false;
        }
    }

    // ========== 以下代码完全未改动 ==========

    /**
     * ✅ 只发送数据，不负责重连
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
            // 正常超时
        } catch (IOException e) {
            about.log(TAG, "读取异常: " + e.getMessage());
            close();
        }

        return responseBuilder.toString();
    }

    /**
     * ✅ 对外统一入口
     * ✅ 负责重连策略
     */
    public String sendAndReceive(String message) {
        int retryCount = 0;
        int maxRetry = 3;

        while (retryCount < maxRetry) {
            try {
                if (!isConnected()) {
                    about.log(TAG, "尝试连接 (" + (retryCount + 1) + "/" + maxRetry + ")");
                    if (!tcpConnect()) {
                        retryCount++;
                        continue;
                    }
                }

                sendMessage(message);
                return receiveMessage();

            } catch (IOException e) {
                about.log(TAG, "通信异常，准备重连...");
                close();
                retryCount++;
            }
        }

        about.log(TAG, "多次重连失败，放弃本次请求");
        return null;
    }

    /**
     * ✅ 连接状态判断
     */
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    /**
     * ✅ 关闭资源
     */
    public void close() {
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
}