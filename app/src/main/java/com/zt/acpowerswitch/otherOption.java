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
import static com.zt.acpowerswitch.set_tcp_page.isValidDomain;
import static com.zt.acpowerswitch.set_tcp_page.isValidIPv4;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class otherOption extends AppCompatActivity {
    private static final String TAG = "otherOption:";
    public SeekBar seekBar;
    public TextView tvValue;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable saveRunnable;

    private TextView target_ip;
    private TextView target_port;
    private TextView w_edit;
    private TextView open_pv_value;
    private TextView low_voltage_set;
    private TextView mos_trigger_value;
    private TextView refresh_time_set;
    private TextView auto_mode;
    private TextView power_grid_mode;
    private TextView pv_mode;
    private TextView lock_us_diff;
    private TextView system_r;
    private TextView request_calibration;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.other_activity);
        str_pro();
    }

    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    public void str_pro() {
        //校准光耦和电阻的物理硬件延迟误差,硬件补偿值
        seekBar = findViewById(R.id.mySeekBar);
        tvValue = findViewById(R.id.tvSliderValue);
        String saved_hardware_offset_us = readDate(otherOption.this, "hardware_offset_us");
        if (saved_hardware_offset_us != null) {
            int value = (int) Float.parseFloat(saved_hardware_offset_us);
            tvValue.setText("当前微调数值: " + value);
            seekBar.setProgress(2000 + value);
        }
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = progress - 2000;  // ✅ 映射为 -2000 ~ 2000
                tvValue.setText("当前微调数值: " + value);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.i(TAG,"先暂停发送数据");
                MainActivity.stop_send = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                String hardware_offset_us = String.valueOf(seekBar.getProgress() - 2000);
                if (send_command_to_server("lock_us:"+ hardware_offset_us)){
                    MainActivity.saveData("hardware_offset_us", hardware_offset_us);
                }
            }
        });
        TextView btnMinus = findViewById(R.id.btnMinus); //点击"-"
        btnMinus.setOnClickListener(view -> {
            goAnim(otherOption.this, 50);
            int m = seekBar.getProgress() - 2000;
            if (m > -2000) {  // 防止越界
                m--;
            }
            tvValue.setText("当前微调数值: " + m); // 更新文字
            seekBar.setProgress(m + 2000); // 同步更新滑块位置
            // 先取消上一次未执行的任务
            if (saveRunnable != null) {
                handler.removeCallbacks(saveRunnable);
            }
            // 重新定义任务
            int finalM = m;
            saveRunnable = () -> {
                if (send_command_to_server("lock_us:" + finalM)) {
                    MainActivity.saveData("hardware_offset_us", String.valueOf(finalM));
                }
            };
            // 延时3秒没有再次点击则执行
            handler.postDelayed(saveRunnable, 3000);
        });

        TextView btnPlus = findViewById(R.id.btnPlus); // 点击"+"
        btnPlus.setOnClickListener(view -> {
            goAnim(otherOption.this, 50);
            int m = seekBar.getProgress() - 2000;
            if (m < 2000) {  // 防止越界
                m++;
            }
            tvValue.setText("当前微调数值: " + m); // 更新文字
            seekBar.setProgress(m + 2000); // 同步更新滑块位置
            // 先取消上一次未执行的任务
            if (saveRunnable != null) {
                handler.removeCallbacks(saveRunnable);
            }
            // 重新定义任务
            int finalM = m;
            saveRunnable = () -> {
                if (send_command_to_server("lock_us:" + finalM)) {
                    MainActivity.saveData("hardware_offset_us", String.valueOf(finalM));
                }
            };
            // 延时3秒没有再次点击则执行
            handler.postDelayed(saveRunnable, 3000);
        });
        //逆变器IP设置
        target_ip = findViewById(R.id.target_ip);
        String saved_wifi_ip = readDate(otherOption.this, "wifi_ip");
        target_ip.setText(saved_wifi_ip != null ? saved_wifi_ip : "");
        target_ip.setOnClickListener(view -> send_arg_server("请输入远程逆变器域名或IP址址"));
        //逆变器端口设置
        target_port = findViewById(R.id.target_port);
        String saved_tcpServerPort = readDate(otherOption.this, "tcpServerPort");
        target_port.setText(saved_tcpServerPort != null ? saved_tcpServerPort : "");
        target_port.setOnClickListener(view -> send_arg_server("请输入远程逆变器端口(默认值:55555)"));
        //功率设置
        w_edit = findViewById(R.id.w_edit);
        String saved_power = readDate(otherOption.this, "power");
        w_edit.setText(saved_power != null ? saved_power : "");
        w_edit.setOnClickListener(view -> send_arg_server("设置负载最大功率阈值(最大不超过5KW)"));
        //开启逆变阈值
        open_pv_value = findViewById(R.id.open_pv_value);
        String saved_open_pv_value = readDate(otherOption.this, "open_pv_value");
        open_pv_value.setText(saved_open_pv_value != null ? saved_open_pv_value : "");
        open_pv_value.setOnClickListener(view -> send_arg_server("开启逆变阈值(高于此电压则开启逆变,默认值:27.2)"));
        //最低电压值设置
        low_voltage_set = findViewById(R.id.low_voltage_set);
        String saved_low_voltage = readDate(otherOption.this, "low_voltage");
        low_voltage_set.setText(saved_low_voltage != null ? saved_low_voltage : "");
        low_voltage_set.setOnClickListener(view -> send_arg_server("低于此电压则关闭逆变器(截止电压默认值:24)"));
        //MOS风扇温度触发值设置
        mos_trigger_value = findViewById(R.id.mos_trigger_value);
        String saved_mos_temp = readDate(otherOption.this, "mos_temp");
        mos_trigger_value.setText(saved_mos_temp != null ? saved_mos_temp : "");
        mos_trigger_value.setOnClickListener(view -> send_arg_server("主功率板MOS温度风扇触发值(默认值:28度)"));
        //刷新时间设置
        refresh_time_set = findViewById(R.id.refresh_time_set);
        String saved_refresh_time = readDate(otherOption.this, "refresh_time");
        refresh_time_set.setText(saved_refresh_time != null ? saved_refresh_time : "");
        refresh_time_set.setOnClickListener(view -> send_arg_server("获取远程数据的时间间隔(默认值:1000ms)"));
        //极致锁相峰值误差范围
        lock_us_diff = findViewById(R.id.lock_us_diff);
        String saved_lock_us_diff = readDate(otherOption.this, "lock_us_diff");
        lock_us_diff.setText(saved_lock_us_diff != null ? saved_lock_us_diff : "");
        lock_us_diff.setOnClickListener(view -> send_arg_server("设置极致锁相峰值误差范围(默认值:200us)"));
        //系统总内阻
        system_r = findViewById(R.id.system_r);
        String saved_system_r = readDate(otherOption.this, "SYSTEM_R");
        system_r.setText(saved_system_r != null ? saved_system_r : "");
        system_r.setOnClickListener(view -> send_arg_server("设置逆变系统总内阻(默认值:0.0mΩ)"));
        //请求电池校准
        request_calibration = findViewById(R.id.request_calibration);
        refresh_calibration_display();
        request_calibration.setOnClickListener(view -> request_bat_calibration());
        //输出模式
        auto_mode = findViewById(R.id.auto_mode);
        power_grid_mode = findViewById(R.id.power_grid_mode);
        pv_mode = findViewById(R.id.pv_mode);
        //输出模式按钮监听
        auto_mode.setOnClickListener(view -> {
            goAnim(otherOption.this, 50);
            send_command_to_server("power_out_mode:自动模式");
            if (send_command_to_server("power_out_mode:自动模式")){
                auto_mode.setBackgroundColor(Color.parseColor("#673AB7"));
                power_grid_mode.setBackgroundColor(Color.TRANSPARENT);
                pv_mode.setBackgroundColor(Color.TRANSPARENT);
                saveData("work_mode", "自动模式");
            }
        });
        power_grid_mode.setOnClickListener(view -> {
            goAnim(otherOption.this, 50);
            if (send_command_to_server("power_out_mode:市电模式")){
                power_grid_mode.setBackgroundColor(Color.parseColor("#673AB7"));
                auto_mode.setBackgroundColor(Color.TRANSPARENT);
                pv_mode.setBackgroundColor(Color.TRANSPARENT);
                saveData("work_mode", "市电模式");
            }
        });
        pv_mode.setOnClickListener(view -> {
            goAnim(otherOption.this, 50);
            if (send_command_to_server("power_out_mode:逆变模式")){
                pv_mode.setBackgroundColor(Color.parseColor("#673AB7"));
                auto_mode.setBackgroundColor(Color.TRANSPARENT);
                power_grid_mode.setBackgroundColor(Color.TRANSPARENT);
                saveData("work_mode", "逆变模式");
            }
        });
        out_mode_display(); //输出模式显示,字体背景加颜色
        // 重置网络及参数处理
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
                    deleteData("lowvoltage");
                    deleteData("work_mode");
                    deleteData("mos_temp_value");
                    deleteData("on_inv_value");
                    deleteData("hardware_offset_us");
                    deleteData("peakToPeakDiff");
                    deleteData("SYSTEM_R");
                    deleteData("request_calibration");
                    if (!MainActivity.isPaused){MainActivity.isPaused=true;}
                    tcpClient.close();
                    finish();
                })
                .show();
        });
    }
    private void refresh_calibration_display() {
        String saved_request_calibration = readDate(otherOption.this, "request_calibration");
        if (saved_request_calibration != null){
            if (Integer.parseInt(saved_request_calibration) == 1){
                request_calibration.setText("正在校准");
            }else if(Integer.parseInt(saved_request_calibration) == 0){
                request_calibration.setText("执行");
            }else if(Integer.parseInt(saved_request_calibration) == -1){
                request_calibration.setText("等待启动校准");
            }
        }
    }
    private void request_bat_calibration() {
        String text = request_calibration.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }
        if (text.equals("正在校准") || text.equals("等待启动校准")){
            new AlertDialog.Builder(otherOption.this)
                .setTitle("提 示:")
                .setMessage("是否取消校准?")
                .setPositiveButton("取消", null)
                .setNegativeButton("确定", (d, i) -> {
                    goAnim(otherOption.this, 50);
                    if (send_command_to_server("request_calibration:0")){
                        waitForCalibrationValue("0");
                    }
                }).show();
        }else if (text.equals("执行")){
            new AlertDialog.Builder(otherOption.this)
                .setTitle("提 示:")
                .setMessage("是否重新校准电池?")
                .setPositiveButton("取消", null)
                .setNegativeButton("确定", (d, i) -> {
                    goAnim(otherOption.this, 50);
                    if (send_command_to_server("request_calibration:1")){
                        waitForCalibrationValue("1");
                    }
                }).show();
        }
    }

    public void out_mode_display() {
        String saved_work_mode = readDate(otherOption.this, "work_mode");
        if (saved_work_mode != null && saved_work_mode.equals("自动模式")) {
            auto_mode.setBackgroundColor(Color.parseColor("#673AB7"));
            power_grid_mode.setBackground(null);
            pv_mode.setBackground(null);
        }
        if (saved_work_mode != null && saved_work_mode.equals("市电模式")) {
            power_grid_mode.setBackgroundColor(Color.parseColor("#673AB7"));
            auto_mode.setBackground(null);
            pv_mode.setBackground(null);
        }
        if (saved_work_mode != null && saved_work_mode.equals("逆变模式")) {
            pv_mode.setBackgroundColor(Color.parseColor("#673AB7"));
            auto_mode.setBackground(null);
            power_grid_mode.setBackground(null);
        }
    }
    private void waitForCalibrationValue(String expectedValue) {
        final int[] attempts = {0};
        final int MAX_ATTEMPTS = 10;  // 最多等 10 次
        final int INTERVAL_MS = 300;  // 每次间隔 300ms

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                attempts[0]++;
                String currentValue =  readDate(otherOption.this, "request_calibration");  // 读本地存储

                if (expectedValue.equals(currentValue)) {
                    // 服务端已更新
                    refresh_calibration_display();
                } else if (attempts[0] >= MAX_ATTEMPTS) {
                    // 超时，放弃等待
                    refresh_calibration_display();
                } else {
                    // 继续等
                    new Handler(Looper.getMainLooper()).postDelayed(this, INTERVAL_MS);
                }
            }
        }, INTERVAL_MS);
    }
    public void send_arg_server(String msg){
        goAnim(otherOption.this, 50);
        EditText editText = new EditText(this);
        editText.setHint("请输入正确的参数......");
        editText.setHintTextColor(0x80AAAAAA); // 半透明灰色
        // 包一层 LinearLayout 限制宽度
        LinearLayout layout = new LinearLayout(this);
        layout.setPadding(70, 0, 70, 0); // 左右留边距，下划线就短了
        layout.addView(editText, new LinearLayout.LayoutParams(
                1200, // 宽度 px，下划线就这么多长
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        new AlertDialog.Builder(otherOption.this)
            .setTitle("提 示:")
            .setMessage(msg)
            .setView(layout)
            .setPositiveButton("取消", null)
            .setNegativeButton("确定", (dialog, which) -> {
                goAnim(otherOption.this, 50);
                switch (msg) {
                    case "设置负载最大功率阈值(最大不超过5KW)":
                        if (!editText.getText().toString().isEmpty()) {
                            w_edit.setText(editText.getText());
                            send_edit_arg(w_edit,"power","功率参数巳改变,发送参数到服务端",
                                    "set_w:","功率设置项请输入整数或小数类型");
                        }
                        break;
                    case "开启逆变阈值(高于此电压则开启逆变,默认值:27.2)":
                        if (!editText.getText().toString().isEmpty()) {
                            open_pv_value.setText(editText.getText());
                            send_edit_arg(open_pv_value,"open_pv_value","开启逆变阈值巳改变,发送参数到服务端",
                                    "set_open_pv_value:","逆变阈值请输入整数或小数类型");
                        }
                        break;
                    case "低于此电压则关闭逆变器(截止电压默认值:24)":
                        if (!editText.getText().toString().isEmpty()) {
                            low_voltage_set.setText(editText.getText());
                            send_edit_arg(low_voltage_set,"low_voltage","最低电压值巳改变,发送参数到服务端",
                                    "low_voltage:","最低电压值项请输入整数或小数类型");
                        }
                        break;
                    case "设置极致锁相峰值误差范围(默认值:200us)":
                        if (!editText.getText().toString().isEmpty()) {
                            lock_us_diff.setText(editText.getText());
                            send_edit_arg(lock_us_diff,"lock_us_diff","最低极致锁相峰值微秒值改变,发送参数到服务端",
                                    "lock_us_diff:","极致锁相峰值微秒值差请输入整数类型");
                        }
                        break;
                    case "设置逆变系统总内阻(默认值:0.0mΩ)":
                        if (!editText.getText().toString().isEmpty()) {
                            system_r.setText(editText.getText());
                            send_edit_arg(system_r,"SYSTEM_R","系统内阻参数巳改变,发送参数到服务端",
                                    "SYSTEM_R:","系统总内阻请输入整数或小数类型");
                        }
                        break;
                    case "主功率板MOS温度风扇触发值(默认值:28度)":
                        if (!editText.getText().toString().isEmpty()) {
                            mos_trigger_value.setText(editText.getText());
                            send_edit_arg(mos_trigger_value,"mos_temp","mos温度触发值巳改变,发送参数到服务端",
                                    "mos_temp:","主功率板风扇温度触发值请输入整数或小数类型");
                        }
                        break;
                    case "获取远程数据的时间间隔(默认值:1000ms)":
                        if (!editText.getText().toString().isEmpty()) {
                            refresh_time_set.setText(editText.getText());
                            refresh_time_set();
                        }
                        break;
                    case "请输入远程逆变器域名或IP址址":
                        if (!editText.getText().toString().isEmpty()) {
                            target_ip.setText(editText.getText());
                            target_ip_set();
                        }
                        break;
                    case "请输入远程逆变器端口(默认值:55555)":
                        if (!editText.getText().toString().isEmpty()) {
                            target_port.setText(editText.getText());
                            target_port_set();
                        }
                        break;
                }
            })
            .show();
    }
    public void send_edit_arg(TextView textview, String save, String msg, String cmd, String err_msg) {
        new Thread(() -> {
            if (!textview.getText().toString().isEmpty() && !textview.getText().toString().equals(readDate(otherOption.this, save))) {
                about.log(TAG, msg);
                if (isInteger(textview.getText().toString()) || isDecimal(textview.getText().toString()) && Float.parseFloat(textview.getText().toString()) > 0) {
                    // 切回主线程更新 UI
                    runOnUiThread(() -> {
                        if (send_command_to_server(cmd + textview.getText().toString())){
                            new AlertDialog.Builder(otherOption.this)
                                    .setTitle("提 示:")
                                    .setMessage("设置成功!")
                                    .setNegativeButton("完成", (dialogInterface13, i13) -> {
                                        goAnim(otherOption.this, 50);
                                        saveData(save, textview.getText().toString());
                                    }).show();
                        }else{
                            new AlertDialog.Builder(otherOption.this)
                                    .setTitle("提 示:")
                                    .setMessage("设置失败,请重试!")
                                    .setNegativeButton("完成", (dialogInterface13, i13) -> {
                                        goAnim(otherOption.this, 50);
                                        textview.setText(readDate(otherOption.this, save));
                                    }).show();
                        }
                    });
                } else {
                    about.log(TAG, err_msg);
                    runOnUiThread(() -> Toast.makeText(otherOption.this, err_msg, Toast.LENGTH_SHORT).show());
                    textview.setText(readDate(otherOption.this, save));
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
    }
}
