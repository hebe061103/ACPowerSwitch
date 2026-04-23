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
        byte[] receiveData = new byte[4096]; // 提高到 4KB 缓冲，更稳健
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        try {
            try {
                socket.receive(receivePacket);
                String Part = new String(receivePacket.getData(), 0, receivePacket.getLength(), StandardCharsets.UTF_8);
                responseBuilder.append(Part);
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
                return null;
            }
            return responseBuilder.toString();
        } catch (IOException e) {
            about.log(TAG, "Socket异常: " + e.getMessage());
            return null;
        }
    }

    public String sendAndReceive(String message) {
        if (socket == null) return null;
        clearBuffer();
        try {
            socket.setSoTimeout(3000);
            // 只对发送加锁
            synchronized (this) {
                sendMessage(message);
            }
            // 接收操作不加锁，允许多个线程同时接收（实际上不会，因为每个线程有独立的响应）
            String response = receiveMessage();
            Conn_status = response == null;
            return response;
        } catch (SocketException e) {
            about.log(TAG, "接收超时异常: " + e.getMessage());
            Conn_status = true;
            return null;
        }
    }

    private void clearBuffer() {
        try {
            socket.setSoTimeout(1);
            byte[] trash = new byte[4096];
            DatagramPacket trashPacket = new DatagramPacket(trash, trash.length);

            while (true){
                try {
                    socket.receive(trashPacket);
                    about.log(TAG, "清空: " + trashPacket.getLength() + " 字节");
                } catch (SocketTimeoutException e) {
                    break;
                }
            }
        } catch (Exception e) {
            about.log(TAG, "清空缓冲区异常: " + e.getMessage());
        }
    }
    public void close() {
        socket.close();
        about.log(TAG, "关闭网络连接");
    }
}