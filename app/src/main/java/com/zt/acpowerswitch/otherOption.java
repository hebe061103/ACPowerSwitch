package com.zt.acpowerswitch;

import static com.zt.acpowerswitch.MainActivity.goAnim;
import static com.zt.acpowerswitch.MainActivity.readDate;
import static com.zt.acpowerswitch.MainActivity.saveData;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class otherOption extends AppCompatActivity {
    public TextView target_ip,ia_set;
    public EditText ia_edit;
    private Handler handler;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.other_activity);
        handler = new Handler(Looper.getMainLooper());
        str_pro();
    }
   public void str_pro(){
       ia_edit= findViewById(R.id.ia_edit);
       if (readDate(otherOption.this,"ia")!=null) {
           ia_edit.setText(readDate(otherOption.this, "ia"));
       }
       target_ip = findViewById(R.id.target_ip);
       if (MainActivity.readDate(otherOption.this,"wifi_ip")!=null){
           target_ip.setText(MainActivity.readDate(otherOption.this,"wifi_ip"));
       }
       ia_set = findViewById(R.id.ia_set);
       ia_set.setOnClickListener(view -> new Thread(() -> {
           // 执行一些后台工作
           goAnim(otherOption.this,50);
           if (!ia_edit.getText().toString().isEmpty()){
               saveData("ia",ia_edit.getText().toString());
               Looper.prepare();
               Toast.makeText(otherOption.this, "巳保存", Toast.LENGTH_SHORT).show();
               Looper.loop();
           }else{
               Looper.prepare();
               Toast.makeText(otherOption.this, "请输入一个数值后再点击", Toast.LENGTH_SHORT).show();
               Looper.loop();
           }
           // 更新UI
           handler.post(() -> {
               //在这里执行要刷新的操作
               if (readDate(otherOption.this,"ia")!=null) {
                   ia_edit.setText(readDate(otherOption.this, "ia"));
               }
           });
       }).start());
    }
}
