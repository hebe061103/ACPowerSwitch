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
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class UDPClient {
    private static final String TAG = "UDPClient:";
    public static DatagramSocket socket;
    public static DatagramPacket packet;
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

    // 在类成员变量位置，缓存解析好的 InetAddress
    private InetAddress cachedServerAddress = null;
    private final Object sendLock = new Object();
    public void sendMessage(String message) {
        synchronized (sendLock) {
            byte[] data = message.getBytes();
            if (cachedServerAddress == null) {
                try {
                    cachedServerAddress = InetAddress.getByName(udpServerAddress);
                    about.log(TAG, "首次解析域名: " + udpServerAddress + " -> IP: " + cachedServerAddress.getHostAddress());
                } catch (UnknownHostException e) {
                    about.log(TAG, "无法解析域名或IP地址");
                    Conn_status = true; // 这里设置为true表示“连接异常”
                    return; // 解析失败，直接返回，不发送
                }
            }

            // 使用缓存的地址创建数据包
            DatagramPacket packet = new DatagramPacket(data, data.length, cachedServerAddress, udpServerPort);

            try {
                if (socket != null) {
                    socket.send(packet);
                }
            } catch (Exception e) {
                about.log(TAG, "发送失败: " + e.getMessage());
            }
        }
    }
    private final Object receiveLock = new Object();
    public String receiveMessage() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // 使用 Callable 可以返回结果
            Future<String> future = executor.submit(() -> {
                synchronized (receiveLock) {
                    try {
                        byte[] receiveData = new byte[1024];
                        packet = new DatagramPacket(receiveData, receiveData.length);

                        // 设置接收超时（避免永久阻塞）
                        socket.setSoTimeout(5000);  // 5秒超时
                        socket.receive(packet);

                        return new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);

                    } catch (SocketTimeoutException e) {
                        about.log(TAG, "接收超时，未收到任何数据");
                        return null;
                    } catch (IOException e) {
                        about.log(TAG, "接收数据异常: " + e.getMessage());
                        return null;
                    }
                }
            });
            // 等待并获取结果（可设置超时）
            return future.get(6, TimeUnit.SECONDS);  // 等待6秒

        } catch (TimeoutException e) {
            about.log(TAG, "接收操作超时");
            return null;
        } catch (Exception e) {
            about.log(TAG, "接收异常: " + e.getMessage());
            return null;
        } finally {
            executor.shutdown();
        }
    }
    /**
     * 发送并接收（原子操作）
     */
    public String sendAndReceive(String message) {
        synchronized (this) {  // 使用this锁，确保整个操作原子性
            sendMessage(message);
            // 等待一下，让服务器有时间处理
            try { Thread.sleep(50); } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return receiveMessage();
        }
    }
    public void close() {
        socket.close();
        udp_connect = false;
        about.log(TAG, "关闭网络连接");
    }
}