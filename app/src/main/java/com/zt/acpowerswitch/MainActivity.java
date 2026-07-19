package com.zt.acpowerswitch;

import static android.widget.Toast.LENGTH_SHORT;
import static com.zt.acpowerswitch.TCPClient.socket;
import static com.zt.acpowerswitch.WifiListActivity.wifilist;
import android.Manifest;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.DefaultValueFormatter;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.MPPointF;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.scwang.smart.refresh.header.MaterialHeader;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

public class MainActivity extends AppCompatActivity{
    public static final String TAG = "MainActivity:";
    public final String top_m = "ComponentInfo{com.zt.acpowerswitch/com.zt.acpowerswitch.MainActivity}";
    public static SharedPreferences sp;
    public static SharedPreferences.Editor editor;
    public ImageView origin_menu_bt,card_menu_bt;
    public long lastBack = 0;
    public static final TCPClient tcpClient = new TCPClient();
    public static String[] info;
    public static String tcpServerAddress;
    public static int tcpServerPort;
    public static boolean data_rec_finish, stop_send,Thread_Run,isPaused;
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
    public int date_num;
    private ComponentName topActivity;
    public static LineDataSet bat_lineDataSet,mem_lineDataSet;
    public static int page_refresh_time;
    private boolean isMemChartInitialized = false;
    public SmartRefreshLayout smartRefreshLayout;
    private boolean request_homepage_run;
    public static int year,month,day;
    Map<String, String> uiData = new HashMap<>();
    private float lastCapValue = -1f;
    private float startX = 0f;
    private float startY = 0f;
    private ViewSwitcher viewSwitcher;
    // ===== 交流输出 =====
    private TextView originOutVoltage, cardOutVoltage;
    private TextView originOutCurrent, cardOutCurrent;
    private TextView originPowerKw, cardPowerKw;
    private TextView originSjPowerKw, cardSjPowerKw;
    private TextView originPf, cardPf;
    private TextView originOutFrequency, cardOutFrequency;
    private TextView originOutMode, cardOutMode;
    private TextView originLoadRateValue, cardLoadRateValue;

    // ===== 光伏输入 =====
    private TextView originSunVoltageValue, cardSunVoltageValue;
    private TextView originCurrentDirection, cardCurrentDirection;
    private TextView originLeCurrent, cardLeCurrent;
    private TextView originPvPowerResult, cardPvPowerResult;

    // ===== 电池系统 =====
    private TextView originBatVoltage, cardBatVoltage;
    private TextView originBatOutCurrent, cardBatOutCurrent;
    private TextView originBatHealthCap, cardBatHealthCap;
    private TextView originBat_use_time, cardBat_use_time;

    // ===== 温度 & 风扇 =====
    private TextView originTemp0Value, cardTemp0Value;
    private TextView originTemp1Value, cardTemp1Value;
    private TextView originFanValue, cardFanValue;

    // ===== 电池统计 =====
    private TextView originPvCharged, cardPvCharged;
    private TextView originTvRollover, cardTvRollover;
    private TextView originTvCharged, cardTvCharged;
    private TextView originTvDischarged, cardTvDischarged;
    private TextView originTvAvailable, cardTvAvailable;

    // ===== 图表 =====
    private LineChart originBatLineChart, cardBatLineChart;
    private PieChart originPieChart, cardPieChart;
    private BarChart originPowerChart, cardPowerChart;
    private LineChart originMemUseChart, cardMemUseChart;

    // ===== 图表上的时,日,月,年按钮 ======
    private TextView origin_hour_power,card_hour_power;
    private TextView origin_day_power,card_day_power;
    private TextView origin_month_power,card_month_power;
    private TextView origin_year_power,card_year_power;

    // ===== 系统信息 =====
    private TextView originMmUse, cardMmUse;
    private TextView originDevIpPort, cardDevIpPort;

    // ===== MarkerView =====
    private CustomMarkerView originMarker, cardMarker;
    private ObjectAnimator originBreathAnim,cardBreathAnim;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // ===== ViewSwitcher =====
        viewSwitcher = findViewById(R.id.viewSwitcher);
        // 恢复显示模式
        SharedPreferences sp = getSharedPreferences("ui", MODE_PRIVATE);
        int mode = sp.getInt("mode", 0); // 0 = 经典，1 = 卡片
        viewSwitcher.setDisplayedChild(mode);
        // ===== 经典布局（child 0）=====
        View originalView = viewSwitcher.getChildAt(0);

        // 交流输出
        originOutVoltage = originalView.findViewById(R.id.out_Voltage);
        originOutCurrent = originalView.findViewById(R.id.out_Current);
        originPowerKw = originalView.findViewById(R.id.power_kw);
        originSjPowerKw = originalView.findViewById(R.id.sj_power_kw);
        originPf = originalView.findViewById(R.id.PF);
        originOutFrequency = originalView.findViewById(R.id.out_frequency);
        originOutMode = originalView.findViewById(R.id.out_mode);
        originLoadRateValue = originalView.findViewById(R.id.load_rate_value);

        // 光伏输入
        originSunVoltageValue = originalView.findViewById(R.id.sun_voltage_value);
        originCurrentDirection = originalView.findViewById(R.id.current_direction);
        originLeCurrent = originalView.findViewById(R.id.le_current);
        originPvPowerResult = originalView.findViewById(R.id.pv_power_result);

        // 电池系统
        originBatVoltage = originalView.findViewById(R.id.bat_Voltage);
        originBatOutCurrent = originalView.findViewById(R.id.bat_out_current);
        originBatHealthCap = originalView.findViewById(R.id.bat_health_cap);
        originBat_use_time = originalView.findViewById(R.id.bat_use_time);

        // 温度 & 风扇
        originTemp0Value = originalView.findViewById(R.id.temp0_value);
        originTemp1Value = originalView.findViewById(R.id.temp1_value);
        originFanValue = originalView.findViewById(R.id.fan_value);

        // 电池统计
        originPvCharged = originalView.findViewById(R.id.pv_charged);
        originTvRollover = originalView.findViewById(R.id.tv_rollover);
        originTvCharged = originalView.findViewById(R.id.tv_charged);
        originTvDischarged = originalView.findViewById(R.id.tv_discharged);
        originTvAvailable = originalView.findViewById(R.id.tv_available);

        // 图表
        originBatLineChart = originalView.findViewById(R.id.line_chart);
        originPieChart = originalView.findViewById(R.id.pieChart);
        originPowerChart = originalView.findViewById(R.id.power_chart);
        originMemUseChart = originalView.findViewById(R.id.mem_use_chart);
        origin_hour_power = originalView.findViewById(R.id.hour_power);
        origin_day_power = originalView.findViewById(R.id.day_power);
        origin_month_power = originalView.findViewById(R.id.month_power);
        origin_year_power = originalView.findViewById(R.id.year_power);

        // 系统信息
        originMmUse = originalView.findViewById(R.id.mm_use);
        originDevIpPort = originalView.findViewById(R.id.dev_ip_port);

        // Marker（✅ 已修复）
        originMarker = new CustomMarkerView(this, R.layout.custom_marker_view, originBatLineChart);
        originMarker.setChartView(originBatLineChart);

        // image
        origin_menu_bt = originalView.findViewById(R.id.menu_img);

        // ===== 卡片布局（child 1）=====
        View cardView = viewSwitcher.getChildAt(1);

        // 交流输出
        cardOutVoltage = cardView.findViewById(R.id.out_Voltage);
        cardOutCurrent = cardView.findViewById(R.id.out_Current);
        cardPowerKw = cardView.findViewById(R.id.power_kw);
        cardSjPowerKw = cardView.findViewById(R.id.sj_power_kw);
        cardPf = cardView.findViewById(R.id.PF);
        cardOutFrequency = cardView.findViewById(R.id.out_frequency);
        cardOutMode = cardView.findViewById(R.id.out_mode);
        cardLoadRateValue = cardView.findViewById(R.id.load_rate_value);

        // 光伏输入
        cardSunVoltageValue = cardView.findViewById(R.id.sun_voltage_value);
        cardCurrentDirection = cardView.findViewById(R.id.current_direction);
        cardLeCurrent = cardView.findViewById(R.id.le_current);
        cardPvPowerResult = cardView.findViewById(R.id.pv_power_result);

        // 电池系统
        cardBatVoltage = cardView.findViewById(R.id.bat_Voltage);
        cardBatOutCurrent = cardView.findViewById(R.id.bat_out_current);
        cardBatHealthCap = cardView.findViewById(R.id.bat_health_cap);
        cardBat_use_time = cardView.findViewById(R.id.bat_use_time);

        // 温度 & 风扇
        cardTemp0Value = cardView.findViewById(R.id.temp0_value);
        cardTemp1Value = cardView.findViewById(R.id.temp1_value);
        cardFanValue = cardView.findViewById(R.id.fan_value);

