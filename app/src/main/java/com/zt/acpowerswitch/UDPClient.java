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
            Conn_status = true;
        }
    }

    public String receiveMessage() {
        if (socket == null || socket.isClosed()) return null;

        StringBuilder responseBuilder = new StringBuilder();
        byte[] receiveData = new byte[4096]; // 提高到 4KB 缓冲，更稳健
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        try {

            try {
                socket.receive(receivePacket);
                String firstPart = new String(receivePacket.getData(), 0, receivePacket.getLength(), StandardCharsets.UTF_8);
                responseBuilder.append(firstPart);
                // 2. 持续接收后续包
                // 注意：Python 端文件间 sleep 了 100ms，所以这里至少设为 300-500ms
                // 这样才能跨过文件间的停顿，把所有文件连成一串
                socket.setSoTimeout(300);
                while (true) {
                    try {
                        socket.receive(receivePacket);
                        String data = new String(receivePacket.getData(), 0, receivePacket.getLength(), StandardCharsets.UTF_8);
                        responseBuilder.append(data);
                    } catch (SocketTimeoutException e) {
                        break;
                    }
                }
            } catch (SocketTimeoutException e) {
                return null; // 一个包都没收到
            }

            return responseBuilder.toString();

        } catch (IOException e) {
            about.log(TAG, "Socket异常: " + e.getMessage());
            return null;
        }
    }

    // 整个通讯过程加锁，防止多线程同时挤占同一个 Socket
    public synchronized String sendAndReceive(String message) {
        if (socket == null) return null;
        try {
            // 1. 安全地清空缓冲区（添加最大尝试次数）
            socket.setSoTimeout(10);  // 改为10ms，更合理
            byte[] trash = new byte[4096];
            DatagramPacket trashPacket = new DatagramPacket(trash, trash.length);

            int maxClearAttempts = 3;  // 最多尝试3次
            for (int i = 0; i < maxClearAttempts; i++) {
                try {
                    socket.receive(trashPacket);
                    about.log(TAG, "清空缓冲区数据: " + i);
                    // 收到数据，继续清空
                } catch (SocketTimeoutException e) {
                    // 超时，说明缓冲区已空
                    break;
                }
            }
        } catch (SocketException e) {
            about.log(TAG, "设置超时异常: " + e.getMessage());
            return null;
        } catch (IOException e) {
            about.log(TAG, "清空缓冲区异常: " + e.getMessage());
        }
        // 2. 发送和接收消息
        try {
            socket.setSoTimeout(3000);
            sendMessage(message);
            String response = receiveMessage();

            Conn_status = response == null;  // 通信成功

            return response;
        } catch (SocketException e) {
            about.log(TAG, "设置接收超时异常: " + e.getMessage());
            Conn_status = true;
            return null;
        }
    }

    public void close() {
        socket.close();
        udp_connect = false;
        about.log(TAG, "关闭网络连接");
    }
}