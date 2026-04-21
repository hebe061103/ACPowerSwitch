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
import com.github.mikephil.charting.utils.MPPointF;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.scwang.smart.refresh.header.MaterialHeader;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class MainActivity extends AppCompatActivity{
    public static final String TAG = "MainActivity:";
    public final String top_m = "ComponentInfo{com.zt.acpowerswitch/com.zt.acpowerswitch.MainActivity}";
    public static SharedPreferences sp;
    public static SharedPreferences.Editor editor;
    public ImageView menu_bt;
    @SuppressLint("StaticFieldLeak")
    public static ImageView mark_status;
    public long lastBack = 0;
    public static final UDPClient udpClient = new UDPClient();
    private TextView out_Voltage,out_Current,power_kw,sj_power_kw,pf,out_frequency,out_mode,bat_Voltage,bat_out_current,current_direction,temp1_value,load_rate_value,sun_voltage_value,le_current,pv_power_result,temp0_value,fan_value,mm_use;
    public static String[] info;
    public static String udpServerAddress;
    public static int udpServerPort=55555;
    public static boolean udp_connect,data_rec_finish, stop_send,Thread_Run,Conn_status,isPaused;
    public static ArrayList<String> _min_bat_list = new ArrayList<>();
    public static ArrayList<String> _H_Total_power = new ArrayList<>();
    public static ArrayList<String> _D_Total_power = new ArrayList<>();
    public static ArrayList<String> _M_Total_power = new ArrayList<>();
    public static ArrayList<String> _Y_Total_power = new ArrayList<>();
    public ArrayList<String> _time_value = new ArrayList<>();
    public ArrayList<Entry> _value_list = new ArrayList<>();
    public ArrayList<BarEntry> _barChart_list = new ArrayList<>();
    public ArrayList <Entry> _mem_use_list = new ArrayList<>();
    public static ArrayList<String> debugList = new ArrayList<>();
    public LineChart bat_line_chart,mem_use_chart;
    public BarChart power_chart;
    public int date_num;
    private ComponentName topActivity;
    public static LineDataSet bat_lineDataSet,mem_lineDataSet;
    public static int page_refresh_time;
    private boolean isMemChartInitialized = false;

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
        pf = findViewById(R.id.PF);
        out_frequency = findViewById(R.id.out_frequency);
        out_mode = findViewById(R.id.out_mode);
        sun_voltage_value = findViewById(R.id.sun_voltage_value);
        current_direction = findViewById(R.id.current_direction);
        temp0_value = findViewById(R.id.temp0_value);//temp0_value为主控板散热片温度
        temp1_value =  findViewById(R.id.temp1_value);//temp1_value定义为主功率板散热片温度
        fan_value = findViewById(R.id.fan_value);//定义为主功率板散热风扇转速值
        load_rate_value = findViewById(R.id.load_rate_value);
        le_current = findViewById(R.id.le_current);
        pv_power_result = findViewById(R.id.pv_power_result);
        bat_Voltage = findViewById(R.id.bat_Voltage);
        bat_out_current = findViewById(R.id.bat_out_current);
        bat_line_chart = findViewById(R.id.line_chart);
        power_chart = findViewById(R.id.power_chart);
        mem_use_chart = findViewById(R.id.mem_use_chart);
        mm_use = findViewById(R.id.mm_use);
        TextView dev_ip_port = findViewById(R.id.dev_ip_port);
        dev_ip_port.setOnLongClickListener(view -> {
            goAnim(MainActivity.this, 50);
            new AlertDialog.Builder(this)
                    .setTitle("注意:")
                    .setMessage("该操作将重置逆变器的网络,如果你不在逆变器旁边,请慬慎执行!!!")
                    .setPositiveButton("取消",null)
                    .setNegativeButton("执行", (dialogInterface, i) -> {
                        goAnim(MainActivity.this, 50);
                        if(send_command_to_server("del_wifi_config")) {
                            new AlertDialog.Builder(this)
                                    .setTitle("提 示")
                                    .setMessage("重置成功")
                                    .setNegativeButton("完成", (dialogInterface1, i1) -> {
                                        goAnim(MainActivity.this, 50);
                                        deleteData("power");
                                        deleteData("low_voltage");
                                        deleteData("out_mode");
                                        deleteData("mos_temp");
                                        deleteData("open_pv_value");
                                        deleteData("wifi_ip");
                                        deleteData("refresh_time");
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
        //小时柱状图长按动作,清除服务端小时历史数据
        hour_power.setOnLongClickListener(view -> {
            goAnim(MainActivity.this, 50);
            new AlertDialog.Builder(this)
                    .setTitle("注意:")
                    .setMessage("是否清除服务端小时历史数据,请慬慎执行!")
                    .setPositiveButton("取消",null)
                    .setNegativeButton("确定", (dialogInterface, i) -> {
                        goAnim(MainActivity.this, 50);
                        if(send_command_to_server("del_file:H_Total_power")) {
                            new AlertDialog.Builder(this)
                                    .setTitle("提 示")
                                    .setMessage("删除完成")
                                    .setNegativeButton("完成", (dialogInterface1, i1) -> goAnim(MainActivity.this, 50)).show();
                        }else{
                            new AlertDialog.Builder(this)
                                    .setTitle("提 示")
                                    .setMessage("设备正忙,请稍后再试!")
                                    .setNegativeButton("完成", (dialogInterface12, i12) -> goAnim(MainActivity.this, 50)).show();
                        }
                    }).show();
            return false;
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
        //日柱状图长按动作,清除服务端日历史数据
        day_power.setOnLongClickListener(view -> {
            goAnim(MainActivity.this, 50);
            new AlertDialog.Builder(this)
                    .setTitle("注意:")
                    .setMessage("是否清除服务端日历史数据,请慬慎执行!")
                    .setPositiveButton("取消",null)
                    .setNegativeButton("确定", (dialogInterface, i) -> {
                        goAnim(MainActivity.this, 50);
                        if(send_command_to_server("del_file:D_Total_power")) {
                            new AlertDialog.Builder(this)
                                    .setTitle("提 示")
                                    .setMessage("删除完成")
                                    .setNegativeButton("完成", (dialogInterface1, i1) -> goAnim(MainActivity.this, 50)).show();
                        }else{
                            new AlertDialog.Builder(this)
                                    .setTitle("提 示")
                                    .setMessage("设备正忙,请稍后再试!")
                                    .setNegativeButton("完成", (dialogInterface12, i12) -> goAnim(MainActivity.this, 50)).show();
                        }
                    }).show();
            return false;
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
        //月柱状图长按动作,清除服务端月历史数据
        month_power.setOnLongClickListener(view -> {
            goAnim(MainActivity.this, 50);
            new AlertDialog.Builder(this)
                    .setTitle("注意:")
                    .setMessage("是否清除服务端月历史数据,请慬慎执行!")
                    .setPositiveButton("取消",null)
                    .setNegativeButton("确定", (dialogInterface, i) -> {
                        goAnim(MainActivity.this, 50);
                        if(send_command_to_server("del_file:M_Total_power")) {
                            new AlertDialog.Builder(this)
                                    .setTitle("提 示")
                                    .setMessage("删除完成")
                                    .setNegativeButton("完成", (dialogInterface1, i1) -> goAnim(MainActivity.this, 50)).show();
                        }else{
                            new AlertDialog.Builder(this)
                                    .setTitle("提 示")
                                    .setMessage("设备正忙,请稍后再试!")
                                    .setNegativeButton("完成", (dialogInterface12, i12) -> goAnim(MainActivity.this, 50)).show();
                        }
                    }).show();
            return false;
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
        //年柱状图长按动作,清除服务端年历史数据
        year_power.setOnLongClickListener(view -> {
            goAnim(MainActivity.this, 50);
            new AlertDialog.Builder(this)
                    .setTitle("注意:")
                    .setMessage("是否清除服务端年历史数据,请慬慎执行!")
                    .setPositiveButton("取消",null)
                    .setNegativeButton("确定", (dialogInterface, i) -> {
                        goAnim(MainActivity.this, 50);
                        if(send_command_to_server("del_file:Y_Total_power")) {
                            new AlertDialog.Builder(this)
                                    .setTitle("提 示")
                                    .setMessage("删除完成")
                                    .setNegativeButton("完成", (dialogInterface1, i1) -> goAnim(MainActivity.this, 50)).show();
                        }else{
                            new AlertDialog.Builder(this)
                                    .setTitle("提 示")
                                    .setMessage("设备正忙,请稍后再试!")
                                    .setNegativeButton("完成", (dialogInterface12, i12) -> goAnim(MainActivity.this, 50)).show();
                        }
                    }).show();
            return false;
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
        about.log(TAG, "线程调用完成");
    }
    public static boolean send_command_to_server(String data) {
        CountDownLatch latch = new CountDownLatch(1); // 创建一个 CountDownLatch，初始计数为 1
        boolean[] result = {false}; // 使用数组来存储返回值
        new Thread(() -> {
            int num = 0;
            stop_send = true;
            String udp_response;
            while (num < 10) {
                udp_response = udpClient.sendAndReceive(data);
                about.log(TAG, "返回数据:" + udp_response);
                if (udp_response != null && udp_response.contains("ACK")) {
                    result[0] = true; // 设置返回值
                    break;
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
            String udp_response = "";
            while (udp_connect) {
                while (!isPaused) {
                    if (checkScreenStatus() && !stop_send) {
                        udp_response = udpClient.sendAndReceive("get_info");
                        sleep(page_refresh_time);
                    }
                    if (udp_response != null && udp_response.startsWith("['AC_voltage:")) {
                        about.log(TAG, "数据内容: " + udp_response);
                        String[] sys_udp_response = new String[]{udp_response};
                        udp_response = null;
                        Conn_status=false;
                        if (sys_udp_response[0] != null && !sys_udp_response[0].isEmpty()){
                            String modifiedString = sys_udp_response[0].substring(1, sys_udp_response[0].length() - 1);
                            modifiedString = modifiedString.replace("'", "").replace(",", ":").replace(" ", "");
                            info = modifiedString.split(":");
                            Map<String, String> uiData = new HashMap<>();
                            DecimalFormat df = new DecimalFormat("#.##");
                            Float sj_power= 0.0F;
                            //交流电压
                            uiData.put("ac_voltage", info[1]);
                            String ac = info[1];
                            //交流电流
                            Float jl_dl = Float.parseFloat(info[3]);
                            String formattedValue_iv_Value = df.format(jl_dl);
                            uiData.put("ac_current",formattedValue_iv_Value);
                            String iv = info[3];
                            //交流功率
                            uiData.put("ac_power",info[5]);
                            //交流视在功率
                            if (ac != null) {
                                sj_power = Float.parseFloat(ac) * Float.parseFloat(iv);
                                String formattedValue = df.format(sj_power);
                                uiData.put("sj_power",formattedValue);
                            }
                            //功率因数
                            String pf_value = df.format(Float.parseFloat(info[5])/sj_power);
                            uiData.put("power_ys",pf_value);
                            //交流频率
                            uiData.put("ac_freq",info[7]+" hz");
                            //负载使用率
                            if (unicodeToString(info[17]).equals("逆变供电")) {
                                String power_use = df.format((sj_power / Float.parseFloat(info[21]) * 100)) + " %"; //这里使用功率切换阈值作为最大功率
                                uiData.put("power_use",power_use);
                            }else{
                                uiData.put("power_use","市电无限制");
                            }
                            //储能电池电压
                            uiData.put("bat_voltage",info[9]);
                            //光伏板电压
                            uiData.put("pv_voltage",info[11]);
                            //光伏板电流
                            uiData.put("pv_current",info[13]);
                            //光伏实时输出功率
                            Float pv_result = Float.parseFloat(info[11])*Float.parseFloat(info[13]);
                            String pv_Value = df.format(pv_result);
                            uiData.put("光伏实时输出功率",pv_Value);
                            //逆变器不同模式下电池的充放电电流计算
                            float pw = Float.parseFloat(info[11]) * Float.parseFloat(info[13]);//太阳能板的发电功率
                            if (unicodeToString(info[17]).equals("逆变供电")) {
                                //为电池充放电电流,其中的30为逆变器自身功耗的估算,具体要测量才知道
                                if (pw - ((Float.parseFloat(info[5]) + 30)) > 0) {
                                    uiData.put("修改电池充放电电流text","\uD83D\uDCA7 电池充电电流(A):");
                                    uiData.put("修改电池充放电电流值",df.format((pw - (Float.parseFloat(info[5]) + 30)) / Float.parseFloat(info[9])));
                                } else {
                                    uiData.put("修改电池充放电电流text","\uD83D\uDCA7 电池放电电流(A):");
                                    uiData.put("修改电池充放电电流值",df.format(((Float.parseFloat(info[5]) + 30) - pw) / Float.parseFloat(info[9])));
                                }
                            }else if (unicodeToString(info[17]).equals("电池电压过低")){
                                if ((pw - 3.6) > 0) {
                                    uiData.put("修改电池充放电电流text","\uD83D\uDCA7 无逆变电池充电电流(A):");
                                    uiData.put("修改电池充放电电流值",df.format((pw - 3.6) / Float.parseFloat(info[9]))); //3.6w为估算值,具体要测量才知道
                                }
                                uiData.put("修改电池充放电电流text","\uD83D\uDCA7 无逆变电池放电电流(A):");
                                uiData.put("修改电池充放电电流值",df.format(3.6 / Float.parseFloat(info[9])));//3.6w为估算值,具体要测量才知道
                            } else{
                                //手动市电供电模式下,逆变为开启状态的时的放电电流
                                uiData.put("修改电池充放电电流text","\uD83D\uDCA7 有逆变电池放电电流(A):");
                                uiData.put("修改电池充放电电流值",df.format(30 / Float.parseFloat(info[9]))); //30w为逆变器自身功耗的估算,具体要测量才知道
                            }
                            //为MPTT散热片温度
                            uiData.put("mptt温度",info[15]+"°C");
                            //当前输出模式
                            uiData.put("当前输出模式",unicodeToString(info[17]));
                            //内存使用信息
                            uiData.put("内存使用信息",info[19]);
                            //市电切换阈值
                            saveData("power",info[21]);
                            //电池低于此值则市电常开
                            saveData("low_voltage",info[23]);
                            //输出模式
                            saveData("out_mode", info[25]);
                            //主功率板散执片风扇开启温度
                            saveData("mos_temp",info[27]);
                            //主功率板散热片实时温度
                            String raw = info[29];
                            String readable = raw.replace("\\xb0", "°");
                            uiData.put("散热片实时温度",readable);
                            //主功率板散热风扇转速值
                            uiData.put("散热风扇转速值",info[31]);
                            //开启逆变的电压阈值
                            saveData("open_pv_value",info[33]);

                            Message message = messageProHandler.obtainMessage();
                            message.what = 2;
                            message.obj = uiData;  // 将计算结果放入Message
                            messageProHandler.sendMessage(message);
                        }
                    }
                    if (getTopActivity().toString().equals(top_m) && !stop_send && !data_rec_finish && checkScreenStatus() && !Conn_status){
                        new Thread(() -> {
                            stop_send = true;
                            sleep(500);
                            request_homepage_date();
                        }).start();
                    }
                    if (!checkScreenStatus()) {
                        about.log(TAG, "屏幕关闭");
                        udpClient.close();
                        isPaused=true;
                    }
                    Message message = new Message();
                    message.what = 1;
                    messageProHandler.sendMessage(message);
                }
            }
            isPaused=false;
            Thread_Run = false;
            about.log(TAG, "数据更新线程巳退出");
        }).start();
    }
    @SuppressLint("HandlerLeak")
    Handler messageProHandler = new Handler(Looper.getMainLooper()) {
        @SuppressLint("SetTextI18n")
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                if (Conn_status) {
                    int currentVisibility = mark_status.getVisibility();
                    if (currentVisibility == View.VISIBLE) {
                        mark_status.setVisibility(View.INVISIBLE);
                        sleep(100);
                    }else{
                        mark_status.setVisibility(View.VISIBLE);
                        sleep(100);
                    }
                } else {
                    mark_status.setVisibility(View.INVISIBLE);
                }
            }else if (msg.what == 2 && msg.obj instanceof Map) {
                Map<String, String> uiData = (Map<String, String>) msg.obj;
                //交流电压
                out_Voltage.setText(uiData.get("ac_voltage"));
                //交流电流
                out_Current.setText(uiData.get("ac_current"));
                //交流有功功率
                power_kw.setText(uiData.get("ac_power"));
                //交流视在功率
                sj_power_kw.setText(uiData.get("sj_power"));
                //功率因数
                pf.setText(uiData.get("power_ys"));
                //交流频率
                out_frequency.setText(uiData.get("ac_freq"));
                //负载使用率
                load_rate_value.setText(uiData.get("power_use"));
                //电池电压
                bat_Voltage.setText(uiData.get("bat_voltage"));
                //光伏电压
                sun_voltage_value.setText(uiData.get("pv_voltage"));
                //太阳能电流
                le_current.setText(uiData.get("pv_current"));
                //光伏实时输出功率
                pv_power_result.setText(uiData.get("光伏实时输出功率"));
                //为逆变模式时计算电池的充放电电流
                current_direction.setText(uiData.get("修改电池充放电电流text"));
                bat_out_current.setText(uiData.get("修改电池充放电电流值"));
                //为MPTT散热片温度
                temp0_value.setText(uiData.get("mptt温度"));
                //当前输出模式
                out_mode.setText(uiData.get("当前输出模式"));
                //内存使用信息
                mem_data_display_to_chart(uiData.get("内存使用信息"));
                //主功率板散热片实时温度
                temp1_value.setText(uiData.get("散热片实时温度"));
                //主功率板散热风扇转速值
                fan_value.setText(uiData.get("散热风扇转速值"));
            }
        }
    };
    private void request_homepage_date() {
        new Thread(() -> {
            pro_data_request();//请求数据
            if (!_min_bat_list.isEmpty() && getTopActivity().toString().equals(top_m) && checkScreenStatus() && data_rec_finish) {
                pro_chart_data(_min_bat_list, "每15分钟电压");//把数据放到折线图上
                about.log(TAG, "15分钟刷新完成");
            }else{
                bat_line_chart.setNoDataText("暂无分时数据");
            }
            if (!_H_Total_power.isEmpty() && getTopActivity().toString().equals(top_m) && checkScreenStatus() && data_rec_finish) {
                pro_chart_data(_H_Total_power,"小时柱状图表");//把数据放到柱状图上
                about.log(TAG, "小时柱状图刷新完成");
            }else{
                power_chart.setNoDataText("暂无小时数据");
            }
        }).start();
    }
    public void pro_data_request(){
        _min_bat_list.clear();
        _H_Total_power.clear();
        _D_Total_power.clear();
        _M_Total_power.clear();
        _Y_Total_power.clear();
        debugList.clear();
        about.log(TAG, "请求全部数据");
        String udp_response = udpClient.sendAndReceive("get_all_file");
        if (udp_response!=null){
            String[] all_data = udp_response.split("\n");
            int dataLength = all_data.length;
            about.log(TAG, "数据行数: " + dataLength);
            ArrayList<String> dataList = new ArrayList<>(Arrays.asList(all_data));
            for (String line : dataList) {
                if (line != null && line.contains("min>")) {
                    //Log.i(TAG, "发现包含 min> 的数据: " + line);
                    String[] _l = line.split(">"); //按>进行分隔
                    _min_bat_list.add(_l[1]);
                }
                else if (line != null && line.contains("H_Total_power>")) {
                    //Log.i(TAG, "发现包含 H_Total_power> 的数据: " + line);
                    String[] _l = line.split(">"); //按>进行分隔
                    _H_Total_power.add(_l[1]);
                }
                else if (line != null && line.contains("D_Total_power>")) {
                    //Log.i(TAG, "发现包含 D_Total_power> 的数据: " + line);
                    String[] _l = line.split(">"); //按>进行分隔
                    _D_Total_power.add(_l[1]);
                }
                else if (line != null && line.contains("M_Total_power>")) {
                    //Log.i(TAG, "发现包含 M_Total_power> 的数据: " + line);
                    String[] _l = line.split(">"); //按>进行分隔
                    _M_Total_power.add(_l[1]);
                }
                else if (line != null && line.contains("Y_Total_power>")) {
                    //Log.i(TAG, "发现包含 Y_Total_power> 的数据: " + line);
                    String[] _l = line.split(">"); //按>进行分隔
                    _Y_Total_power.add(_l[1]);
                }
                else if (line != null && line.contains("debug>")) {
                    //Log.i(TAG, "发现包含 debug> 的数据: " + line);
                    String[] _l = line.split(">"); //按>进行分隔
                    debugList.add(_l[1]);
                }
                else if (line != null && line.contains("all_file_send_finish")) {
                    //Log.i(TAG, "发现包含 all_file_send_finish 的数据: " + line);
                    about.log(TAG, "所有数据接收完成,分时数据数量:" + _min_bat_list.size() + " 小时平均功率数据数量:" + _H_Total_power.size() +
                            " 日功率数据数量:" + _D_Total_power.size() + " 月功率数据数量:" + _M_Total_power.size() + " 年功率数据数量:" + _Y_Total_power.size());
                    data_rec_finish = true;
                    stop_send = false;
                    isPaused = false;
                }
            }
        }
        // 检查是否完成，如果没完成则递归调用自身
        if (!data_rec_finish) {
            about.log(TAG, "数据接收未完成，重新请求...");
            pro_data_request();  // 递归调用自身
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
                String[] _bat_voltage = _split_bat_value[1].split(",");
                _value_list.add(new Entry(i, Float.parseFloat(_bat_voltage[0])));
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
            pro_date_power_data(_barChart_list,"前一小时用电量统计(单位:"+ String.format("%.3f", Float.parseFloat(last_power)) +" kWh(度))",begin_time  + over_time,"小时");
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
                _barChart_list.add(new BarEntry(Integer.parseInt(_e[0].split("-")[2]), Float.parseFloat(String.format("%.1f",Float.parseFloat(_e[1])))));
            }
            pro_date_power_data(_barChart_list,"前一日用电量统计(单位:"+ String.format("%.1f", Float.parseFloat(last_power)) +" kWh(度))",begin_time + over_time,"日期");
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
                _barChart_list.add(new BarEntry(Integer.parseInt(_e[0].split("-")[1]), Float.parseFloat(String.format("%.1f",Float.parseFloat(_e[1])))));
            }
            pro_date_power_data(_barChart_list,"上一月用电量统计(单位:"+ String.format("%.1f", Float.parseFloat(last_power)) +" kWh(度))",begin_time + over_time,"月份");
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
                _barChart_list.add(new BarEntry(Integer.parseInt(_e[0].split("-")[0]),  Float.parseFloat(String.format("%.1f",Float.parseFloat(_e[1])))));
            }
            pro_date_power_data(_barChart_list,"前一年用电量统计(单位:"+ String.format("%.1f", Float.parseFloat(last_power)) +" kWh(度))",begin_time + over_time,"年份");
            power_chart.notifyDataSetChanged();//通知数据巳改变
            power_chart.invalidate();//清理无效数据,用于动态刷新
        }
    }

    @SuppressLint("SetTextI18n")
    public void mem_data_display_to_chart(String _s){
        float _mem = Float.parseFloat(_s);

        // 1. 更新内存使用百分比（这部分很快，可以保留在主线程）
        DecimalFormat decimalFormat = new DecimalFormat("#.0");
        String formattedValue = decimalFormat.format(_mem/150 * 100);
        mm_use.setText(formattedValue + "%");

        // 2. 优化后的图表更新逻辑
        updateMemoryChart(_mem);
    }

    /**
     * 优化的内存图表更新方法
     */
    private void updateMemoryChart(float memValue) {
        // 如果图表未初始化，先进行初始化
        if (!isMemChartInitialized) {
            initMemoryChart();
        }
        // 添加新数据点
        if (mem_lineDataSet != null) {
            addMemDataPoint(memValue);
        }
    }

    /**
     * 初始化内存图表（只执行一次）
     */
    private void initMemoryChart() {
        _mem_use_list = new ArrayList<>();
        _mem_use_list.add(new Entry(0, 0)); // 初始点

        mem_lineDataSet = new LineDataSet(_mem_use_list, "设备内存使用情况");
        mem_lineDataSet.setValueFormatter(new NoValueFormatter());
        mem_lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        mem_lineDataSet.setDrawCircles(false);
        mem_lineDataSet.setLineWidth(1.5f);
        mem_lineDataSet.setDrawFilled(true);
        mem_lineDataSet.setFillColor(Color.parseColor("#1A4CAF50")); // 浅绿色填充
        mem_lineDataSet.setColor(Color.parseColor("#4CAF50")); // 绿色线条

        // 配置图表属性（只配置一次）
        mem_use_chart.getDescription().setText(" ");
        mem_use_chart.setExtraTopOffset(5f);
        mem_use_chart.getXAxis().setEnabled(false);
        mem_use_chart.setTouchEnabled(false);
        mem_use_chart.getXAxis().setAxisMinimum(0f);
        mem_use_chart.getXAxis().setAxisMaximum(100f);
        mem_use_chart.getAxisLeft().setAxisMinimum(0f);
        mem_use_chart.getAxisLeft().setAxisMaximum(160f);
        mem_use_chart.getAxisRight().setAxisMinimum(0f);
        mem_use_chart.getAxisRight().setAxisMaximum(160f);

        // 设置动画
        mem_use_chart.animateY(1000);

        LineData data = new LineData(mem_lineDataSet);
        mem_use_chart.setData(data);

        isMemChartInitialized = true;
    }

    /**
     * 添加新的数据点（高效更新）
     */
    private void addMemDataPoint(float memValue) {
        if (mem_use_chart.getData() != null && mem_use_chart.getData().getDataSetCount() > 0) {
            LineDataSet set = (LineDataSet) mem_use_chart.getData().getDataSetByIndex(0);

            // 添加新点
            int newIndex = set.getEntryCount();
            set.addEntry(new Entry(newIndex, memValue));

            // 动态更新图表标签
            @SuppressLint("DefaultLocale") String label = String.format("设备内存使用情况(已使用:%.1f kb  空闲:%.1f kb)", memValue, 150 - memValue);
            set.setLabel(label);

            // 限制数据点数量（保持最近100个点）
            if (set.getEntryCount() > 100) {
                set.removeEntry(0);

                // 重新索引所有点
                List<Entry> entries = new ArrayList<>();
                for (int i = 0; i < set.getEntryCount(); i++) {
                    Entry entry = set.getEntryForIndex(i);
                    entries.add(new Entry(i, entry.getY()));
                }
                set.setValues(entries);
            }

            // 自动调整X轴范围
            int dataCount = set.getEntryCount();
            if (dataCount > 0) {
                mem_use_chart.getXAxis().setAxisMinimum(0f);
                mem_use_chart.getXAxis().setAxisMaximum(Math.max(dataCount, 100f));
            }

            // 通知图表更新
            mem_use_chart.getData().notifyDataChanged();
            mem_use_chart.notifyDataSetChanged();
            mem_use_chart.invalidate();
        }
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
        // --- 添加以下代码来强化焦点显示 ---
        // 1. 开启十字线指示器（必须）
        bat_lineDataSet.setHighlightEnabled(true);
        bat_lineDataSet.setDrawHighlightIndicators(true);
        // 2. 设置十字线的样式
        bat_lineDataSet.setHighLightColor(Color.RED); // 十字线颜色
        bat_lineDataSet.setHighlightLineWidth(0.8f);   // 十字线粗细
        // 3. 关键：设置焦点处的“准星”圆圈
        // 注意：该功能在某些版本中通过控制间隔线或自定义渲染实现
        // 最直接的方法是启用特定的指示器绘制
        bat_lineDataSet.setDrawVerticalHighlightIndicator(true);   // 垂直线
        bat_lineDataSet.setDrawHorizontalHighlightIndicator(true); // 水平线
        // 设置为虚线：线长10，间距5，偏移0
        bat_lineDataSet.enableDashedHighlightLine(10f, 20f, 0f);

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

    private final TextView pv_voltage;
    private final TextView pv_current;
    private final TextView pv_power;


    public CustomMarkerView (Context context, int layoutResource) {
        super(context, layoutResource);
        m_year = findViewById(R.id.m_year);
        m_time = findViewById(R.id.m_time);
        m_value = findViewById(R.id.m_value);
        pv_voltage = findViewById(R.id.pv_voltage);
        pv_current = findViewById(R.id.pv_current);
        pv_power = findViewById(R.id.pv_power);
    }
    @SuppressLint("SetTextI18n")
    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        DecimalFormat df = new DecimalFormat("#.##");
        String [] _tmp = MainActivity._min_bat_list.get((int) e.getX()).split(" ");
        m_year.setText(_tmp[0]);
        m_time.setText(_tmp[1]);
        String [] all_data = _tmp[2].split(",");
        m_value.setText("电压值:" + all_data[0].split(":")[1]);
        pv_voltage.setText("光伏电压:" + all_data[1].split(":")[1]);
        pv_current.setText("光伏电流:" + all_data[2].split(":")[1]);
        Float _pv_power = Float.parseFloat(all_data[1].split(":")[1])* Float.parseFloat(all_data[2].split(":")[1]);
        String pv_power_result = df.format(_pv_power);
        pv_power.setText("光伏功率:"+ pv_power_result);

        super.refreshContent(e, highlight);
    }
    /**
     * 核心：动态调整位置
     * posX, posY 是当前点击位置在屏幕上的绝对像素坐标
     */
    @Override
    public MPPointF getOffsetForDrawingAtPoint(float posX, float posY) {
        MPPointF offset = new MPPointF();

        // 1. 水平方向：默认居中
        offset.x = -(getWidth() / 2f);

        // 2. 垂直方向：默认在准星上方，并留出 40 像素的空隙（不挡住准星中心）
        offset.y = -getHeight() - 40;

        // 3. 动态避让逻辑
        // 如果上方空间不够（posY 小于气泡高度），则把气泡显示在准星下方
        if (posY + offset.y < 0) {
            offset.y = 40; // 显示在下方，40 是距离准星中心的距离
        }

        // 4. 防止左右溢出屏幕
        if (posX + offset.x < 0) {
            offset.x = -posX; // 贴着左边
        } else if (getChartView() != null && posX + getWidth() + offset.x > getChartView().getWidth()) {
            offset.x = getChartView().getWidth() - posX - getWidth(); // 贴着右边
        }

        return offset;
    }
}