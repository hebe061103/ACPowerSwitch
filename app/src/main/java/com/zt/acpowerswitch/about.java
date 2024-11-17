package com.zt.acpowerswitch;

import static com.zt.acpowerswitch.MainActivity.goAnim;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;


public class about extends AppCompatActivity {
    public  TextView about_tx;
    @SuppressLint("StaticFieldLeak")
    public static TextView run_log;
    private static int i;
    private static final ArrayList<String> logList = new ArrayList<>();
    protected static ArrayAdapter<String> adapter;
    @SuppressLint("StaticFieldLeak")
    public static ListView listView;
    public static Boolean stop_refresh=false;
    @SuppressLint("SetTextI18n")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        TextView mBlueMessage = findViewById(R.id.blue_info);
        about_tx = findViewById(R.id.about);
        listView = findViewById(R.id.log_list_view);
        run_log = findViewById(R.id.run_log);
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
                goAnim(this,50);
                if(i==6){
                    i=0;
                    Intent intent = new Intent(about.this, Engineering.class);
                    startActivities(new Intent[]{intent});
                }
            });
            run_log.setOnLongClickListener(view -> {
                goAnim(about.this, 50);
                logList.clear();
                Message msg = new Message();
                msg.what = 1;
                gHandler.sendMessage(msg);
                return false;
            });
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // 当滚动状态变为停止时，检查是否已到达底部
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    // 已到达底部
                    if (view.getLastVisiblePosition() >= (view.getCount() - 1)) {
                        // 设置ListView不可滚动
                        Message msg = new Message();
                        msg.what = 3;
                        gHandler.sendMessage(msg);
                    }else{
                        Message msg = new Message();
                        msg.what = 2;
                        gHandler.sendMessage(msg);
                    }
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                // 这里可以放置其他的滚动响应逻辑
            }
        });
    }
    public static void log(String tag, String m) {
        Log.e(tag, m);
        if (logList.size() < 1000) {
            logList.add(tag + m);
            if (!stop_refresh) {
                if (adapter != null) {
                    Message msg = new Message();
                    msg.what = 1;
                    gHandler.sendMessage(msg);
                }
            }
        } else logList.clear();
    }
    @SuppressLint("HandlerLeak")
    public static Handler gHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                listView.setAdapter(adapter);
                listView.setSelection(listView.getCount());
                adapter.notifyDataSetChanged();
            }
            if (msg.what == 2) {
                run_log.setText("暂停刷新:");
                stop_refresh=true;
            }
            if (msg.what == 3) {
                run_log.setText("运行日志:");
                stop_refresh=false;
            }
        }
    };
    protected void onPause() {
        super.onPause();
    }
    protected void onDestroy() {
        super.onDestroy();
        stop_refresh=false;
    }
}
