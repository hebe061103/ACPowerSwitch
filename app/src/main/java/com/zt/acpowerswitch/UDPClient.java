package com.zt.acpowerswitch;

import static com.zt.acpowerswitch.MainActivity.udpServerAddress;
import static com.zt.acpowerswitch.MainActivity.udpServerPort;
import static com.zt.acpowerswitch.MainActivity.udp_connect;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UDPClient {
    private static final String TAG = "UDPClient:";
    public static DatagramSocket socket;
    public static boolean rec_fail;
    public void udpConnect() {
        new Thread(() -> {
            // 创建Socket对象，并指定服务器的IP地址和端口号
            try {
                if (!udp_connect ) {
                    socket = new DatagramSocket();
                    udp_connect = true;
                    about.log(TAG, "创建套接字成功");
                }
            } catch (SocketException e) {
                //throw new RuntimeException(e);
                about.log(TAG, "创建套接字失败");
            }
        }).start();
    }
    public void sendMessage(String message){
        new Thread(() -> {
            byte[] data = message.getBytes();
            DatagramPacket packet;
            try {
                packet = new DatagramPacket(data, data.length, InetAddress.getByName(udpServerAddress), udpServerPort);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
            try {
                if (socket != null) {
                    socket.send(packet);
                    socket.setSoTimeout(3000);
                }
            } catch (Exception e) {
                // 这里捕获所有send方法可能抛出的异常
                e.printStackTrace();
            }
        }).start();
    }
    public String receiveMessage(){
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try {
            if (socket != null) {
                socket.receive(packet);
            }
        } catch (Exception e) {
            // 这里捕获所有send方法可能抛出的异常
            //e.printStackTrace();
            Log.i(TAG,"接收超时");
            rec_fail = true;
        }
        return new String(packet.getData(), 0, packet.getLength());
    }
    public void close() {
        socket.close();
        udp_connect = false;
        about.log(TAG, "关闭网络连接");
    }
}