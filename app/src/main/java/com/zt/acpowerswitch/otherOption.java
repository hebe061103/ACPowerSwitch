package com.zt.acpowerswitch;

import static com.zt.acpowerswitch.MainActivity.goAnim;
import static com.zt.acpowerswitch.MainActivity.readDate;
import static com.zt.acpowerswitch.MainActivity.saveData;
import static com.zt.acpowerswitch.MainActivity.udpClient;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class otherOption extends AppCompatActivity {
    public TextView target_ip,ia_set,adc2_set,adc3_set;
    public EditText w_edit,adc2_edit,adc3_edit;
    private Handler handler;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.other_activity);
        handler = new Handler(Looper.getMainLooper());
        str_pro();
    }

    public void str_pro(){
        target_ip = findViewById(R.id.target_ip);
        if (MainActivity.readDate(otherOption.this,"wifi_ip")!=null){
            target_ip.setText(MainActivity.readDate(otherOption.this,"wifi_ip"));
        }
        w_edit= findViewById(R.id.w_edit);
        if (readDate(otherOption.this,"w")!=null) {
            w_edit.setText(MainActivity.readDate(otherOption.this, "w"));
        }
       ia_set = findViewById(R.id.ia_set);
       ia_set.setOnClickListener(view -> new Thread(() -> {
           // 执行一些后台工作
           goAnim(otherOption.this,50);
           if (!w_edit.getText().toString().isEmpty()){
               saveData("w",w_edit.getText().toString());
               udpClient.sendMessage("set_w:"+w_edit.getText().toString());
               String save_result= udpClient.receiveMessage();
               if (save_result.contains("save_ok")) {
                   Looper.prepare();
                   Toast.makeText(otherOption.this, "巳保存", Toast.LENGTH_SHORT).show();
                   Looper.loop();
               }
           }else{
               Looper.prepare();
               Toast.makeText(otherOption.this, "请输入一个数值后再点击", Toast.LENGTH_SHORT).show();
               Looper.loop();
           }
           // 更新UI
           handler.post(() -> {
               //在这里执行要刷新的操作
               if (MainActivity.readDate(otherOption.this,"w")!=null) {
                   w_edit.setText(MainActivity.readDate(otherOption.this, "w"));
               }
           });
       }).start());

        adc2_edit= findViewById(R.id.adc2_edit);
        if (MainActivity.readDate(otherOption.this,"adc2")!=null) {
            adc2_edit.setText(MainActivity.readDate(otherOption.this, "adc2"));
        }
        adc2_set = findViewById(R.id.adc2_set);
        adc2_set.setOnClickListener(view -> new Thread(() -> {
            // 执行一些后台工作
            goAnim(otherOption.this,50);
            if (!adc2_edit.getText().toString().isEmpty()){
                MainActivity.saveData("adc2",adc2_edit.getText().toString());
                udpClient.sendMessage("adc2_set_value:"+adc2_edit.getText().toString());
                String save_result= udpClient.receiveMessage();
                if (save_result.contains("adc2_set_ok")) {
                    Looper.prepare();
                    Toast.makeText(otherOption.this, "巳保存", Toast.LENGTH_SHORT).show();
                    Looper.loop();
                }
            }else{
                Looper.prepare();
                Toast.makeText(otherOption.this, "请输入一个数值后再点击", Toast.LENGTH_SHORT).show();
                Looper.loop();
            }
            // 更新UI
            handler.post(() -> {
                //在这里执行要刷新的操作
                if (MainActivity.readDate(otherOption.this,"adc2")!=null) {
                    adc2_edit.setText(MainActivity.readDate(otherOption.this, "adc2"));
                }
            });
        }).start());

        adc3_edit= findViewById(R.id.adc3_edit);
        if (MainActivity.readDate(otherOption.this,"adc3")!=null) {
            adc3_edit.setText(MainActivity.readDate(otherOption.this, "adc3"));
        }
        adc3_set = findViewById(R.id.adc3_set);
        adc3_set.setOnClickListener(view -> new Thread(() -> {
            // 执行一些后台工作
            goAnim(otherOption.this,50);
            if (!adc3_edit.getText().toString().isEmpty()){
                saveData("adc3",adc3_edit.getText().toString());
                udpClient.sendMessage("adc3_set_value:"+adc3_edit.getText().toString());
                String save_result= udpClient.receiveMessage();
                if (save_result.contains("adc3_set_ok")) {
                    Looper.prepare();
                    Toast.makeText(otherOption.this, "巳保存", Toast.LENGTH_SHORT).show();
                    Looper.loop();
                }
            }else{
                Looper.prepare();
                Toast.makeText(otherOption.this, "请输入一个数值后再点击", Toast.LENGTH_SHORT).show();
                Looper.loop();
            }
            // 更新UI
            handler.post(() -> {
                //在这里执行要刷新的操作
                if (MainActivity.readDate(otherOption.this,"adc3")!=null) {
                    adc3_edit.setText(readDate(otherOption.this, "adc3"));
                }
            });
        }).start());
    }
}
