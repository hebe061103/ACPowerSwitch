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
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TCPClient {

    private static final String TAG = "TCPClient:";
    private static final int CONNECT_TIMEOUT = 3000;
    private static final int READ_TIMEOUT = 1000;

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

    // =================================================

    /**
     * ✅ 建立/重建 TCP 连接（DOH 解析）
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

        List<InetAddress> addresses = dohResolve(tcpServerAddress.trim());
        if (addresses.isEmpty()) {
            about.log(TAG, "DOH 解析失败，无可用地址");
            return false;
        }

        about.log(TAG, "DOH 解析到 " + addresses.size() + " 个地址，开始尝试连接");

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

        about.log(TAG, "所有地址连接失败");
        return false;
    }

    // =================================================
    // DOH 解析（纯解析，无缓存逻辑）
    // =================================================

    /**
     * DOH 解析，返回所有 AAAA 记录
     * 不缓存、不管 TTL、不做兜底
     */
    private List<InetAddress> dohResolve(String hostname) {
        List<InetAddress> results = new ArrayList<>();

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
                    }
                }

                if (!results.isEmpty()) {
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