package com.zt.acpowerswitch;

import static com.zt.acpowerswitch.MainActivity.TAG;
import static com.zt.acpowerswitch.MainActivity.connect_tcp;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TCPClient {
    public static PrintWriter out;
    public static BufferedReader in;
    public static Socket socket = null;
    public void TcpConnect(String address,int port) {
        new Thread(() -> {
            // 创建Socket对象，并指定服务器的IP地址和端口号
            if (!connect_tcp) {
                try {
                    socket = new Socket(address, port);
                    // 获取输入流和输出流
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    out = new PrintWriter(socket.getOutputStream(), true);
                    Log.e(TAG, "获取到输入输出流");
                    connect_tcp = true;
                    Log.e(TAG,"connect_tcp:"+true);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    public String send(String s){
        if(out!=null) {
            out.println(s);
        }
        String response = null;
            if(in!=null) {
                try {
                    if(!socket.isClosed()) {
                        response = in.readLine();
                    }
                } catch (IOException e) {
                    //throw new RuntimeException(e);
                }
            }
        return response;
    }
    public void close_socket(){
        try {
            socket.close();
            in.close();
            out.close();
            connect_tcp=false;
            Log.e(TAG,"connect_tcp:"+false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}