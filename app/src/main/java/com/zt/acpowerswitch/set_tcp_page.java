package com.zt.acpowerswitch;

import static com.zt.acpowerswitch.MainActivity.goAnim;
import static com.zt.acpowerswitch.MainActivity.readDate;
import static com.zt.acpowerswitch.MainActivity.saveData;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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
    private long lastBack = 0;
    public EditText ip_input;

    @SuppressLint("SetTextI18n")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.set_tcp_activity);
        bl_ip_get = findViewById(R.id.bl_ip_get);
        manual_set = findViewById(R.id.manual_set);
        ip_input = findViewById(R.id.ip_input);
        wf_ip_get = findViewById(R.id.wf_ip_get);

        String wifiIp = readDate(this, "wifi_ip");
        String tcpPort = readDate(this, "tcpServerPort");
        if (!TextUtils.isEmpty(wifiIp) && !TextUtils.isEmpty(tcpPort)) {
            ip_input.setText(wifiIp + ":" + tcpPort);
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
            String inputText = ip_input.getText().toString().trim(); // trim() 去除前后空格

            if (inputText.isEmpty()) {
                Toast.makeText(this, "你还没有输入地址或域名", Toast.LENGTH_SHORT).show();
                return; // 提前退出，减少嵌套
            }

            // 1. 严格校验格式，必须包含冒号且不能在开头或结尾
            if (!inputText.contains(":") || inputText.startsWith(":") || inputText.endsWith(":")) {
                Toast.makeText(this, "格式错误！请输入正确的 'IP/域名:端口'，例如 192.168.1.1:8080", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. 切割字符串
            String[] ip_port = inputText.split(":");

            // 安全防范：确保数组长度恰好为 2
            if (ip_port.length != 2) {
                Toast.makeText(this, "格式错误！只能包含一个冒号", Toast.LENGTH_SHORT).show();
                return;
            }

            String inputIp = ip_port[0].trim();
            String inputPortStr = ip_port[1].trim();

            // 3. 校验 IP 或 域名
            boolean isIpValid = isValidIPv4(inputIp) || isValidDomain(inputIp);
            if (!isIpValid) {
                Toast.makeText(this, "请输入正确的IP地址或域名", Toast.LENGTH_SHORT).show();
                return;
            }

            // 4. 校验端口号（加入 try-catch 防止用户输入非数字导致 Integer.parseInt 崩溃）
            int portNumber;
            try {
                portNumber = Integer.parseInt(inputPortStr);
                if (portNumber < 0 || portNumber > 65535) {
                    throw new NumberFormatException(); // 手动抛出异常，走到下面的提示
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "请输入正确的端口号（0-65535之间的数字）", Toast.LENGTH_SHORT).show();
                return;
            }

            // 5. 校验全部通过，保存数据并跳转（无需再读取本地进行比对，因为上面的数据就是即将保存成功的）
            saveData("wifi_ip", inputIp);
            saveData("tcpServerPort", inputPortStr);

            // 提示用户保存成功并跳转
            Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(set_tcp_page.this, MainActivity.class);
            startActivity(intent); // 注意：启动单个 Activity 用 startActivity 即可，原代码里的 startActivities 有些多余
            finish();

        });
    }
    // 1. 校验 IPv4 格式 (保持不变，你的正则很标准)
    public static boolean isValidIPv4(String ip) {
        if (ip == null || ip.isEmpty()) return false;

        String ipv4Pattern = "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

        Pattern pattern = Pattern.compile(ipv4Pattern);
        Matcher matcher = pattern.matcher(ip);
        return matcher.matches();
    }

    // 2. 校验 域名 格式 (修复了变量未定义的问题，并加入了通用的域名正则)
    public static boolean isValidDomain(String domain) {
        if (domain == null || domain.isEmpty()) {
            return false;
        }

        // 这是一个标准的域名正则表达式，支持类似 h.zjw.cloudns.be 的多级域名
        String domainPattern = "^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,6}$";

        // 💡 注意：这里必须重新 Compile 域名专用的正则
        Pattern domainRegex = Pattern.compile(domainPattern);
        Matcher matcher = domainRegex.matcher(domain);
        return matcher.matches();
    }
    /**
     * 再次返回键退出程序
     */
    @SuppressLint("MissingSuperCall")
    public void onBackPressed() {
        if (lastBack == 0 || System.currentTimeMillis() - lastBack > 2000) {
            Toast.makeText(set_tcp_page.this, "再按一次返回退出", Toast.LENGTH_SHORT).show();
            lastBack = System.currentTimeMillis();
            return;
        }
        finishAffinity();
    }
    protected void onResume() {
        super.onResume();
    }

}
