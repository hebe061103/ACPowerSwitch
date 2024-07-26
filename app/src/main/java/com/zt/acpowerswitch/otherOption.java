package com.zt.acpowerswitch;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class otherOption extends AppCompatActivity {
    public TextView target_ip;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.other_activity);
        str_pro();
    }
   public void str_pro(){
       target_ip = findViewById(R.id.target_ip);
       if (MainActivity.readDate(otherOption.this,"wifi_ip")!=null){
           target_ip.setText(MainActivity.readDate(otherOption.this,"wifi_ip"));
       }
    }
}
