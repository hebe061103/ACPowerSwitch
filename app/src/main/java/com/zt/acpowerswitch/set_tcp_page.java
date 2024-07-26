package com.zt.acpowerswitch;

import static com.zt.acpowerswitch.MainActivity.goAnim;
import static com.zt.acpowerswitch.MainActivity.readDate;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class set_tcp_page extends AppCompatActivity {
    public static String TAG = "set_tcp_page";
    public Button bl_ip_get,shoudong_get,bt_clean;
    public EditText ip_input;
    public TextView get_ip_from_bl;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.set_tcp_activity);
        bl_ip_get = findViewById(R.id.bl_ip_get);
        shoudong_get = findViewById(R.id.shoudong_get);
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
        shoudong_get.setOnClickListener(view -> {
            goAnim(set_tcp_page.this,50);
            if (!ip_input.getText().toString().isEmpty()) {
                MainActivity.saveData("wifi_ip", ip_input.getText().toString());
                Toast.makeText(this, "巳保存,请返回主页", Toast.LENGTH_SHORT).show();
            }else {
                Toast.makeText(this, "请输入目标IP", Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    public void onBackPressed() {
        // 默认返回键行为
        super.onBackPressed(); // 或者你可以自定义返回逻辑
    }
}
