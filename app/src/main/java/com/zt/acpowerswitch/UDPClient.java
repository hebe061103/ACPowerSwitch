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

    public void sendMessage(String message) {
        new Thread(() -> {
            byte[] data = message.getBytes();
            try {
                packet = new DatagramPacket(data, data.length, InetAddress.getByName(udpServerAddress), udpServerPort);
            } catch (UnknownHostException e) {
                //e.printStackTrace();
                about.log(TAG,"无法解析域名或IP地址");
                Conn_status=true;
            }
            try {
                if (socket != null) {
                    socket.send(packet);
                }
            } catch (Exception e) {
                e.printStackTrace();
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
            e.printStackTrace(); // 打印堆栈跟踪
        }
        return new String(packet.getData(), 0, packet.getLength());
    }
    public void close() {
        socket.close();
        udp_connect = false;
        about.log(TAG, "关闭网络连接");
    }
}