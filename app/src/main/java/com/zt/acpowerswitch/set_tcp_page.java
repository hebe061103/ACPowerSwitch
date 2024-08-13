package com.zt.acpowerswitch;

import static com.zt.acpowerswitch.MainActivity.goAnim;
import static com.zt.acpowerswitch.MainActivity.readDate;
import static com.zt.acpowerswitch.MainActivity.saveData;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class set_tcp_page extends AppCompatActivity {
    public static String TAG = "set_tcp_page:";
    public Button bl_ip_get,manual_set,bt_clean;
    public EditText ip_input;
    public TextView get_ip_from_bl;
    private Handler handler;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.set_tcp_activity);
        handler = new Handler(Looper.getMainLooper());
        bl_ip_get = findViewById(R.id.bl_ip_get);
        manual_set = findViewById(R.id.manual_set);
        bt_clean = findViewById(R.id.bt_clean);
        ip_input = findViewById(R.id.ip_input);
        get_ip_from_bl = findViewById(R.id.get_ip_from_bl);

        if (readDate(this,"wifi_ip")!=null){
            get_ip_from_bl.setText(readDate(this,"wifi_ip"));
        }
        bl_ip_get.setOnClickListener(view -> {
            goAnim(set_tcp_page.this,50);
            Intent intent = new Intent(set_tcp_page.this, BleClientActivity.class);
            startActivities(new Intent[]{intent});
        });
        bt_clean.setOnClickListener(view -> {
            goAnim(set_tcp_page.this,50);
            new AlertDialog.Builder(set_tcp_page.this)
                    .setTitle("提 示")
                    .setMessage("该操作会清空数据，将无法连接到指定设备")
                    .setPositiveButton("取消", null)
                    .setNegativeButton("确定", (dialog, which) -> {
                        goAnim(set_tcp_page.this,50);
                        MainActivity.deleteData("wifi_ip");
                    })
                    .show();
        });
        manual_set.setOnClickListener(view -> new Thread(() -> {
            // 执行一些后台工作
            goAnim(set_tcp_page.this, 50);
            if (!ip_input.getText().toString().isEmpty()) {
                saveData("wifi_ip", ip_input.getText().toString());
                about.log(TAG, "IP巳保存,请返回主页");
                Looper.prepare();
                Toast.makeText(set_tcp_page.this, "巳保存,请返回主页", Toast.LENGTH_SHORT).show();
                Looper.loop();
            } else {
                Looper.prepare();
                Toast.makeText(set_tcp_page.this, "请输入一个正确IP地址", Toast.LENGTH_SHORT).show();
                Looper.loop();
            }
            // 更新UI
            handler.post(() -> {
                //在这里执行要刷新的操作
                if (readDate(set_tcp_page.this, "wifi_ip") != null) {
                    ip_input.setText(readDate(set_tcp_page.this, "wifi_ip"));
                }
            });
        }).start());
    }
    @Override
    public void onBackPressed() {
        // 默认返回键行为
        super.onBackPressed(); // 或者你可以自定义返回逻辑
    }
}
