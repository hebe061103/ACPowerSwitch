package com.zt.acpowerswitch;

import static android.widget.Toast.LENGTH_SHORT;
import static com.zt.acpowerswitch.MainActivity.deleteData;
import static com.zt.acpowerswitch.MainActivity.goAnim;
import static com.zt.acpowerswitch.MainActivity.page_refresh_time;
import static com.zt.acpowerswitch.MainActivity.readDate;
import static com.zt.acpowerswitch.MainActivity.saveData;
import static com.zt.acpowerswitch.MainActivity.send_command_to_server;
import static com.zt.acpowerswitch.MainActivity.tcpClient;
import static com.zt.acpowerswitch.MainActivity.tcpServerPort;
import static com.zt.acpowerswitch.MainActivity.unicodeToString;
import static com.zt.acpowerswitch.set_tcp_page.isValidDomain;
import static com.zt.acpowerswitch.set_tcp_page.isValidIPv4;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Looper;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class otherOption extends AppCompatActivity {
    private static final String TAG = "otherOption:";
    public String _tmp;
    private volatile boolean mShouldCheckMode = true;
    private TextView target_ip,target_port,w_edit,open_pv_value,low_voltage_set,mos_trigger_value,refresh_time_set,auto_mode,power_grid_mode,pv_mode;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.other_activity);
        str_pro();
        new Thread(() -> {
            while (mShouldCheckMode) {
                if (readDate(otherOption.this,"out_mode")!=null && !readDate(otherOption.this, "out_mode").equals(_tmp)) {
                    _tmp = readDate(otherOption.this, "out_mode");
                    runOnUiThread(this::out_mode_display);
                }
                try {
                    Thread.sleep(500); // 每次循环间隔 500ms
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    public void str_pro() {
        //逆变器IP设置
        target_ip = findViewById(R.id.target_ip);
        if (readDate(otherOption.this, "wifi_ip") != null) {
            target_ip.setText(readDate(otherOption.this, "wifi_ip"));
        }
        target_ip.setOnClickListener(view -> send_arg_server("逆变器IP设置"));
        //逆变器端口设置
        target_port = findViewById(R.id.target_port);
        if (readDate(otherOption.this, "tcpServerPort") != null) {
            target_port.setText(readDate(otherOption.this, "tcpServerPort"));
        }
        target_port.setOnClickListener(view -> send_arg_server("逆变器端口设置"));
        //功率设置
        w_edit = findViewById(R.id.w_edit);
        if (readDate(otherOption.this, "power") != null) {
            w_edit.setText(readDate(otherOption.this, "power"));
        }
        w_edit.setOnClickListener(view -> send_arg_server("功率参数设置"));
        //开启逆变阈值
        open_pv_value = findViewById(R.id.open_pv_value);
        if (readDate(otherOption.this, "open_pv_value") != null) {
            open_pv_value.setText(readDate(otherOption.this, "open_pv_value"));
        }
        open_pv_value.setOnClickListener(view -> send_arg_server("开启逆变阈值"));
        //最低电压值设置
        low_voltage_set = findViewById(R.id.low_voltage_set);
        if (readDate(otherOption.this, "low_voltage") != null) {
            low_voltage_set.setText(readDate(otherOption.this, "low_voltage"));
        }
        low_voltage_set.setOnClickListener(view -> send_arg_server("最低电压值"));
        //MOS风扇温度触发值设置
        mos_trigger_value = findViewById(R.id.mos_trigger_value);
        if (readDate(otherOption.this, "mos_temp") != null) {
            mos_trigger_value.setText(readDate(otherOption.this, "mos_temp"));
        }
        mos_trigger_value.setOnClickListener(view -> send_arg_server("MOS温度触发值"));
        //刷新时间设置
        refresh_time_set = findViewById(R.id.refresh_time_set);
        if (readDate(otherOption.this, "refresh_time") != null) {
            refresh_time_set.setText(readDate(otherOption.this, "refresh_time"));
        }
        refresh_time_set.setOnClickListener(view -> send_arg_server("页面刷新时间设置"));
        //输出模式
        auto_mode = findViewById(R.id.auto_mode);
        power_grid_mode = findViewById(R.id.power_grid_mode);
        pv_mode = findViewById(R.id.pv_mode);
        //输出模式显示,字体背景加颜色
        out_mode_display();
        //输出模式按钮监听
        auto_mode.setOnClickListener(view -> {
            goAnim(otherOption.this, 50);
            if (send_command_to_server("power_out_mode:自动模式")){
                auto_mode.setBackgroundColor(Color.parseColor("#673AB7"));
                power_grid_mode.setBackgroundColor(Color.TRANSPARENT);
                pv_mode.setBackgroundColor(Color.TRANSPARENT);
                saveData("out_mode", "自动模式");
            }
        });
        power_grid_mode.setOnClickListener(view -> {
            goAnim(otherOption.this, 50);
            if (send_command_to_server("power_out_mode:市电模式")){
                power_grid_mode.setBackgroundColor(Color.parseColor("#673AB7"));
                auto_mode.setBackgroundColor(Color.TRANSPARENT);
                pv_mode.setBackgroundColor(Color.TRANSPARENT);
                saveData("out_mode", "市电模式");
            }
        });
        pv_mode.setOnClickListener(view -> {
            goAnim(otherOption.this, 50);
            if (send_command_to_server("power_out_mode:逆变模式")){
                pv_mode.setBackgroundColor(Color.parseColor("#673AB7"));
                auto_mode.setBackgroundColor(Color.TRANSPARENT);
                power_grid_mode.setBackgroundColor(Color.TRANSPARENT);
                saveData("out_mode", "逆变模式");
            }
        });
        TextView reset_network = findViewById(R.id.reset_network);
        reset_network.setOnClickListener(view -> {
            goAnim(otherOption.this, 50);
            new AlertDialog.Builder(otherOption.this)
                .setTitle("提 示")
                .setMessage("该操作会清空巳保存的本地连接信息!!!")
                .setPositiveButton("取消", null)
                .setNegativeButton("确定", (dialog, which) -> {
                    goAnim(otherOption.this, 50);
                    deleteData("power");
                    deleteData("low_voltage");
                    deleteData("out_mode");
                    deleteData("mos_temp");
                    deleteData("open_pv_value");
                    deleteData("wifi_ip");
                    deleteData("refresh_time");
                    if (!MainActivity.isPaused){MainActivity.isPaused=true;}
                    tcpClient.close();
                    finish();
                })
                .show();
        });
    }

    public void out_mode_display() {
        if (readDate(otherOption.this, "out_mode") != null && unicodeToString(readDate(otherOption.this, "out_mode")).equals("自动模式")) {
            auto_mode.setBackgroundColor(Color.parseColor("#673AB7"));
            power_grid_mode.setBackground(null);
            pv_mode.setBackground(null);
        }
        if (readDate(otherOption.this, "out_mode") != null && unicodeToString(readDate(otherOption.this, "out_mode")).equals("市电模式")) {
            power_grid_mode.setBackgroundColor(Color.parseColor("#673AB7"));
            auto_mode.setBackground(null);
            pv_mode.setBackground(null);
        }
        if (readDate(otherOption.this, "out_mode") != null && unicodeToString(readDate(otherOption.this, "out_mode")).equals("逆变模式")) {
            pv_mode.setBackgroundColor(Color.parseColor("#673AB7"));
            auto_mode.setBackground(null);
            power_grid_mode.setBackground(null);
        }
    }

    public void send_arg_server(String msg){
        goAnim(otherOption.this, 50);
        EditText editText = new EditText(this);
        new AlertDialog.Builder(otherOption.this)
            .setTitle("提 示")
            .setMessage(msg)
            .setView(editText)
            .setPositiveButton("取消", null)
            .setNegativeButton("确定", (dialog, which) -> {
                goAnim(otherOption.this, 50);
                switch (msg) {
                    case "功率参数设置":
                        if (!editText.getText().toString().isEmpty()) {
                            w_edit.setText(editText.getText());
                            send_w_edit();
                        }
                        break;
                    case "开启逆变阈值":
                        if (!editText.getText().toString().isEmpty()) {
                            open_pv_value.setText(editText.getText());
                            send_pv_value_edit();
                        }
                        break;
                    case "最低电压值":
                        if (!editText.getText().toString().isEmpty()) {
                            low_voltage_set.setText(editText.getText());
                            lo_voltage_set();
                        }
                        break;
                    case "页面刷新时间设置":
                        if (!editText.getText().toString().isEmpty()) {
                            refresh_time_set.setText(editText.getText());
                            refresh_time_set();
                        }
                        break;
                    case "MOS温度触发值":
                        if (!editText.getText().toString().isEmpty()) {
                            mos_trigger_value.setText(editText.getText());
                            mos_trigger_value_set();
                        }
                        break;
                    case "逆变器IP设置":
                        if (!editText.getText().toString().isEmpty()) {
                            target_ip.setText(editText.getText());
                            target_ip_set();
                        }
                        break;
                    case "逆变器端口设置":
                        if (!editText.getText().toString().isEmpty()) {
                            target_port.setText(editText.getText());
                            target_port_set();
                        }
                        break;
                }
            })
            .show();
    }
    public void send_w_edit() {
        new Thread(() -> {
            if (!w_edit.getText().toString().isEmpty() && !w_edit.getText().toString().equals(readDate(otherOption.this, "power"))) {
                about.log(TAG, "功率参数巳改变,发送参数到服务端");
                if (isInteger(w_edit.getText().toString()) || isDecimal(w_edit.getText().toString()) && Float.parseFloat(w_edit.getText().toString()) > 0) {
                        // 切回主线程更新 UI
                        runOnUiThread(() -> {
                        if (send_command_to_server("set_w:" + w_edit.getText().toString())){
                            new AlertDialog.Builder(otherOption.this)
                                    .setTitle("提 示:")
                                    .setMessage("设置成功!")
                                    .setNegativeButton("完成", (dialogInterface13, i13) -> {
                                        goAnim(otherOption.this, 50);
                                        saveData("power", w_edit.getText().toString());
                                    }).show();
                        }else{
                            new AlertDialog.Builder(otherOption.this)
                                    .setTitle("提 示:")
                                    .setMessage("设置失败,请重试!")
                                    .setNegativeButton("完成", (dialogInterface13, i13) -> {
                                        goAnim(otherOption.this, 50);
                                        w_edit.setText(readDate(otherOption.this, "power"));
                                    }).show();
                        }
                    });
                } else {
                    about.log(TAG, "功率设置项请输入整数类型");
                    Toast.makeText(otherOption.this, "功率设置项请输入整数类型", LENGTH_SHORT).show();
                    w_edit.setText(readDate(otherOption.this, "power"));
                }
            }
        }).start();
    }
    public void send_pv_value_edit() {
        new Thread(() -> {
            if (!open_pv_value.getText().toString().isEmpty() && !open_pv_value.getText().toString().equals(readDate(otherOption.this, "open_pv_value"))) {
                about.log(TAG, "开启逆变阈值巳改变,发送参数到服务端");
                if (isInteger(open_pv_value.getText().toString()) || isDecimal(open_pv_value.getText().toString()) && Float.parseFloat(open_pv_value.getText().toString()) > 0) {
                    // 切回主线程更新 UI
                    runOnUiThread(() -> {
                        if (send_command_to_server("set_open_pv_value:" + open_pv_value.getText().toString())){
                            new AlertDialog.Builder(otherOption.this)
                                    .setTitle("提 示:")
                                    .setMessage("设置成功!")
                                    .setNegativeButton("完成", (dialogInterface13, i13) -> {
                                        goAnim(otherOption.this, 50);
                                        saveData("open_pv_value", open_pv_value.getText().toString());
                                    }).show();
                        }else{
                            new AlertDialog.Builder(otherOption.this)
                                    .setTitle("提 示:")
                                    .setMessage("设置失败,请重试!")
                                    .setNegativeButton("完成", (dialogInterface13, i13) -> {
                                        goAnim(otherOption.this, 50);
                                        open_pv_value.setText(readDate(otherOption.this, "open_pv_value"));
                                    }).show();
                        }
                    });
                } else {
                    about.log(TAG, "逆变阈值请输入整数类型");
                    runOnUiThread(() -> Toast.makeText(otherOption.this, "逆变阈值请输入整数类型", Toast.LENGTH_SHORT).show());
                    open_pv_value.setText(readDate(otherOption.this, "open_pv_value"));
                }
            }
        }).start();
    }
    public void lo_voltage_set() {
        new Thread(() -> {
            if (!low_voltage_set.getText().toString().isEmpty() && !low_voltage_set.getText().toString().equals(readDate(otherOption.this, "low_voltage"))) {
                about.log(TAG, "最低电压值巳改变,发送参数到服务端");
                if (isInteger(low_voltage_set.getText().toString()) || isDecimal(low_voltage_set.getText().toString()) && Float.parseFloat(low_voltage_set.getText().toString()) > 0) {
                    runOnUiThread(() -> {
                        if (send_command_to_server("low_voltage:" + low_voltage_set.getText().toString())){
                            new AlertDialog.Builder(otherOption.this)
                                    .setTitle("提 示:")
                                    .setMessage("设置成功!")
                                    .setNegativeButton("完成", (dialogInterface13, i13) -> {
                                        goAnim(otherOption.this, 50);
                                        saveData("low_voltage", low_voltage_set.getText().toString());
                                    }).show();
                        }else{
                            new AlertDialog.Builder(otherOption.this)
                                    .setTitle("提 示:")
                                    .setMessage("设置失败,请重试!")
                                    .setNegativeButton("完成", (dialogInterface13, i13) -> {
                                        goAnim(otherOption.this, 50);
                                        low_voltage_set.setText(readDate(otherOption.this, "low_voltage"));
                                    }).show();
                        }
                    });
                } else {
                    about.log(TAG, "最低电压值项请输入整数或小数类型");
                    Toast.makeText(otherOption.this, "最低电压值项请输入整数或小数类型", LENGTH_SHORT).show();
                    low_voltage_set.setText(readDate(otherOption.this, "low_voltage"));
                }
            }
        }).start();
    }
    public void refresh_time_set(){
        if (!refresh_time_set.getText().toString().isEmpty() && !refresh_time_set.getText().toString().equals(readDate(otherOption.this,"refresh_time"))){
            about.log(TAG,"页面刷新时间巳改变");
            if(isInteger(refresh_time_set.getText().toString())) {
                saveData("refresh_time", refresh_time_set.getText().toString());
                page_refresh_time = Integer.parseInt(refresh_time_set.getText().toString());
            }else {
                about.log(TAG,"页面刷新项请输入整数类型");
                Looper.prepare();
                Toast.makeText(otherOption.this, "页面刷新项请输入整数类型", LENGTH_SHORT).show();
                Looper.loop();
            }
        }
    }
    public void mos_trigger_value_set(){
        new Thread(() -> {
            if (!mos_trigger_value.getText().toString().isEmpty() && !mos_trigger_value.getText().toString().equals(readDate(otherOption.this,"mos_temp"))){
                about.log(TAG,"mos温度触发值巳改变");
                if (isInteger(mos_trigger_value.getText().toString()) || isDecimal(mos_trigger_value.getText().toString()) && Float.parseFloat(mos_trigger_value.getText().toString()) > 0) {
                    runOnUiThread(() -> {
                        if (send_command_to_server("mos_temp:" + mos_trigger_value.getText().toString())){
                            new AlertDialog.Builder(otherOption.this)
                                    .setTitle("提 示:")
                                    .setMessage("设置成功!")
                                    .setNegativeButton("完成", (dialogInterface13, i13) -> {
                                        goAnim(otherOption.this, 50);
                                        saveData("mos_temp", mos_trigger_value.getText().toString());
                                    }).show();
                        }else{
                            new AlertDialog.Builder(otherOption.this)
                                    .setTitle("提 示:")
                                    .setMessage("设置失败,请重试!")
                                    .setNegativeButton("完成", (dialogInterface13, i13) -> {
                                        goAnim(otherOption.this, 50);
                                        mos_trigger_value.setText(readDate(otherOption.this, "mos_temp"));
                                    }).show();
                        }
                    });
                } else {
                    about.log(TAG,"主功率板风扇温度触发值请输入整数或小数类型");
                    Looper.prepare();
                    Toast.makeText(otherOption.this, "主功率板风扇温度触发值请输入整数或小数类型", LENGTH_SHORT).show();
                    Looper.loop();
                }
            }
        }).start();
    }
    public void target_ip_set(){
        if (!target_ip.getText().toString().isEmpty() && !target_ip.getText().toString().equals(readDate(otherOption.this,"wifi_ip"))){
            about.log(TAG,"目标IP巳改变");
            String inputText = target_ip.getText().toString().trim(); // trim() 去除前后空格

            if (inputText.isEmpty()) {
                Toast.makeText(this, "你还没有输入地址或域名", Toast.LENGTH_SHORT).show();
                target_ip.setText(readDate(otherOption.this,"wifi_ip"));
                return; // 提前退出，减少嵌套
            }
            boolean isIpValid = isValidIPv4(inputText) || isValidDomain(inputText);
            if (!isIpValid) {
                Toast.makeText(this, "请输入正确的IP地址或域名", Toast.LENGTH_SHORT).show();
                target_ip.setText(readDate(otherOption.this,"wifi_ip"));
                return;
            }
            target_ip.setText(inputText);
            saveData("wifi_ip", inputText);
            MainActivity.tcpServerAddress = inputText;
        }
    }
    public void target_port_set(){
        if (!target_port.getText().toString().isEmpty() && !target_port.getText().toString().equals(readDate(otherOption.this,"tcpServerPort"))){
            about.log(TAG,"目标端口巳改变");
            try {
                int portNumber = Integer.parseInt(target_port.getText().toString());
                if (portNumber < 0 || portNumber > 65535) {
                    throw new NumberFormatException(); // 手动抛出异常，走到下面的提示
                }else{
                    target_port.setText(target_port.getText().toString());
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "请输入正确的端口号（0-65535之间的数字）", Toast.LENGTH_SHORT).show();
                target_port.setText(readDate(otherOption.this,"tcpServerPort"));
                return;
            }
            saveData("tcpServerPort", target_port.getText().toString());
            tcpServerPort = Integer.parseInt(target_port.getText().toString());
        }
    }
    public boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean isDecimal(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mShouldCheckMode = false; // 退出循环
    }
}