        // 电池统计
        cardPvCharged = cardView.findViewById(R.id.pv_charged);
        cardTvRollover = cardView.findViewById(R.id.tv_rollover);
        cardTvCharged = cardView.findViewById(R.id.tv_charged);
        cardTvDischarged = cardView.findViewById(R.id.tv_discharged);
        cardTvAvailable = cardView.findViewById(R.id.tv_available);

        // 图表
        cardBatLineChart = cardView.findViewById(R.id.line_chart);
        cardPieChart = cardView.findViewById(R.id.pieChart);
        cardPowerChart = cardView.findViewById(R.id.power_chart);
        cardMemUseChart = cardView.findViewById(R.id.mem_use_chart);
        card_hour_power = cardView.findViewById(R.id.hour_power);
        card_day_power = cardView.findViewById(R.id.day_power);
        card_month_power = cardView.findViewById(R.id.month_power);
        card_year_power = cardView.findViewById(R.id.year_power);

        // 系统信息
        cardMmUse = cardView.findViewById(R.id.mm_use);
        cardDevIpPort = cardView.findViewById(R.id.dev_ip_port);

        // Marker（✅ 已修复）
        cardMarker = new CustomMarkerView(this, R.layout.custom_marker_view, cardBatLineChart);
        cardMarker.setChartView(cardBatLineChart);

