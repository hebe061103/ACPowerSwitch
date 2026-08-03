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
    public static volatile Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;

    @SuppressLint("DefaultLocale")
    public boolean tcpConnect() {
        final int MAX_RETRY = 3;

        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                // ✅ 关键：手动解析 DNS（每次都会重新解析）
                InetAddress address = InetAddress.getByName(tcpServerAddress.trim());

                socket = new Socket();
                socket.connect(new InetSocketAddress(address, tcpServerPort), 3000);
                socket.setSoTimeout(1000);

                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();

                about.log(TAG, String.format(
                        "创建连接成功 | 域名=%s IP=%s 第%d次尝试",
                        tcpServerAddress, address.getHostAddress(), attempt
                ));
                return true;

            } catch (UnknownHostException e) {
                about.log(TAG, String.format(
                        "DNS解析失败 | 域名=%s 第%d/%d次 异常=%s",
                        tcpServerAddress, attempt, MAX_RETRY, e.getClass().getSimpleName()
                ));
            } catch (IOException e) {
                about.log(TAG, String.format(
                        "创建连接异常 | 域名=%s 第%d/%d次 异常=%s 原因=%s",
                        tcpServerAddress, attempt, MAX_RETRY,
                        e.getClass().getSimpleName(), e.getMessage()
                ));
            }

            // ✅ 指数退避
            if (attempt < MAX_RETRY) {
                try {
                    Thread.sleep(500 * attempt);
                } catch (InterruptedException ignored) {}
            }
        }

        about.log(TAG, "创建连接最终失败 | 域名=" + tcpServerAddress);
        return false;
    }

    public void sendMessage(String message) {
        try {
            // 1. 检查 socket 状态。如果未连接或已关闭，直接触发重连
            if (socket == null || socket.isClosed() || !socket.isConnected() || outputStream == null) {
                about.log(TAG, "检测到连接已断开，尝试自动重连...");
                if (!tcpConnect()) {
                    return; // 重连失败则退出
                }
            }

            // 2. 发送数据
            byte[] data = message.getBytes(StandardCharsets.UTF_8);
            outputStream.write(data);
            outputStream.flush();

        } catch (IOException e) {
            about.log(TAG, "发送异常 (可能连接已损坏): " + e.getMessage());

            // 3. 核心：如果捕获到 Broken pipe 等 IO 异常，立即关闭旧 Socket，强制下次发送时重连
            close();
        }
    }


    public String receiveMessage() {
        if (socket == null || socket.isClosed() || inputStream == null) return null;
        StringBuilder responseBuilder = new StringBuilder();
        byte[] buffer = new byte[4096];

        try {
            int bytesRead;
            // TCP 使用 read 从输入流中持续读取数据
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                String chunk = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                if (!chunk.isEmpty()) {
                    responseBuilder.append(chunk);
                }

                // 检查是否包含结束标记
                String current = responseBuilder.toString();
                if (current.contains("mark1") || current.contains("mark2") || current.contains("mark3") || current.contains("ACK")) {
                    break;
                }
            }
        } catch (SocketTimeoutException e) {
            // 超时，返回已接收的部分
        } catch (IOException e) {
            about.log(TAG, "读取异常: " + e.getMessage());
        }

        return responseBuilder.toString();
    }

    public String sendAndReceive(String message) {
        // 如果 socket 未连接，尝试重新连接
        if (socket == null || socket.isClosed() || !socket.isConnected()) {
            if (!tcpConnect()) return null;
        }
        synchronized (this) {
            sendMessage(message);
            return receiveMessage();
        }
    }

    public void close() {
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            about.log(TAG, "关闭网络连接");
        } catch (IOException e) {
            about.log(TAG, "关闭网络连接异常: " + e.getMessage());
        }
    }
}
