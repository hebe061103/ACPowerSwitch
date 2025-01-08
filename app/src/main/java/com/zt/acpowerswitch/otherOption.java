package com.zt.acpowerswitch;

import static android.widget.Toast.LENGTH_SHORT;
import static com.zt.acpowerswitch.MainActivity.deleteData;
import static com.zt.acpowerswitch.MainActivity.goAnim;
import static com.zt.acpowerswitch.MainActivity.page_refresh_time;
import static com.zt.acpowerswitch.MainActivity.readDate;
import static com.zt.acpowerswitch.MainActivity.saveData;
import static com.zt.acpowerswitch.MainActivity.send_command_to_server;
import static com.zt.acpowerswitch.MainActivity.udpClient;
import static com.zt.acpowerswitch.MainActivity.udpServerPort;
import static com.zt.acpowerswitch.MainActivity.unicodeToString;

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
    private TextView w_edit, adc2_edit, adc3_vsens_edit,adc3_vcc_edit,low_voltage_set, refresh_time_set,auto_mode,power_grid_mode,pv_mode;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.other_activity);
        str_pro();
    }

    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    public void str_pro() {
        TextView target_ip = findViewById(R.id.target_ip);
        target_ip.setText(readDate(otherOption.this, "wifi_ip"));
        TextView target_port = findViewById(R.id.target_port);
        target_port.setText(String.valueOf(udpServerPort));
        //功率设置
        w_edit = findViewById(R.id.w_edit);
        if (readDate(otherOption.this, "power") != null) {
            w_edit.setText(readDate(otherOption.this, "power"));
        }
        w_edit.setOnClickListener(view -> send_arg_server("功率参数设置"));
        //ADC2设置
        adc2_edit = findViewById(R.id.adc2_edit);
        if (readDate(otherOption.this, "adc2_offset_value") != null) {
            adc2_edit.setText(readDate(otherOption.this, "adc2_offset_value"));
        }
        adc2_edit.setOnClickListener(view -> send_arg_server("ADC2参数设置"));
        //ADC3 VCC/2设置
        adc3_vcc_edit = findViewById(R.id.adc3_vcc_edit);
        if (readDate(otherOption.this, "adc3_vcc_value") != null) {
            adc3_vcc_edit.setText(readDate(otherOption.this, "adc3_vcc_value"));
        }
        adc3_vcc_edit.setOnClickListener(view -> send_arg_server("ADC3电压值设置"));
        //ADC3系数设置
        adc3_vsens_edit = findViewById(R.id.adc3_vsens_edit);
        if (readDate(otherOption.this, "adc3vsens") != null) {
            adc3_vsens_edit.setText(readDate(otherOption.this, "adc3vsens"));
        }
        adc3_vsens_edit.setOnClickListener(view -> send_arg_server("ADC3系数设置"));
        //最低电压值设置
        low_voltage_set = findViewById(R.id.low_voltage_set);
        if (readDate(otherOption.this, "low_voltage") != null) {
            low_voltage_set.setText(readDate(otherOption.this, "low_voltage"));
        }
        low_voltage_set.setOnClickListener(view -> send_arg_server("最低电压值"));
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
        if (readDate(otherOption.this, "out_mode") != null && unicodeToString(readDate(otherOption.this, "out_mode")).equals("自动模式")) {
            auto_mode.setBackgroundColor(Color.parseColor("#673AB7"));
        }
        if (readDate(otherOption.this, "out_mode") != null && unicodeToString(readDate(otherOption.this, "out_mode")).equals("市电模式")) {
            power_grid_mode.setBackgroundColor(Color.parseColor("#673AB7"));
        }
        if (readDate(otherOption.this, "out_mode") != null && unicodeToString(readDate(otherOption.this, "out_mode")).equals("逆变模式")) {
            pv_mode.setBackgroundColor(Color.parseColor("#673AB7"));
        }
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
                    deleteData("wifi_ip");
                    deleteData("power");
                    deleteData("adc2_offset_value");
                    deleteData("adc3_vcc_value");
                    deleteData("adc3vsens");
                    deleteData("low_voltage");
                    deleteData("refresh_time");
                    deleteData("out_mode");
                    udpClient.close();
                })
                .show();
        });
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
                    case "ADC2参数设置":
                        if (!editText.getText().toString().isEmpty()) {
                            adc2_edit.setText(editText.getText());
                            send_adc2_edit();
                        }
                        break;
                    case "ADC3电压值设置":
                        if (!editText.getText().toString().isEmpty()) {
                            adc3_vcc_edit.setText(editText.getText());
                            send_adc3_vcc_edit();
                        }
                        break;
                    case "ADC3系数设置":
                        if (!editText.getText().toString().isEmpty()) {
                            adc3_vsens_edit.setText(editText.getText());
                            send_adc3_vsens_edit();
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
                }
            })
            .show();
    }
    public void send_w_edit() {
        if (!w_edit.getText().toString().isEmpty() && !w_edit.getText().toString().equals(readDate(otherOption.this, "power"))) {
            about.log(TAG, "功率参数巳改变,发送参数到服务端");
            if (isInteger(w_edit.getText().toString()) || isDecimal(w_edit.getText().toString()) && Float.parseFloat(w_edit.getText().toString()) > 0) {
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
            } else {
                about.log(TAG, "功率设置项请输入数字类型");
                Toast.makeText(otherOption.this, "功率设置项请输入数字类型", LENGTH_SHORT).show();
                w_edit.setText(readDate(otherOption.this, "power"));
            }
        }
    }

    public void send_adc2_edit() {
        if (!adc2_edit.getText().toString().isEmpty() && !adc2_edit.getText().toString().equals(readDate(otherOption.this, "adc2_offset_value"))) {
            about.log(TAG, "ADC2参数巳改变,发送参数到服务端");
            if (isInteger(adc2_edit.getText().toString()) || isDecimal(adc2_edit.getText().toString()) && Float.parseFloat(adc2_edit.getText().toString()) > 0) {
                if (send_command_to_server("adc2_set_offset:" + adc2_edit.getText().toString())){
                    new AlertDialog.Builder(otherOption.this)
                            .setTitle("提 示:")
                            .setMessage("设置成功!")
                            .setNegativeButton("完成", (dialogInterface13, i13) -> {
                                goAnim(otherOption.this, 50);
                                saveData("adc2_offset_value", adc2_edit.getText().toString());
                            }).show();
                }else{
                    new AlertDialog.Builder(otherOption.this)
                            .setTitle("提 示:")
                            .setMessage("设置失败,请重试!")
                            .setNegativeButton("完成", (dialogInterface13, i13) -> {
                                goAnim(otherOption.this, 50);
                                adc2_edit.setText(readDate(otherOption.this, "adc2_offset_value"));
                            }).show();
                }
            } else {
                about.log(TAG, "adc2项请输入数字类型");
                Toast.makeText(otherOption.this, "adc2项请输入数字类型", LENGTH_SHORT).show();
                adc2_edit.setText(readDate(otherOption.this, "adc2_offset_value"));
            }
        }
    }
    public void send_adc3_vcc_edit() {
        if (!adc3_vcc_edit.getText().toString().isEmpty() && !adc3_vcc_edit.getText().toString().equals(readDate(otherOption.this, "adc3_vcc_value"))) {
            about.log(TAG, "ADC3电压值参数巳改变,发送参数到服务端");
            if (isInteger(adc3_vcc_edit.getText().toString()) || isDecimal(adc3_vcc_edit.getText().toString()) && Float.parseFloat(adc3_vcc_edit.getText().toString()) > 0) {
                if (send_command_to_server("adc3_set_vcc_value:" + adc3_vcc_edit.getText().toString())){
                    new AlertDialog.Builder(otherOption.this)
                            .setTitle("提 示:")
                            .setMessage("设置成功!")
                            .setNegativeButton("完成", (dialogInterface13, i13) -> {
                                goAnim(otherOption.this, 50);
                                saveData("adc3_vcc_value", adc3_vcc_edit.getText().toString());
                            }).show();
                }else{
                    new AlertDialog.Builder(otherOption.this)
                            .setTitle("提 示:")
                            .setMessage("设置失败,请重试!")
                            .setNegativeButton("完成", (dialogInterface13, i13) -> {
                                goAnim(otherOption.this, 50);
                                adc3_vcc_edit.setText(readDate(otherOption.this, "adc3_vcc_value"));
                            }).show();
                }
            } else {
                about.log(TAG, "adc3电压值项请输入数字类型");
                Toast.makeText(otherOption.this, "adc3电压值项请输入数字类型", LENGTH_SHORT).show();
                adc3_vcc_edit.setText(readDate(otherOption.this, "adc3_vcc_value"));
            }
        }
    }
    public void send_adc3_vsens_edit() {
        if (!adc3_vsens_edit.getText().toString().isEmpty() && !adc3_vsens_edit.getText().toString().equals(readDate(otherOption.this, "adc3vsens"))) {
            about.log(TAG, "ADC3系数巳改变,发送参数到服务端");
            if (isInteger(adc3_vsens_edit.getText().toString()) || isDecimal(adc3_vsens_edit.getText().toString()) && Float.parseFloat(adc3_vsens_edit.getText().toString()) > 0) {
                if (send_command_to_server("adc3_vsens_set:" + adc3_vsens_edit.getText().toString())){
                    new AlertDialog.Builder(otherOption.this)
                            .setTitle("提 示:")
                            .setMessage("设置成功!")
                            .setNegativeButton("完成", (dialogInterface13, i13) -> {
                                goAnim(otherOption.this, 50);
                                saveData("adc3vsens", adc3_vsens_edit.getText().toString());
                            }).show();
                }else{
                    new AlertDialog.Builder(otherOption.this)
                            .setTitle("提 示:")
                            .setMessage("设置失败,请重试!")
                            .setNegativeButton("完成", (dialogInterface13, i13) -> {
                                goAnim(otherOption.this, 50);
                                adc3_vsens_edit.setText(readDate(otherOption.this, "adc3vsens"));
                            }).show();
                }
            } else {
                about.log(TAG, "adc3系数项请输入数字类型");
                Toast.makeText(otherOption.this, "adc3系数项请输入数字类型", LENGTH_SHORT).show();
                adc3_vsens_edit.setText(readDate(otherOption.this, "adc3vsens"));
            }
        }
    }

    public void lo_voltage_set() {
        if (!low_voltage_set.getText().toString().isEmpty() && !low_voltage_set.getText().toString().equals(readDate(otherOption.this, "low_voltage"))) {
            about.log(TAG, "最低电压值巳改变,发送参数到服务端");
            if (isInteger(low_voltage_set.getText().toString()) || isDecimal(low_voltage_set.getText().toString()) && Float.parseFloat(low_voltage_set.getText().toString()) > 0) {
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
            } else {
                about.log(TAG, "最低电压值项请输入数字类型");
                Toast.makeText(otherOption.this, "最低电压值项请输入数字类型", LENGTH_SHORT).show();
                low_voltage_set.setText(readDate(otherOption.this, "low_voltage"));
            }
        }
    }
    public void refresh_time_set(){
        new Thread(() -> {
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
        }).start();
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
}