        // image
        card_menu_bt = cardView.findViewById(R.id.menu_img);
    }
    private void init_module(){
        Calendar calendar = Calendar.getInstance();
        year = calendar.get(Calendar.YEAR);       // 年
        month = calendar.get(Calendar.MONTH) + 1; // 月 (注意要+1)
        day = calendar.get(Calendar.DAY_OF_MONTH); // 日
        smartRefreshLayout = findViewById(R.id.refreshLayout);
        //设置 Header 为 贝塞尔雷达 样式
        smartRefreshLayout.setRefreshHeader(new MaterialHeader(this));
        smartRefreshLayout.setOnRefreshListener(refreshLayout -> {
            if (originBatLineChart.getData() != null || cardBatLineChart.getData() != null) {
                originBatLineChart.clear();//清除15分钟图表
                cardBatLineChart.clear();//清除15分钟卡片图表

                originBatLineChart.invalidate(); // 使改变生效
                cardBatLineChart.invalidate(); // 使改变生效
            }
            if (originPowerChart.getData() != null || cardPowerChart.getData() != null){
                originPowerChart.clear();//清除小时图表
                cardPowerChart.clear();//清除小时card图表

                originPowerChart.invalidate(); // 使改变生效
                cardPowerChart.invalidate(); // 使改变生效
            }
            about.log(TAG, "下拉刷新");
            if (socket!=null) {
                request_homepage_date();
            }
        });
        date_num = getCurrentMonthLastDay();
        tcpServerAddress = readDate(this, "wifi_ip");
        tcpServerPort = Integer.parseInt(readDate(this, "tcpServerPort"));
        page_refresh_time = request_delay_ms();
        proEsp32Text(originDevIpPort);
        proEsp32Text(cardDevIpPort);
        origin_menu_bt.setOnClickListener(view -> {
            goAnim(this, 50);
            MainActivity.this.showPopupMenu(origin_menu_bt);
        });
        card_menu_bt.setOnClickListener(view -> {
            goAnim(this, 50);
            MainActivity.this.showPopupMenu(card_menu_bt);
        });

        //小时图表按键监听
        bt_listen(origin_hour_power,originPowerChart,_H_Total_power, "小时柱状图表", "暂无小时数据");
        bt_listen(card_hour_power,cardPowerChart,_H_Total_power, "小时柱状图表", "暂无小时数据");
        //日期图表按键监听
        bt_listen(origin_day_power,originPowerChart,_D_Total_power, "日期柱状图表", "暂无日期数据");
        bt_listen(card_day_power,cardPowerChart,_D_Total_power, "日期柱状图表", "暂无日期数据");
        //月份图表按键监听
        bt_listen(origin_month_power,originPowerChart,_M_Total_power, "月份柱状图表", "暂无月份数据");
        bt_listen(card_month_power,cardPowerChart,_M_Total_power, "月份柱状图表", "暂无月份数据");
        //年图表按键监听
        bt_listen(origin_year_power,originPowerChart,_Y_Total_power, "年份柱状图表", "暂无年份数据");
        bt_listen(card_year_power,cardPowerChart,_Y_Total_power, "年份柱状图表", "暂无年份数据");

        originBatLineChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry entry, Highlight highlight) {
                originBatLineChart.setMarkerView(originMarker);
            }
            @Override
            public void onNothingSelected() {
                // 可以不做处理
            }
        });
        cardBatLineChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry entry, Highlight highlight) {
                cardBatLineChart.setMarkerView(cardMarker);
            }
            @Override
            public void onNothingSelected() {
                // 可以不做处理
            }
        });
        start_Thread();
    }
    public void bt_listen(TextView bt,BarChart chart,ArrayList<String> data, String text, String data_null){
        bt.setOnClickListener(view -> {
            goAnim(MainActivity.this, 50);
            chart.clear();//清除origin图表
            chart.invalidate(); // 使origin改变生效
            if (!data.isEmpty()) {
                pro_day_chart_data(data,text,chart);//把数据放到柱状图上
            }else{
                chart.setNoDataText(data_null);
            }
        });
    }
    public void start_Thread(){
        new Thread(() -> {
            while (!Thread_Run) {
                // 现在它在子线程运行，不会再报 NetworkOnMainThreadException 了
                if (tcpClient.tcpConnect() && !Thread_Run) {
                    about.log(TAG, "开始调用线程");
                    mData_pro_thread();
                    break;
                }
            }
        }).start();
        about.log(TAG, "线程调用完成");
        if (originBatLineChart.isEmpty() || originPowerChart.isEmpty() || cardBatLineChart.isEmpty() || cardPowerChart.isEmpty()) {
            new Thread(() -> {
                while (socket == null || originOutVoltage.getText().toString().isEmpty() || cardOutVoltage.getText().toString().isEmpty()) {
                    try {
                        Thread.sleep(100); // 每次检查休眠 100ms，降低 CPU 占用
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (!request_homepage_run && socket!=null) {
                    request_homepage_run=true;
                    request_homepage_date();
                }
            }).start();
        }
    }
    public void proEsp32Text(TextView Dev) {
        Dev.setOnLongClickListener(view -> {
            goAnim(MainActivity.this, 50);
            new AlertDialog.Builder(this)
                    .setTitle("注意:")
                    .setMessage("该操作将重置逆变器的网络,如果你不在逆变器旁边,请慬慎执行!!!")
                    .setPositiveButton("取消", null)
                    .setNegativeButton("执行", (dialogInterface, i) -> {
                        goAnim(MainActivity.this, 50);
                        if (send_command_to_server("del_wifi_config")) {
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
                                        tcpClient.close();
                                    }).show();
                        } else {
                            new AlertDialog.Builder(this)
                                    .setTitle("提 示")
                                    .setMessage("设备正忙,请稍后再试!")
                                    .setNegativeButton("完成", (dialogInterface12, i12) -> goAnim(MainActivity.this, 50)).show();
                        }
                    }).show();
            return false;
        });
    }
    //安全保存硬件参数（带 Flash 写入保护：仅当数据改变且有效时才擦写）
    private void safeSaveFlash(String[] info, int index, String key) {
        // 1. 统一阻断越界：索引必须小于数组长度
        if (info != null && index < info.length) {
            String newValue = info[index];
            String oldValue = readDate(this, key);

            // 2. 核心保护机制：只有当最新值有效，且与本地旧值不同时，才允许写入 Flash
            if (newValue != null && !newValue.equals(oldValue)) {
                saveData(key, newValue);
            }
        }
    }
    public static boolean send_command_to_server(String data) {
        CountDownLatch latch = new CountDownLatch(1); // 创建一个 CountDownLatch，初始计数为 1
        boolean[] result = {false}; // 使用数组来存储返回值
        new Thread(() -> {
            int num = 0;
            stop_send = true;
            String udp_response;
            while (num < 10) {
                udp_response = tcpClient.sendAndReceive(data);
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
    private void updateChart(float percent) {
        // 防止超过100%
        if (percent > 100) percent = 100;
        if (percent < 0) percent = 0;

        float decayPercent = 100f - percent;

        List<PieEntry> entries = new ArrayList<>();
        // 第一个是剩余容量（天蓝色），第二个是衰减（灰色）
        entries.add(new PieEntry(percent, ""));
        entries.add(new PieEntry(decayPercent, ""));

        PieDataSet dataSet = new PieDataSet(entries, "");

        int ringColor = getHealthColor(percent);
        // 颜色设置：天蓝色 (#4FC3F7) + 浅灰色 (#E0E0E0)
        dataSet.setColors(
                ringColor, //动态颜色
                Color.parseColor("#E0E0E0")   // 衰竭为灰色
        );

        // 不显示扇区上的默认数值，我们自己画中间的
        dataSet.setDrawValues(false); // 如果想在扇区上也显示百分比，设为 true
        dataSet.setValueTextSize(8f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueFormatter(new PercentFormatter()); // 格式化显示为 %

        PieData data = new PieData(dataSet);
        originPieChart.setData(data);
        cardPieChart.setData(data);

        // --- 关键：设置成圆环效果 ---
        originPieChart.setAlpha(0.7f); // 0.0~1.0，值越小越透明
        cardPieChart.setAlpha(0.7f); // 0.0~1.0，值越小越透明

        originPieChart.setDrawHoleEnabled(true);
        cardPieChart.setDrawHoleEnabled(true);

        originPieChart.setHoleRadius(70f); // 内圆大小（越小环越粗）
        cardPieChart.setHoleRadius(70f); // 内圆大小（越小环越粗）

        originPieChart.setTransparentCircleRadius(80f); // 外圈透明半径（制造图中那种浅色过渡环）
        cardPieChart.setTransparentCircleRadius(80f); // 外圈透明半径（制造图中那种浅色过渡环)

        originPieChart.setTransparentCircleColor(Color.parseColor("#88FFFFFF")); // 半透明白，模拟图中的光晕感
        cardPieChart.setTransparentCircleColor(Color.parseColor("#88FFFFFF")); // 半透明白，模拟图中的光晕感

        // --- 中间文字 ---
        originPieChart.setCenterText(generateCenterText(percent));
        cardPieChart.setCenterText(generateCenterText(percent));

        originPieChart.setCenterTextSize(8f);
        cardPieChart.setCenterTextSize(8f);

        originPieChart.setCenterTextColor(Color.parseColor("#333333")); // 深灰色文字
        cardPieChart.setCenterTextColor(Color.parseColor("#333333")); // 深灰色文字

        originPieChart.setCenterTextTypeface(Typeface.DEFAULT_BOLD);
        cardPieChart.setCenterTextTypeface(Typeface.DEFAULT_BOLD);

        // 其他配置
        originPieChart.getDescription().setEnabled(false);
        cardPieChart.getDescription().setEnabled(false);

        originPieChart.getLegend().setEnabled(false);
        cardPieChart.getLegend().setEnabled(false);

        originPieChart.setTouchEnabled(false); // 禁用触摸
        cardPieChart.setTouchEnabled(false); // 禁用触摸
        // 动画
        originPieChart.animateY(1000);
        cardPieChart.animateY(1000);

        if (percent <= 0) {
            startBreathAnimation();
        } else {
            stopBreathAnimation();
        }

        originPieChart.invalidate();
        cardPieChart.invalidate();
    }

    // 生成中间的文字
    private SpannableString generateCenterText(float percent) {
        String text;
        if (percent <= 0) {
            text = "⚠️ \n待校准";
        } else {
            text = String.format(Locale.getDefault(), "可用电量\n%.1f%%", percent);
        }

        SpannableString s = new SpannableString(text);

        int firstLineEnd = text.indexOf('\n');
        boolean hasSecondLine = firstLineEnd >= 0;

        // ---- 第一行 ----
        int line1End = hasSecondLine ? firstLineEnd : text.length();
        s.setSpan(new StyleSpan(Typeface.BOLD), 0, line1End, 0);
        s.setSpan(new RelativeSizeSpan(1.3f), 0, line1End, 0);

        // ---- 第二行（仅当有换行时） ----
        if (hasSecondLine) {
            int secondLineStart = firstLineEnd + 1;
            int secondLineEnd = text.length();

            s.setSpan(new RelativeSizeSpan(1.3f), secondLineStart, secondLineEnd, 0);
            s.setSpan(new StyleSpan(Typeface.BOLD), secondLineStart, secondLineEnd, 0);
        }

        return s;
    }
    // 启动呼吸动画
    private void startBreathAnimation() {
        // 原始布局
        originPieChart.setAlpha(1.0f);
        originBreathAnim = ObjectAnimator.ofFloat(originPieChart, "alpha", 0.2f, 1.0f);
        originBreathAnim.setDuration(1200);
        originBreathAnim.setRepeatCount(ObjectAnimator.INFINITE);
        originBreathAnim.setRepeatMode(ObjectAnimator.REVERSE);
        originBreathAnim.start();

        // 卡片布局
        cardPieChart.setAlpha(1.0f);
        cardBreathAnim = ObjectAnimator.ofFloat(cardPieChart, "alpha", 0.2f, 1.0f);
        cardBreathAnim.setDuration(1200);
        cardBreathAnim.setRepeatCount(ObjectAnimator.INFINITE);
        cardBreathAnim.setRepeatMode(ObjectAnimator.REVERSE);
        cardBreathAnim.start();
    }
    // 停止呼吸动画
    private void stopBreathAnimation() {
        if (originBreathAnim != null) {
            originBreathAnim.cancel();
            originBreathAnim = null;
        }
        if (cardBreathAnim != null) {
            cardBreathAnim.cancel();
            cardBreathAnim = null;
        }
    }
    //圆环动态颜色
    private int getHealthColor(float socPercent) {
        if (socPercent >= 80f) {
            return Color.parseColor("#4CAF50"); // 绿色：充裕
        } else if (socPercent >= 20f) {
            return Color.parseColor("#FF9800"); // 橙色：提醒
        } else {
            return Color.parseColor("#F44336"); // 红色：告急
        }
    }
    private void mData_pro_thread() {
        new Thread(() -> {
            Thread_Run = true;
            while (!isPaused) {
                if (!stop_send){
                    String udp_response = tcpClient.sendAndReceive("get_info");
                    sleep(page_refresh_time);
                    if (udp_response != null && udp_response.startsWith("['AC_voltage:")) {
                        Log.i(TAG, "数据内容: " + udp_response );
                        String modifiedString = udp_response.substring(1, udp_response.length() - 1);
                        modifiedString = modifiedString.replace("'", "").replace(",", ":").replace(" ", "");
                        info = modifiedString.split(":");
                        if (info.length >= 46) {
                            DecimalFormat df = new DecimalFormat("#.##");
                            Float sj_power = 0.0F;
                            //交流电压
                            uiData.put("ac_voltage", info[1]);
                            String ac = info[1];
                            //交流电流
                            Float jl_dl = Float.parseFloat(info[3]);
                            String formattedValue_iv_Value = df.format(jl_dl);
                            uiData.put("ac_current", formattedValue_iv_Value);
                            String iv = info[3];
                            //交流有功功率
                            uiData.put("ac_power", info[5]);
                            //交流视在功率
                            if (ac != null) {
                                sj_power = Float.parseFloat(ac) * Float.parseFloat(iv);
                                String formattedValue = df.format(sj_power);
                                uiData.put("sj_power", formattedValue);
                            }
                            //功率因数
                            String pf_value = df.format(Float.parseFloat(info[5]) / sj_power);
                            uiData.put("power_ys", pf_value);
                            //交流频率
                            uiData.put("ac_freq", info[7] + " hz");
                            //负载使用率
                            if (unicodeToString(info[17]).equals("逆变供电")) {
                                String power_use = df.format((sj_power / Float.parseFloat(info[21]) * 100)) + " %"; //这里使用功率切换阈值作为最大功率
                                uiData.put("power_use", power_use);
                            } else {
                                uiData.put("power_use", "无限制");
                            }
                            //储能电池电压
                            uiData.put("bat_voltage", info[9]);
                            //光伏板电压
                            uiData.put("pv_voltage", info[11]);
                            //光伏板电流
                            if (Float.parseFloat(info[13]) < 0.6) { //防止夜晚功率计算错误
                                info[13] = String.valueOf(0);
                            }
                            Float PV_I = Float.parseFloat(info[13]) * Float.parseFloat(info[9]) / Float.parseFloat(info[11]);
                            String pv_current = df.format(PV_I);
                            uiData.put("pv_current", pv_current);
                            //光伏实时输出功率
                            Float pv_result = Float.parseFloat(info[11]) * PV_I;
                            String pv_power = df.format(pv_result);
                            uiData.put("光伏实时输出功率", pv_power);
                            //逆变器不同模式下电池的充放电电流计算
                            //充放电电流计算,其中的30为逆变器开启时自身功耗的估算,3.6为逆变器关闭时控制板功耗的估算,0.9为逆变器的转换效率
                            float pw = Float.parseFloat(pv_power);//太阳能板的发电功率
                            if (unicodeToString(info[17]).equals("逆变供电")) {
                                //逆变供电模式下,逆变器为开启状态的充放电电流计算
                                if (pw - ((Float.parseFloat(info[5])/0.9 + 30)) > 0) {
                                    uiData.put("修改电池充放电电流text", "\uD83D\uDCA7 充电电流(A):");
                                    uiData.put("修改电池充放电电流值", df.format((pw - (Float.parseFloat(info[5])/0.9 + 30)) / Float.parseFloat(info[9])));
                                } else {
                                    uiData.put("修改电池充放电电流text", "\uD83D\uDCA7 放电电流(A):");
                                    uiData.put("修改电池充放电电流值", df.format(((Float.parseFloat(info[5])/0.9 + 30) - pw) / Float.parseFloat(info[9])));
                                }
                            } else if (unicodeToString(info[17]).equals("市电供电")) {
                                //市电供电模式下,逆变器为关闭状态的充放电电流计算
                                if ((pw - 3.6) > 0) {
                                    uiData.put("修改电池充放电电流text", "\uD83D\uDCA7 无逆变充电电流(A):");
                                    uiData.put("修改电池充放电电流值", df.format((pw - 3.6) / Float.parseFloat(info[9]))); //3.6w为估算值,具体要测量才知道
                                } else {
                                    uiData.put("修改电池充放电电流text", "\uD83D\uDCA7 无逆变放电电流(A):");
                                    uiData.put("修改电池充放电电流值", df.format(3.6 / Float.parseFloat(info[9])));//3.6w为估算值,具体要测量才知道
                                }
                            } else if (unicodeToString(info[17]).equals("电池电压过低")) {
                                //电池电压过低,逆变器为关闭状态的充放电电流计算
                                if ((pw - 3.6) > 0) {
                                    uiData.put("修改电池充放电电流text", "\uD83D\uDCA7 无逆变充电电流(A):");
                                    uiData.put("修改电池充放电电流值", df.format((pw - 3.6) / Float.parseFloat(info[9]))); //3.6w为估算值,具体要测量才知道
                                } else {
                                    uiData.put("修改电池充放电电流text", "\uD83D\uDCA7 无逆变放电电流(A):");
                                    uiData.put("修改电池充放电电流值", df.format(3.6 / Float.parseFloat(info[9])));//3.6w为估算值,具体要测量才知道
                                }
                            } else if (unicodeToString(info[17]).equals("固定逆变模式")) {
                                //固定逆变模式下,逆变器为开启状态的充放电电流计算
                                if (pw - ((Float.parseFloat(info[5])/0.9 + 30)) > 0) {
                                    uiData.put("修改电池充放电电流text", "\uD83D\uDCA7 充电电流(A):");
                                    uiData.put("修改电池充放电电流值", df.format((pw - (Float.parseFloat(info[5])/0.9 + 30)) / Float.parseFloat(info[9])));
                                } else {
                                    uiData.put("修改电池充放电电流text", "\uD83D\uDCA7 放电电流(A):");
                                    uiData.put("修改电池充放电电流值", df.format(((Float.parseFloat(info[5])/0.9 + 30) - pw) / Float.parseFloat(info[9])));
                                }
                            } else if (unicodeToString(info[17]).equals("固定市电模式")) {
                                //固定市电模式下,逆变器为关闭状态的充放电电流计算
                                if ((pw - 3.6) > 0) {
                                    uiData.put("修改电池充放电电流text", "\uD83D\uDCA7 无逆变充电电流(A):");
                                    uiData.put("修改电池充放电电流值", df.format((pw - 3.6) / Float.parseFloat(info[9]))); //3.6w为估算值,具体要测量才知道
                                } else {
                                    uiData.put("修改电池充放电电流text", "\uD83D\uDCA7 无逆变放电电流(A):");
                                    uiData.put("修改电池充放电电流值", df.format(3.6 / Float.parseFloat(info[9])));//3.6w为估算值,具体要测量才知道
                                }
                            }
                            //为MPTT散热片温度
                            uiData.put("mptt温度", info[15] + "°C");
                            //当前输出模式
                            if (unicodeToString(info[17]).equals("电池电压过低")){
                                uiData.put("当前输出模式", "电池低压");
                            }else if (unicodeToString(info[17]).equals("固定市电模式")){
                                uiData.put("当前输出模式", "固定市电");
                            }else if (unicodeToString(info[17]).equals("固定逆变模式")){
                                uiData.put("当前输出模式", "固定逆变");
                            }else{
                                uiData.put("当前输出模式", unicodeToString(info[17]));
                            }
                            //内存使用信息
                            uiData.put("内存使用信息", info[19]);
                            //市电切换阈值
                            safeSaveFlash(info, 21, "power");
                            //电池低于此值则市电常开
                            safeSaveFlash(info, 23, "low_voltage");
                            //输出模式
                            safeSaveFlash(info, 25, "out_mode");
                            //主功率板散执片风扇开启温度
                            safeSaveFlash(info, 27, "mos_temp");
                            //主功率板散热片实时温度
                            String raw = info[29];
                            String readable = raw.replace("\\xb0", "°");
                            uiData.put("散热片实时温度", readable);
                            //主功率板散热风扇转速值
                            uiData.put("散热风扇转速值", info[31]);
                            //开启逆变的电压阈值
                            safeSaveFlash(info, 33, "open_pv_value");
                            uiData.put("光伏发电度数计量", info[35]);
                            uiData.put("电池充电度数计量", info[37]);
                            uiData.put("电池放电度数计量", info[39]);
                            uiData.put("电池健康度计量", info[41]);
                            uiData.put("电池总容量计量", info[43]);
                            uiData.put("电池可用容量计量", info[45]);

                            Message message = messageProHandler.obtainMessage();
                            message.what = 1;
                            message.obj = uiData;  // 将计算结果放入Message
                            messageProHandler.sendMessage(message);
                        }else{
                            about.log("TAG", "收到的数据长度不正确");
                        }
                    }
                    if (checkScreenStatus() && udp_response != null && udp_response.startsWith("live>") && udp_response.contains("mark3")){
                        about.log(TAG, "收到实时分时数据,更新分时图表");
                        //live>16:30 26.8,39.9,1.5#h>15 0.03,mark3
                        String[] str = udp_response.split("#");
                        String[] min = str[0].split(">");
                        String[] _f = min[1].split(" ");
                        String[] _s = _f[0].split(":");
                        String h = String.format(Locale.getDefault(),"%02d", Integer.parseInt(_s[0]));
                        String m = String.format(Locale.getDefault(),"%02d", Integer.parseInt(_s[1]));
                        String _min = h+":"+m+" "+_f[1];
                        _min_bat_list.add(_min);
                        pro_min_chart_data(_min_bat_list, "每15分钟电压",originBatLineChart);
                        pro_min_chart_data(_min_bat_list, "每15分钟电压",cardBatLineChart);
                        if (!h.equals("00") && !str[1].contains("none")) {
                            String[] h_ = str[1].split(">");
                            String[] hour = h_[1].split(",");
                            _H_Total_power.add(hour[0]);
                            pro_day_chart_data(_H_Total_power, "小时柱状图表",originPowerChart);
                            pro_day_chart_data(_H_Total_power, "小时柱状图表",cardPowerChart);
                        }
                    }
                    if (!checkScreenStatus()) {
                        about.log(TAG, "屏幕关闭");
                        tcpClient.close();
                        isPaused=true;
                    }
                }
            }
            isPaused=false;
            Thread_Run = false;
            about.log(TAG, "数据更新线程巳退出");
        }).start();
    }
    @SuppressLint("HandlerLeak")
    Handler messageProHandler = new Handler(Looper.getMainLooper()) {
        @SuppressLint({"SetTextI18n", "DefaultLocale"})
        public void handleMessage(Message msg) {
            if (msg.what == 1 && msg.obj instanceof Map) {
                Map<String, String> uiData = (Map<String, String>) msg.obj;
                //交流电压
                originOutVoltage.setText(uiData.get("ac_voltage"));
                cardOutVoltage.setText(uiData.get("ac_voltage"));
                //交流电流
                originOutCurrent.setText(uiData.get("ac_current"));
                cardOutCurrent.setText(uiData.get("ac_current"));
                //交流有功功率
                originPowerKw.setText(uiData.get("ac_power"));
                cardPowerKw.setText(uiData.get("ac_power"));
                //交流视在功率
                originSjPowerKw.setText(uiData.get("sj_power"));
                cardSjPowerKw.setText(uiData.get("sj_power"));
                //功率因数
                originPf.setText(uiData.get("power_ys"));
                cardPf.setText(uiData.get("power_ys"));
                //交流频率
                originOutFrequency.setText(uiData.get("ac_freq"));
                cardOutFrequency.setText(uiData.get("ac_freq"));
                //负载使用率
                originLoadRateValue.setText(uiData.get("power_use"));
                cardLoadRateValue.setText(uiData.get("power_use"));
                //电池电压
                originBatVoltage.setText(uiData.get("bat_voltage"));
                cardBatVoltage.setText(uiData.get("bat_voltage"));
                //光伏电压
                originSunVoltageValue.setText(uiData.get("pv_voltage"));
                cardSunVoltageValue.setText(uiData.get("pv_voltage"));
                //太阳能电流
                originLeCurrent.setText(uiData.get("pv_current"));
                cardLeCurrent.setText(uiData.get("pv_current"));
                //光伏实时输出功率
                originPvPowerResult.setText(uiData.get("光伏实时输出功率"));
                cardPvPowerResult.setText(uiData.get("光伏实时输出功率"));
                //为逆变模式时修改计算电池的充放电电流文本
                originCurrentDirection.setText(uiData.get("修改电池充放电电流text"));
                if (Objects.equals(uiData.get("修改电池充放电电流text"), "\uD83D\uDCA7 无逆变充电电流(A):")){
                    cardCurrentDirection.setText("无逆变充电电流");
                }else if (Objects.equals(uiData.get("修改电池充放电电流text"), "\uD83D\uDCA7 无逆变放电电流(A):")){
                    cardCurrentDirection.setText("无逆变放电电流");
                }else if (Objects.equals(uiData.get("修改电池充放电电流text"), "\uD83D\uDCA7 充电电流(A):")){
                    cardCurrentDirection.setText("充电电流");
                }else if (Objects.equals(uiData.get("修改电池充放电电流text"), "\uD83D\uDCA7 放电电流(A):")){
                    cardCurrentDirection.setText("放电电流");
                }
                //为逆变模式时计算电池的充放电电流
                originBatOutCurrent.setText(uiData.get("修改电池充放电电流值"));
                cardBatOutCurrent.setText(uiData.get("修改电池充放电电流值"));
                //为MPTT散热片温度
                originTemp0Value.setText(uiData.get("mptt温度"));
                cardTemp0Value.setText(uiData.get("mptt温度"));
                //当前输出模式
                originOutMode.setText(uiData.get("当前输出模式"));
                cardOutMode.setText(uiData.get("当前输出模式"));
                //内存使用信息
                mem_data_display_to_chart(uiData.get("内存使用信息"),originMmUse);
                mem_data_display_to_chart(uiData.get("内存使用信息"),cardMmUse);
                //主功率板散热片实时温度
                originTemp1Value.setText(uiData.get("散热片实时温度"));
                cardTemp1Value.setText(uiData.get("散热片实时温度"));
                //主功率板散热风扇转速值
                originFanValue.setText(uiData.get("散热风扇转速值"));
                cardFanValue.setText(uiData.get("散热风扇转速值"));
                //电池充放电信息表
                float p_charged = Float.parseFloat(Objects.requireNonNull(uiData.get("光伏发电度数计量")));
                float charged = Float.parseFloat(Objects.requireNonNull(uiData.get("电池充电度数计量")));
                float discharged = Float.parseFloat(Objects.requireNonNull(uiData.get("电池放电度数计量")));
                float total_cap = Float.parseFloat(Objects.requireNonNull(uiData.get("电池总容量计量")));
                float available_cap = Float.parseFloat(Objects.requireNonNull(uiData.get("电池可用容量计量")));

                originPvCharged.setText(String.format("☀️ 今日光伏发电: %.3f kWh", p_charged));
                cardPvCharged.setText(String.format("☀️ 今日光伏发电: %.3f kWh", p_charged));

                originTvRollover.setText(String.format("⛽️ 今日电池充电: %.3f kWh", charged));
                cardTvRollover.setText(String.format("⛽️ 今日电池充电: %.3f kWh", charged));

                originTvCharged.setText(String.format("⚡ 今日电池放电: %.3f kWh", discharged));
                cardTvCharged.setText(String.format("⚡ 今日电池放电: %.3f kWh", discharged));
                if (total_cap==0){
                    originTvDischarged.setText("📋 电池总容量: 待校准");
                    cardTvDischarged.setText("📋 电池总容量: 待校准");
                }else {
                    originTvDischarged.setText(String.format("📋 电池总容量: %.3f kWh", total_cap));
                    cardTvDischarged.setText(String.format("📋 电池总容量: %.3f kWh", total_cap));
                }
                if (total_cap==0){
                    originTvAvailable.setText("🔋 电池可用电量: 待校准");
                    cardTvAvailable.setText("🔋 电池可用电量: 待校准");
                }else {
                    originTvAvailable.setText(String.format("🔋 电池可用电量: %.3f kWh", available_cap));
                    cardTvAvailable.setText(String.format("🔋 电池可用电量: %.3f kWh", available_cap));
                }
                //计算电池可用时长
                // 光伏实时输出功率（W）
                float pvPowerAc = Float.parseFloat(Objects.requireNonNull(uiData.get("光伏实时输出功率")));
                // 负载交流有功功率（W）
                float loadPowerAc = Float.parseFloat(Objects.requireNonNull(uiData.get("ac_power")));
                // 逆变器参数
                float invEff = 0.9f;
                float invSelfConsumption = 30f;
                // 电池可用电量（Wh）
                float availableCapWh = available_cap * 1000;
                // 系统总交流消耗（用于判断是否充电）
                float totalAcLoad = loadPowerAc + invSelfConsumption;
                String useTimeStr;
                if (pvPowerAc >= totalAcLoad) {
                    // 光伏够用，电池不放电
                    useTimeStr = "充电中";
                } else {
                    // 光伏不足，电池需要放电
                    // 交流缺口折算到直流侧
                    float dcDischargePower = (totalAcLoad - pvPowerAc) / invEff;

                    if (dcDischargePower <= 0) {
                        useTimeStr = "无需放电";
                    } else {
                        double hours = availableCapWh / dcDischargePower;
                        long totalMinutes = Math.round(hours * 60);
                        long h = totalMinutes / 60;
                        long m = totalMinutes % 60;
                        useTimeStr = String.format("%d时%02d分", h, m);
                    }
                }
                originBat_use_time.setText(useTimeStr);
                cardBat_use_time.setText(useTimeStr);
                //计算电池健康度
                float bat_healthy_value = Float.parseFloat(Objects.requireNonNull(uiData.get("电池健康度计量")));
                if (bat_healthy_value > 0){
                    if (bat_healthy_value >= 90){
                        originBatHealthCap.setText("优秀");
                        cardBatHealthCap.setText("优秀");
                    }else if (bat_healthy_value >= 85){
                        originBatHealthCap.setText("良好");
                        cardBatHealthCap.setText("良好");
                    }else if (bat_healthy_value >= 80){
                        originBatHealthCap.setText("预警");
                        cardBatHealthCap.setText("预警");
                    }else{
                        originBatHealthCap.setText("严重衰减");
                        cardBatHealthCap.setText("严重衰减");
                    }
                }else if (bat_healthy_value < 0){
                    originBatHealthCap.setText("校准中...");
                    cardBatHealthCap.setText("校准中...");
                }else {
                    originBatHealthCap.setText("暂未校准");
                    cardBatHealthCap.setText("暂未校准");
                }
                // 在数据更新回调里
                if (total_cap==0){
                    float invalidate = -2;
                    if (invalidate != lastCapValue) {
                        lastCapValue = invalidate;
                        updateChart(lastCapValue);
                    }
                }else {
                    float batValue = Float.parseFloat(String.format("%.1f", available_cap / total_cap * 100));
                    if (batValue != lastCapValue) {
                        lastCapValue = batValue;
                        updateChart(lastCapValue);
                    }
                }
            }
        }
    };
    private void request_homepage_date() {
        new Thread(() -> {
            pro_data_request();//请求数据
            if (!_min_bat_list.isEmpty() && getTopActivity().toString().equals(top_m) && checkScreenStatus() && data_rec_finish) {
                pro_min_chart_data(_min_bat_list, "每15分钟电压",originBatLineChart);//把数据放到折线图上
                pro_min_chart_data(_min_bat_list, "每15分钟电压",cardBatLineChart);//把数据放到折线图上
                about.log(TAG, "15分钟刷新完成");
            }else{
                originBatLineChart.setNoDataText("暂无分时数据");
                cardBatLineChart.setNoDataText("暂无分时数据");
            }
            if (!_H_Total_power.isEmpty() && getTopActivity().toString().equals(top_m) && checkScreenStatus() && data_rec_finish) {
                pro_day_chart_data(_H_Total_power,"小时柱状图表",originPowerChart);//把数据放到柱状图上
                pro_day_chart_data(_H_Total_power,"小时柱状图表",cardPowerChart);//把数据放到柱状图上
                about.log(TAG, "小时柱状图刷新完成");
            }else{
                originPowerChart.setNoDataText("暂无小时数据");
                cardPowerChart.setNoDataText("暂无小时数据");
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
        stop_send = true;
        data_rec_finish=false;
        about.log(TAG, "获取所有历史记录数据");
        String result = tcpClient.sendAndReceive("get_all_file");
        // 如果结果不为 null，则拆分并赋值；否则给一个空数组或 null
        String[] all_data = (result != null) ? result.split("\n") : new String[0];
        for (String line : all_data) {
            if (line != null && line.contains("f>")) {
                //Log.i(TAG, "发现包含分时的数据: " + line);
                String[] _l = line.split(">"); //按>进行分隔
                String[] _f = _l[1].split(" ");

                String[] _s = _f[0].split(":");
                String h = String.format(Locale.getDefault(),"%02d", Integer.parseInt(_s[0]));
                String m = String.format(Locale.getDefault(),"%02d", Integer.parseInt(_s[1]));
                String min = h+":"+m+" "+_f[1];
                _min_bat_list.add(min);
            } else if (line != null && line.contains("h>")) {
                //Log.i(TAG, "发现包含小时的数据: " + line);
                String[] _l = line.split(">"); //按>进行分隔
                _H_Total_power.add(_l[1]);
            } else if (line != null && line.contains("d>")) {
                //Log.i(TAG, "发现包含每天的数据: " + line);
                String[] _l = line.split(">"); //按>进行分隔
                _D_Total_power.add(year+"-"+_l[1]);
            } else if (line != null && line.contains("m>")) {
                //Log.i(TAG, "发现包含每月的数据: " + line);
                String[] _l = line.split(">"); //按>进行分隔
                _M_Total_power.add(year+"-"+_l[1]);
            } else if (line != null && line.contains("y>")) {
                //Log.i(TAG, "发现包含每年的数据: " + line);
                String[] _l = line.split(">"); //按>进行分隔
                String[] _j = _l[1].split(" ");

                String[] _n = _j[0].split("-");
                int y = 2000 + Integer.parseInt(_n[0]);
                String m = String.format(Locale.getDefault(),"%02d", Integer.parseInt(_n[1]));
                String d = String.format(Locale.getDefault(),"%02d", Integer.parseInt(_n[2]));
                String _y = y+"-"+m+"-"+d+" "+ _j[1];
                _Y_Total_power.add(_y);
            } else if (line != null && line.contains("debug>")) {
                //Log.i(TAG, "发现包含调试的数据: " + line);
                String[] _l = line.split(">"); //按>进行分隔
                debugList.add(_l[1]);
            } else if (line != null && line.contains("mark2")) {
                //Log.i(TAG, "发现包含结尾的数据: " + line);
                about.log(TAG, "所有数据接收完成,分时数据数量:" + _min_bat_list.size() + " 小时平均功率数据数量:" + _H_Total_power.size() +
                        " 日功率数据数量:" + _D_Total_power.size() + " 月功率数据数量:" + _M_Total_power.size() + " 年功率数据数量:" + _Y_Total_power.size());
                data_rec_finish = true;
            }
        }
        if (!data_rec_finish){
            about.log(TAG, "数据不完整,再次请求!");
            sleep(2000);
            pro_data_request();
        }
        stop_send = false;
        request_homepage_run = false;
        smartRefreshLayout.finishRefresh();
    }
    @SuppressLint("DefaultLocale")
    public void pro_min_chart_data(List<String> _sd, String label, LineChart Chart) {
        if (label.equals("每15分钟电压")) {
            String minute_des = "";
            _time_value.clear();
            _value_list.clear();
            // 👇 新增：用于记录最高值和对应的 Entry
            Entry maxEntry = null;
            float maxValue = -Float.MAX_VALUE;
            for (int i = 0; i < _sd.size(); i++) {
                String[] _e = _sd.get(i).split(" ");
                minute_des = _e[0]; //分时时间
                _time_value.add(minute_des.split(":")[0]);
                String[] _u = _e[1].split(",");
                _value_list.add(new Entry(i, Float.parseFloat(_u[0])));
                //寻找光伏最大功率
                float pv = Float.parseFloat(_u[2]) * Float.parseFloat(_u[0]) / Float.parseFloat(_u[1]);
                float max_pv_power = pv * Float.parseFloat(_u[1]);
                if (max_pv_power > maxValue) {
                    maxValue = max_pv_power;
                    maxEntry = new Entry(i, Float.parseFloat(_u[0]));
                }
            }
            bat_data_display_to_chart(originBatLineChart, _time_value, _value_list, minute_des);
            bat_data_display_to_chart(cardBatLineChart, _time_value, _value_list, minute_des);
            // 👇 新增：在数据填充后，为图表绑定自定义红点渲染器
            if (maxEntry != null) {
                MyLineChartRenderer customRenderer = new MyLineChartRenderer(
                        Chart,
                        Chart.getAnimator(),
                        Chart.getViewPortHandler(),
                        maxEntry
                );
                Chart.setRenderer(customRenderer);
            }
        }
    }
    @SuppressLint("DefaultLocale")
    public void pro_day_chart_data(List<String> _sd, String label, BarChart Chart){
        if (label.equals("小时柱状图表")) {
            String begin_time = "";
            String over_time = "";
            String last_power = "";
            float total_power = 0.0F;
            _barChart_list.clear();
            for (int i = 0; i < _sd.size(); i++) {
                String[] _e = _sd.get(i).split(" ");
                String current_power = _e[1]; // 当前项的功率值
                total_power += Float.parseFloat(current_power);
                last_power = current_power;
                if (_sd.size() > 1) {
                    if (i == 0) {
                        begin_time = "今日: " + _e[0] + ":00:00" + "  ->  ";
                    } else if (i == _sd.size() - 1) {
                        over_time = _e[0]+ ":00:00";
                    }
                }else{
                    begin_time = "今日: " + _e[0]+ ":00:00";
                    over_time = "";
                }
                _barChart_list.add(new BarEntry(Integer.parseInt(_e[0]), Float.parseFloat(String.format("%.2f", Float.parseFloat(last_power)))));
            }
            String total_power_str = String.format("%.2f", total_power);
            pro_date_power_data(Chart, _barChart_list,"前一小时用电量:("+ String.format("%.2f", Float.parseFloat(last_power)) +" kWh) | " + "今日目前共计用电量:("+ total_power_str + " kWh)",begin_time  + over_time,"小时");
        }
        if (label.equals("日期柱状图表")) {
            String begin_time = "";
            String over_time = "";
            String last_power = "";
            float total_power = 0.0F;
            _barChart_list.clear();
            for (int i = 0; i < _sd.size(); i++) {
                String[] _e = _sd.get(i).split(" ");
                String current_power = _e[1]; // 当前项的功率值
                total_power += Float.parseFloat(current_power);
                last_power = current_power;
                if (_sd.size() > 1) {
                    if (i == 0) {
                        begin_time = _e[0] + "  ->  ";
                    } else if (i == _sd.size() - 1) {
                        over_time = _e[0];
                    }
                }else{
                    begin_time = _e[0];
                    over_time = "";
                }
                _barChart_list.add(new BarEntry(Integer.parseInt(_e[0].split("-")[2]), Float.parseFloat(String.format("%.2f",Float.parseFloat(last_power)))));
            }
            String total_power_str = String.format("%.2f", total_power);
            pro_date_power_data(Chart,_barChart_list,"昨日用电量:("+ String.format("%.2f", Float.parseFloat(last_power)) +" kWh) | " + "本月目前共计用电量:("+ total_power_str + " kWh)",begin_time + over_time,"日期");
        }
        if (label.equals("月份柱状图表")) {
            String begin_time = "";
            String over_time = "";
            String last_power = "";
            float total_power = 0.0F;
            _barChart_list.clear();
            for (int i = 0; i < _sd.size(); i++) {
                String[] _e = _sd.get(i).split(" ");
                String current_power = _e[1]; // 当前项的功率值
                total_power += Float.parseFloat(current_power);
                last_power = current_power;
                if (_sd.size() > 1) {
                    if (i == 0) {
                        begin_time = _e[0].split("-")[0] + "-" + _e[0].split("-")[1]+"月"+"  ->  ";
                    } else if (i == _sd.size() - 1) {
                        over_time = _e[0].split("-")[0] + "-" + _e[0].split("-")[1]+"月";
                    }
                }else{
                    begin_time = _e[0].split("-")[0] + "-" + _e[0].split("-")[1]+"月";
                    over_time = "";
                }
                _barChart_list.add(new BarEntry(Integer.parseInt(_e[0].split("-")[1]), Float.parseFloat(String.format("%.2f",Float.parseFloat(last_power)))));
            }
            String total_power_str = String.format("%.2f", total_power);
            pro_date_power_data(Chart,_barChart_list,"上月用电量:("+ String.format("%.2f", Float.parseFloat(last_power)) +" kWh) | " + "本年度目前共计用电量:("+ total_power_str + " kWh)",begin_time + over_time,"月份");
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
                        begin_time = _e[0].split("-")[0]+"年"+"  ->  ";
                    } else if (i == _sd.size() - 1) {
                        over_time = _e[0].split("-")[0];
                        last_power = _e[1];
                    }
                }else{
                    begin_time = _e[0].split("-")[0]+"年";
                    over_time = "";
                    last_power = _e[1];
                }
                _barChart_list.add(new BarEntry(Integer.parseInt(_e[0].split("-")[0]),  Float.parseFloat(String.format("%.2f",Float.parseFloat(_e[1])))));
            }
            pro_date_power_data(Chart,_barChart_list,"上一年用电量:("+ String.format("%.2f", Float.parseFloat(last_power)) +" kWh)",begin_time + over_time,"年份");
        }
    }

    @SuppressLint("SetTextI18n")
    public void mem_data_display_to_chart(String _s, TextView MmUse){
        float _mem = Float.parseFloat(_s);

        // 1. 更新内存使用百分比（这部分很快，可以保留在主线程）
        DecimalFormat decimalFormat = new DecimalFormat("#.0");
        String formattedValue = decimalFormat.format(_mem/150 * 100);
        MmUse.setText(formattedValue + "%");

        // 2. 优化后的图表更新逻辑
        updateMemoryChart(_mem);
    }

    /**
     * 优化的内存图表更新方法
     */
    private void updateMemoryChart(float memValue) {
        // 如果图表未初始化，先进行初始化
        if (!isMemChartInitialized) {
            initMemoryChart(originMemUseChart);
            initMemoryChart(cardMemUseChart);
        }
        // 添加新数据点
        if (mem_lineDataSet != null) {
            addMemDataPoint(originMemUseChart,memValue);
            addMemDataPoint(cardMemUseChart,memValue);
        }
    }

    /**
     * 初始化内存图表（只执行一次）
     */
    private void initMemoryChart(LineChart chart) {
        _mem_use_list = new ArrayList<>();
        _mem_use_list.add(new Entry(0, 0)); // 初始点

        mem_lineDataSet = new LineDataSet(_mem_use_list, "设备内存使用情况");
        mem_lineDataSet.setValueFormatter(new NoValueFormatter());
        mem_lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        mem_lineDataSet.setDrawCircles(false);
        mem_lineDataSet.setLineWidth(1.5f);
        mem_lineDataSet.setDrawFilled(true);
        mem_lineDataSet.setFillColor(Color.parseColor("#98EBFC")); // 浅绿色填充
        mem_lineDataSet.setColor(Color.parseColor("#98EBFC")); // 绿色线条

        // 配置图表属性（只配置一次）
        chart.getDescription().setText(" ");
        chart.setExtraTopOffset(5f);
        chart.getXAxis().setEnabled(false);
        // X 轴网格
        chart.getXAxis().setGridColor(Color.GRAY);
        chart.getXAxis().setGridColor(0x26808080); // ✅ 隐约可见
        // 左 Y 轴网格
        chart.getAxisLeft().setGridColor(Color.GRAY);
        chart.getAxisLeft().setGridColor(0x26808080);
        // 这里不为false的话,网格线设置不起作用
        chart.getAxisRight().setDrawGridLines(false);// 右 Y 轴（通常关掉)
        chart.getAxisRight().setEnabled(false);// 直接禁用右侧 Y 轴
        chart.setTouchEnabled(false);
        chart.getXAxis().setAxisMinimum(0f);
        chart.getXAxis().setAxisMaximum(100f);
        chart.getAxisLeft().setAxisMinimum(0f);
        chart.getAxisLeft().setAxisMaximum(160f);
        chart.getAxisRight().setAxisMinimum(0f);
        chart.getAxisRight().setAxisMaximum(160f);
        // 设置动画
        chart.animateY(1000);
        LineData data = new LineData(mem_lineDataSet);
        chart.setData(data);
        isMemChartInitialized = true;
    }

    /**
     * 添加新的数据点（高效更新）
     */
    private void addMemDataPoint(LineChart chart, float memValue) {
        if (chart.getData() != null && chart.getData().getDataSetCount() > 0) {
            LineDataSet set = (LineDataSet) chart.getData().getDataSetByIndex(0);

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
                chart.getXAxis().setAxisMinimum(0f);
                chart.getXAxis().setAxisMaximum(Math.max(dataCount, 100f));
            }

            // 通知图表更新
            chart.getData().notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.invalidate();
        }
    }
    @SuppressLint("ClickableViewAccessibility")
    public void bat_data_display_to_chart(LineChart chart,ArrayList<String> time_value, ArrayList<Entry> bat_list_value, String des){
        String[] bat_value = String.valueOf(_value_list.get(_value_list.size()-1)).split(":");
        bat_lineDataSet = new LineDataSet(bat_list_value, "最近一次更新电压为: " + bat_value[2] + " v");
        bat_lineDataSet.setValueFormatter(new NoValueFormatter());//使用自定义的值格式化器
        bat_lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);//这里是圆滑曲线
        bat_lineDataSet.setDrawCircles(false);//在点上画圆 默认true
        bat_lineDataSet.setLineWidth(2f);//设置线条的宽度，最大10f,最小0.2f
        bat_lineDataSet.setHighlightEnabled(true);
        bat_lineDataSet.setDrawHighlightIndicators(true);
        bat_lineDataSet.setHighLightColor(Color.RED); // 十字线颜色
        bat_lineDataSet.setHighlightLineWidth(0.8f);   // 十字线粗细
        bat_lineDataSet.setDrawVerticalHighlightIndicator(true);   // 垂直线
        bat_lineDataSet.setDrawHorizontalHighlightIndicator(true); // 水平线
        bat_lineDataSet.enableDashedHighlightLine(10f, 20f, 0f); // 设置为虚线：线长10，间距5，偏移0

        LineData bat_data = new LineData(bat_lineDataSet);
        chart.getXAxis().setValueFormatter(new ExamModelOneXValueFormatter(time_value));//X轴时间显示
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getDescription().setText("最后更新时间: "+des+":00");//右下角描述
        chart.getDescription().setTextSize(9f);
        chart.setExtraTopOffset(10f);//顶部数据距离边框距离
        chart.getXAxis().setTextSize(10f); //设置顶部文字大小
        // X 轴网格
        chart.getXAxis().setGridColor(Color.GRAY);
        chart.getXAxis().setGridColor(0x26808080); // ✅ 隐约可见
        // 左 Y 轴网格
        chart.getAxisLeft().setGridColor(Color.GRAY);
        chart.getAxisLeft().setGridColor(0x26808080);
        // 这里不为false的话,网格线设置不起作用
        chart.getAxisRight().setDrawGridLines(false);// 右 Y 轴（通常关掉）
        chart.getAxisRight().setEnabled(false);// 直接禁用右侧 Y 轴
        chart.getXAxis().setAxisMinimum(0f);
        chart.getXAxis().setAxisMaximum(95f);
        chart.getXAxis().setSpaceMax(1.5f); //额外给 X 轴右侧虚设 1.5 个单位的空白缓冲区
        chart.getAxisLeft().setAxisMinimum(20f);//左侧Y轴最小值
        chart.getAxisLeft().setAxisMaximum(30f);//左侧Y轴最大值
        chart.setData(bat_data);//调置数据
        chart.setScaleEnabled(false); // 彻底禁用缩放（最强力开关）
        chart.setDoubleTapToZoomEnabled(false); // 禁用双击缩放（很多时候是这个在起作用）
        chart.setDragEnabled(true); // 必须启用拖拽（否则你设置了 setVisibleXRangeMaximum 后无法滑动查看）
        chart.setScaleXEnabled(false); // 如果你想针对单轴（保险起见）
        chart.setScaleYEnabled(false);
        chart.setPinchZoom(false);// 禁用捏合缩放
        chart.notifyDataSetChanged();//通知数据巳改变
        chart.invalidate();//清理无效数据,用于动态刷新
        // 强制拦截父布局手势，防止滑动坐标时“断线”
        // 强制拦截父布局手势，防止滑动坐标时“断线”
        chart.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // 1. 记录按下的初始绝对坐标
                    startX = event.getRawX();
                    startY = event.getRawY();
                    // 按下时先默认不拦截，等待滑动方向明确
                    if (v.getParent() != null) {
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    // 2. 计算当前位置与按下位置的绝对距离
                    float distanceX = Math.abs(event.getRawX() - startX);
                    float distanceY = Math.abs(event.getRawY() - startY);

                    // 3. 判断是否为明显的横向滑动（横向位移大于纵向位移，且超过防误触阈值）
                    if (distanceX > distanceY && distanceX > 10) {
                        if (v.getParent() != null) {
                            // 确认是横向滑动，强制禁止父布局拦截
                            v.getParent().requestDisallowInterceptTouchEvent(true);
                        }
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // 4. 手指抬起，恢复父布局拦截权限
                    if (v.getParent() != null) {
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                    }
                    break;
            }
            return false; // 返回 false，让 MPAndroidChart 内部继续处理高亮十字线手势
        });
    }
    /**
     * 初始化BarChart图表
     */
    @SuppressLint({"DefaultLocale", "ClickableViewAccessibility"})
    private void pro_date_power_data(BarChart carChart,ArrayList<BarEntry> barChart, String label, String des, String type) {
        //X轴设置显示位置在底部
        carChart.fitScreen();
        XAxis xAxis = carChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        YAxis leftAxis = carChart.getAxisLeft();//左侧Y轴保留两位小数
        leftAxis.setValueFormatter((value, axis) -> {
            return String.format("%.2f", value); // value 就是坐标轴上的数值
        });
        carChart.getAxisRight().setEnabled(false);
        // X 轴网格
        carChart.getXAxis().setGridColor(Color.GRAY);
        carChart.getXAxis().setGridColor(0x26808080); // ✅ 隐约可见
        // 左 Y 轴网格
        carChart.getAxisLeft().setGridColor(Color.GRAY);
        carChart.getAxisLeft().setGridColor(0x26808080);
        // 这里不为false的话,网格线设置不起作用
        carChart.getAxisRight().setDrawGridLines(false);// 右 Y 轴（通常关掉)
        switch (type) {
            case "小时":
                xAxis.setAxisMinimum(-0.5f);
                xAxis.setAxisMaximum(23.5f);
                carChart.getAxisLeft().setAxisMinimum(0f);//左侧Y轴最小值
                break;
            case "日期":
                xAxis.setAxisMinimum(1-0.5f);
                xAxis.setAxisMaximum(date_num+0.5f);
                carChart.getAxisLeft().setAxisMinimum(0f);//左侧Y轴最小值
                break;
            case "月份":
                xAxis.setAxisMinimum(0.5f);
                xAxis.setAxisMaximum(12.5f);
                carChart.getAxisLeft().setAxisMinimum(0f);//左侧Y轴最小值
                break;
            case "年份":
                xAxis.setAxisMinimum(2025-0.5f);
                xAxis.setAxisMaximum(2055+0.5f);
                carChart.getAxisLeft().setAxisMinimum(0f);//左侧Y轴最小值
                break;
        }
        xAxis.setGranularity(1f);
        BarData barData = getBarData(barChart, label);
        barData.setValueTextSize(8f);//柱状图顶部文字大小
        barData.setValueTypeface(Typeface.DEFAULT_BOLD);//顶部文字加粗
        barData.setValueTextColor(Color.DKGRAY);
        barData.setValueFormatter(new DefaultValueFormatter(2));
        barData.setBarWidth(0.92f);//柱状图的分隔宽度
        carChart.getDescription().setText(des);//右下角描述
        carChart.getDescription().setTextSize(9f);
        carChart.setData(barData);//调置数据
        carChart.setDoubleTapToZoomEnabled(false);// 禁用双击缩放
        carChart.setScaleXEnabled(false); // 允许水平缩放（或设为 false 仅允许滑动）
        carChart.setScaleYEnabled(false); // 禁止垂直缩放，防止 Y 轴乱跳
        carChart.setDragEnabled(true); // 必须开启，否则无法滑动查看后面的数据
        carChart.setVisibleXRangeMaximum(18f);
        int count = barData.getEntryCount();
        if (count > 18) {
            int x = count - 18;
            carChart.moveViewToX((carChart.getLowestVisibleX() + x) + 1);
        }
        carChart.notifyDataSetChanged();//通知数据巳改变
        carChart.invalidate();//清理无效数据,用于动态刷新
    }
    @NonNull
    private static BarData getBarData(ArrayList<BarEntry> barChart, String label) {
        BarDataSet dataSet = new BarDataSet(barChart, label);
        dataSet.setValueFormatter((value, entry, dataSetIndex, viewPortHandler) -> String.format(Locale.getDefault(), "%.2f", value));// 自定义值格式
        dataSet.setColors(Color.parseColor("#C5FD87"),
                Color.parseColor("#F8F989"),
                Color.parseColor("#F7D48C"),
                Color.parseColor("#98EBFC")); // 设置柱子的颜色
        dataSet.setDrawValues(true); //是否绘制柱状图顶部的数值
        dataSet.setValueTextSize(6f);
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

        MenuItem switchItem = popupMenu.getMenu().findItem(R.id.switch_card_mode);
        if (switchItem != null) {
            boolean isCardMode = (viewSwitcher.getDisplayedChild() == 1);
            switchItem.setTitle(isCardMode ? "经典模式" : "卡片模式");
        }
        //点击事件
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.switch_card_mode) {
                if (viewSwitcher != null) {
                    viewSwitcher.showNext();
                    int mode = viewSwitcher.getDisplayedChild();

                    getSharedPreferences("ui", MODE_PRIVATE)
                            .edit()
                            .putInt("mode", mode)
                            .apply();
                }
            } else if (itemId == R.id.other_option) {
                goAnim(this, 50);
                startActivity(new Intent(this, otherOption.class));
            } else if (itemId == R.id.about) {
                goAnim(this, 50);
                startActivity(new Intent(this, about.class));
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
            if (readDate(this, "wifi_ip") == null || readDate(this, "tcpServerPort") == null) {
                Intent intent = new Intent(this, set_tcp_page.class);
                startActivity(intent);
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
    private final Chart chart;
    private final TextView m_year,m_time,m_value,pv_voltage,pv_current,pv_power;
    public CustomMarkerView (Context context, int layoutResource, Chart chart) {
        super(context, layoutResource);
        m_year = findViewById(R.id.m_year);
        m_time = findViewById(R.id.m_time);
        m_value = findViewById(R.id.m_value);
        pv_voltage = findViewById(R.id.pv_voltage);
        pv_current = findViewById(R.id.pv_current);
        pv_power = findViewById(R.id.pv_power);
        this.chart = chart;
        setChartView(chart);
    }
    @SuppressLint("SetTextI18n")
    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        DecimalFormat df = new DecimalFormat("#.##");
        String [] _tmp = MainActivity._min_bat_list.get((int) e.getX()).split(" ");
        m_year.setText(" " + MainActivity.year +"-"+ MainActivity.month+ "-" +MainActivity.day);
        m_time.setText(" " + _tmp[0]+":00");
        String [] all_data = _tmp[1].split(",");
        m_value.setText(" 电池电压:" + all_data[0]);
        pv_voltage.setText(" 光伏电压:" + all_data[1]);
        if (Float.parseFloat(all_data[2]) < 0.6){
            all_data[2] = String.valueOf(0);
        }
        Float _pv_i = Float.parseFloat(all_data[2]) * Float.parseFloat(all_data[0])/Float.parseFloat(all_data[1]);
        String _pv_i_value = df.format(_pv_i);
        pv_current.setText(" 光伏电流:" + _pv_i_value);
        Float _pv_power = Float.parseFloat(all_data[1])* _pv_i;
        String pv_power_result = df.format(_pv_power);
        pv_power.setText(" 光伏功率:"+ pv_power_result);

        super.refreshContent(e, highlight);
    }
    @Override
    public MPPointF getOffsetForDrawingAtPoint(float posX, float posY) {
        MPPointF offset = new MPPointF();
        offset.x = -(getWidth() / 2f);
        offset.y = -getHeight() - 20; // 默认：Marker 在数据点正上方

        // 防止超出左边界
        if (posX + offset.x < 0) {
            offset.x = -posX + 8; // 左边留 8px 边距
        }

        // 防止超出右边界
        if (chart != null && posX + offset.x + getWidth() > chart.getWidth()) {
            offset.x = chart.getWidth() - posX - getWidth() - 8; // 右边留 8px 边距
        }

        // 防止超出上边界（新增）
        if (posY + offset.y < 0) {
            offset.y = -posY + 8; // 顶部留 8px 边距
        }

        // 防止超出下边界（新增）
        if (chart != null && posY + offset.y + getHeight() > chart.getHeight()) {
            offset.y = chart.getHeight() - posY - getHeight() - 8; // 底部留 8px 边距
        }

        // 右上角碰到圆环 → 翻到下方（保留你之前的业务逻辑）
        if (chart != null && posX > chart.getWidth() * 0.6f && posY < chart.getHeight() * 0.4f) {
            offset.y = 20; // 翻到下方
        }

        // 左上角碰到圆环 → 翻到下方（新增，对称处理）
        if (chart != null && posX < chart.getWidth() * 0.4f && posY < chart.getHeight() * 0.4f) {
            offset.y = 20; // 翻到下方
        }

        return offset;
    }
}
