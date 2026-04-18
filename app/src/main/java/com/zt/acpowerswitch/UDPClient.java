package com.zt.acpowerswitch;

import static com.zt.acpowerswitch.MainActivity.Conn_status;
import static com.zt.acpowerswitch.MainActivity.udpServerAddress;
import static com.zt.acpowerswitch.MainActivity.udpServerPort;
import static com.zt.acpowerswitch.MainActivity.udp_connect;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

public class UDPClient {
    private static final String TAG = "UDPClient:";
    public static DatagramSocket socket;

    // 移除不必要的单线程池，改为复用 InetAddress
    private InetAddress cachedServerAddress = null;

    public void udpConnect() {
        new Thread(() -> {
            try {
                if (!udp_connect) {
                    socket = new DatagramSocket();
                    socket.setSoTimeout(3000);
                    udp_connect = true;
                    about.log(TAG, "创建套接字成功");
                }
            } catch (SocketException e) {
                about.log(TAG, "创建套接字失败");
            }
        }).start();
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
        }
    }

    public String receiveMessage() {
        try {
            if (socket == null || socket.isClosed()) return null;

            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            // 物理上的超时控制（核心：由 Socket 自身处理超时）
            socket.setSoTimeout(3000); // 缩短至3秒，提高响应
            socket.receive(receivePacket);

            return new String(receivePacket.getData(), 0, receivePacket.getLength(), StandardCharsets.UTF_8);

        } catch (SocketTimeoutException e) {
            about.log(TAG, "接收超时");
            Conn_status = true;
            return null;
        } catch (IOException e) {
            about.log(TAG, "读取异常: " + e.getMessage());
            return null;
        }
    }

    // 整个通讯过程加锁，防止多线程同时挤占同一个 Socket
    public synchronized String sendAndReceive(String message) {
        if (socket == null) return null;

        // --- 1. 暴力清空底层缓冲区 ---
        try {
            // 设置一个极短的超时，快速试探
            socket.setSoTimeout(1);
            byte[] trash = new byte[1024];
            DatagramPacket trashPacket = new DatagramPacket(trash, trash.length);
            while (true) {
                socket.receive(trashPacket); // 只要能收到东西，就说明有存货
                // 循环直到抛出 SocketTimeoutException，说明清空了
            }
        } catch (SocketTimeoutException e) {
            // 缓冲区已空，这是正常现象，跳出循环
        } catch (IOException e) {
            // 其他错误
        }

        // --- 2. 恢复正常的超时设置，发送新请求 ---
        try {
            socket.setSoTimeout(3000); // 设回正常的 3 秒
            sendMessage(message);
            return receiveMessage();
        } catch (SocketException e) {
            return null;
        }
    }

    public void close() {
        socket.close();
        udp_connect = false;
        about.log(TAG, "关闭网络连接");
    }
}