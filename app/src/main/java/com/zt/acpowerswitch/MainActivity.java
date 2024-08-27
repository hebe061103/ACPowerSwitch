package com.zt.acpowerswitch;

import static android.widget.Toast.LENGTH_SHORT;
import static com.zt.acpowerswitch.WifiListActivity.wifilist;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks{
    private final String TAG = "MainActivity:";
    private static final int REQUEST_CODE_BLUETOOTH_PERMISSIONS = 123;
    private static final String[] BLUETOOTH_PERMISSIONS = {
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    public static SharedPreferences sp;
    public static SharedPreferences.Editor editor;
    public ImageView menu_bt;
    public long lastBack = 0;
    private boolean Permissions_allow;
    public static final UDPClient udpClient = new UDPClient();
    private TextView out_Voltage,out_Current,power_kw,sj_power_kw,out_frequency,out_mode,bat_Voltage,le_current;
    public static String udp_value;
    public String[] info;
    public static String udpServerAddress;
    public static Integer udPort;
    public static boolean udp_connect,min_rec_finish;
    public List<String> _month_value = new ArrayList<>();
    public List<String> _day_value = new ArrayList<>();
    public List<String> _time_value = new ArrayList<>();
    public ArrayList <Entry> _bat_list = new ArrayList<>();
    public LineChart line_chart;

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
        connect_udp_service();
        min_rec_finish=false;
        out_Voltage = findViewById(R.id.out_Voltage);
        out_Current = findViewById(R.id.out_Current);
        power_kw = findViewById(R.id.power_kw);
        sj_power_kw = findViewById(R.id.sj_power_kw);
        out_frequency = findViewById(R.id.out_frequency);
        out_mode = findViewById(R.id.out_mode);
        le_current = findViewById(R.id.le_current);
        bat_Voltage = findViewById(R.id.bat_Voltage);
        line_chart = findViewById(R.id.line_chart);
        TextView _min = findViewById(R.id._min);
        TextView _day = findViewById(R.id._day);
        TextView _month = findViewById(R.id._month);
        menu_bt = findViewById(R.id.menu_img);
        menu_bt.setOnClickListener(view -> {
            goAnim(this, 50);
            MainActivity.this.showPopupMenu(menu_bt);
        });
        TextView dev_ip_port = findViewById(R.id.dev_ip_port);
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
            }
            return false;
        });
        _min.setOnClickListener(view -> new Thread(() -> {
            goAnim(MainActivity.this, 50);
            line_chart.clear();
            line_chart.notifyDataSetChanged(); // 通知图表数据集已更新
            line_chart.invalidate(); // 请求图表重绘
            min_rec_finish=false;
            pro_min_data("get_minute_file","分时电压值");
        }).start());
        _day.setOnClickListener(view -> new Thread(() -> {
            goAnim(MainActivity.this, 50);
            line_chart.clear();
            line_chart.notifyDataSetChanged(); // 通知图表数据集已更新
            line_chart.invalidate(); // 请求图表重绘
            min_rec_finish=false;
            pro_min_data("get_day_file","日期电压值");
        }).start());
        _month.setOnClickListener(view -> new Thread(() -> {
            goAnim(MainActivity.this, 50);
            line_chart.clear();
            line_chart.notifyDataSetChanged(); // 通知图表数据集已更新
            line_chart.invalidate(); // 请求图表重绘
            min_rec_finish=false;
            pro_min_data("get_month_file","月度电压值");
        }).start());
        Thread thread = new Thread(() -> {
            while (true) {
                while (udp_connect) {
                    if (UDPClient.socket != null) {
                        udpClient.sendMessage("get_info");
                        about.log(TAG, "发送请求信息");
                        udp_value = udpClient.receiveMessage();
                        if (udp_value != null && udp_value.contains("AC_voltage")) {
                            about.log(TAG, "请求返回信息:" + udp_value);
                            String modifiedString = udp_value.substring(1, udp_value.length() - 1);
                            modifiedString = modifiedString.replace("'", "");
                            modifiedString = modifiedString.replace(",", ":");
                            modifiedString = modifiedString.replace(" ", "");
                            info = modifiedString.split(":");
                            Message message = new Message();
                            message.what = 1;
                            udpProHandler.sendMessage(message);
                        }
                        if (!min_rec_finish && line_chart.getData() == null) pro_min_data("get_minute_file","分时电压值");
                        if (readDate(this, "refresh_time") != null) {
                            sleep(Integer.parseInt(readDate(this, "refresh_time")) * 1000);
                        } else {
                            sleep(1000);//默认延时1s
                        }
                    }
                }
            }
        });
        thread.start();
    }
    @SuppressLint("HandlerLeak")
    Handler udpProHandler = new Handler() {
        @SuppressLint("SetTextI18n")
        public void handleMessage(Message msg) {
            DecimalFormat df = new DecimalFormat("#.##");
            if (msg.what == 1) {
                try {
                    //交流电压
                    out_Voltage.setText(info[1]+" v");
                    String ac = info[1];
                    //交流电流
                    Float jl_dl = Float.parseFloat(info[3]);
                    String formattedValue_iv_Value = df.format(jl_dl);
                    out_Current.setText(formattedValue_iv_Value+" a");
                    String iv = info[3];
                    //交流有功功率
                    power_kw.setText(info[5]+" w");
                    //交流实际功率
                    Float sj_power = Float.parseFloat(ac) * Float.parseFloat(iv);
                    String formattedValue = df.format(sj_power);
                    sj_power_kw.setText(formattedValue+" w");
                    //交流频率
                    out_frequency.setText(info[7]+" hz");
                    //为电池电压
                    bat_Voltage.setText(info[9]);
                    //为太阳能电流
                    le_current.setText(info[11]);
                    //当前输出模式
                    out_mode.setText(unicodeToString(info[13]));
                }catch (Exception e){
                    about.log(TAG,"拆分数据时接收到空数据");
                }
            }
        }
    };
    public void pro_min_data(String _sd,String value_type){ //分时图可见时请求分时数据
        about.log(TAG, "发送请求统计数据命令");
        udpClient.sendMessage(_sd);
        _month_value.clear();
        _bat_list.clear();
        _time_value.clear();
        while (!min_rec_finish) {
            udp_value = udpClient.receiveMessage();
            if (udp_value != null && udp_value.contains("line>")) {
                String[] _l = udp_value.split(">");
                if (!_l[1].isEmpty()) {
                    String[] _e = _l[1].split(" ");
                    String _data = _e[0]; //提取空格分隔的第1个值为日期
                    String [] _month_ = _data.split("-"); //年月日按"-"分隔
                    String _year_month = _month_[0]+"-"+_month_[1]; //获取年份+月份
                    String _year = _month_[0]; //获取年份
                    String _time = _e[1]; //提取空格分隔的第2个值为时间
                    String[] _split_bat_value = _e[2].split(":"); //分隔空格分隔的第3个值
                    _month_value.add(_month_[1]);
                    _day_value.add(_month_[2]);
                    _time_value.add(_time);
                    _bat_list.add(new Entry(_bat_list.size()-1, Float.parseFloat(_split_bat_value[1])));
                    switch (value_type) {
                        case "分时电压值":
                            displayToChart(_time_value, _bat_list, _data, value_type);
                            break;
                        case "日期电压值":
                            displayToChart(_day_value, _bat_list, _year_month, value_type);
                            break;
                        case "月度电压值":
                            displayToChart(_month_value, _bat_list, _year, value_type);
                            break;
                    }
                }
            } else if (udp_value != null && udp_value.contains("min_send_finish")) {
                about.log(TAG, "分时文件接收完成");
                min_rec_finish = true;
            } else {
                min_rec_finish = true;
            }
        }
    }
    public void displayToChart(List<String> time_value,ArrayList <Entry> bat_list,String d,String des){
        try{
            LineDataSet lineDataSet = new LineDataSet(bat_list, des);
            lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);//这里是圆滑曲线
            lineDataSet.setCircleColor(Color.GREEN);
            lineDataSet.setValueTextSize(8f);
            LineData data = new LineData(lineDataSet);

            line_chart.getXAxis().setValueFormatter(new MyXAxisValueFormatter(time_value));
            line_chart.getDescription().setText(d);
            line_chart.setData(data);
            line_chart.notifyDataSetChanged(); // 通知图表数据集已更新
            line_chart.invalidate(); // 请求图表重绘
        } catch (Exception e){
            about.log(TAG,"MPAndroidChart异常:"+e);
            min_rec_finish = false;
        }
    }
    public void connect_udp_service() {
        if (readDate(this, "wifi_ip") == null) {
            Intent intent = new Intent(this, set_tcp_page.class);
            startActivities(new Intent[]{intent});
        }else{
            udpServerAddress=readDate(this, "wifi_ip");
            udPort = 55555;
            udpClient.udpConnect(udpServerAddress, udPort);
        }
    }
    public static String unicodeToString(String unicode) {
        StringBuilder sb = new StringBuilder();
        String[] hex = unicode.split("\\\\u");
        for (int i = 1; i < hex.length; i++) {
            int value = Integer.parseInt(hex[i], 16);
            sb.append((char) value);
        }
        return sb.toString();
    }
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
    }
    protected void onPause() {
        super.onPause();
        if (UDPClient.socket!=null) {
            udpClient.close();
            about.log(TAG, "网络连接中断");
        }
    }
    protected void onDestroy() {
        super.onDestroy();
        if (wifilist != null) {
            wifilist.clear();
        }
        if (UDPClient.socket!=null) {
            udpClient.close();
            about.log(TAG, "程序退出,网络连接关闭");
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
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    EasyPermissions.requestPermissions(this, "需要蓝牙权限以扫描周围的蓝牙设备",
                            REQUEST_CODE_BLUETOOTH_PERMISSIONS, Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN);
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
            about.log(TAG,"权限巳允许");
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

class MyXAxisValueFormatter  implements IAxisValueFormatter {
    private final List<String> labels;

    public MyXAxisValueFormatter(List<String> labels) {
        this.labels = labels;
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        int index = (int) value;
        if (index < 0 || index >= labels.size()) {
            return "";
        }
        return labels.get(index);
    }
}