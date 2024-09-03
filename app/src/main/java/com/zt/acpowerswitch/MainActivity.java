package com.zt.acpowerswitch;

import static android.widget.Toast.LENGTH_SHORT;
import static com.zt.acpowerswitch.WifiListActivity.wifilist;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
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
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity{
    private final String TAG = "MainActivity:";
    public final String top_m = "ComponentInfo{com.zt.acpowerswitch/com.zt.acpowerswitch.MainActivity}";
    public final String top_a = "ComponentInfo{com.zt.acpowerswitch/com.zt.acpowerswitch.about}";
    public static SharedPreferences sp;
    public static SharedPreferences.Editor editor;
    public ImageView menu_bt;
    public long lastBack = 0;
    public static final UDPClient udpClient = new UDPClient();
    private TextView out_Voltage,out_Current,power_kw,sj_power_kw,out_frequency,out_mode,bat_Voltage,le_current;
    public static String udp_value;
    public String[] info;
    public static String udpServerAddress;
    public static int udpServerPort;
    public static boolean udp_connect,data_rec_finish,click_mem_confirm,stop_send,delete_wifi_finish,delete_udp_finish;
    public ArrayList<String> _min_bat_list = new ArrayList<>();
    public ArrayList<String> _date_bat_list = new ArrayList<>();
    public ArrayList<String> _month_bat_list = new ArrayList<>();
    public ArrayList<String> _time_value = new ArrayList<>();
    public ArrayList<String> _mem_value = new ArrayList<>();
    public ArrayList<Entry> _bat_list = new ArrayList<>();
    public ArrayList <Entry> _mem_use_list = new ArrayList<>();
    public LineChart line_chart;
    public int cycle_size=0;
    private ComponentName topActivity;
    public LineDataSet lineDataSet;
    public Thread data_pro_thread;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    private void init_module(){
        udpServerAddress = readDate(this, "wifi_ip");
        udpServerPort = Integer.parseInt(readDate(this, "port"));
        udpClient.udpConnect(udpServerAddress, udpServerPort);
        data_rec_finish=false;
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
        TextView mem_use_status = findViewById(R.id.mem_use_status);
        line_chart.setNoDataText("暂无数据!");
        menu_bt = findViewById(R.id.menu_img);
        menu_bt.setOnClickListener(view -> {
            goAnim(this, 50);
            MainActivity.this.showPopupMenu(menu_bt);
        });
        TextView dev_ip_port = findViewById(R.id.dev_ip_port);
        dev_ip_port.setOnLongClickListener(view -> {
            goAnim(MainActivity.this,50);
            stop_send=true;
            if (readDate(this,"wifi_ip")!=null) {
                new AlertDialog.Builder(MainActivity.this)
                .setTitle("网络切换")
                .setMessage("确定要切换目标设备的网络吗?")
                .setPositiveButton("取消", (dialogInterface, i) -> stop_send=false)
                .setNegativeButton("确定", (dialog, which) -> {
                    goAnim(MainActivity.this, 50);
                    udpClient.sendMessage("del_wifi_config");
                    int num = 0;
                    while(num <5) {
                        sleep(1000);
                        if (delete_wifi_finish) {
                            delete_wifi_finish = false;
                            Intent intent = new Intent(this, set_tcp_page.class);
                            startActivities(new Intent[]{intent});
                        } else {
                            new AlertDialog.Builder(this)
                                    .setTitle("提示:")
                                    .setMessage("网络连接失败,请再试一次!")
                                    .setNegativeButton("确定", null)
                                    .show();
                        }
                        num++;
                    }
                }).show();
            }
            return false;
        });
        _min.setOnClickListener(view -> new Thread(() -> {
            goAnim(MainActivity.this, 50);
            about.log(TAG, "查询分时电压值");
            click_mem_confirm=false;//用于停止显示内存使用情况
            _mem_use_list.clear(); //清理内存数组
            if (line_chart.getData() != null && line_chart.getData().getDataSetCount() > 0) {
                line_chart.getData().removeDataSet(line_chart.getData().getDataSetByIndex(0));
            }
            pro_time_data(_min_bat_list,"分时电压值");
        }).start());
        _min.setOnLongClickListener(view -> {
            goAnim(MainActivity.this, 50);
            new AlertDialog.Builder(MainActivity.this)
                .setTitle("提 示")
                .setMessage("该操作将清除分时数据,无法恢复!")
                .setPositiveButton("取消", null)
                .setNegativeButton("确定", (dialog, which) -> new Thread(() -> {
                    goAnim(MainActivity.this, 50);
                    udpClient.sendMessage("clean_minute_file");
                }).start()).show();
            return false;
        });
        _day.setOnClickListener(view -> new Thread(() -> {
            goAnim(MainActivity.this, 50);
            about.log(TAG, "查询日期电压值");
            click_mem_confirm=false;//用于停止显示内存使用情况
            _mem_use_list.clear(); //清理内存数组
            if (line_chart.getData() != null && line_chart.getData().getDataSetCount() > 0) {
                line_chart.getData().removeDataSet(line_chart.getData().getDataSetByIndex(0));
            }
            pro_time_data(_date_bat_list,"日期电压值");
        }).start());
        _day.setOnLongClickListener(view -> {
            goAnim(MainActivity.this, 50);
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("提 示")
                    .setMessage("该操作将清除日期数据,无法恢复!")
                    .setPositiveButton("取消", null)
                    .setNegativeButton("确定", (dialog, which) -> new Thread(() -> {
                        goAnim(MainActivity.this, 50);
                        udpClient.sendMessage("clean_date_file");
                    }).start()).show();
            return false;
        });
        _month.setOnClickListener(view -> new Thread(() -> {
            goAnim(MainActivity.this, 50);
            click_mem_confirm=false;//用于停止显示内存使用情况
            _mem_use_list.clear();//清理内存数组
            about.log(TAG, "查询月份电压值");
            if (line_chart.getData() != null && line_chart.getData().getDataSetCount() > 0) {
                line_chart.getData().removeDataSet(line_chart.getData().getDataSetByIndex(0));
            }
            pro_time_data(_month_bat_list,"月份电压值");
        }).start());
        _month.setOnLongClickListener(view -> {
            goAnim(MainActivity.this, 50);
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("提 示")
                    .setMessage("该操作将清除月份数据,无法恢复!")
                    .setPositiveButton("取消", null)
                    .setNegativeButton("确定", (dialog, which) -> new Thread(() -> {
                        goAnim(MainActivity.this, 50);
                        udpClient.sendMessage("clean_month_file");
                    }).start()).show();
            return false;
        });
        mem_use_status.setOnClickListener(view -> {
            goAnim(MainActivity.this, 50);
            click_mem_confirm = true;
            about.log(TAG, "查询内存使用量");
            if (line_chart.getData() != null && line_chart.getData().getDataSetCount() > 0) {
                line_chart.getData().removeDataSet(line_chart.getData().getDataSetByIndex(0));
            }
        });
        if (data_pro_thread==null || !data_pro_thread.isAlive()) {
            data_refresh_pro();
        }
    }
    public void data_refresh_pro(){
        data_pro_thread = new Thread(() -> {
            while(true) {
                while (udp_connect) {
                    if (checkScreenStatus() && getTopActivity().toString().equals(top_m) ||
                            getTopActivity().toString().equals(top_a) && !stop_send) {
                        udpClient.sendMessage("get_info");
                        about.log(TAG, "发送请求信息");
                        sleep(request_delay_ms());
                    }
                    udp_value = udpClient.receiveMessage();
                    if (udp_value != null && udp_value.contains("['AC_voltage")) {
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
                    if (udp_value != null && udp_value.contains("min>") && data_rec_finish && !stop_send && checkScreenStatus()
                            && getTopActivity().toString().equals(top_m) && lineDataSet.getLabel().contains("分时电压值")) {
                        String[] _l = udp_value.split(">"); //按>进行分隔
                        if (_l[1] != null && !_l[1].isEmpty()) {
                            about.log(TAG, "动态分时数据:"+_l[1]);
                            _min_bat_list.add(_l[1]);
                            pro_time_data(_min_bat_list, "分时电压值");
                        }
                    }
                    if (udp_value != null && udp_value.contains("mem>") && click_mem_confirm && checkScreenStatus()) {
                        pro_mem_use_status();
                    }
                    if (udp_value != null && udp_value.contains("del_wifi_finish")) {
                        about.log(TAG, "重置WIFI完成");
                        delete_wifi_finish=true;
                    }
                    if (udp_value != null && udp_value.contains("set_udp_finish")) {
                        about.log(TAG, "重置UDP端口完成");
                        delete_udp_finish=true;
                    }
                    if (!data_rec_finish && line_chart.getData() == null && !stop_send && checkScreenStatus()){
                        pro_data_request();
                    }
                    if (line_chart.getData() == null && getTopActivity().toString().equals(top_m) && checkScreenStatus()){
                        pro_time_data(_min_bat_list, "分时电压值");
                    }
                    if (!checkScreenStatus()){
                        about.log(TAG, "屏幕关闭");
                        udpClient.close();
                    }
                }
            }
        });
        data_pro_thread.start();
    }
    @SuppressLint("HandlerLeak")
    Handler udpProHandler = new Handler() {
        @SuppressLint("SetTextI18n")
        public void handleMessage(Message msg) {
            DecimalFormat df = new DecimalFormat("#.##");
            String ac = null;
            if (msg.what == 1) {
                try {
                    //交流电压
                    if (unicodeToString(info[13]).equals("市电供电") || unicodeToString(info[13]).equals("电池电压过低")){
                        out_Voltage.setText(info[1]);
                        ac = info[1];
                    }else if (unicodeToString(info[13]).equals("逆变供电")){
                        out_Voltage.setText(String.valueOf(220));
                        ac = String.valueOf(220);
                    }
                    //交流电流
                    Float jl_dl = Float.parseFloat(info[3]);
                    String formattedValue_iv_Value = df.format(jl_dl);
                    out_Current.setText(formattedValue_iv_Value);
                    String iv = info[3];
                    //交流有功功率
                    power_kw.setText(info[5]);
                    //交流实际功率
                    Float sj_power = null;
                    if (ac != null) {
                        sj_power = Float.parseFloat(ac) * Float.parseFloat(iv);
                    }
                    String formattedValue = df.format(sj_power);
                    sj_power_kw.setText(formattedValue);
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
    public void pro_data_request(){
        about.log(TAG, "请求全部数据命令");
        udpClient.sendMessage("get_all_file");
        if (stop_send) data_rec_finish = false;
        _min_bat_list.clear();
        _date_bat_list.clear();
        _month_bat_list.clear();
        while (!data_rec_finish) {
            udp_value = udpClient.receiveMessage();
            if (udp_value != null && udp_value.contains("min>")) {
                about.log(TAG, udp_value);
                String[] _l = udp_value.split(">"); //按>进行分隔
                int length = _l.length; // 获取数组长度
                if (length > 1) {
                    _min_bat_list.add(_l[1]);
                }
            } else if (udp_value != null && udp_value.contains("date>")) {
                about.log(TAG, udp_value);
                String[] _l = udp_value.split(">"); //按>进行分隔
                int length = _l.length; // 获取数组长度
                if (length > 1) {
                    _date_bat_list.add(_l[1]);
                }
            } else if (udp_value != null && udp_value.contains("month>")) {
                about.log(TAG, udp_value);
                String[] _l = udp_value.split(">"); //按>进行分隔
                int length = _l.length; // 获取数组长度
                if (length > 1) {
                    _month_bat_list.add(_l[1]);
                }
            } else if (udp_value != null && udp_value.contains("all_file_send_finish")) {
                about.log(TAG, "所有数据接收完成,分时数据:" + _min_bat_list.size() + " 日期数据:"
                        + _date_bat_list.size() + " 月份数据:" + _month_bat_list.size());
                data_rec_finish = true;
                stop_send=false;
                cycle_size=0;
            } else if(cycle_size == 3){
                data_rec_finish = true;
                stop_send=false;
                cycle_size=0;
            } else if (!data_rec_finish) {
                _min_bat_list.clear();
                _date_bat_list.clear();
                _month_bat_list.clear();
                about.log(TAG, "请求失败,再来一次");
                udpClient.sendMessage("get_all_file");
                cycle_size++;
            }
        }
    }
    public void pro_time_data(List<String> _sd,String label){
        _time_value.clear();
        _bat_list.clear();
        String minute_des = "";
        String date_des = "";
        String month_des = "";
        switch (label) {
            case "分时电压值":
                for (int i = 0; i < _sd.size(); i++) {
                    String[] _e = _sd.get(i).split(" ");
                    minute_des = _e[0];
                    int _e_length = _e.length;
                    if (_e_length > 1) {
                        String[] _u = _e[1].split(":");
                        _time_value.add(_u[0] + ":" + _u[1]);
                        String[] _split_bat_value = _e[2].split(":");
                        String _bat_value = _split_bat_value[1]; //截取电压值
                        _bat_list.add(new Entry(i, Float.parseFloat(_bat_value)));
                    }
                }
                displayToChart(_time_value, _bat_list, minute_des, label);
                line_chart.notifyDataSetChanged();//通知数据巳改变
                line_chart.invalidate();//清理无效数据,用于动态刷新
                break;
            case "日期电压值":
                for (int i = 0; i < _sd.size(); i++) {
                    String[] _e = _sd.get(i).split(" ");
                    int _e_length = _e.length;
                    if (_e_length > 1) {
                        String[] _s = _e[0].split("-");
                        date_des = _s[0] + "-" + _s[1];
                        _time_value.add(_s[2] + "日");
                        String[] _split_bat_value = _e[2].split(":");
                        String _bat_value = _split_bat_value[1]; //截取电压值
                        _bat_list.add(new Entry(i, Float.parseFloat(_bat_value)));
                    }
                }
                displayToChart(_time_value, _bat_list, date_des, label);
                line_chart.notifyDataSetChanged();//通知数据巳改变
                line_chart.invalidate();//清理无效数据,用于动态刷新
                break;
            case "月份电压值":
                for (int i = 0; i < _sd.size(); i++) {
                    String[] _e = _sd.get(i).split(" ");
                    int _e_length = _e.length;
                    if (_e_length > 1) {
                        String[] _s = _e[0].split("-");
                        month_des = _s[0];
                        _time_value.add(_s[1] + "月");
                        String[] _split_bat_value = _e[2].split(":");
                        String _bat_value = _split_bat_value[1]; //截取电压值
                        _bat_list.add(new Entry(i, Float.parseFloat(_bat_value)));

                    }
                }
                displayToChart(_time_value, _bat_list, month_des, label);
                line_chart.notifyDataSetChanged();//通知数据巳改变
                line_chart.invalidate();//清理无效数据,用于动态刷新
                break;
        }
    }
    public void pro_mem_use_status(){
        about.log(TAG, "收到内存使用信息");
        String [] mem = udp_value.split(">");
        float _mem = Float.parseFloat(mem[1]);
        _mem_value.add("");
        _mem_use_list.add(new Entry(_mem_use_list.size(), _mem));
        lineDataSet = new LineDataSet(_mem_use_list, "设备内存使用情况(单位:"+_mem+" kb)");
        lineDataSet.setValueFormatter(new NoValueFormatter());//使用自定义的值格式化器
        lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);//这里是圆滑曲线
        lineDataSet.setDrawCircles(false);//在点上画圆 默认true
        /*lineDataSet.setCircleColor(Color.GREEN);//关键点的圆点颜色
        lineDataSet.setValueTextSize(6f);//关键点的字体大小*/
        lineDataSet.setLineWidth(1f);//设置线条的宽度，最大10f,最小0.2f
        lineDataSet.setDrawFilled(true);//设置是否填充
        LineData data = new LineData(lineDataSet);
        line_chart.getXAxis().setValueFormatter(new ExamModelOneXValueFormatter(_mem_value));//顶部X轴显示
        line_chart.getDescription().setText("esp32c3");//右下角描述
        line_chart.setExtraTopOffset(10f);//顶部数据距离边框距离
        line_chart.getAxisLeft().setAxisMinimum(0f);//左侧Y轴最小值
        line_chart.getAxisLeft().setAxisMaximum(400f);//左侧Y轴最大值
        line_chart.getAxisRight().setAxisMinimum(0f);//右侧Y轴最小值
        line_chart.getAxisRight().setAxisMaximum(400f);//右侧Y轴最大值
        line_chart.setData(data);//调置数据
        line_chart.notifyDataSetChanged();//通知数据巳改变
        line_chart.invalidate();//清理无效数据,用于动态刷新
    }
    public void displayToChart(ArrayList<String> time_value,ArrayList<Entry> bat_list_value,String des,String label){
        try{
            String[] bat_value = String.valueOf(_bat_list.get(_bat_list.size()-1)).split(":");
            lineDataSet = new LineDataSet(bat_list_value, label+": " + bat_value[2] + " v");
            lineDataSet.setValueFormatter(new NoValueFormatter());//使用自定义的值格式化器
            lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);//这里是圆滑曲线
            lineDataSet.setDrawCircles(true);//在点上画圆 默认true
            /*lineDataSet.setCircleColor(Color.GREEN);//关键点的圆点颜色
            lineDataSet.setValueTextSize(6f);//关键点的字体大小*/
            lineDataSet.setLineWidth(1f);//设置线条的宽度，最大10f,最小0.2f
            LineData data = new LineData(lineDataSet);
            line_chart.getXAxis().setValueFormatter(new ExamModelOneXValueFormatter(time_value));//顶部X轴显示
            line_chart.getDescription().setText(des);//右下角描述
            line_chart.setExtraTopOffset(10f);//顶部数据距离边框距离
            line_chart.getAxisLeft().setAxisMinimum(10f);//左侧Y轴最小值
            line_chart.getAxisLeft().setAxisMaximum(30f);//左侧Y轴最大值
            line_chart.getAxisRight().setAxisMinimum(10f);//右侧Y轴最小值
            line_chart.getAxisRight().setAxisMaximum(30f);//右侧Y轴最大值
            line_chart.setData(data);//调置数据
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    /* 获取屏幕状态通过PowerManager */
    @SuppressLint("ObsoleteSdkInt")
    public boolean checkScreenStatus() {
            PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
            boolean isScreenOn;
            // Android 4.4W (KitKat Wear)系统及以上使用新接口获取亮屏状态
            if (Build.VERSION.SDK_INT >= 20) {
                isScreenOn = pm.isInteractive();
            } else {
                isScreenOn = pm.isScreenOn();
            }
            return isScreenOn;
    }
    /*获取最上层activity*/
    public ComponentName getTopActivity(){
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTasks = activityManager.getRunningTasks(1);

        if (runningTasks != null && !runningTasks.isEmpty()) {
            topActivity = runningTasks.get(0).topActivity;
        }
        return topActivity;
    }
    /*发送请求数据延时*/
    public int request_delay_ms(){
        if (readDate(this, "refresh_time") != null) {
            return Integer.parseInt(readDate(this, "refresh_time"));
        } else {
            return 1000;
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
        sp = context.getSharedPreferences("CONFIG_INFO", MODE_PRIVATE);
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
    public static void sleep(int s){
        try {
            Thread.sleep(s);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    @SuppressLint("MissingPermission")
    protected void onResume() {
        super.onResume();
        check_request_permissions();
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

    // 请求多个权限
    private void check_request_permissions() {
        // 创建一个权限列表，把需要使用而没用授权的的权限存放在这里
        List<String> permissionList = new ArrayList<>();

        // 判断权限是否已经授予，没有就把该权限添加到列表中
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)
                != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.BLUETOOTH_ADMIN);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissionList.add(Manifest.permission.BLUETOOTH_SCAN);
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissionList.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        }
        // 如果列表为空，就是全部权限都获取了，不用再次获取了。不为空就去申请权限
        if (!permissionList.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionList.toArray(new String[0]), 1002);
        } else {
            sp = getSharedPreferences("CONFIG_INFO", MODE_PRIVATE);//获取 SharedPreferences对象
            editor = sp.edit(); // 获取编辑器对象
            if (readDate(this, "wifi_ip") == null || readDate(this, "port") == null) {
                Intent intent = new Intent(this, set_tcp_page.class);
                startActivities(new Intent[]{intent});
            }else{
                init_module();
            }
        }
    }

    // 请求权限回调方法
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1002) {// 1002请求码对应的是申请多个权限
            // 因为是多个权限，所以需要一个循环获取每个权限的获取情况
            for (int grantResult : grantResults) {
                // PERMISSION_DENIED 这个值代表是没有授权，我们可以把被拒绝授权的权限显示出来
                if (grantResult == PackageManager.PERMISSION_DENIED) {
                    finish();
                }
            }
        }
    }
}

class ExamModelOneXValueFormatter implements IAxisValueFormatter {
    private final ArrayList<String> list;

    public ExamModelOneXValueFormatter(ArrayList<String> list){
        this.list = list;
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        int values = (int) value;
        if(values<0){
            values = 0;
        }
        if(values>=list.size()){
            values = list.size()-1;
        }
        return list.get(values%list.size());
    }
}
/*数据值格式化器*/
class NoValueFormatter implements IValueFormatter {
    @Override
    public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
        return ""; // 返回空字符串，不显示任何值
    }
}