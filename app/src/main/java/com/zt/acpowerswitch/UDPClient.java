package com.zt.acpowerswitch;

import static com.zt.acpowerswitch.MainActivity.Conn_status;
import static com.zt.acpowerswitch.MainActivity.udpServerAddress;
import static com.zt.acpowerswitch.MainActivity.udpServerPort;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

public class UDPClient {
    private static final String TAG = "UDPClient:";
    public static volatile DatagramSocket socket;
    private InetAddress cachedServerAddress = null;

    public boolean udpConnect() {
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(3000);
            about.log(TAG, "创建套接字成功");
            return true;
        } catch (SocketException e) {
            about.log(TAG, "创建套接字失败");
            return false;
        }
    }

    public void sendMessage(String message) {
        try {
            byte[] data = message.getBytes(StandardCharsets.UTF_8);
            if (cachedServerAddress == null) {
                cachedServerAddress = InetAddress.getByName(udpServerAddress);
            }
            DatagramPacket packet = new DatagramPacket(data, data.length, cachedServerAddress, udpServerPort);
            if (socket != null && !socket.isClosed()) {
                socket.send(packet);
            }
        } catch (Exception e) {
            about.log(TAG, "发送异常: " + e.getMessage());
            Conn_status = true;
        }
    }
    public String receiveMessage() {
        if (socket == null || socket.isClosed()) return null;
        StringBuilder responseBuilder = new StringBuilder();
        byte[] buffer = new byte[4096];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        try {
            socket.setSoTimeout(1000);

            while (true) {
                // 接收一个数据块
                socket.receive(packet);

                // 将接收到的字节转换为字符串
                String chunk = new String(buffer, 0, packet.getLength(), StandardCharsets.UTF_8);
                if (!chunk.isEmpty()) {
                    responseBuilder.append(chunk);
                }
                // 检查是否包含结束标记
                String current = responseBuilder.toString();
                if (current.contains("mark1") || current.contains("mark2") || current.contains("mark3") || current.contains("ACK")) {
                    break;
                }

                // 重置缓冲区接收下一个数据块
                buffer = new byte[4096];
                packet.setData(buffer);
            }
        } catch (SocketTimeoutException e) {
            // 超时，返回已接收的部分
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return responseBuilder.toString();
    }

    public String sendAndReceive(String message) {
        if (socket == null) return null;
        synchronized (this) {
            sendMessage(message);
            String response = receiveMessage();
            Conn_status = response == null;
            return response;
        }
    }

    public void close() {
        socket.close();
        about.log(TAG, "关闭网络连接");
    }
}