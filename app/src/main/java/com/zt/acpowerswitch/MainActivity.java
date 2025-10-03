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
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
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
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.scwang.smart.refresh.header.MaterialHeader;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

public class MainActivity extends AppCompatActivity{
    public static final String TAG = "MainActivity:";
    public final String top_m = "ComponentInfo{com.zt.acpowerswitch/com.zt.acpowerswitch.MainActivity}";
    public static SharedPreferences sp;
    public static SharedPreferences.Editor editor;
    public ImageView menu_bt,mark_status;
    public long lastBack = 0;
    public static final UDPClient udpClient = new UDPClient();
    private TextView out_Voltage,out_Current,power_kw,sj_power_kw,out_frequency,out_mode,bat_Voltage,sun_voltage_value,le_current,mos_temp_value,mm_use;
    public static String udp_response;
    public String[] info;
    public static String udpServerAddress;
    public static int udpServerPort=55555;
    public static boolean udp_connect,data_rec_finish, stop_send,Thread_Run,Conn_status,isPaused;
    public static ArrayList<String> _min_bat_list = new ArrayList<>();
    public static ArrayList<String> _H_Total_power = new ArrayList<>();
    public static ArrayList<String> _D_Total_power = new ArrayList<>();
    public static ArrayList<String> _M_Total_power = new ArrayList<>();
    public static ArrayList<String> _Y_Total_power = new ArrayList<>();
    public ArrayList<String> _time_value = new ArrayList<>();
    public ArrayList<String> _mem_value = new ArrayList<>();
    public ArrayList<Entry> _value_list = new ArrayList<>();
    public ArrayList<BarEntry> _barChart_list = new ArrayList<>();
    public ArrayList <Entry> _mem_use_list = new ArrayList<>();
    public static ArrayList<String> debugList = new ArrayList<>();
    public LineChart bat_line_chart,mem_use_chart;
    public BarChart power_chart;
    public int cycle_size=0;
    public int date_num;
    private ComponentName topActivity;
    public static LineDataSet bat_lineDataSet,mem_lineDataSet;
    public static int page_refresh_time;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    private void init_module(){
        CustomMarkerView mv = new CustomMarkerView(this,R.layout.custom_marker_view);
        SmartRefreshLayout smartRefreshLayout = findViewById(R.id.refreshLayout);
        //设置 Header 为 贝塞尔雷达 样式
        smartRefreshLayout.setRefreshHeader(new MaterialHeader(this));
        smartRefreshLayout.setOnRefreshListener(refreshLayout -> {
            if (bat_line_chart.getData() != null) {
                bat_line_chart.clear();//清除图表
                bat_line_chart.invalidate(); // 使改变生效
            }
            if (power_chart.getData() != null){
                power_chart.clear();//清除图表
                power_chart.invalidate(); // 使改变生效
            }
            if (mem_use_chart.getData() != null){
                mem_use_chart.clear();//清除图表
                mem_use_chart.invalidate(); // 使改变生效
            }
            isPaused=true;
            stop_send=true;
            data_rec_finish=false;
            request_homepage_date();
            smartRefreshLayout.finishRefresh(3000);
            Log.e(TAG,"刷新完成");
        });
        mark_status = findViewById(R.id.mark_status);
        date_num = getCurrentMonthLastDay();
        udpServerAddress = readDate(this, "wifi_ip");
        page_refresh_time = request_delay_ms();
        out_Voltage = findViewById(R.id.out_Voltage);
        out_Current = findViewById(R.id.out_Current);
        power_kw = findViewById(R.id.power_kw);
        sj_power_kw = findViewById(R.id.sj_power_kw);
        out_frequency = findViewById(R.id.out_frequency);
        out_mode = findViewById(R.id.out_mode);
        sun_voltage_value = findViewById(R.id.sun_voltage_value);
        le_current = findViewById(R.id.le_current);
        bat_Voltage = findViewById(R.id.bat_Voltage);
        bat_line_chart = findViewById(R.id.line_chart);
        power_chart = findViewById(R.id.power_chart);
        mem_use_chart = findViewById(R.id.mem_use_chart);
        mos_temp_value = findViewById(R.id.mos_temp_value);
        mm_use = findViewById(R.id.mm_use);
        TextView dev_ip_port = findViewById(R.id.dev_ip_port);
        dev_ip_port.setOnLongClickListener(view -> {
            goAnim(MainActivity.this, 50);
            new AlertDialog.Builder(this)
                    .setTitle("注意:")
                    .setMessage("该操作将重置远端设备网络,请慬慎执行!!!")
                    .setPositiveButton("取消",null)
                    .setNegativeButton("执行", (dialogInterface, i) -> {
                        goAnim(MainActivity.this, 50);
                        if(send_command_to_server("del_wifi_config")) {
                            new AlertDialog.Builder(this)
                                    .setTitle("提 示")
                                    .setMessage("重置成功")
                                    .setNegativeButton("完成", (dialogInterface1, i1) -> {
                                        goAnim(MainActivity.this, 50);
                                        deleteData("wifi_ip");
                                        deleteData("power");
                                        deleteData("adc2_offset_value");
                                        deleteData("adc3_vcc_value");
                                        deleteData("adc3vsens");
                                        deleteData("low_voltage");
                                        deleteData("refresh_time");
                                        deleteData("out_mode");
                                        udpClient.close();
                                    }).show();
                        }else{
                            new AlertDialog.Builder(this)
                                    .setTitle("提 示")
                                    .setMessage("设备正忙,请稍后再试!")
                                    .setNegativeButton("完成", (dialogInterface12, i12) -> goAnim(MainActivity.this, 50)).show();
                        }
                    }).show();
            return false;
        });
        menu_bt = findViewById(R.id.menu_img);
        menu_bt.setOnClickListener(view -> {
            goAnim(this, 50);
            MainActivity.this.showPopupMenu(menu_bt);
        });
        TextView hour_power = findViewById(R.id.hour_power);
        hour_power.setOnClickListener(view -> {
            goAnim(MainActivity.this, 50);
            power_chart.clear();//清除图表
            power_chart.invalidate(); // 使改变生效
            if (!_H_Total_power.isEmpty()) {
                pro_chart_data(_H_Total_power,"小时柱状图表");//把数据放到柱状图上
            }else{
                power_chart.setNoDataText("暂无小时数据");
            }
        });
        TextView day_power = findViewById(R.id.day_power);
        day_power.setOnClickListener(view -> {
            goAnim(MainActivity.this, 50);
            power_chart.clear();//清除图表
            power_chart.invalidate(); // 使改变生效
            if (!_D_Total_power.isEmpty()) {
                pro_chart_data(_D_Total_power,"日期柱状图表");//把数据放到柱状图上
            }else{
                power_chart.setNoDataText("暂无日期数据");
            }
        });
        TextView month_power = findViewById(R.id.month_power);
        month_power.setOnClickListener(view -> {
            goAnim(MainActivity.this, 50);
            power_chart.clear();//清除图表
            power_chart.invalidate(); // 使改变生效
            if (!_M_Total_power.isEmpty()) {
                pro_chart_data(_M_Total_power,"月份柱状图表");//把数据放到柱状图上
            }else{
                power_chart.setNoDataText("暂无月份数据");
            }
        });
        TextView year_power = findViewById(R.id.year_power);
        year_power.setOnClickListener(view -> {
            goAnim(MainActivity.this, 50);
            power_chart.clear();//清除图表
            power_chart.invalidate(); // 使改变生效
            if (!_Y_Total_power.isEmpty()) {
                pro_chart_data(_Y_Total_power,"年份柱状图表");//把数据放到柱状图上
            }else{
                power_chart.setNoDataText("暂无年份数据");
            }
        });
        bat_line_chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry entry, Highlight highlight) {
                /*// 显示被选中的数值
                String selectedValue = "X-Index: " + entry.getX() + ", Y-Value: " + entry.getY();
                Toast.makeText(getApplicationContext(), selectedValue, Toast.LENGTH_SHORT).show();*/
                //点击指示器可显示更多详情
                bat_line_chart.setMarkerView(mv);
            }
            @Override
            public void onNothingSelected() {
                // 可以不做处理
            }
        });
        start_Thread();
    }

    public void start_Thread(){
        udpClient.udpConnect();
        while (!Thread_Run) {
            if (udp_connect) {
                about.log(TAG, "开始调用线程");
                mData_pro_thread();
                break;
            }
        }
    }
    public static boolean send_command_to_server(String data) {
        CountDownLatch latch = new CountDownLatch(1); // 创建一个 CountDownLatch，初始计数为 1
        boolean[] result = {false}; // 使用数组来存储返回值
        new Thread(() -> {
            int num = 0;
            stop_send = true;
            udp_response = null;
            while (num < 10) {
                udpClient.sendMessage(data);
                sleep(100);
                udp_response = udpClient.receiveMessage();
                about.log(TAG, "返回数据:" + udp_response);
                if (udp_response != null && udp_response.contains("ACK")) {
                    result[0] = true; // 设置返回值
                    break;
                } else {
                    udp_response = null;
                }
                num++;
            }
            stop_send = false;
            latch.countDown(); // 计数器减一，表示任务完成
        }).start();

        try {
            latch.await(); // 等待线程完成
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return result[0]; // 返回结果
    }
    private void mData_pro_thread() {
        new Thread(() -> {
            Thread_Run = true;
            while (udp_connect) {
                while (!isPaused) {
                    if (checkScreenStatus() && !stop_send) {
                        udpClient.sendMessage("get_info");
                        sleep(page_refresh_time);
                        udp_response = udpClient.receiveMessage();
                    }
                    if (udp_response != null && udp_response.contains("['AC_voltage")) {
                        about.log(TAG, "服务端返回数据:" + udp_response);
                        String modifiedString = udp_response.substring(1, udp_response.length() - 1);
                        modifiedString = modifiedString.replace("'", "");
                        modifiedString = modifiedString.replace(",", ":");
                        modifiedString = modifiedString.replace(" ", "");
                        info = modifiedString.split(":");
                        if (info[1] != null) {
                            Message message = new Message();
                            message.what = 1;
                            messageProHandler.sendMessage(message);
                        }
                        Message message = new Message();
                        message.what = 4;
                        messageProHandler.sendMessage(message);
                    }
                    if (udp_response != null && udp_response.contains("live>") && data_rec_finish && !stop_send && checkScreenStatus()
                            && getTopActivity().toString().equals(top_m) && bat_lineDataSet.getLabel().contains("每15分钟电压")) {
                        String[] _l = udp_response.split(">"); //按>进行分隔
                        if (_l[1] != null && !_l[1].isEmpty()) {
                            about.log(TAG, "动态分时数据:" + _l[1]);
                            _min_bat_list.add(_l[1]);
                            pro_chart_data(_min_bat_list, "每15分钟电压");
                        }
                    }
                    if (!data_rec_finish && !stop_send && checkScreenStatus()) {
                        Message message = new Message();
                        message.what = 2;
                        messageProHandler.sendMessage(message);
                    }
                    if (!checkScreenStatus()) {
                        about.log(TAG, "屏幕关闭");
                        udpClient.close();
                        isPaused=true;
                    }
                    if (Conn_status) {
                        Message message = new Message();
                        message.what = 3;
                        messageProHandler.sendMessage(message);
                    }
                }
            }
            isPaused=false;
            Thread_Run = false;
            about.log(TAG, "数据更新线程巳退出");
        }).start();
        about.log(TAG, "线程调用完成");
    }
    @SuppressLint("HandlerLeak")
    Handler messageProHandler = new Handler(Looper.getMainLooper()) {
        @SuppressLint("SetTextI18n")
        public void handleMessage(Message msg) {
            DecimalFormat df = new DecimalFormat("#.##");
            String ac;
            if (msg.what == 1) {
                //交流电压
                out_Voltage.setText(info[1]);
                ac = info[1];
                //交流电流
                Float jl_dl = Float.parseFloat(info[3]);
                String formattedValue_iv_Value = df.format(jl_dl);
                out_Current.setText(formattedValue_iv_Value);
                String iv = info[3];
                //交流有功功率
                power_kw.setText(info[5]);
                //交流实际功率
                if (ac != null) {
                    Float sj_power = Float.parseFloat(ac) * Float.parseFloat(iv);
                    String formattedValue = df.format(sj_power);
                    sj_power_kw.setText(formattedValue);
                }
                //交流频率
                out_frequency.setText(info[7]+" hz");
                //为电池电压
                bat_Voltage.setText(info[9]);
                //为光伏电压
                sun_voltage_value.setText(info[11]);
                //为太阳能电流
                le_current.setText(info[13]);
                //为MOS管散热片温度
                mos_temp_value.setText(info[15]);
                //当前输出模式
                out_mode.setText(unicodeToString(info[17]));
                //内存使用信息
                mem_data_display_to_chart();//把内存使用信息放到折线图上
                //市电切换阈值
                saveData("power",info[21]);
                //电池低于此值则市电常开
                saveData("low_voltage",info[23]);
                //输出模式
                saveData("out_mode", info[25]);
                //MOS开启的阈值温度
                saveData("mos_temp",info[27]);
            }else if (msg.what == 2){
                request_homepage_date();
            }else if (msg.what == 3){
                if (mark_status.getVisibility() == View.VISIBLE) {
                    mark_status.setVisibility(View.INVISIBLE);
                } else {
                    mark_status.setVisibility(View.VISIBLE);
                }
                Conn_status=false;
            }else if (msg.what == 4){
                mark_status.setVisibility(View.INVISIBLE);
                Conn_status=false;
            }
        }
    };
    private void request_homepage_date() {
        new Thread(() -> {
            pro_data_request();//请求数据
            if (!_min_bat_list.isEmpty() && getTopActivity().toString().equals(top_m) && checkScreenStatus() && data_rec_finish) {
                pro_chart_data(_min_bat_list, "每15分钟电压");//把数据放到折线图上
            }else{
                bat_line_chart.setNoDataText("暂无分时数据");
            }
            about.log(TAG, "15分钟刷新完成");
            if (!_H_Total_power.isEmpty() && getTopActivity().toString().equals(top_m) && checkScreenStatus() && data_rec_finish) {
                pro_chart_data(_H_Total_power,"小时柱状图表");//把数据放到柱状图上
            }else{
                power_chart.setNoDataText("暂无小时数据");
            }
            about.log(TAG, "小时柱状图刷新完成");
        }).start();
    }
    public void pro_data_request(){
        stop_send = true;
        _min_bat_list.clear();
        _H_Total_power.clear();
        _D_Total_power.clear();
        _M_Total_power.clear();
        _Y_Total_power.clear();
        udp_response=null;
        about.log(TAG, "请求全部数据");
        udpClient.sendMessage("get_all_file");
        sleep(page_refresh_time);
        while (!data_rec_finish) {
            udp_response=udpClient.receiveMessage();
            if (udp_response != null && udp_response.contains("min>")) {
                Log.e(TAG, udp_response);
                String[] _l = udp_response.split(">"); //按>进行分隔
                int length = _l.length; // 获取数组长度
                if (length > 1) {
                    _min_bat_list.add(_l[1]);
                }
            } else if (udp_response != null && udp_response.contains("H_Total_power>")) {
                Log.e(TAG, udp_response);
                String[] _l = udp_response.split(">"); //按>进行分隔
                int length = _l.length; // 获取数组长度
                if (length > 1) {
                    _H_Total_power.add(_l[1]);
                }
            } else if (udp_response != null && udp_response.contains("D_Total_power>")) {
                Log.e(TAG, udp_response);
                String[] _l = udp_response.split(">"); //按>进行分隔
                int length = _l.length; // 获取数组长度
                if (length > 1) {
                    _D_Total_power.add(_l[1]);
                }
            } else if (udp_response != null && udp_response.contains("M_Total_power>")) {
                Log.e(TAG, udp_response);
                String[] _l = udp_response.split(">"); //按>进行分隔
                int length = _l.length; // 获取数组长度
                if (length > 1) {
                    _M_Total_power.add(_l[1]);
                }
            } else if (udp_response != null && udp_response.contains("Y_Total_power>")) {
                Log.e(TAG, udp_response);
                String[] _l = udp_response.split(">"); //按>进行分隔
                int length = _l.length; // 获取数组长度
                if (length > 1) {
                    _Y_Total_power.add(_l[1]);
                }
            } else if (udp_response != null && udp_response.contains("debug>")) {
                Log.e(TAG, udp_response);
                String[] _l = udp_response.split(">"); //按>进行分隔
                int length = _l.length; // 获取数组长度
                if (length > 1) {
                    debugList.add(_l[1]);
                }
            } else if (udp_response != null && udp_response.contains("all_file_send_finish")) {
                about.log(TAG, "所有数据接收完成,分时数据数量:" + _min_bat_list.size() + " 小时平均功率数据数量:" + _H_Total_power.size() + " 日功率数据数量:" + _D_Total_power.size() + " 月功率数据数量:" + _M_Total_power.size() + " 年功率数据数量:" + _Y_Total_power.size());
                data_rec_finish = true;
                stop_send=false;
                isPaused=false;
                cycle_size=0;
            } else if(cycle_size == 3){
                data_rec_finish = true;
                stop_send=false;
                cycle_size=0;
                isPaused=false;
            } else if (!data_rec_finish) {
                _min_bat_list.clear();
                _H_Total_power.clear();
                _D_Total_power.clear();
                _M_Total_power.clear();
                _Y_Total_power.clear();
                udpClient.sendMessage("get_all_file");
                sleep(page_refresh_time);
                cycle_size++;
            }
        }
    }
    @SuppressLint("DefaultLocale")
    public void pro_chart_data(List<String> _sd, String label){
        if (label.equals("每15分钟电压")) {
            String minute_des = "";
            _time_value.clear();
            _value_list.clear();
            for (int i = 0; i < _sd.size(); i++) {
                String[] _e = _sd.get(i).split(" ");
                minute_des= _e[0];
                String[] _u = _e[1].split(":");
                _time_value.add(_u[0] + ":" + _u[1]);
                String[] _split_bat_value = _e[2].split(":");
                _value_list.add(new Entry(i, Float.parseFloat(_split_bat_value[1])));
            }
            bat_data_display_to_chart(_time_value, _value_list, minute_des, label);
            bat_line_chart.notifyDataSetChanged();//通知数据巳改变
            bat_line_chart.invalidate();//清理无效数据,用于动态刷新
        }
        if (label.equals("小时柱状图表")) {
            String begin_time = "";
            String over_time = "";
            String last_power = "";
            _barChart_list.clear();
            for (int i = 0; i < _sd.size(); i++) {
                String[] _e = _sd.get(i).split(" ");
                if (_sd.size() > 1) {
                    if (i == 0) {
                        begin_time = "今日: " + _e[0].split(":")[0] + ":" + _e[0].split(":")[1] + "  ->  ";
                    } else if (i == _sd.size() - 1) {
                        over_time = _e[0].split(":")[0] + ":" + _e[0].split(":")[1];
                        last_power = _e[1];
                    }
                }else{
                    begin_time = "今日: " + _e[0].split(":")[0] + ":" + _e[0].split(":")[1];
                    over_time = "";
                    last_power = _e[1];
                }
                _barChart_list.add(new BarEntry(Integer.parseInt(_e[0].split(":")[0]), Float.parseFloat(String.format("%.1f", Float.parseFloat(_e[1])))));
            }
            pro_date_power_data(_barChart_list,"前一小时用电量统计(单位:"+ String.format("%.3f", Float.parseFloat(last_power)) +" kw.h(度))",begin_time  + over_time,"小时");
            power_chart.notifyDataSetChanged();//通知数据巳改变
            power_chart.invalidate();//清理无效数据,用于动态刷新
        }
        if (label.equals("日期柱状图表")) {
            String begin_time = "";
            String over_time = "";
            String last_power = "";
            _barChart_list.clear();
            for (int i = 0; i < _sd.size(); i++) {
                String[] _e = _sd.get(i).split(" ");
                if (_sd.size() > 1) {
                    if (i == 0) {
                        begin_time = _e[0] + "  ->  ";
                    } else if (i == _sd.size() - 1) {
                        over_time = _e[0];
                        last_power = _e[1];
                    }
                }else{
                    begin_time = _e[0];
                    over_time = "";
                    last_power = _e[1];
                }
                _barChart_list.add(new BarEntry(Integer.parseInt(_e[0].split("-")[2]), Float.parseFloat(String.format("%.1f",Float.parseFloat(_e[1])/1000))));
            }
            pro_date_power_data(_barChart_list,"前一日用电量统计(单位:"+ String.format("%.1f", Float.parseFloat(last_power)) +" kw.h(度))",begin_time + over_time,"日期");
            power_chart.notifyDataSetChanged();//通知数据巳改变
            power_chart.invalidate();//清理无效数据,用于动态刷新
        }
        if (label.equals("月份柱状图表")) {
            String begin_time = "";
            String over_time = "";
            String last_power = "";
            _barChart_list.clear();
            for (int i = 0; i < _sd.size(); i++) {
                String[] _e = _sd.get(i).split(" ");
                if (_sd.size() > 1) {
                    if (i == 0) {
                        begin_time = _e[0].split("-")[0] + "-" + _e[0].split("-")[1]+"  ->  ";
                    } else if (i == _sd.size() - 1) {
                        over_time = _e[0].split("-")[0] + "-" + _e[0].split("-")[1];
                        last_power = _e[1];
                    }
                }else{
                    begin_time = _e[0].split("-")[0] + "-" + _e[0].split("-")[1];
                    over_time = "";
                    last_power = _e[1];
                }
                _barChart_list.add(new BarEntry(Integer.parseInt(_e[0].split("-")[1]), Float.parseFloat(String.format("%.1f",Float.parseFloat(_e[1])/1000))));
            }
            pro_date_power_data(_barChart_list,"上一月用电量统计(单位:"+ String.format("%.1f", Float.parseFloat(last_power)) +" kw.h(度))",begin_time + over_time,"月份");
            power_chart.notifyDataSetChanged();//通知数据巳改变
            power_chart.invalidate();//清理无效数据,用于动态刷新
        }
        if (label.equals("年份柱状图表")) {
            String begin_time = "";
            String over_time = "";
            String last_power = "";
            _barChart_list.clear();
            for (int i = 0; i < _sd.size(); i++) {
                String[] _e = _sd.get(i).split(" ");
                if (_sd.size() > 1) {
                    if (i == 0) {
                        begin_time = _e[0].split("-")[0]+"  ->  ";
                    } else if (i == _sd.size() - 1) {
                        over_time = _e[0].split("-")[0];
                        last_power = _e[1];
                    }
                }else{
                    begin_time = _e[0].split("-")[0];
                    over_time = "";
                    last_power = _e[1];
                }
                _barChart_list.add(new BarEntry(Integer.parseInt(_e[0].split("-")[0]),  Float.parseFloat(String.format("%.1f",Float.parseFloat(_e[1])/1000))));
            }
            pro_date_power_data(_barChart_list,"前一年用电量统计(单位:"+ String.format("%.1f", Float.parseFloat(last_power)) +" kw.h(度))",begin_time + over_time,"年份");
            power_chart.notifyDataSetChanged();//通知数据巳改变
            power_chart.invalidate();//清理无效数据,用于动态刷新
        }
    }
    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    public void mem_data_display_to_chart(){
        float _mem = Float.parseFloat(info[19]);
        DecimalFormat decimalFormat = new DecimalFormat("#.0");
        String formattedValue = decimalFormat.format(_mem/150*100);
        mm_use.setText(formattedValue + "%");
        _mem_value.add("");
        if (!_mem_use_list.isEmpty()) {
            if (_mem_use_list.size() <= 100) {
                _mem_use_list.add(new Entry(_mem_use_list.size(), _mem));
            }else{
                _mem_use_list.clear();
                _mem_use_list.add(new Entry(_mem_use_list.size(), _mem));
            }
        }else{
            _mem_use_list.add(new Entry(_mem_use_list.size(), _mem));
        }
        mem_lineDataSet = new LineDataSet(_mem_use_list, "设备内存使用情况(巳使用:"+_mem+" kb"+"  空闲:"+String.format("%.2f", (150-_mem))+"kb)");
        mem_lineDataSet.setValueFormatter(new NoValueFormatter());//使用自定义的值格式化器
        mem_lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);//这里是圆滑曲线
        mem_lineDataSet.setDrawCircles(false);//在点上画圆 默认true
        /*mem_lineDataSet.setCircleRadius(2f);
        mem_lineDataSet.setCircleColor(Color.GREEN);//关键点的圆点颜色
        mem_lineDataSet.setValueTextSize(6f);//关键点的字体大小*/
        mem_lineDataSet.setLineWidth(2f);//设置线条的宽度，最大10f,最小0.2f
        mem_lineDataSet.setDrawFilled(true);//设置是否填充
        LineData mem_data = new LineData(mem_lineDataSet);
        mem_use_chart.getXAxis().setAxisMinimum(1f);
        mem_use_chart.getXAxis().setAxisMaximum(100f);
        mem_use_chart.getXAxis().setEnabled(false);
        mem_use_chart.getDescription().setText(" ");//右下角描述
        mem_use_chart.setExtraTopOffset(10f);//顶部数据距离边框距离
        //mem_use_chart.getAxisLeft().setTextColor(Color.BLUE); //Y轴左侧文本颜色
        //mem_use_chart.getAxisRight().setTextColor(Color.BLUE); //Y轴左侧文本颜色
        mem_use_chart.getAxisLeft().setAxisMinimum(0f);//左侧Y轴最小值
        mem_use_chart.getAxisLeft().setAxisMaximum(160f);//左侧Y轴最大值
        mem_use_chart.getAxisRight().setAxisMinimum(0f);//右侧Y轴最小值
        mem_use_chart.getAxisRight().setAxisMaximum(160f);//右侧Y轴最大值
        mem_use_chart.setData(mem_data);//调置数据
        mem_use_chart.notifyDataSetChanged();//通知数据巳改变
        mem_use_chart.invalidate();//清理无效数据,用于动态刷新
    }
    public void bat_data_display_to_chart(ArrayList<String> time_value,ArrayList<Entry> bat_list_value,String des,String label){
        String[] bat_value = String.valueOf(_value_list.get(_value_list.size()-1)).split(":");
        bat_lineDataSet = new LineDataSet(bat_list_value, label+": " + bat_value[2] + " v");
        bat_lineDataSet.setValueFormatter(new NoValueFormatter());//使用自定义的值格式化器
        bat_lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);//这里是圆滑曲线
        bat_lineDataSet.setDrawCircles(false);//在点上画圆 默认true
        /*bat_lineDataSet.setCircleRadius(2f);
        bat_lineDataSet.setCircleColor(Color.BLUE);//关键点的圆点颜色
        bat_lineDataSet.setValueTextSize(6f);//关键点的字体大小*/
        bat_lineDataSet.setLineWidth(2f);//设置线条的宽度，最大10f,最小0.2f
        LineData bat_data = new LineData(bat_lineDataSet);
        bat_line_chart.getXAxis().setValueFormatter(new ExamModelOneXValueFormatter(time_value));//顶部X轴显示
        bat_line_chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        bat_line_chart.getDescription().setText(des);//右下角描述
        bat_line_chart.getDescription().setTextSize(9f);
        bat_line_chart.setExtraTopOffset(10f);//顶部数据距离边框距离
        bat_line_chart.getXAxis().setTextSize(10f); //设置顶部文字大小
        //bat_line_chart.getAxisLeft().setTextColor(Color.BLUE); //Y轴左侧文本颜色
        //bat_line_chart.getAxisRight().setTextColor(Color.BLUE); //Y轴左侧文本颜色
        bat_line_chart.getXAxis().setAxisMinimum(0f);
        bat_line_chart.getXAxis().setAxisMaximum(95f);
        bat_line_chart.getAxisLeft().setAxisMinimum(20f);//左侧Y轴最小值
        bat_line_chart.getAxisLeft().setAxisMaximum(30f);//左侧Y轴最大值
        bat_line_chart.getAxisRight().setAxisMinimum(20f);//右侧Y轴最小值
        bat_line_chart.getAxisRight().setAxisMaximum(30f);//右侧Y轴最大值
        bat_line_chart.setData(bat_data);//调置数据
    }
    /**
     * 初始化BarChart图表
     */
    private void pro_date_power_data(ArrayList<BarEntry> barChart,String label,String des,String type) {
        //X轴设置显示位置在底部
        XAxis xAxis = power_chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        switch (type) {
            case "小时":
                xAxis.setAxisMinimum(-0.5f);
                xAxis.setAxisMaximum(23.5f);
                power_chart.getAxisLeft().setAxisMinimum(0f);//左侧Y轴最小值
                break;
            case "日期":
                xAxis.setAxisMinimum(0.5f);
                xAxis.setAxisMaximum(date_num+0.5f);
                power_chart.getAxisLeft().setAxisMinimum(0f);//左侧Y轴最小值
                break;
            case "月份":
                xAxis.setAxisMinimum(0.5f);
                xAxis.setAxisMaximum(12.5f);
                power_chart.getAxisLeft().setAxisMinimum(0f);//左侧Y轴最小值
                break;
            case "年份":
                xAxis.setAxisMinimum(2024-0.5f);
                xAxis.setAxisMaximum(2054.5f);
                power_chart.getAxisLeft().setAxisMinimum(0f);//左侧Y轴最小值
                break;
        }
        xAxis.setGranularity(1f);
        BarData barData = getBarData(barChart, label);
        power_chart.getDescription().setText(des);//右下角描述
        power_chart.getDescription().setTextSize(9f);
        power_chart.setData(barData);//调置数据
    }

    @NonNull
    private static BarData getBarData(ArrayList<BarEntry> barChart, String label) {
        BarDataSet dataSet = new BarDataSet(barChart, label);
        dataSet.setValueFormatter((value, entry, dataSetIndex, viewPortHandler) -> String.format(Locale.getDefault(), "%.1f", value));// 自定义值格式
        dataSet.setBarBorderWidth(0.2f); // 设置条形图之间的间距
        dataSet.setColor(Color.GREEN); // 设置柱子的颜色
        dataSet.setDrawValues(true); //是否绘制柱状图顶部的数值
        dataSet.setValueTextSize(8f);
        return new BarData(dataSet);
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
    /**
     * 取得当月天数
     * */
    public int getCurrentMonthLastDay()
    {
        Calendar a = Calendar.getInstance();
        a.set(Calendar.DATE, 1);//把日期设置为当月第一天
        a.roll(Calendar.DATE, -1);//日期回滚一天，也就是最后一天
        return a.get(Calendar.DATE);
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
            //throw new RuntimeException(e);
        }
    }
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        // 如果列表为空，就是全部权限都获取了，不用再次获取了。不为空就去申请权限
        if (!permissionList.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionList.toArray(new String[0]), 1002);
        } else {
            sp = getSharedPreferences("CONFIG_INFO", MODE_PRIVATE);//获取 SharedPreferences对象
            editor = sp.edit(); // 获取编辑器对象
            if (readDate(this, "wifi_ip") == null) {
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

    public ExamModelOneXValueFormatter(ArrayList<String> list) {
        this.list = list;
    }
    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        int values = (int) value;
        if (values <= 0 || values >= list.size()) {
            return "";
        }
        return list.get(values);
    }
}
/*数据值格式化器*/
class NoValueFormatter implements IValueFormatter {
    @Override
    public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
        return ""; // 返回空字符串，不显示任何值
    }
}
@SuppressLint("ViewConstructor")
class CustomMarkerView extends MarkerView {

    private final TextView m_year;
    private final TextView m_time;
    private final TextView m_value;
    public CustomMarkerView (Context context, int layoutResource) {
        super(context, layoutResource);
        m_year = findViewById(R.id.m_year);
        m_time = findViewById(R.id.m_time);
        m_value = findViewById(R.id.m_value);
    }
    @SuppressLint("SetTextI18n")
    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        String [] _tmp = MainActivity._min_bat_list.get((int) e.getX()).split(" ");
        m_year.setText(_tmp[0]);
        m_time.setText(_tmp[1]);
        m_value.setText("电压值:" + e.getY());
        setOffset(-((float) getWidth() /2), (-getHeight() - 50));
        super.refreshContent(e, highlight);
    }
}