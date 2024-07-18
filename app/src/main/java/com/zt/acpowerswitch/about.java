package com.zt.acpowerswitch;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;


public class about extends AppCompatActivity {
    private TextView about_tx;
    private static int i;
    @SuppressLint("SetTextI18n")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        TextView mBlueMessage = findViewById(R.id.blue_info);
        about_tx = findViewById(R.id.about);
        mBlueMessage.setText(getBluetoothMAC(this));
        clickEvent();
    }
    @SuppressLint("HardwareIds")
    @SuppressWarnings("MissingPermission")
    public static String getBluetoothMAC(Context context) {
        String info = null;
        try {
            if (context.checkCallingOrSelfPermission(android.Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) {
                BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
                info = "name:"+ bta.getName()+"\n"+"address:"+bta.getAddress();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return info ;
    }
private void clickEvent() {
        about_tx.setOnClickListener(view -> {
            i++;
            if(i==6){
                i=0;
                Intent intent = new Intent(about.this, Engineering.class);
                startActivities(new Intent[]{intent});
            }
        });
}
    protected void onDestroy() {
        super.onDestroy();
    }
}
