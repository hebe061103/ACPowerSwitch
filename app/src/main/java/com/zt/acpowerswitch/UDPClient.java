package com.zt.acpowerswitch;

import static com.zt.acpowerswitch.MainActivity.udPort;
import static com.zt.acpowerswitch.MainActivity.udpServerAddress;
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
        new Thread(() -> {
            // 创建Socket对象，并指定服务器的IP地址和端口号
            try {
                about.log(TAG, "连接服务器");
                try {
                    this.serverAddress = InetAddress.getByName(address);
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                }
                this.serverPort = port;
                socket = new DatagramSocket();
                socket.connect(serverAddress, serverPort);
                // 连接成功，可以开始发送和接收数据
                about.log(TAG, "连接成功");
            } catch (SocketException e) {
                //throw new RuntimeException(e);
                reconnect();
                about.log(TAG, "连接失败,重新连接");
            }
        }).start();
    }
    private void reconnect() {
        new Thread(() -> {
            while (socket == null || socket.isClosed()) {
                try {
                    Thread.sleep(3000); // 等待一段时间后重连
                    udpConnect(udpServerAddress, udPort);
                } catch (InterruptedException e) {
                    // 处理异常，可能需要记录日志或通知上层
                    about.log(TAG, "重新连接失败");
                }
            }
        }).start();
    }
    public void sendMessage(String message){
        byte[] data = message.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
        try {
            socket.send(packet);
        } catch (IOException e) {
           // throw new RuntimeException(e);
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
    }
}