package com.zt.acpowerswitch;

import static com.zt.acpowerswitch.MainActivity.Conn_status;
import static com.zt.acpowerswitch.MainActivity.tcpServerAddress;
import static com.zt.acpowerswitch.MainActivity.tcpServerPort;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

public class TCPClient {
    private static final String TAG = "TCPClient:";
    public static volatile Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;

    public boolean tcpConnect() {
        try {
            // 关闭旧连接
            close();

            socket = new Socket();
            // 设置连接超时时间为 3000ms
            socket.connect(new InetSocketAddress(tcpServerAddress, tcpServerPort), 3000);
            // 设置读取超时时间为 1000ms
            socket.setSoTimeout(1000);

            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            about.log(TAG, "创建连接成功");
            return true;
        } catch (IOException e) {
            about.log(TAG, "创建连接失败: " + e.getMessage());
            return false;
        }
    }

    public void sendMessage(String message) {
        try {
            // 1. 检查 socket 状态。如果未连接或已关闭，直接触发重连
            if (socket == null || socket.isClosed() || !socket.isConnected() || outputStream == null) {
                about.log(TAG, "检测到连接已断开，尝试自动重连...");
                if (!tcpConnect()) {
                    Conn_status = true;
                    return; // 重连失败则退出
                }
            }

            // 2. 发送数据
            byte[] data = message.getBytes(StandardCharsets.UTF_8);
            outputStream.write(data);
            outputStream.flush();

        } catch (IOException e) {
            about.log(TAG, "发送异常 (可能连接已损坏): " + e.getMessage());
            Conn_status = true;

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
            Conn_status = true;
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
            String response = receiveMessage();
            Conn_status = (response == null || response.isEmpty());
            return response;
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
