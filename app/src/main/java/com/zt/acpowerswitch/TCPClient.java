package com.zt.acpowerswitch;

import static com.zt.acpowerswitch.MainActivity.tcpServerAddress;
import static com.zt.acpowerswitch.MainActivity.tcpServerPort;

import android.annotation.SuppressLint;

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
    private static final int RETRY_INTERVAL_MS = 3000;

    private volatile Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;

    /**
     * ✅ 只负责建立 TCP 连接
     * ✅ synchronized 防止并发重连
     */
    @SuppressLint("DefaultLocale")
    public synchronized boolean tcpConnect() {
        // 如果已经连上了，直接返回
        if (isConnected()) {
            return true;
        }

        while (!Thread.currentThread().isInterrupted()) {
            try {
                close();

                about.log(TAG, "开始解析 DNS...");
                InetAddress address = InetAddress.getByName(tcpServerAddress.trim());

                socket = new Socket();
                socket.connect(new InetSocketAddress(address, tcpServerPort), CONNECT_TIMEOUT);
                socket.setSoTimeout(READ_TIMEOUT);

                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();

                about.log(TAG, "创建连接成功");
                return true;

            } catch (UnknownHostException e) {
                about.log(TAG, "DNS解析失败，3秒后重试...");
            } catch (IOException e) {
                about.log(TAG, "创建连接异常，3秒后重试...");
            }

            try {
                Thread.sleep(RETRY_INTERVAL_MS);
            } catch (InterruptedException e) {
                about.log(TAG, "连接线程被中断");
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return false;
    }

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
        return socket != null
                && socket.isConnected()
                && !socket.isClosed();
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