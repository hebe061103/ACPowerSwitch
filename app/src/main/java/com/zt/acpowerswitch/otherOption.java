package com.zt.acpowerswitch;

import static com.zt.acpowerswitch.MainActivity.readDate;
import static com.zt.acpowerswitch.MainActivity.saveData;
import static com.zt.acpowerswitch.MainActivity.udpClient;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Timer;
import java.util.TimerTask;

public class otherOption extends AppCompatActivity {
    public static String TAG = "otherOption:";
    public TextView target_ip;
    public EditText w_edit,adc2_edit,adc3_edit,low_voltage_set;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.other_activity);
        MainActivity.connect_udp_service();
        str_pro();
    }

    @SuppressLint("ClickableViewAccessibility")
    public void str_pro(){
        target_ip = findViewById(R.id.target_ip);
        if (MainActivity.readDate(otherOption.this,"wifi_ip")!=null){
            target_ip.setText(MainActivity.readDate(otherOption.this,"wifi_ip"));
        }
        //功率设置
        w_edit= findViewById(R.id.w_edit);
        if (readDate(otherOption.this,"w")!=null) {
            w_edit.setText(MainActivity.readDate(otherOption.this, "w"));
        }
        w_edit.addTextChangedListener(new TextWatcher() {
            private Timer timer;
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // 如果已经有一个延时任务在执行，则取消它
                if (timer != null) {
                    timer.cancel();
                }
                // 创建一个新的延时任务
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        // 这里编写输入完成后想要执行的代码
                        runOnUiThread(() -> {
                            w_edit.clearFocus();
                            send_w_edit();
                        });
                    }
                }, 2500);
            }
            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        //ADC2设置
        adc2_edit= findViewById(R.id.adc2_edit);
        if (MainActivity.readDate(otherOption.this,"adc2")!=null) {
            adc2_edit.setText(MainActivity.readDate(otherOption.this, "adc2"));
        }
        adc2_edit.addTextChangedListener(new TextWatcher() {
            private Timer timer;
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // 如果已经有一个延时任务在执行，则取消它
                if (timer != null) {
                    timer.cancel();
                }
                // 创建一个新的延时任务
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        // 这里编写输入完成后想要执行的代码
                        runOnUiThread(() -> {
                            adc2_edit.clearFocus();
                            send_adc2_edit();
                        });
                    }
                }, 2500);
            }
            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        //ADC3设置
        adc3_edit= findViewById(R.id.adc3_edit);
        if (MainActivity.readDate(otherOption.this,"adc3")!=null) {
            adc3_edit.setText(MainActivity.readDate(otherOption.this, "adc3"));
        }
        adc3_edit.addTextChangedListener(new TextWatcher() {
            private Timer timer;
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // 如果已经有一个延时任务在执行，则取消它
                if (timer != null) {
                    timer.cancel();
                }
                // 创建一个新的延时任务
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        // 这里编写输入完成后想要执行的代码
                        runOnUiThread(() -> {
                            adc3_edit.clearFocus();
                            send_adc3_edit();
                        });
                    }
                }, 2500);
            }
            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        low_voltage_set = findViewById(R.id.low_voltage_set);
        if (MainActivity.readDate(otherOption.this,"low_voltage")!=null) {
            low_voltage_set.setText(MainActivity.readDate(otherOption.this, "low_voltage"));
        }
        low_voltage_set.addTextChangedListener(new TextWatcher() {
            private Timer timer;
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // 如果已经有一个延时任务在执行，则取消它
                if (timer != null) {
                    timer.cancel();
                }
                // 创建一个新的延时任务
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        // 这里编写输入完成后想要执行的代码
                        runOnUiThread(() -> {
                            low_voltage_set.clearFocus();
                            lo_voltage_set();
                        });
                    }
                }, 2500);
            }
            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }
    public void send_w_edit(){
        new Thread(() -> {
            if (!w_edit.getText().toString().isEmpty() && !w_edit.getText().toString().equals(readDate(otherOption.this,"w"))){
                about.log(TAG,"功率参数巳改变,发送参数到服务端");
                if(isInteger(w_edit.getText().toString())||isDecimal(w_edit.getText().toString())) {
                    saveData("w", w_edit.getText().toString());
                    udpClient.sendMessage("set_w:" + w_edit.getText().toString());
                }else {
                    about.log(TAG,"功率设置项请输入数字类型");
                    Looper.prepare();
                    Toast.makeText(otherOption.this, "功率设置项请输入数字类型", Toast.LENGTH_SHORT).show();
                    Looper.loop();
                }
            }
        }).start();
    }
    public void send_adc2_edit(){
        new Thread(() -> {
            if (!adc2_edit.getText().toString().isEmpty() && !adc2_edit.getText().toString().equals(readDate(otherOption.this,"adc2"))) {
                about.log(TAG,"ADC2参数巳改变,发送参数到服务端");
                if (isInteger(adc2_edit.getText().toString()) || isDecimal(adc2_edit.getText().toString())) {
                    saveData("adc2", adc2_edit.getText().toString());
                    udpClient.sendMessage("adc2_set_value:" + adc2_edit.getText().toString());
                } else {
                    about.log(TAG,"adc2项请输入数字类型");
                    Looper.prepare();
                    Toast.makeText(otherOption.this, "adc2项请输入数字类型", Toast.LENGTH_SHORT).show();
                    Looper.loop();
                }
            }
        }).start();
    }
    public void send_adc3_edit(){
        new Thread(() -> {
            if (!adc3_edit.getText().toString().isEmpty() && !adc3_edit.getText().toString().equals(readDate(otherOption.this,"adc3"))) {
                about.log(TAG,"ADC3参数巳改变,发送参数到服务端");
                if (isInteger(adc3_edit.getText().toString()) || isDecimal(adc3_edit.getText().toString())) {
                    saveData("adc3", adc3_edit.getText().toString());
                    udpClient.sendMessage("adc3_set_value:" + adc3_edit.getText().toString());
                } else {
                    about.log(TAG,"adc3项请输入数字类型");
                    Looper.prepare();
                    Toast.makeText(otherOption.this, "adc3项请输入数字类型", Toast.LENGTH_SHORT).show();
                    Looper.loop();
                }
            }
        }).start();
    }

    public void lo_voltage_set(){
        new Thread(() -> {
            if (!low_voltage_set.getText().toString().isEmpty() && !low_voltage_set.getText().toString().equals(readDate(otherOption.this,"low_voltage"))) {
                about.log(TAG,"最低电压值巳改变,发送参数到服务端");
                if (isInteger(low_voltage_set.getText().toString()) || isDecimal(low_voltage_set.getText().toString())) {
                    saveData("low_voltage", low_voltage_set.getText().toString());
                    udpClient.sendMessage("low_voltage:" + low_voltage_set.getText().toString());
                } else {
                    about.log(TAG,"最低电压值项请输入数字类型");
                    Looper.prepare();
                    Toast.makeText(otherOption.this, "最低电压值项请输入数字类型", Toast.LENGTH_SHORT).show();
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
    protected void onDestroy() {
        super.onDestroy();
    }
}
