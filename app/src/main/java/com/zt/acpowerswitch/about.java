package com.zt.acpowerswitch;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;


public class about extends AppCompatActivity {
    private TextView about_tx,clean_log;
    private static int i;
    private static final ArrayList<String> logList = new ArrayList<>();
    protected static ArrayAdapter<String> adapter;
    @SuppressLint("StaticFieldLeak")
    public static ListView listView;
    @SuppressLint("SetTextI18n")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        TextView mBlueMessage = findViewById(R.id.blue_info);
        about_tx = findViewById(R.id.about);
        listView = findViewById(R.id.log_list_view);
        clean_log = findViewById(R.id.clean_log);
        mBlueMessage.setText(getBluetoothMAC(this));
        clickEvent();
        adapter = new ArrayAdapter<>(about.this, R.layout.list_item, logList);
        listView.setAdapter(adapter);
    }

    @SuppressLint("HardwareIds")
    @SuppressWarnings("MissingPermission")
    public static String getBluetoothMAC(Context context) {
        String info = null;
        try {
            if (context.checkCallingOrSelfPermission(android.Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) {
                BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
                info = "name:"+ bta.getName()+"\n"+"address:"+bta.getAddress();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return info ;
    }
    private void clickEvent() {
            about_tx.setOnClickListener(view -> {
                i++;
                if(i==6){
                    i=0;
                    Intent intent = new Intent(about.this, Engineering.class);
                    startActivities(new Intent[]{intent});
                }
            });
            clean_log.setOnClickListener(view -> logList.clear());
    }
    public static void log(String tag, String m) {
        if(logList.size()<1000) {
            logList.add(tag + m);
            if (adapter!=null){
                Message msg = new Message();
                msg.what = 1;
                gHandler.sendMessage(msg);
            }
        }else logList.clear();
    }
    @SuppressLint("HandlerLeak")
    public static Handler gHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                listView.setAdapter(adapter);
                listView.setSelection(listView.getCount() - 1);
            }
        }
    };
    protected void onDestroy() {
        super.onDestroy();
    }
}
