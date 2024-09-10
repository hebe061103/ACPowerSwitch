package com.zt.acpowerswitch;

import static android.widget.Toast.LENGTH_SHORT;
import static com.zt.acpowerswitch.MainActivity.delete_udp_finish;
import static com.zt.acpowerswitch.MainActivity.file_name;
import static com.zt.acpowerswitch.MainActivity.goAnim;
import static com.zt.acpowerswitch.MainActivity.readDate;
import static com.zt.acpowerswitch.MainActivity.saveData;
import static com.zt.acpowerswitch.MainActivity.sleep;
import static com.zt.acpowerswitch.MainActivity.udpClient;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

public class otherOption extends AppCompatActivity {
    private static final String TAG = "otherOption:";
    private EditText w_edit,adc2_edit,adc3_edit,low_voltage_set,refresh_time_set ;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.other_activity);
        str_pro();
    }

    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    public void str_pro(){
        TextView target_ip = findViewById(R.id.target_ip);
        target_ip.setText(readDate(otherOption.this,"wifi_ip"));
        TextView target_port = findViewById(R.id.target_port);
        target_port.setText(readDate(otherOption.this,"port"));
        target_port.setOnClickListener(view -> {
            goAnim(otherOption.this,50);
            EditText portText = new EditText(otherOption.this);
            new AlertDialog.Builder(otherOption.this)
            .setTitle("请输入端口号:")
            .setView(portText)
            .setPositiveButton("取消", null)
            .setNegativeButton("确定", (dialog, which) -> {
                if (!portText.getText().toString().isEmpty()) {
                    if (Integer.parseInt(portText.getText().toString())>0 && Integer.parseInt(portText.getText().toString()) < 65536) {
                        String inputText = portText.getText().toString();
                        udpClient.sendMessage("set_udp_port:"+inputText);
                        int num = 0;
                        while(num <2) {
                            sleep(2000);
                            if (delete_udp_finish) {
                                delete_udp_finish = false;
                                new AlertDialog.Builder(this)
                                    .setTitle("提示:")
                                    .setMessage("设置成功")
                                    .setNegativeButton("完成", (dialogInterface, i) -> {
                                        udpClient.close();
                                        saveData("port", inputText);
                                        target_port.setText(inputText);
                                    })
                                    .show();
                                break;
                            } else {
                                if (num == 1) {
                                    new AlertDialog.Builder(this)
                                        .setTitle("提示:")
                                        .setMessage("网络连接失败,请再试一次!")
                                        .setNegativeButton("确定", null)
                                        .show();
                                }

                            }
                            num++;
                        }
                    }else{
                        Toast.makeText(otherOption.this, "输入的端口不合法", LENGTH_SHORT).show();
                    }
                }
            })
            .show();
        });
        //功率设置
        w_edit= findViewById(R.id.w_edit);
        if (readDate(otherOption.this,"w")!=null) {
            w_edit.setText(readDate(otherOption.this, "w"));
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
        if (readDate(otherOption.this,"adc2")!=null) {
            adc2_edit.setText(readDate(otherOption.this, "adc2"));
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
        if (readDate(otherOption.this,"adc3")!=null) {
            adc3_edit.setText(readDate(otherOption.this, "adc3"));
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
        if (readDate(otherOption.this,"low_voltage")!=null) {
            low_voltage_set.setText(readDate(otherOption.this, "low_voltage"));
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
        //刷新时间设置
        refresh_time_set = findViewById(R.id.refresh_time_set);
        if (readDate(otherOption.this,"refresh_time")!=null) {
            refresh_time_set.setText(readDate(otherOption.this, "refresh_time"));
        }
        refresh_time_set.addTextChangedListener(new TextWatcher() {
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
                            refresh_time_set.clearFocus();
                            refresh_time_set();
                        });
                    }
                }, 2500);
            }
            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        TextView reset_network = findViewById(R.id.reset_network);
        reset_network.setOnClickListener(view -> {
            goAnim(otherOption.this,50);
            new AlertDialog.Builder(otherOption.this)
                .setTitle("提 示")
                .setMessage("该操作会清空巳保存的连接信息，将无法连接到指定设备")
                .setPositiveButton("取消", null)
                .setNegativeButton("确定", (dialog, which) -> {
                    goAnim(otherOption.this,50);
                    MainActivity.deleteData("wifi_ip");
                    MainActivity.deleteData("port");
                    File file = new File(getFilesDir(), file_name);
                    if (file.exists()) {
                        boolean deleted = file.delete();
                        if (deleted){
                            Toast.makeText(otherOption.this, "删除电池历史数据成功", LENGTH_SHORT).show();
                        }else {
                            Toast.makeText(otherOption.this, "删除电池历史数据失败", LENGTH_SHORT).show();
                        }
                    }else {
                        Toast.makeText(otherOption.this, "电池历史数据不存在", LENGTH_SHORT).show();
                    }
                })
                .show();
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
                    Toast.makeText(otherOption.this, "功率设置项请输入数字类型", LENGTH_SHORT).show();
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
                    Toast.makeText(otherOption.this, "adc2项请输入数字类型", LENGTH_SHORT).show();
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
                    Toast.makeText(otherOption.this, "adc3项请输入数字类型", LENGTH_SHORT).show();
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
                    Toast.makeText(otherOption.this, "最低电压值项请输入数字类型", LENGTH_SHORT).show();
                    Looper.loop();
                }
            }
        }).start();
    }

    public void refresh_time_set(){
        new Thread(() -> {
            if (!refresh_time_set.getText().toString().isEmpty() && !refresh_time_set.getText().toString().equals(readDate(otherOption.this,"refresh_time"))){
                about.log(TAG,"页面刷新时间巳改变");
                if(isInteger(refresh_time_set.getText().toString())) {
                    saveData("refresh_time", refresh_time_set.getText().toString());
                    MainActivity.page_refresh_time = Integer.parseInt(refresh_time_set.getText().toString());
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
    protected void onPause() {
        super.onPause();
    }
    protected void onDestroy() {
        super.onDestroy();
    }
}
