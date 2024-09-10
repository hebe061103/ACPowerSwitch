package com.zt.acpowerswitch;

import static com.zt.acpowerswitch.BleClientActivity.chara;
import static com.zt.acpowerswitch.BleClientActivity.connect_ok;
import static com.zt.acpowerswitch.BleClientActivity.write_data_ble;
import static com.zt.acpowerswitch.MainActivity.goAnim;
import static com.zt.acpowerswitch.MainActivity.saveData;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class WifiListActivity extends AppCompatActivity {
    private static final String TAG = "WifiListActivity:";
    public static List<String> wifilist = new ArrayList<>();
    public static AlertDialog.Builder builder;
    private RecyclerView mRecyclerViewList;
    public  wifiListAdapter mRecycler;
    public String wifi_ap_name,IP_address,PORT;
    public ProgressDialog pd;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_list_activity);
        pd = new ProgressDialog(WifiListActivity.this);
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        builder = new AlertDialog.Builder(WifiListActivity.this);
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
        display_wifiList();
        @SuppressLint("MissingPermission") List<android.net.wifi.ScanResult> scanResults = wifiManager.getScanResults();
        for (ScanResult scanResult : scanResults) {
            String ssid = scanResult.SSID;
            String bssid = scanResult.BSSID;
            if (!ssid.isEmpty()) {
                about.log(TAG, "WIFI信息:" + ssid + "MAC:" + bssid);
                // 其他信息，如BSSID、capabilities等
                if (!wifilist.contains(ssid)) {
                    wifilist.add(ssid);
                    Message message = new Message();
                    message.what = 1;
                    myHandler.sendMessage(message);
                }
            }
        }
    }
    private void display_wifiList() {
        mRecyclerViewList = findViewById(R.id.wifi_dev_list);//设置固定大小
        mRecyclerViewList.setHasFixedSize(true);//创建线性布局
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        mRecyclerViewList.addItemDecoration(new LinearSpacingItemDecoration(8));//添加间距
        mRecyclerViewList.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL)); //添加分隔线
        mRecyclerViewList.setLayoutManager(layoutManager);
    }
    private void send_data(String data) {
        pd.setMessage("正在设置WIFI,请稍等......");
        pd.show();
        pd.setCancelable(false);
        Thread thread = new Thread(() -> {
            int readLength = 10; // 设置每次读取的字符数量
            int stringLength = data.length(); // 获取字符串的总长度
            about.log(TAG, "发送字符的总长度:" + stringLength);
            write_data_ble("len:"+ stringLength);
            sleep(2000);
            for (int i = 0; i < stringLength; i += readLength) {
                // 计算还剩多少字符可以读取
                int remaining = stringLength - i;
                // 如果剩余字符数少于readLength，则本次读取应该少于或等于剩余的字符数
                if (remaining < readLength) {
                    readLength = remaining;
                }
                // 使用substring方法读取字符串
                String readString = data.substring(i, i + readLength);
                write_data_ble(readString);
                sleep(1000);
            }
            write_data_ble("&");
            about.log(TAG, "分包发送完成");
            sleep(1000);
            wait_callback(); //刷新连接状态
        });
        thread.start();
    }
    public void wait_callback(){
        Thread thread = new Thread(() -> {
            while(true) {
                if (!connect_ok) {
                    if (pd !=null) {
                        pd.dismiss();
                    }
                }
                if (chara != null && chara.contains("rec_ok")) {
                    about.log(TAG, "接收成功");
                    Message message = new Message();
                    message.what = 2;
                    myHandler.sendMessage(message);
                    chara = "";
                }else if (chara != null && chara.contains("rec_error")) {
                    about.log(TAG, "接收失败");
                    Message message = new Message();
                    message.what = 3;
                    myHandler.sendMessage(message);
                    chara = "";
                }else if (chara != null && chara.contains("pass_err")) {
                    about.log(TAG, "密码错误");
                    Message message = new Message();
                    message.what = 4;
                    myHandler.sendMessage(message);
                    chara = "";
                }else if (chara != null && chara.contains("IP:")) {
                    about.log(TAG, "接收IP成功");
                    IP_address = chara;
                    Message message = new Message();
                    message.what = 5;
                    myHandler.sendMessage(message);
                    chara = "";
                }else if (chara != null && chara.contains("PORT:")) {
                    about.log(TAG, "接收PORT成功");
                    PORT = chara;
                    Message message = new Message();
                    message.what = 6;
                    myHandler.sendMessage(message);
                    chara = "";
                    break;
                }
            }
        });
        thread.start();
    }
    @SuppressLint("HandlerLeak")
    Handler myHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                mRecycler = new wifiListAdapter(wifilist, WifiListActivity.this);
                mRecyclerViewList.setAdapter(mRecycler);
                mRecycler.setRecyclerItemClickListener(position -> {
                    goAnim(WifiListActivity.this,50);
                    EditText editText = new EditText(WifiListActivity.this);
                    new AlertDialog.Builder(WifiListActivity.this)
                            .setTitle("请输入密码")
                            .setMessage(wifilist.get(position))
                            .setView(editText)
                            .setPositiveButton("取消", null)
                            .setNegativeButton("确定", (dialog, which) -> {
                                if (!editText.getText().toString().isEmpty()) {
                                    String inputText = editText.getText().toString();
                                    String ble_data = "{" + "\"" + "ssid" + "\"" + ":" + "\"" + wifilist.get(position) + "\"" + "," + "\"" + "password" + "\"" + ":" + "\"" + inputText + "\"" + "}";
                                    about.log(TAG, "发送数据:" + ble_data);
                                    wifi_ap_name=wifilist.get(position);
                                    send_data(ble_data);
                                }
                            })
                            .show();

                });
            }
            if (msg.what == 2) {
                pd.dismiss();
                pd.setMessage("设置成功，正在重启模块，请稍后......");
                pd.show();
                pd.setCancelable(false);
            }
            if (msg.what == 3) {
                pd.dismiss();
                builder.setTitle("提醒"); // 设置弹窗的标题
                builder.setMessage("设置失败，请重新设置"); // 设置弹窗的消息内容
                builder.show();

            }
            if (msg.what == 4) {
                pd.dismiss();
                builder.setTitle("提醒"); // 设置弹窗的标题
                builder.setMessage("密码错误，请重新设置"); // 设置弹窗的消息内容
                builder.show();
            }
            if (msg.what == 5) {
                if (IP_address != null && IP_address.contains("IP:")) {
                    about.log(TAG, "蓝牙返回的IP:" + IP_address);
                    String[] parts = IP_address.split(":");
                    saveData("wifi_ip",parts[1].trim());
                    about.log(TAG, "IP保存成功");
                    IP_address="";
                }
            }
            if (msg.what == 6) {
                pd.dismiss();
                builder.setTitle("提醒"); // 设置弹窗的标题
                builder.setMessage("OK,成功连接到热点:"+ wifi_ap_name); // 设置弹窗的消息内容
                builder.show();
                if (PORT != null && PORT.contains("PORT:")){
                    about.log(TAG, "蓝牙返回的端口:"+PORT);
                    String[] parts = PORT.split(":");
                    saveData("port",parts[1].trim());
                    about.log(TAG, "端口保存成功");
                    PORT="";
                }
            }
        }
    };
    public void sleep(int s){
        try {
            Thread.sleep(s);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    @Override
    protected void onPause() {
        super.onPause();
    }
}
