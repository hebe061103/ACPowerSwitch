package com.zt.acpowerswitch;

import static com.zt.acpowerswitch.MainActivity.Conn_status;
import static com.zt.acpowerswitch.MainActivity.udpServerAddress;
import static com.zt.acpowerswitch.MainActivity.udpServerPort;
import static com.zt.acpowerswitch.MainActivity.udp_connect;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UDPClient {
    private static final String TAG = "UDPClient:";
    public static DatagramSocket socket;
    public static DatagramPacket packet;
    public void udpConnect() {
        new Thread(() -> {
            // 创建Socket对象，并指定服务器的IP地址和端口号
            try {
                if (!udp_connect) {
                    socket = new DatagramSocket(udpServerPort);
                    socket.setSoTimeout(1000);
                    udp_connect = true;
                    about.log(TAG, "创建套接字成功");
                }
            } catch (SocketException e) {
                //throw new RuntimeException(e);
                about.log(TAG, "创建套接字失败");
            }
        }).start();
    }

    // 在类成员变量位置，缓存解析好的 InetAddress
    private InetAddress cachedServerAddress = null;
    private final Object addressLock = new Object(); // 用于同步的锁对象

    public void sendMessage(String message) {
        new Thread(() -> {
            byte[] data = message.getBytes();

            // 核心优化：只在第一次或需要时解析
            synchronized (addressLock) {
                if (cachedServerAddress == null) {
                    try {
                        cachedServerAddress = InetAddress.getByName(udpServerAddress);
                        about.log(TAG, "【首次解析】域名: " + udpServerAddress + " -> IP: " + cachedServerAddress.getHostAddress());
                    } catch (UnknownHostException e) {
                        about.log(TAG, "无法解析域名或IP地址");
                        Conn_status = true; // 注意：这里设置为true似乎表示“连接异常”？变量名建议优化
                        return; // 解析失败，直接返回，不发送
                    }
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
                // 可以考虑在这里处理发送失败，例如标记cachedServerAddress = null，让下次重试解析
            }
        }).start();
    }

    public String receiveMessage() {
        byte[] buffer = new byte[1024];
        packet = new DatagramPacket(buffer, buffer.length);
        try {
            if (socket != null) {
                socket.receive(packet);
            }
        } catch (Exception e) {
            //e.printStackTrace(); // 打印堆栈跟踪
        }
        return new String(packet.getData(), 0, packet.getLength());
    }
    public void close() {
        socket.close();
        udp_connect = false;
        about.log(TAG, "关闭网络连接");
    }
}