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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TCPClient {

    private static final String TAG = "TCPClient:";
    private static final int CONNECT_TIMEOUT = 3000;
    private static final int READ_TIMEOUT = 3000;

    // ============ DOH 配置 ============
    private static final String[] DOH_URLS = {
            "https://dns.alidns.com/resolve?name=%s&type=28",
            "https://doh.pub/dns-query?name=%s&type=28"
    };

    private static final OkHttpClient DOH_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .writeTimeout(3, TimeUnit.SECONDS)
            .build();

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

    // ============ DOH 缓存（新增） ============
    /**
     * 缓存条目：地址列表 + DNS返回的TTL(秒) + 缓存写入时间(毫秒)
     */
    private static class DnsCacheEntry {
        final List<InetAddress> addresses;
        final long ttl;
        final long cacheTime;

        DnsCacheEntry(List<InetAddress> addresses, long ttl) {
            this.addresses = addresses;
            this.ttl = ttl;
            this.cacheTime = System.currentTimeMillis();
        }

        boolean isExpired() {
            if (ttl <= 0) return false; // TTL=0 视为永不过期
            return System.currentTimeMillis() > cacheTime + ttl * 1000;
        }
    }

    // hostname → 缓存条目，线程安全
    private final ConcurrentHashMap<String, DnsCacheEntry> dnsCache = new ConcurrentHashMap<>();

    // =================================================

    /**
     * ✅ 建立/重建 TCP 连接（DOH 解析，优先用缓存）
     */
    @SuppressLint("DefaultLocale")
    public synchronized boolean tcpConnect() {
        if (isConnected()) {
            return true;
        }
        if (socket != null) {
            close();
            about.log(TAG, "关闭之前的旧连接");
        }

        // ---- 优先用缓存，过期才重新DOH ----
        String hostname = tcpServerAddress.trim();
        List<InetAddress> addresses = resolveWithCache(hostname);

        if (addresses.isEmpty()) {
            about.log(TAG, "DOH 解析失败，无可用地址");
            return false;
        }

        about.log(TAG, "共 " + addresses.size() + " 个地址，开始尝试连接");

        for (InetAddress address : addresses) {
            try {
                about.log(TAG, "尝试连接: " + address.getHostAddress() + ":" + tcpServerPort);

                socket = new Socket();
                socket.connect(new InetSocketAddress(address, tcpServerPort), CONNECT_TIMEOUT);
                socket.setSoTimeout(READ_TIMEOUT);
                socket.setKeepAlive(true);
                socket.setTcpNoDelay(true);

                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();

                about.log(TAG, "创建到设备的连接成功 -> " + address.getHostAddress());
                return true;
            } catch (IOException e) {
                about.log(TAG, "创建到设备的连接异常: " + e.getMessage());
                close();
            }
        }

        // 所有地址都失败 → 清除缓存，下次重新DOH
        about.log(TAG, "所有地址连接失败，清除 DNS 缓存");
        invalidateDnsCache(hostname);

        return false;
    }

    // =================================================
    // DOH 解析（带 TTL 缓存）
    // =================================================

    /**
     * 优先从缓存取，缓存过期或不存在才走 DOH
     */
    private List<InetAddress> resolveWithCache(String hostname) {
        DnsCacheEntry entry = dnsCache.get(hostname);

        if (entry != null && !entry.isExpired()) {
            long remain = (entry.cacheTime + entry.ttl * 1000 - System.currentTimeMillis()) / 1000;
            about.log(TAG, "使用 DNS 缓存（TTL剩余 " + remain + "s）");
            return new ArrayList<>(entry.addresses);
        }

        if (entry != null && entry.isExpired()) {
            about.log(TAG, "DNS 缓存已过期，重新解析");
            dnsCache.remove(hostname);
        }

        // 缓存没有或已过期 → 走 DOH
        return dohResolve(hostname);
    }

    /**
     * DOH 解析，返回所有 AAAA 记录，并写入缓存
     * 不缓存、不管 TTL、不做兜底
     */
    private List<InetAddress> dohResolve(String hostname) {
        List<InetAddress> results = new ArrayList<>();
        long minTtl = Long.MAX_VALUE;

        for (String dohTemplate : DOH_URLS) {
            String url = String.format(dohTemplate, hostname);
            Request request = new Request.Builder()
                    .url(url)
                    .header("Accept", "application/dns-json")
                    .build();

            try (Response response = DOH_CLIENT.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    about.log(TAG, "DOH HTTP " + response.code() + "，尝试下一个...");
                    continue;
                }

                JSONObject json = new JSONObject(response.body().string());
                JSONArray answers = json.optJSONArray("Answer");
                if (answers == null) {
                    about.log(TAG, "DOH 无 Answer 段，尝试下一个...");
                    continue;
                }

                for (int i = 0; i < answers.length(); i++) {
                    JSONObject ans = answers.getJSONObject(i);
                    if (ans.optInt("type") == 28) { // 28 = AAAA
                        String ipv6 = ans.getString("data").trim();
                        InetAddress addr = InetAddress.getByName(ipv6);
                        results.add(addr);
                        about.log(TAG, "DOH 解析到 IPv6: " + ipv6);

                        // 取所有 AAAA 记录中最小的 TTL
                        long ttl = ans.optLong("TTL", 60);
                        minTtl = Math.min(minTtl, ttl);
                    }
                }

                if (!results.isEmpty()) {
                    // TTL 至少 60 秒，防止 DNS 返回 0 或极小值
                    long cacheTtl = (minTtl == Long.MAX_VALUE) ? 60 : Math.max(minTtl, 60);
                    dnsCache.put(hostname, new DnsCacheEntry(results, cacheTtl));
                    about.log(TAG, "DNS 缓存写入，TTL=" + cacheTtl + "s");
                    return results;
                }

                about.log(TAG, "DOH 无 AAAA 记录，尝试下一个...");

            } catch (Exception e) {
                about.log(TAG, "DOH 请求异常: " + e.getMessage() + "，尝试下一个...");
            }
        }

        return results;
    }

    // =================================================
    // 对外接口：清除 DNS 缓存（新增）
    // =================================================

    /**
     * 清除指定 hostname 的 DNS 缓存
     */
    public void invalidateDnsCache(String hostname) {
        dnsCache.remove(hostname);
        about.log(TAG, "DNS 缓存已清除: " + hostname);
    }

    // =================================================
    // 以下原有方法完全不变
    // =================================================

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
        } catch (IOException e) {
            about.log(TAG, "读取异常: " + e.getMessage()+ "关闭连接");
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
            return null;
        } catch (SocketTimeoutException e) {
            about.log(TAG, "读超时 - 可能连接已断或被系统限制: " + e.getMessage());
            close(); // 主动关闭僵尸连接
            return null;
        } catch (IOException e) {
            about.log(TAG, "通信异常，关闭连接: " + e.getMessage());
            close(); // 主动关闭连接
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
}