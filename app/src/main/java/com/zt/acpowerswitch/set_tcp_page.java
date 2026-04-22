package com.zt.acpowerswitch;

import static android.widget.Toast.LENGTH_SHORT;
import static com.zt.acpowerswitch.MainActivity.goAnim;
import static com.zt.acpowerswitch.MainActivity.readDate;
import static com.zt.acpowerswitch.MainActivity.saveData;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class set_tcp_page extends AppCompatActivity {
    //private static final String TAG = "set_tcp_page:";
    public Button bl_ip_get,wf_ip_get,manual_set;
    public long lastBack = 0;
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
        ip_input = findViewById(R.id.ip_input);
        wf_ip_get = findViewById(R.id.wf_ip_get);

        if (readDate(this,"wifi_ip")!=null && !readDate(this,"wifi_ip").isEmpty()){
            ip_input.setText(readDate(this,"wifi_ip"));
        }
        bl_ip_get.setOnClickListener((View view) -> {
            goAnim(set_tcp_page.this,50);
            Intent intent = new Intent(set_tcp_page.this, BleClientActivity.class);
            startActivities(new Intent[]{intent});
        });

        wf_ip_get.setOnClickListener(view -> {
            goAnim(set_tcp_page.this,50);
            Intent intent = new Intent(set_tcp_page.this, WifiListActivity.class);
            intent.putExtra("value", "wf");
            startActivities(new Intent[]{intent});
        });
        manual_set.setOnClickListener(view -> {
            // 执行一些后台工作
            goAnim(this, 50);
            if (!ip_input.getText().toString().isEmpty()) {
                if (isValidIPv4(ip_input.getText().toString())||isValidDomain(ip_input.getText().toString())) {
                    saveData("wifi_ip", ip_input.getText().toString());
                    Intent intent = new Intent(set_tcp_page.this, MainActivity.class);
                    startActivities(new Intent[]{intent});
                    finish();
                } else {
                    Toast.makeText(this, "请输入正确的IP地址或域名", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "你还没有输入地址或域名", Toast.LENGTH_SHORT).show();
            }
        });
    }
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
    /**
     * 再次返回键退出程序
     */
    @Override
    public void onBackPressed() {
        if (lastBack == 0 || System.currentTimeMillis() - lastBack > 2000) {
            Toast.makeText(set_tcp_page.this, "再按一次返回退出", LENGTH_SHORT).show();
            lastBack = System.currentTimeMillis();
            return;
        }
        super.onBackPressed();
    }
    protected void onResume() {
        super.onResume();
    }

}
