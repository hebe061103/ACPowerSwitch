package com.zt.acpowerswitch;

import static com.zt.acpowerswitch.MainActivity.goAnim;
import static com.zt.acpowerswitch.MainActivity.readDate;
import static com.zt.acpowerswitch.MainActivity.saveData;
import static com.zt.acpowerswitch.MainActivity.udpClient;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class set_tcp_page extends AppCompatActivity {
    //private static final String TAG = "set_tcp_page:";
    public Button bl_ip_get,manual_set,bt_clean;
    public EditText ip_input;
    // 正则表达式用于匹配域名或子域名
    private static final String DOMAIN_PATTERN =
            "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)+([A-Za-z]{2,}|[A-Za-z]{2,}\\.[A-Za-z]{2,})$";

    private static final Pattern pattern = Pattern.compile(DOMAIN_PATTERN);

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.set_tcp_activity);
        bl_ip_get = findViewById(R.id.bl_ip_get);
        manual_set = findViewById(R.id.manual_set);
        bt_clean = findViewById(R.id.bt_clean);
        ip_input = findViewById(R.id.ip_input);
        if (readDate(this,"wifi_ip")!=null && !readDate(this,"wifi_ip").isEmpty()){
            ip_input.setText(readDate(this,"wifi_ip"));
        }
        bl_ip_get.setOnClickListener((View view) -> {
            goAnim(set_tcp_page.this,50);
            if (readDate(this,"wifi_ip")==null) {
                Intent intent = new Intent(set_tcp_page.this, BleClientActivity.class);
                startActivities(new Intent[]{intent});
            }else{
                Toast.makeText(this, "请先点击下方重置按钮,然后再次尝试重新配网", Toast.LENGTH_SHORT).show();
            }
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
                        udpClient.close();
                        Message message = new Message();
                        message.what = 1;
                        myHandler.sendMessage(message);
                    })
                    .show();
        });
        manual_set.setOnClickListener(view -> {
            // 执行一些后台工作
            goAnim(this, 50);
            if (!ip_input.getText().toString().isEmpty()) {
                if (isValidIPv4(ip_input.getText().toString())||isValidDomain(ip_input.getText().toString())) {
                    saveData("wifi_ip", ip_input.getText().toString());
                } else {
                    Toast.makeText(this, "请输入正确的IP地址或域名", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "地址为空......", Toast.LENGTH_SHORT).show();
            }
        });
    }
    Handler myHandler = new Handler(Looper.getMainLooper()) {
        @SuppressLint("SetTextI18n")
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                ip_input.setText("输入域名或IP地址");
            }
            if (msg.what == 2) {
                ip_input.setText(readDate(set_tcp_page.this,"wifi_ip"));
            }
        }
    };
    // 检查字符串是否为有效的 IPv4 地址
    public static boolean isValidIPv4(String ip) {
        String ipv4Pattern = "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        Pattern pattern = Pattern.compile(ipv4Pattern);
        Matcher matcher = pattern.matcher(ip);
        return matcher.matches();
    }
    public static boolean isValidDomain(String domain) {
        if (domain == null || domain.isEmpty()) {
            return false;
        }
        Matcher matcher = pattern.matcher(domain);
        return matcher.matches();
    }
    @Override
    public void onBackPressed() {
        // 默认返回键行为
        super.onBackPressed(); // 或者你可以自定义返回逻辑
    }
    protected void onResume() {
        super.onResume();
        if (readDate(this,"wifi_ip")!=null && !readDate(this,"wifi_ip").isEmpty()){
            Message message = new Message();
            message.what = 2;
            myHandler.sendMessage(message);
        }
    }
}
