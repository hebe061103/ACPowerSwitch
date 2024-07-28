package com.zt.acpowerswitch;

import static com.zt.acpowerswitch.MainActivity.connect_udp;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UDPClient {
    public static String TAG = "UDPClient:";
    public static DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort;
    public void udpConnect(String address,int port) {
        if(!connect_udp) {
            new Thread(() -> {
                // 创建Socket对象，并指定服务器的IP地址和端口号
                try {
                    try {
                        Log.e(TAG, "连接服务器......");
                        about.log(TAG, "连接服务器......");
                        this.serverAddress = InetAddress.getByName(address);
                    } catch (UnknownHostException e) {
                        //throw new RuntimeException(e);
                    }
                    this.serverPort = port;
                    socket = new DatagramSocket();
                    connect_udp = true;
                } catch (SocketException e) {
                    //throw new RuntimeException(e);
                }
            }).start();
        }
    }

    public void sendMessage(String message){
        byte[] data = message.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
        try {
            socket.send(packet);
        } catch (IOException e) {
            //throw new RuntimeException(e);
        }
    }
    public String receiveMessage(){
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try {
            socket.receive(packet);
        } catch (IOException e) {
            //throw new RuntimeException(e);
        }
        return new String(packet.getData(), 0, packet.getLength());
    }
    public void close() {
        socket.close();
        connect_udp=false;
    }
}