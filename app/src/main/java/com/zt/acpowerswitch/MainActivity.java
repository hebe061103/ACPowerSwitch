package com.zt.acpowerswitch;

import static android.widget.Toast.LENGTH_SHORT;
import static com.zt.acpowerswitch.WifiListActivity.wifilist;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks{
    public static String TAG = "MainActivity:";
    private static final int REQUEST_CODE_BLUETOOTH_PERMISSIONS = 123;
    private static final String[] BLUETOOTH_PERMISSIONS = {
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION
    };
    public static SharedPreferences sp;
    public static SharedPreferences.Editor editor;
    public ImageView menu_bt;
    public long lastBack = 0;
    public static boolean connect_udp;
    private boolean Permissions_allow;
    private final UDPClient udpClient = new UDPClient();
    private TextView dev_ip_port,out_Voltage,out_Current,power_w,out_frequency,bat_Voltage,le_Voltage,le_current;
    private String udp_value;
    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sp = getSharedPreferences("WIFI_INFO", MODE_PRIVATE);//获取 SharedPreferences对象
        editor = sp.edit(); // 获取编辑器对象
        requestBluetoothPermissions();
    }
    private void init(){
        if (readDate(this, "wifi_ip") == null) {
            Intent intent = new Intent(MainActivity.this, set_tcp_page.class);
            startActivities(new Intent[]{intent});
        } else {
            udpClient.udpConnect(readDate(this, "wifi_ip"), 55555);
        }
        out_Voltage = findViewById(R.id.out_Voltage);
        out_Current = findViewById(R.id.out_Current);
        bat_Voltage = findViewById(R.id.bat_Voltage);
        le_Voltage = findViewById(R.id.le_Voltage);
        le_current = findViewById(R.id.le_current);
        power_w = findViewById(R.id.power_kw);
        out_frequency = findViewById(R.id.out_frequency);
        menu_bt = findViewById(R.id.menu_img);
        menu_bt.setOnClickListener(view -> {
            goAnim(this, 50);
            MainActivity.this.showPopupMenu(menu_bt);
        });
        dev_ip_port = findViewById(R.id.dev_ip_port);
        dev_ip_port.setOnLongClickListener(view -> {
            goAnim(MainActivity.this,50);
            if (readDate(this,"wifi_ip")!=null) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("提 示")
                        .setMessage("该操作会清空数据，将无法连接到指定设备")
                        .setPositiveButton("取消", null)
                        .setNegativeButton("确定", (dialog, which) -> {
                            goAnim(MainActivity.this, 50);
                            MainActivity.deleteData("wifi_ip");
                        })
                        .show();
            }else{
                Log.e(TAG,"wifi_ip好像是null的");
            }
            return false;
        });
        new Thread(() -> {
            while (true) {
                if(connect_udp) {
                    dev_ip_port.setTextColor(0x33cc33);
                    udpClient.sendMessage("get_info");
                    udp_value = udpClient.receiveMessage();
                    Log.e(TAG, "Receive_data:" + udp_value);
                    if (udp_value != null && udp_value.contains("Battery Voltage")) {
                        Message message = new Message();
                        message.what = 1;
                        udpProHandler.sendMessage(message);
                    }else if (udp_value != null && udp_value.contains("Sun Voltage")) {
                        Message message = new Message();
                        message.what = 2;
                        udpProHandler.sendMessage(message);
                    }else if (udp_value != null && udp_value.contains("Sun Current")) {
                        Message message = new Message();
                        message.what = 3;
                        udpProHandler.sendMessage(message);
                    }
                    sleep(1000);
                }
            }
        }).start();
    }
    @SuppressLint("HandlerLeak")
    Handler udpProHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                //adc1value为电池电压
                if (udp_value!=null) {
                    String[] value = udp_value.split(":");
                    bat_Voltage.setText(value[1]);
                    udp_value = null;
                }
            }
            if (msg.what == 2) {
                //adc2value为太阳能电压
                if (udp_value!=null) {
                    String[] value = udp_value.split(":");
                    le_Voltage.setText(value[1]);
                    udp_value = null;
                }
            }
            if (msg.what == 3) {
                //adc2value为太阳能电压
                if (udp_value!=null) {
                    String[] value = udp_value.split(":");
                    le_current.setText(value[1]);
                    udp_value = null;
                }
            }
        }
    };
    @SuppressLint("MissingPermission")
    public void showPopupMenu(final View view) {
        final PopupMenu popupMenu = new PopupMenu(this, view);
        //menu 布局
        popupMenu.getMenuInflater().inflate(R.menu.main, popupMenu.getMenu());
        //点击事件
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.other_option) {
                goAnim(this, 50);
                Intent intent = new Intent(MainActivity.this, otherOption.class);
                startActivities(new Intent[]{intent});
            } else if (itemId == R.id.about) {
                goAnim(this, 50);
                Intent intent = new Intent(MainActivity.this, about.class);
                startActivities(new Intent[]{intent});
            }
            return false;
        });
        //显示菜单，不要少了这一步
        popupMenu.show();
    }
    public static String readDate(Context context, String s) {
        sp = context.getSharedPreferences("WIFI_INFO", MODE_PRIVATE);
        return sp.getString(s, null);
    }
    public static void saveData(String l, String s) {//l为保存的名字，s为要保存的字符串
        editor.putString(l, s);
        editor.apply();
    }

    public static void deleteData(String l) {
        editor.remove(l); // 根据l删除数据
        editor.apply();
    }
    public void sleep(int s){
        try {
            Thread.sleep(s);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    @SuppressLint("MissingPermission")
    protected void onResume() {
        super.onResume();
        if (Permissions_allow){
            init();
        }
        if(MainActivity.readDate(this,"wifi_ip")!=null) {
            dev_ip_port.setTextColor(0x33cc33);
        }else{
            dev_ip_port.setTextColor(0x999999);
        }
    }
    protected void onPause() {
        super.onPause();
    }
    protected void onDestroy() {
        super.onDestroy();
        if (wifilist != null) {
            wifilist.clear();
        }
        if (UDPClient.socket!=null) {
            udpClient.close();
            Log.e(TAG, "onDestroy网络连接关闭");
            about.log(TAG, "onDestroy网络连接关闭");
        }
    }
    public static void goAnim(Context context, int millisecond) {
        Vibrator vibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(millisecond);
    }

    /**
     * 再次返回键退出程序
     */
    @Override
    public void onBackPressed() {
        if (lastBack == 0 || System.currentTimeMillis() - lastBack > 2000) {
            Toast.makeText(MainActivity.this, "再按一次返回退出", LENGTH_SHORT).show();
            lastBack = System.currentTimeMillis();
            return;
        }
        super.onBackPressed();
    }
    @AfterPermissionGranted(REQUEST_CODE_BLUETOOTH_PERMISSIONS)
    private void requestBluetoothPermissions() {
        if  (EasyPermissions.hasPermissions(this, BLUETOOTH_PERMISSIONS)) {
            //从这里进入主程序
            Permissions_allow=true;
        } else {
            // 没有获得权限，请求权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    EasyPermissions.requestPermissions(this, "需要蓝牙权限以扫描周围的蓝牙设备",
                            REQUEST_CODE_BLUETOOTH_PERMISSIONS, android.Manifest.permission.BLUETOOTH_CONNECT,
                            android.Manifest.permission.BLUETOOTH_SCAN);
                }
            }
            EasyPermissions.requestPermissions(this, "需要蓝牙权限以扫描周围的蓝牙设备", REQUEST_CODE_BLUETOOTH_PERMISSIONS, BLUETOOTH_PERMISSIONS);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // 将结果传递给EasyPermissions处理
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        if (requestCode == REQUEST_CODE_BLUETOOTH_PERMISSIONS) {
            // 相关权限被授予，可以进行蓝牙操作
            Log.e(TAG,"相关权限巳允许");
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        if (requestCode == REQUEST_CODE_BLUETOOTH_PERMISSIONS) {
            // 权限被拒绝，可以适当处理
            finish();
        }
    }
}