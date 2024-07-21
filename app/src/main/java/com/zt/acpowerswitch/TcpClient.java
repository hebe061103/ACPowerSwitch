package com.zt.acpowerswitch;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;

public class TcpClient {

    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;

    public void connect(String host, int port) {
        try {
            InetAddress serverAddr = InetAddress.getByName(host);

            // 创建Socket对象并连接到ESP32
            socket = new Socket(serverAddr, port);

            // 创建IO流
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendData(String message) {
        try {
            out.write(message);
            out.newLine();
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String receiveData() {
        String data = null;
        try {
            data = in.readLine();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    public void close() {
        try {
            if (socket != null) {
                socket.close();
            }
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static boolean isIpAvailable(String ipAddress) {
        try {
            // 使用Runtime执行ping命令
            Runtime runtime = Runtime.getRuntime();
            Process ipProcess = runtime.exec("/system/bin/ping -c 1 " + ipAddress);
            int exitValue = ipProcess.waitFor();

            // 检查exitValue来判断ping是否成功
            return (exitValue == 0);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }
}