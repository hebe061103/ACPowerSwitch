package com.zt.acpowerswitch;

import static com.zt.acpowerswitch.MainActivity.goAnim;
import static com.zt.acpowerswitch.MainActivity.readDate;
import static com.zt.acpowerswitch.MainActivity.saveData;
import static com.zt.acpowerswitch.MainActivity.udpClient;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class set_tcp_page extends AppCompatActivity {
    private static final String TAG = "set_tmp_page:";
    public Button bl_ip_get,manual_set,bt_clean;
    public EditText ip_input;
    private Handler handler;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.set_tcp_activity);
        handler = new Handler(Looper.getMainLooper());
        bl_ip_get = findViewById(R.id.bl_ip_get);
        manual_set = findViewById(R.id.manual_set);
        bt_clean = findViewById(R.id.bt_clean);
        ip_input = findViewById(R.id.ip_input);
        bl_ip_get.setOnClickListener((View view) -> {
            goAnim(set_tcp_page.this,50);
            Intent intent = new Intent(set_tcp_page.this, BleClientActivity.class);
            startActivities(new Intent[]{intent});
        });
        bt_clean.setOnClickListener(view -> {
            goAnim(this,50);
            new AlertDialog.Builder(this)
                    .setTitle("提 示")
                    .setMessage("该操作会清空巳保存的连接信息，将无法连接到指定设备")
                    .setPositiveButton("取消", null)
                    .setNegativeButton("确定", (dialog, which) -> {
                        goAnim(this,50);
                        MainActivity.deleteData("wifi_ip");
                        delete_history_data(MainActivity.bat_value_data);
                        delete_history_data(MainActivity.D_Total_power);
                        delete_history_data(MainActivity.M_Total_power);
                        delete_history_data(MainActivity.Y_Total_power);
                        udpClient.close();
                    })
                    .show();
        });
        manual_set.setOnClickListener(view -> new Thread(() -> {
            // 执行一些后台工作
            goAnim(this, 50);
            if (!ip_input.getText().toString().isEmpty()) {
                saveData("wifi_ip", ip_input.getText().toString());
            } else {
                saveData("wifi_ip", ip_input.getHint().toString());
            }
            // 更新UI
            handler.post(() -> {
                //在这里执行要刷新的操作
                if (readDate(this, "wifi_ip") != null) {
                    ip_input.setText(readDate(this, "wifi_ip"));
                }
            });
        }).start());
    }
    public void delete_history_data(String filename){
        File file = new File(getFilesDir(), filename);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                about.log(TAG, "删除成功");
            } else {
                about.log(TAG, "删除失败");
            }
        }else {
            about.log(TAG, "文件不存在");
        }
    }
    @Override
    public void onBackPressed() {
        // 默认返回键行为
        super.onBackPressed(); // 或者你可以自定义返回逻辑
    }
}
