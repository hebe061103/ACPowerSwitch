package com.zt.acpowerswitch;

import static com.zt.acpowerswitch.BleClientActivity.write_data_ble;
import static com.zt.acpowerswitch.MainActivity.goAnim;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class WifiListActivity extends AppCompatActivity {
    public static String TAG = "WifiListActivity";
    public static List<String> wifilist = new ArrayList<>();
    private RecyclerView mRecyclerViewList;
    public  wifiListAdapter mRecycler;
    public static ProgressDialog pd;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_list_activity);
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
        display_wifiList();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        List<android.net.wifi.ScanResult> scanResults = wifiManager.getScanResults();
        for (ScanResult scanResult : scanResults) {
            String ssid = scanResult.SSID;
            String bssid = scanResult.BSSID;
            if (!ssid.isEmpty()) {
                Log.e(TAG, "WIFI信息:" + ssid + "MAC:" + bssid);
                // 其他信息，如BSSID、capabilities等
                wifilist.add(ssid);
                Message message = new Message();
                message.what = 1;
                myHandler.sendMessage(message);
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
    @SuppressLint("HandlerLeak")
    Handler myHandler = new Handler() {
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
                                    Log.e(TAG, "发送数据:" + ble_data);
                                    send_data(ble_data);
                                }
                            })
                            .show();

                });
            }
        }
    };

    private void send_data(String data) {
        pd = new ProgressDialog(this);
        pd.setMessage("正在设置WIFI,请稍等......");
        pd.show();
        pd.setCancelable(false);
        Thread thread = new Thread(() -> {
            int readLength = 10; // 设置每次读取的字符数量
            int stringLength = data.length(); // 获取字符串的总长度
            Log.e(TAG, "发送字符的总长度:" + stringLength);
            write_data_ble("len:"+ stringLength);
            sleep(500);
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
                sleep(500);
            }
            write_data_ble("/");
            Log.e(TAG, "分包发送完成");
        });
        thread.start();
    }

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
        if (wifilist != null) {
            wifilist.clear();
        }
        super.onDestroy();
    }
    @Override
    protected void onPause() {
        super.onPause();
    }
}
