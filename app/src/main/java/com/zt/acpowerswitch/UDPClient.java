package com.zt.acpowerswitch;

import static com.zt.acpowerswitch.MainActivity.udPort;
import static com.zt.acpowerswitch.MainActivity.udpServerAddress;
import static com.zt.acpowerswitch.MainActivity.udp_connect;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UDPClient {
    private static final String TAG = "UDPClient:";
    public static DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort;
    public void udpConnect(String address,int port) {
        new Thread(() -> {
            // 创建Socket对象，并指定服务器的IP地址和端口号
            try {
                if (!udp_connect) {
                    about.log(TAG, "连接至服务器");
                    try {
                        this.serverAddress = InetAddress.getByName(address);
                    } catch (UnknownHostException e) {
                        throw new RuntimeException(e);
                    }
                    this.serverPort = port;
                    socket = new DatagramSocket();
                    socket.connect(serverAddress, serverPort);
                    if (socket.isConnected()&&!socket.isClosed()){
                        udp_connect = true;
                        // 连接成功，可以开始发送和接收数据
                        about.log(TAG, "创建套接字成功");
                    }
                }
            } catch (SocketException e) {
                //throw new RuntimeException(e);
                reconnect();
                about.log(TAG, "创建套接字失败");
            }
        }).start();
    }
    private void reconnect() {
        new Thread(() -> {
            try {
                Thread.sleep(3000); // 等待一段时间后重连
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            udpConnect(udpServerAddress, udPort);
        }).start();
    }
    public void sendMessage(String message){
        byte[] data = message.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
        try {
            if (socket != null) {
                socket.send(packet);
            }
        } catch (IOException e) {
            // throw new RuntimeException(e);
        }
    }
    public String receiveMessage(){
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try {
            if (socket != null) {
                socket.receive(packet);
            }
        } catch (IOException e) {
            //throw new RuntimeException(e);
            reconnect();
        }
        return new String(packet.getData(), 0, packet.getLength());
    }
    public void close() {
        socket.close();
        udp_connect=false;
    }
}