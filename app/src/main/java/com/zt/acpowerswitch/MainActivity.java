package com.zt.acpowerswitch;

import static android.widget.Toast.LENGTH_SHORT;
import static com.zt.acpowerswitch.TCPClient.socket;
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
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.FrameLayout;
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
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.DefaultValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
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
    private ComponentName topActivity;
    public static LineDataSet bat_lineDataSet,mem_lineDataSet;
    public static int page_refresh_time;
    private boolean isMemChartInitialized = false;
    public SmartRefreshLayout smartRefreshLayout;
    private boolean request_homepage_run;
    public static int year,month,day;
    private final Map<String, String> uiData = new HashMap<>();
    private final DecimalFormat df = new DecimalFormat("#.##");
    private float startX = 0f;
    private float startY = 0f;
    private ViewSwitcher viewSwitcher;
    private static final int MAX_RETRY = 3;
    private static int retryCount = 0;
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
    private TextView originBatVoltage, cardBatVoltage ,card_one_bat_Voltage;
    private TextView originBatOutCurrent, cardBatOutCurrent;
    private TextView originBatHealthCap, cardBatHealthCap, card_bat_3;
    private TextView originBat_use_time, cardBat_use_time;
    private TextView card_switch_point;

    // ===== 温度 & 风扇 =====
    private TextView originTemp0Value, cardTemp0Value;
    private TextView originTemp1Value, cardTemp1Value;
    private TextView originFanValue, cardFanValue;

    // ===== 电量统计 =====
    private TextView originPvCharged, cardPvCharged;
    private TextView originTvRollover, cardTvRollover;
    private TextView originTvCharged, cardTvCharged;
    private TextView originTvDischarged, cardTvDischarged;
    private TextView originTvAvailable, cardTvAvailable;

    // ===== 图表 =====
    private LineChart originBatLineChart, cardBatLineChart;
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
    private FrameLayout originmarkerContainer,cardmarkerContainer;
    private View origincustomMarker,cardcustomMarker;
    private TextView origin_m_year, origin_m_time, origin_m_value,origin_pv_voltage, origin_pv_current, origin_pv_power;
    private TextView card_m_year, card_m_time, card_m_value,card_pv_voltage, card_pv_current, card_pv_power;
    private FluidBubbleView originFluidView,cardFluidView;
    private ImageView origin_solarIcon,origin_houseIcon,card_solarIcon,card_houseIcon;
    private final Map<String, String> info = new HashMap<>();
    private Float max_chargerCurrent,chargeCurrent,dischargeCurrent,bat_healthy_value,switch_point_voltage,bat_energy_ball;
    private String p_charged,charged, discharged,total_cap,available_cap,useTimeStr;
    private int layout_mode,fluidColor,date_num;
    private static final int COLOR_GREEN = Color.parseColor("#39FF14");
    private static final int COLOR_RED = Color.parseColor("#F44336");
    private static final int COLOR_ORANGE = Color.parseColor("#FF9800");
    // 新旧值对比
    private String last_AcVoltage;
    private String last_ac_current;
    private String last_ac_power;
    private String last_sj_power;
    private String last_power_ys;
    private String last_ac_freq;
    private String last_power_use;
    private String last_bat_voltage;
    private String last_alone_bat_voltage;
    private String last_pv_voltage;
    private String last_pv_current;
    private String last_pv_time_power;
    private String last_bat_charged_discharged_text;
    private String last_bat_charged_discharged_value;
    private String last_mp_pt_temp;
    private String last_current_out_mode;
    private String last_mem_use_info;
    private String last_mos_time_temp;
    private String last_fan_time_speed;
    private String last_p_charged;
    private String last_charged;
    private String last_discharged;
    private String last_total_cap;
    private String last_available_cap;
    private String last_switch_point_voltage;
    private String last_useTimeStr;
    private String last_bat_health_text;
    private String last_bat_health_detail;
    // 逆变器自身功耗
    private final float invSelfConsumption = 30f;
    private void resetLastValues() {
        last_AcVoltage = null;
        last_ac_current = null;
        last_ac_power = null;
        last_sj_power = null;
        last_power_ys = null;
        last_ac_freq = null;
        last_power_use = null;
        last_bat_voltage = null;
        last_alone_bat_voltage = null;
        last_pv_voltage = null;
        last_pv_current = null;
        last_pv_time_power = null;
        last_bat_charged_discharged_text = null;
        last_bat_charged_discharged_value = null;
        last_mp_pt_temp = null;
        last_current_out_mode = null;
        last_mem_use_info = null;
        last_mos_time_temp = null;
        last_fan_time_speed = null;
        last_p_charged = null;
        last_charged = null;
        last_discharged = null;
        last_total_cap = null;
        last_available_cap = null;
        last_switch_point_voltage = null;
        last_useTimeStr = null;
        last_bat_health_text = null;
        last_bat_health_detail = null;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // ===== ViewSwitcher =====
        viewSwitcher = findViewById(R.id.viewSwitcher);
        // 恢复显示模式
        SharedPreferences sp = getSharedPreferences("ui", MODE_PRIVATE);
        layout_mode = sp.getInt("mode", 0); // 0 = 经典，1 = 卡片
        viewSwitcher.setDisplayedChild(layout_mode);
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
        originmarkerContainer = originalView.findViewById(R.id.marker_container);
        origincustomMarker = originalView.findViewById(R.id.custom_marker);
        origin_m_year = originalView.findViewById(R.id.m_year);
        origin_m_time = originalView.findViewById(R.id.m_time);
        origin_m_value = originalView.findViewById(R.id.m_value);
        origin_pv_voltage = originalView.findViewById(R.id.pv_voltage);
        origin_pv_current = originalView.findViewById(R.id.pv_current);
        origin_pv_power = originalView.findViewById(R.id.pv_power);

        // image
        origin_menu_bt = originalView.findViewById(R.id.menu_img);

        // ===== 充电动画控件 =====
        origin_solarIcon = originalView.findViewById(R.id.solar_icon);
        origin_houseIcon = originalView.findViewById(R.id.house_icon);
        originFluidView = originalView.findViewById(R.id.fluidView);

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
        card_one_bat_Voltage = cardView.findViewById(R.id.one_bat_Voltage);
        cardBatOutCurrent = cardView.findViewById(R.id.bat_out_current);
        cardBatHealthCap = cardView.findViewById(R.id.bat_health_cap);
        cardBat_use_time = cardView.findViewById(R.id.bat_use_time);
        card_switch_point = cardView.findViewById(R.id.switch_point);
        card_bat_3 = cardView.findViewById(R.id.bat_3);

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
        cardmarkerContainer = cardView.findViewById(R.id.marker_container);
        cardcustomMarker = cardView.findViewById(R.id.custom_marker);
        card_m_year = cardView.findViewById(R.id.m_year);
        card_m_time = cardView.findViewById(R.id.m_time);
        card_m_value = cardView.findViewById(R.id.m_value);
        card_pv_voltage = cardView.findViewById(R.id.pv_voltage);
        card_pv_current = cardView.findViewById(R.id.pv_current);
        card_pv_power = cardView.findViewById(R.id.pv_power);

        // image
        card_menu_bt = cardView.findViewById(R.id.menu_img);

        // ===== 充电动画控件 =====
        card_solarIcon = cardView.findViewById(R.id.solar_icon);
        card_houseIcon = cardView.findViewById(R.id.house_icon);
        cardFluidView = cardView.findViewById(R.id.fluidView);

        // 一行绑定坐标（必须在 setContentView 之后）
        originFluidView.bindIconCoords(origin_solarIcon, origin_houseIcon);
        cardFluidView.bindIconCoords(card_solarIcon, card_houseIcon);
        originFluidView.setSolarSize(80f); // 跟布局里 80dp 一致
        cardFluidView.setSolarSize(80f);
    }
    private final Handler markerHandler = new Handler(Looper.getMainLooper());
    private final Runnable markerHideRunnable = new Runnable() {
        @Override
        public void run() {
            if (layout_mode == 0) {
                origincustomMarker.setVisibility(View.GONE);
                originBatLineChart.highlightValue(null);
            }else{
                cardcustomMarker.setVisibility(View.GONE);
                cardBatLineChart.highlightValue(null);
            }
        }
    };
    private void init_module(){
        Calendar calendar = Calendar.getInstance();
        year = calendar.get(Calendar.YEAR);       // 年
        month = calendar.get(Calendar.MONTH) + 1; // 月 (注意要+1)
        day = calendar.get(Calendar.DAY_OF_MONTH); // 日
        smartRefreshLayout = findViewById(R.id.refreshLayout);
        //设置 Header 为 贝塞尔雷达 样式
        smartRefreshLayout.setRefreshHeader(new MaterialHeader(this));
        smartRefreshLayout.setOnRefreshListener(refreshLayout -> {
            about.log(TAG, "下拉刷新");
            if (!request_homepage_run && socket != null) {
                request_homepage_date();
            } else {
                refreshLayout.finishRefresh();
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
        if (layout_mode == 0) {
            bt_listen(origin_hour_power, originPowerChart, _H_Total_power, "小时柱状图表", "暂无小时数据");
        }else {
            bt_listen(card_hour_power, cardPowerChart, _H_Total_power, "小时柱状图表", "暂无小时数据");
        }
        //日期图表按键监听
        if (layout_mode == 0) {
            bt_listen(origin_day_power, originPowerChart, _D_Total_power, "日期柱状图表", "暂无日期数据");
        }else {
            bt_listen(card_day_power, cardPowerChart, _D_Total_power, "日期柱状图表", "暂无日期数据");
        }
        //月份图表按键监听
        if (layout_mode == 0) {
            bt_listen(origin_month_power, originPowerChart, _M_Total_power, "月份柱状图表", "暂无月份数据");
        }else {
            bt_listen(card_month_power, cardPowerChart, _M_Total_power, "月份柱状图表", "暂无月份数据");
        }
        //年图表按键监听
        if (layout_mode == 0) {
            bt_listen(origin_year_power, originPowerChart, _Y_Total_power, "年份柱状图表", "暂无年份数据");
        }else {
            bt_listen(card_year_power, cardPowerChart, _Y_Total_power, "年份柱状图表", "暂无年份数据");
        }

        originBatLineChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                markerHandler.removeCallbacks(markerHideRunnable);

                // ===== 1. 坐标计算 =====
                float chartX = h.getXPx();
                float chartY = h.getYPx();

                int[] chartLoc = new int[2];
                int[] containerLoc = new int[2];
                originBatLineChart.getLocationOnScreen(chartLoc);
                originmarkerContainer.getLocationOnScreen(containerLoc);

                float offsetX = chartLoc[0] - containerLoc[0];
                float offsetY = chartLoc[1] - containerLoc[1];

                float markerX = chartX + offsetX;
                float markerY = chartY + offsetY;

                // ===== 2. 设置文字内容 =====
                int index = (int) e.getX();
                if (index >= 0 && index < MainActivity._min_bat_list.size()) {
                    String[] _tmp = MainActivity._min_bat_list.get(index).split(" ");

                    origin_m_year.setText(" " + MainActivity.year + "-" + MainActivity.month + "-" + MainActivity.day);
                    origin_m_time.setText(" " + _tmp[0] + ":00");

                    String[] all_data = _tmp[1].split(",");
                    origin_m_value.setText(" 电池电压:" + all_data[0]);
                    origin_pv_voltage.setText(" 光伏电压:" + all_data[1]);
                    origin_pv_current.setText(" 光伏电流:" + all_data[2]);
                    origin_pv_power.setText(" 光伏功率:" + all_data[3]);
                }

                // ===== 3. 显示 + 定位 =====
                origincustomMarker.setVisibility(View.VISIBLE);

                origincustomMarker.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                int markerW = origincustomMarker.getMeasuredWidth();
                int markerH = origincustomMarker.getMeasuredHeight();

                float finalX = markerX + 20;
                float finalY = markerY - markerH - 10;

                if (finalX + markerW > originmarkerContainer.getWidth()) {
                    finalX = markerX - markerW - 20;
                }
                if (finalY < 0) {
                    finalY = markerY + 20;
                }

                origincustomMarker.setX(finalX);
                origincustomMarker.setY(finalY);

                // 4 .启动3秒倒计时，到点自动隐藏
                markerHandler.postDelayed(markerHideRunnable, 5000);
            }
            @Override
            public void onNothingSelected() {
                // 当用户点击/触摸了图表上「没有数据点」的区域时
                origincustomMarker.setVisibility(View.GONE);
                originBatLineChart.highlightValue(null);
            }
        });
        cardBatLineChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                markerHandler.removeCallbacks(markerHideRunnable);

                // ===== 1. 坐标计算 =====
                float chartX = h.getXPx();
                float chartY = h.getYPx();

                int[] chartLoc = new int[2];
                int[] containerLoc = new int[2];
                cardBatLineChart.getLocationOnScreen(chartLoc);
                cardmarkerContainer.getLocationOnScreen(containerLoc);

                float offsetX = chartLoc[0] - containerLoc[0];
                float offsetY = chartLoc[1] - containerLoc[1];

                float markerX = chartX + offsetX;
                float markerY = chartY + offsetY;

                // ===== 2. 设置文字内容 =====
                int index = (int) e.getX();
                if (index >= 0 && index < MainActivity._min_bat_list.size()) {
                    String[] _tmp = MainActivity._min_bat_list.get(index).split(" ");

                    card_m_year.setText(" " + MainActivity.year + "-" + MainActivity.month + "-" + MainActivity.day);
                    card_m_time.setText(" " + _tmp[0] + ":00");

                    String[] all_data = _tmp[1].split(",");
                    card_m_value.setText(" 电池电压:" + all_data[0]);
                    card_pv_voltage.setText(" 光伏电压:" + all_data[1]);
                    card_pv_current.setText(" 光伏电流:" + all_data[2]);
                    card_pv_power.setText(" 光伏功率:" + all_data[3]);
                }

                // ===== 3. 显示 + 定位 =====
                cardcustomMarker.setVisibility(View.VISIBLE);

                cardcustomMarker.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                int markerW = cardcustomMarker.getMeasuredWidth();
                int markerH = cardcustomMarker.getMeasuredHeight();

                float finalX = markerX + 20;
                float finalY = markerY - markerH - 10;

                if (finalX + markerW > cardmarkerContainer.getWidth()) {
                    finalX = markerX - markerW - 20;
                }
                if (finalY < 0) {
                    finalY = markerY + 20;
                }

                cardcustomMarker.setX(finalX);
                cardcustomMarker.setY(finalY);

                // 4 .启动3秒倒计时，到点自动隐藏
                markerHandler.postDelayed(markerHideRunnable, 5000);
            }
            @Override
            public void onNothingSelected() {
                // 当用户点击/触摸了图表上「没有数据点」的区域时
                cardcustomMarker.setVisibility(View.GONE);
                cardBatLineChart.highlightValue(null);
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
                if (tcpClient.tcpConnect() && !Thread_Run) {
                    about.log(TAG, "开始调用线程");
                    mData_pro_thread();
                    break;
                }
                if (Thread_Run) {
                    about.log(TAG, "线程调用完成");
                }
            }
        }).start();
        new Thread(() -> {
            while (true) {
                if (!request_homepage_run && !data_rec_finish && socket != null) {
                    request_homepage_date();
                    Log.d(TAG,"请求首页数据");
                }
                sleep(2000);
            }
        }).start();
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
                                        deleteData("lowvoltage");
                                        deleteData("work_mode");
                                        deleteData("mos_temp_value");
                                        deleteData("on_inv_value");
                                        deleteData("hardware_offset_us");
                                        deleteData("peakToPeakDiff");
                                        deleteData("SYSTEM_R");
                                        deleteData("request_calibration");
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
    private void safeSaveFlash(Map<String, String> infoMap, String key) {
        if (infoMap == null) return;

        String newValue = infoMap.get(key);
        if (newValue == null || newValue.isEmpty()) return;

        String oldValue = readDate(this, key);
        if (!newValue.equals(oldValue)) {
            saveData(key, newValue);
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
    @SuppressLint("DefaultLocale")
    private void mData_pro_thread() {
        new Thread(() -> {
            Thread_Run = true;
            while (!isPaused) {
                if (!stop_send){
                    String udp_response = tcpClient.sendAndReceive("get_info");
                    sleep(page_refresh_time);
                    if (udp_response != null && udp_response.startsWith("AC_voltage")) {
                        Log.d(TAG, "数据内容: " + udp_response );
                        for (String pair : udp_response.trim().replace("mark1", "").split(",")) {
                            String[] kv = pair.split(":", 2);
                            if (kv.length == 2) info.put(kv[0].trim(), kv[1].trim());
                        }
                        Float sj_power = 0.0F;
                        //交流电压
                        uiData.put("ac_voltage", info.get("AC_voltage"));
                        String ac = info.get("AC_voltage");
                        //交流电流
                        Float jl_dl = Float.parseFloat(Objects.requireNonNull(info.get("AC_current")));
                        String formattedValue_iv_Value = df.format(jl_dl);
                        uiData.put("ac_current", formattedValue_iv_Value);
                        String iv = info.get("AC_current");
                        //交流有功功率
                        uiData.put("ac_power", info.get("AC_power"));
                        float AC_power = Float.parseFloat(Objects.requireNonNull(info.get("AC_power")));
                        //交流视在功率
                        if (ac != null && iv != null) {
                            sj_power = Float.parseFloat(ac) * Float.parseFloat(iv);
                            String formattedValue = df.format(sj_power);
                            uiData.put("sj_power", formattedValue);
                        }
                        //功率因数
                        String pf_value = df.format(AC_power / sj_power);
                        uiData.put("power_ys", pf_value);
                        //交流频率
                        uiData.put("ac_freq", info.get("AC_frequency") + " hz");
                        //负载使用率
                        if (Objects.equals(info.get("out_mode"), "逆变供电")) {
                            String power_use = df.format((sj_power / Float.parseFloat(Objects.requireNonNull(info.get("power"))) * 100)) + " %"; //这里使用功率切换阈值作为最大功率
                            uiData.put("power_use", power_use);
                        } else {
                            uiData.put("power_use", "无限制");
                        }
                        //储能电池电压
                        uiData.put("bat_voltage", info.get("Battery_Voltage"));
                        float bat_voltage = Float.parseFloat(Objects.requireNonNull(info.get("Battery_Voltage")));
                        //单电池电压
                        String alone_bat_voltage = df.format(bat_voltage/8);
                        uiData.put("alone_bat_voltage",alone_bat_voltage);
                        //光伏板电压
                        uiData.put("pv_voltage", info.get("Sun_Voltage"));
                        //光伏板电流
                        if (Float.parseFloat(Objects.requireNonNull(info.get("Sun_Current"))) < 0.5) { //防止夜晚功率计算错误
                            uiData.put("pv_current", String.valueOf(0));
                        }else {
                            uiData.put("pv_current", info.get("Sun_Current"));
                        }
                        //光伏实时输出功率
                        uiData.put("pv_time_power", info.get("Sun_time_power"));
                        //逆变器不同模式下电池的充放电电流计算
                        //充放电电流计算,其中的30为逆变器开启时自身功耗的估算包含主板功耗3W,3.0为逆变器关闭时控制板3W功耗的估算
                        float pw = Float.parseFloat(Objects.requireNonNull(info.get("Sun_time_power")));//太阳能板的发电功率
                        // 逆变器参数
                        float invEff;
                        if (Float.parseFloat(Objects.requireNonNull(uiData.get("ac_power"))) < 200.0f){
                            invEff = 0.75f;
                        }else{
                            invEff = 0.94f;
                        }
                        // 负载交流有功功率（W）
                        float loadPowerAc = Float.parseFloat(Objects.requireNonNull(uiData.get("ac_power")));
                        // 系统交流侧总功率消耗
                        float totalAcLoad = loadPowerAc / invEff + invSelfConsumption;
                        if (Objects.equals(info.get("out_mode"), "逆变供电")) {
                            //逆变供电模式下,逆变器为开启状态的充放电电流计算
                            if (pw - totalAcLoad > 0) {
                                uiData.put("bat_charged_discharged_text", "\uD83D\uDCA7 充电电流(A):");
                                uiData.put("bat_charged_discharged_value", df.format((pw - totalAcLoad) / bat_voltage));
                            } else {
                                uiData.put("bat_charged_discharged_text", "\uD83D\uDCA7 放电电流(A):");
                                uiData.put("bat_charged_discharged_value", df.format((totalAcLoad - pw) / bat_voltage));
                            }
                        } else if (Objects.equals(info.get("out_mode"), "市电供电")) {
                            //市电供电模式下,逆变器为关闭状态的充放电电流计算
                            if ((pw - 3.0) > 0) {
                                uiData.put("bat_charged_discharged_text", "\uD83D\uDCA7 充电电流(A):");
                                uiData.put("bat_charged_discharged_value", df.format((pw - 3.0) / bat_voltage)); //3.0w为估算值,具体要测量才知道
                            } else {
                                uiData.put("bat_charged_discharged_text", "\uD83D\uDCA7 放电电流(A):");
                                uiData.put("bat_charged_discharged_value", df.format(3.0 / bat_voltage));//3.0w为估算值,具体要测量才知道
                            }
                        } else if (Objects.equals(info.get("out_mode"), "电池电压过低")) {
                            //电池电压过低,逆变器为关闭状态的充放电电流计算
                            if ((pw - 3.0) > 0) {
                                uiData.put("bat_charged_discharged_text", "\uD83D\uDCA7 充电电流(A):");
                                uiData.put("bat_charged_discharged_value", df.format((pw - 3.0) / bat_voltage)); //3.0w为估算值,具体要测量才知道
                            } else {
                                uiData.put("bat_charged_discharged_text", "\uD83D\uDCA7 放电电流(A):");
                                uiData.put("bat_charged_discharged_value", df.format(3.0 / bat_voltage));//3.0w为估算值,具体要测量才知道
                            }
                        } else if (Objects.equals(info.get("out_mode"), "固定逆变模式")) {
                            //固定逆变模式下,逆变器为开启状态的充放电电流计算
                            if (pw - totalAcLoad > 0) {
                                uiData.put("bat_charged_discharged_text", "\uD83D\uDCA7 充电电流(A):");
                                uiData.put("bat_charged_discharged_value", df.format((pw - totalAcLoad) / bat_voltage));
                            } else {
                                uiData.put("bat_charged_discharged_text", "\uD83D\uDCA7 放电电流(A):");
                                uiData.put("bat_charged_discharged_value", df.format((totalAcLoad - pw) / bat_voltage));
                            }
                        } else if (Objects.equals(info.get("out_mode"), "固定市电模式")) {
                            //固定市电模式下,逆变器为关闭状态的充放电电流计算
                            if ((pw - 3.0) > 0) {
                                uiData.put("bat_charged_discharged_text", "\uD83D\uDCA7 充电电流(A):");
                                uiData.put("bat_charged_discharged_value", df.format((pw - 3.0) / bat_voltage)); //3.0w为估算值,具体要测量才知道
                            } else {
                                uiData.put("bat_charged_discharged_text", "\uD83D\uDCA7 放电电流(A):");
                                uiData.put("bat_charged_discharged_value", df.format(3.0 / bat_voltage));//3.0w为估算值,具体要测量才知道
                            }
                        }
                        //为MPTT散热片温度
                        uiData.put("mp_pt_temp", info.get("MPPT温度") + "°C");
                        //当前输出模式
                        if (Objects.equals(info.get("out_mode"), "电池电压过低")){
                            uiData.put("current_out_mode", "电池低压");
                        }else if (Objects.equals(info.get("out_mode"), "固定市电模式")){
                            uiData.put("current_out_mode", "固定市电");
                        }else if (Objects.equals(info.get("out_mode"), "固定逆变模式")){
                            uiData.put("current_out_mode", "固定逆变");
                        }else{
                            uiData.put("current_out_mode", info.get("out_mode"));
                        }
                        //内存使用信息
                        uiData.put("mem_use_info", info.get("mem_usage"));
                        //市电切换阈值
                        safeSaveFlash(info,"power");
                        //电池低于此值则市电常开
                        safeSaveFlash(info,"lowvoltage");
                        //输出模式
                        safeSaveFlash(info,"work_mode");
                        //主功率板散执片风扇开启温度阈值
                        safeSaveFlash(info,"mos_temp_value");
                        //主功率板散热片实时温度
                        uiData.put("mos_time_temp", info.get("sys_ntc_value"));
                        //主功率板散热风扇转速值
                        uiData.put("fan_time_speed", info.get("fan_speed_value"));
                        //开启逆变的电压阈值
                        safeSaveFlash(info,"on_inv_value");
                        // 光耦和电阻的物理硬件延迟误差
                        safeSaveFlash(info,"hardware_offset_us");
                        // 极致锁相峰值微秒差
                        safeSaveFlash(info,"peakToPeakDiff");
                        // 系统总内阻
                        safeSaveFlash(info,"SYSTEM_R");
                        // 请求电池校准
                        safeSaveFlash(info,"request_calibration");
                        // 电池死区容量
                        safeSaveFlash(info,"est_dead_zone_kwh");
                        //电池充放电信息表
                        p_charged = String.format("☀️ 今日光伏发电: %.3f kWh",Float.parseFloat(Objects.requireNonNull(info.get("pv_energy_today"))));
                        charged = String.format("⛽️ 今日电池充电: %.3f kWh",Float.parseFloat(Objects.requireNonNull(info.get("bat_charged_today"))));
                        discharged = String.format("⚡ 今日电池放电: %.3f kWh",Float.parseFloat(Objects.requireNonNull(info.get("bat_discharged_today"))));
                        total_cap = String.format("📋 当前电池总容量: %.3f kWh",Float.parseFloat(Objects.requireNonNull(info.get("bat_cap_data"))));
                        available_cap = String.format("🔋 当前电池可用电量: %.3f kWh",Float.parseFloat(Objects.requireNonNull(info.get("bat_energy_last"))));
                        switch_point_voltage = Float.parseFloat(Objects.requireNonNull(info.get("switch_point_voltage")));
                        bat_healthy_value = Float.parseFloat(Objects.requireNonNull(info.get("bat_healthy_data")));
                        // 显示电池健康度
                        if (bat_healthy_value > 0) {
                            uiData.put("bat_health_text", bat_healthy_value >= 90 ? "优秀" :
                                    bat_healthy_value >= 85 ? "良好" :
                                            bat_healthy_value >= 80 ? "预警" : "严重衰减");
                            uiData.put("bat_health_detail", "健康度(" + String.format("%.1f", bat_healthy_value) + "%)");
                        } else if (bat_healthy_value < 0) {
                            uiData.put("bat_health_text", "校准中");
                            uiData.put("bat_health_detail", "健康度");
                        } else {
                            uiData.put("bat_health_text", "暂未校准");
                            uiData.put("bat_health_detail", "健康度");
                        }
                        // 计算在最高允许功率下的电池放电电流
                        max_chargerCurrent = Float.parseFloat(Objects.requireNonNull(info.get("power"))) / bat_voltage;
                        // 电池可用电量（Wh）
                        float availableCapWh = Float.parseFloat(Objects.requireNonNull(info.get("bat_energy_last"))) * 1000f;
                        if (pw >= totalAcLoad) {
                            // 光伏够用，电池不放电
                            useTimeStr = "充电中";
                        } else {
                            // 光伏不足，电池需要放电,交流缺口折算到直流侧
                            float dcDischargePower = totalAcLoad - pw;
                            // 防止极小放电功率导致“天文数字”
                            if (dcDischargePower < 10f) {
                                useTimeStr = "无需放电";
                            } else {
                                if ( Objects.requireNonNull(info.get("out_mode")).contains("逆变")) {
                                    double hours = availableCapWh / dcDischargePower;
                                    long totalMinutes = (long) (hours * 60); // 偏保守
                                    long d = totalMinutes / 1440;
                                    long h = (totalMinutes % 1440) / 60;
                                    long m = totalMinutes % 60;
                                    useTimeStr = String.format("%d天%d时%02d分", d, h, m);
                                }else{
                                    useTimeStr = "任意时长";
                                }
                            }
                        }
                        if (availableCapWh > 0) {
                            float rawValue = (availableCapWh / 1000f / Float.parseFloat(Objects.requireNonNull(info.get("bat_cap_data")))) * 100f;
                            bat_energy_ball = Math.round(rawValue * 10f) / 10f;
                        }else if(availableCapWh <= 0){
                            bat_energy_ball = 0f;
                        }
                        if (bat_energy_ball >= 100f) bat_energy_ball = 100f;
                        if (bat_energy_ball <= 0f) bat_energy_ball = 0f;

                        // 1. 根据电量，在外部精准计算出当前应该呈现的科技主题颜色
                        if (bat_energy_ball <= 20) {
                            fluidColor = COLOR_RED; // 低电量：红
                        } else if (bat_energy_ball <= 60) {
                            fluidColor = COLOR_ORANGE; // 中电量：橙
                        }else {
                            fluidColor = COLOR_GREEN; // 高电量：绿
                        }
                        if ( Objects.requireNonNull(info.get("out_mode")).contains("逆变")) {
                            if (pw > 0 && totalAcLoad > pw) {
                                // 光伏在充电，电池在补缺口(负载功率大于光伏输出功率)
                                chargeCurrent = pw / Float.parseFloat(Objects.requireNonNull(info.get("Battery_Voltage")));
                                dischargeCurrent = (totalAcLoad - pw) / Float.parseFloat(Objects.requireNonNull(info.get("Battery_Voltage")));
                            } else if (pw > 0 && totalAcLoad < pw) {
                                // 光伏提供主功率,剩余功率给电池充电,电池未放电(光伏输出功率大于负载所需的功率)
                                chargeCurrent = (pw - totalAcLoad) / Float.parseFloat(Objects.requireNonNull(info.get("Battery_Voltage")));
                                dischargeCurrent = 0f;
                            } else if (pw <= 0) {
                                // 光伏无功率,不充电,电池放电(光伏无功率,纯电池放电)
                                chargeCurrent = 0f;
                                dischargeCurrent = totalAcLoad / Float.parseFloat(Objects.requireNonNull(info.get("Battery_Voltage")));
                            }
                        }else{
                            if (pw > 0) {
                                // 光伏给控制板供电,剩余功率给电池充电(非逆变模式下,光伏给电池充电)
                                chargeCurrent = (pw - 3f) / Float.parseFloat(Objects.requireNonNull(info.get("Battery_Voltage")));
                                dischargeCurrent = 0f;
                            }else{
                                // 光伏功率小于0,电池只给控制板供电(非逆变模式下,纯电池给控制板供电)
                                chargeCurrent = 0f;
                                dischargeCurrent = 3f;
                            }
                        }
                        // 通知数据刷新
                        Message message = messageProHandler.obtainMessage();
                        message.what = 1;
                        message.obj = uiData;  // 将计算结果放入Message
                        messageProHandler.sendMessage(message);
                    }
                    if (checkScreenStatus() && udp_response != null && udp_response.startsWith("live>") && udp_response.contains("mark3")){
                        about.log(TAG, "收到实时分时数据,更新分时图表");
                        String[] str = udp_response.split("#");
                        String[] min = str[0].split(">");
                        String[] _f = min[1].split(" ");
                        String[] _s = _f[0].split(":");
                        String h = String.format(Locale.getDefault(),"%02d", Integer.parseInt(_s[0]));
                        String m = String.format(Locale.getDefault(),"%02d", Integer.parseInt(_s[1]));
                        String _min = h+":"+m+" "+_f[1];
                        _min_bat_list.add(_min);
                        if (layout_mode == 0) {
                            pro_min_chart_data(_min_bat_list, "每15分钟电压", originBatLineChart);
                        }else {
                            pro_min_chart_data(_min_bat_list, "每15分钟电压", cardBatLineChart);
                        }
                        if (!h.equals("00") && !str[1].contains("none")) {
                            String[] h_ = str[1].split(">");
                            String[] hour = h_[1].split(",");
                            _H_Total_power.add(hour[0]);
                            if (layout_mode == 0) {
                                pro_day_chart_data(_H_Total_power, "小时柱状图表", originPowerChart);
                            }else {
                                pro_day_chart_data(_H_Total_power, "小时柱状图表", cardPowerChart);
                            }
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
                String newVal = uiData.get("ac_voltage");
                if (!Objects.equals(newVal, last_AcVoltage)) {
                    ((layout_mode == 0) ? originOutVoltage : cardOutVoltage).setText(newVal);
                    last_AcVoltage = newVal;
                }
                //交流电流
                newVal = uiData.get("ac_current");
                if (!Objects.equals(newVal, last_ac_current)) {
                    ((layout_mode == 0) ? originOutCurrent : cardOutCurrent).setText(newVal);
                    last_ac_current = newVal;
                }
                //交流有功功率
                newVal = uiData.get("ac_power");
                if (!Objects.equals(newVal, last_ac_power)) {
                    ((layout_mode == 0) ? originPowerKw : cardPowerKw).setText(newVal);
                    last_ac_power = newVal;
                }
                //交流视在功率
                newVal = uiData.get("sj_power");
                if (!Objects.equals(newVal, last_sj_power)) {
                    ((layout_mode == 0) ? originSjPowerKw : cardSjPowerKw).setText(newVal);
                    last_sj_power = newVal;
                }
                //功率因数
                newVal = uiData.get("power_ys");
                if (!Objects.equals(newVal, last_power_ys)) {
                    ((layout_mode == 0) ? originPf : cardPf).setText(newVal);
                    last_power_ys = newVal;
                }
                //交流频率
                newVal = uiData.get("ac_freq");
                if (!Objects.equals(newVal, last_ac_freq)) {
                    ((layout_mode == 0) ? originOutFrequency : cardOutFrequency).setText(newVal);
                    last_ac_freq = newVal;
                }
                //负载使用率
                newVal = uiData.get("power_use");
                if (!Objects.equals(newVal, last_power_use)) {
                    ((layout_mode == 0) ? originLoadRateValue : cardLoadRateValue).setText(newVal);
                    last_power_use = newVal;
                }
                //电池电压
                newVal = uiData.get("bat_voltage");
                if (!Objects.equals(newVal, last_bat_voltage)) {
                    ((layout_mode == 0) ? originBatVoltage : cardBatVoltage).setText(newVal);
                    last_bat_voltage = newVal;
                }
                //单节电池电压
                newVal = uiData.get("alone_bat_voltage");
                if (!Objects.equals(newVal, last_alone_bat_voltage)) {
                    card_one_bat_Voltage.setText(newVal);
                    last_alone_bat_voltage = newVal;
                }
                //光伏电压
                newVal = uiData.get("pv_voltage");
                if (!Objects.equals(newVal, last_pv_voltage)) {
                    ((layout_mode == 0) ? originSunVoltageValue : cardSunVoltageValue).setText(newVal);
                    last_pv_voltage = newVal;
                }
                //太阳能电流
                newVal = uiData.get("pv_current");
                if (!Objects.equals(newVal, last_pv_current)) {
                    ((layout_mode == 0) ? originLeCurrent : cardLeCurrent).setText(newVal);
                    last_pv_current = newVal;
                }
                //光伏实时输出功率
                newVal = uiData.get("pv_time_power");
                if (!Objects.equals(newVal, last_pv_time_power)) {
                    ((layout_mode == 0) ? originPvPowerResult : cardPvPowerResult).setText(newVal);
                    last_pv_time_power = newVal;
                }
                //为逆变模式时修改计算电池的充放电电流文本
                newVal = uiData.get("bat_charged_discharged_text");
                if (!Objects.equals(newVal, last_bat_charged_discharged_text)) {
                    if (layout_mode == 0) {
                        originCurrentDirection.setText(newVal);
                    } else {
                        if (Objects.equals(newVal, "\uD83D\uDCA7 充电电流(A):")) {
                            cardCurrentDirection.setText("充电电流");
                        } else if (Objects.equals(newVal, "\uD83D\uDCA7 放电电流(A):")) {
                            cardCurrentDirection.setText("放电电流");
                        }
                    }
                    last_bat_charged_discharged_text = newVal;
                }
                //为逆变模式时计算电池的充放电电流
                newVal = uiData.get("bat_charged_discharged_value");
                if (!Objects.equals(newVal, last_bat_charged_discharged_value)) {
                    ((layout_mode == 0) ? originBatOutCurrent : cardBatOutCurrent).setText(newVal);
                    last_bat_charged_discharged_value = newVal;
                }
                //为MPTT散热片温度
                newVal = uiData.get("mp_pt_temp");
                if (!Objects.equals(newVal, last_mp_pt_temp)) {
                    ((layout_mode == 0) ? originTemp0Value : cardTemp0Value).setText(newVal);
                    last_mp_pt_temp = newVal;
                }
                //当前输出模式
                newVal = uiData.get("current_out_mode");
                if (!Objects.equals(newVal, last_current_out_mode)) {
                    ((layout_mode == 0) ? originOutMode : cardOutMode).setText(newVal);
                    last_current_out_mode = newVal;
                }
                //内存使用信息
                newVal = uiData.get("mem_use_info");
                if (!Objects.equals(newVal, last_mem_use_info)) {
                    if (layout_mode == 0) {
                        mem_data_display_to_chart(newVal, originMmUse);
                    } else {
                        mem_data_display_to_chart(newVal, cardMmUse);
                    }
                    last_mem_use_info = newVal;
                }
                //主功率板散热片实时温度
                newVal = uiData.get("mos_time_temp");
                if (!Objects.equals(newVal, last_mos_time_temp)) {
                    ((layout_mode == 0) ? originTemp1Value : cardTemp1Value).setText(newVal);
                    last_mos_time_temp = newVal;
                }
                //主功率板散热风扇转速值
                newVal = uiData.get("fan_time_speed");
                if (!Objects.equals(newVal, last_fan_time_speed)) {
                    ((layout_mode == 0) ? originFanValue : cardFanValue).setText(newVal);
                    last_fan_time_speed = newVal;
                }
                //今日光伏发电
                if (!Objects.equals(p_charged, last_p_charged)) {
                    ((layout_mode == 0) ? originPvCharged : cardPvCharged).setText(p_charged);
                    last_p_charged = p_charged;
                }
                //今日电池充电
                if (!Objects.equals(charged, last_charged)) {
                    ((layout_mode == 0) ? originTvRollover : cardTvRollover).setText(charged);
                    last_charged = charged;
                }
                //今日电池放电
                if (!Objects.equals(discharged, last_discharged)) {
                    ((layout_mode == 0) ? originTvCharged : cardTvCharged).setText(discharged);
                    last_discharged = discharged;
                }
                //当前电池总容量
                if (!Objects.equals(total_cap, last_total_cap)) {
                    ((layout_mode == 0) ? originTvDischarged : cardTvDischarged).setText(total_cap);
                    last_total_cap = total_cap;
                }
                //当前电池可用电量
                if (!Objects.equals(available_cap, last_available_cap)) {
                    ((layout_mode == 0) ? originTvAvailable : cardTvAvailable).setText(available_cap);
                    last_available_cap = available_cap;
                }
                //电池低压的动态切换点
                newVal = switch_point_voltage < 0 ? "免切换" : String.valueOf(switch_point_voltage);
                if (!Objects.equals(newVal, last_switch_point_voltage)) {
                    card_switch_point.setText(newVal);
                    last_switch_point_voltage = newVal;
                }
                //电池可用时长
                if (!Objects.equals(useTimeStr, last_useTimeStr)) {
                    ((layout_mode == 0) ? originBat_use_time : cardBat_use_time).setText(useTimeStr);
                    last_useTimeStr = useTimeStr;
                }
                //电池健康度结果显示
                newVal = uiData.get("bat_health_text");
                if (!Objects.equals(newVal, last_bat_health_text)) {
                    ((layout_mode == 0) ? originBatHealthCap : cardBatHealthCap).setText(newVal);
                    last_bat_health_text = newVal;
                }
                //电池健康度上的小文字
                newVal = uiData.get("bat_health_detail");
                if (!Objects.equals(newVal, last_bat_health_detail)) {
                    card_bat_3.setText(newVal);
                    last_bat_health_detail = newVal;
                }
                //充电动画刷新（这个每次都调，因为粒子动画需要持续驱动）
                if (layout_mode == 0) {
                    originFluidView.updateConfig(bat_energy_ball, fluidColor, chargeCurrent, dischargeCurrent, max_chargerCurrent);
                } else {
                    cardFluidView.updateConfig(bat_energy_ball, fluidColor, chargeCurrent, dischargeCurrent, max_chargerCurrent);
                }
            }
        }
    };
    private void request_homepage_date() {
        if (request_homepage_run) {
            return;
        }
        request_homepage_run = true;
        new Thread(() -> {
            pro_data_request();//请求数据
            display_data(); //显示数据
            request_homepage_run = false;
        }).start();
    }
    private void display_data() {
        if (!_min_bat_list.isEmpty() && getTopActivity().toString().equals(top_m) && checkScreenStatus() && data_rec_finish) {
            if (originBatLineChart != null || cardBatLineChart != null){
                if (layout_mode == 0) {
                    assert originBatLineChart != null;
                    originBatLineChart.clear();//清空图表
                    originBatLineChart.notifyDataSetChanged();//通知数据巳改变
                    originBatLineChart.invalidate();//清理无效数据,用于动态刷新
                }else{
                    assert cardBatLineChart != null;
                    cardBatLineChart.clear();//清空图表
                    cardBatLineChart.notifyDataSetChanged();//通知数据巳改变
                    cardBatLineChart.invalidate();//清理无效数据,用于动态刷新
                }
            }
            if (layout_mode == 0) {
                pro_min_chart_data(_min_bat_list, "每15分钟电压", originBatLineChart);//把数据放到折线图上
            }else {
                pro_min_chart_data(_min_bat_list, "每15分钟电压", cardBatLineChart);//把数据放到折线图上
            }
            about.log(TAG, "15分钟刷新完成");
        }else{
            if (layout_mode == 0) {
                originBatLineChart.setNoDataText("暂无分时数据");
            }else {
                cardBatLineChart.setNoDataText("暂无分时数据");
            }
        }
        if (!_H_Total_power.isEmpty() && getTopActivity().toString().equals(top_m) && checkScreenStatus() && data_rec_finish) {
            if (originPowerChart != null || cardPowerChart != null){
                //清理无效数据,用于动态刷新
                //通知数据巳改变
                if (layout_mode == 0) {
                    assert originPowerChart != null;
                    originPowerChart.clear();//清空图表
                    originPowerChart.notifyDataSetChanged();//通知数据巳改变
                    originPowerChart.invalidate();//清理无效数据,用于动态刷新
                }else{
                    assert cardPowerChart != null;
                    cardPowerChart.clear();//清空图表
                    cardPowerChart.notifyDataSetChanged();//通知数据巳改变
                    cardPowerChart.invalidate();//清理无效数据,用于动态刷新
                }
            }
            if (layout_mode == 0) {
                pro_day_chart_data(_H_Total_power, "小时柱状图表", originPowerChart);//把小时数据放到柱状图上
            }else {
                pro_day_chart_data(_H_Total_power, "小时柱状图表", cardPowerChart);//把小时数据放到柱状图上
            }
            about.log(TAG, "小时柱状图刷新完成");
        }else{
            if (layout_mode == 0) {
                originPowerChart.setNoDataText("暂无小时数据");
            }else {
                cardPowerChart.setNoDataText("暂无小时数据");
            }
        }
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
                //Log.d(TAG, "发现包含分时的数据: " + line);
                String[] _l = line.split(">"); //按>进行分隔
                String[] _f = _l[1].split(" ");

                String[] _s = _f[0].split(":");
                String h = String.format(Locale.getDefault(),"%02d", Integer.parseInt(_s[0]));
                String m = String.format(Locale.getDefault(),"%02d", Integer.parseInt(_s[1]));
                String min = h+":"+m+" "+_f[1];
                _min_bat_list.add(min);
            } else if (line != null && line.contains("h>")) {
                //Log.d(TAG, "发现包含小时的数据: " + line);
                String[] _l = line.split(">"); //按>进行分隔
                _H_Total_power.add(_l[1]);
            } else if (line != null && line.contains("d>")) {
                //Log.d(TAG, "发现包含每天的数据: " + line);
                String[] _l = line.split(">"); //按>进行分隔
                _D_Total_power.add(year+"-"+_l[1]);
            } else if (line != null && line.contains("m>")) {
                //Log.d(TAG, "发现包含每月的数据: " + line);
                String[] _l = line.split(">"); //按>进行分隔
                _M_Total_power.add(year+"-"+_l[1]);
            } else if (line != null && line.contains("y>")) {
                //Log.d(TAG, "发现包含每年的数据: " + line);
                String[] _l = line.split(">"); //按>进行分隔
                String[] _j = _l[1].split(" ");

                String[] _n = _j[0].split("-");
                int y = 2000 + Integer.parseInt(_n[0]);
                String m = String.format(Locale.getDefault(),"%02d", Integer.parseInt(_n[1]));
                String d = String.format(Locale.getDefault(),"%02d", Integer.parseInt(_n[2]));
                String _y = y+"-"+m+"-"+d+" "+ _j[1];
                _Y_Total_power.add(_y);
            } else if (line != null && line.contains("debug>")) {
                //Log.d(TAG, "发现包含调试的数据: " + line);
                String[] _l = line.split(">"); //按>进行分隔
                debugList.add(_l[1]);
            } else if (line != null && line.contains("mark2")) {
                //Log.d(TAG, "发现包含结尾的数据: " + line);
                about.log(TAG, "所有数据接收完成,分时数据数量:" + _min_bat_list.size() + " 小时平均功率数据数量:" + _H_Total_power.size() +
                        " 日功率数据数量:" + _D_Total_power.size() + " 月功率数据数量:" + _M_Total_power.size() + " 年功率数据数量:" + _Y_Total_power.size());
                data_rec_finish = true;
            }
        }
        if (!data_rec_finish && retryCount < MAX_RETRY ){
            retryCount++;
            about.log(TAG, "数据不完整,重试"+retryCount+"次");
            _min_bat_list.clear();
            _H_Total_power.clear();
            _D_Total_power.clear();
            _M_Total_power.clear();
            _Y_Total_power.clear();
            debugList.clear();
            sleep(3000);
            pro_data_request();
        }
        retryCount = 0;
        stop_send = false;
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
                float max_pv_power = Float.parseFloat(_u[3]);
                if (max_pv_power > maxValue) {
                    maxValue = max_pv_power;
                    maxEntry = new Entry(i, Float.parseFloat(_u[0]));
                }
            }
            if (layout_mode == 0) {
                bat_data_display_to_chart(originBatLineChart, _time_value, _value_list, minute_des);
            }else {
                bat_data_display_to_chart(cardBatLineChart, _time_value, _value_list, minute_des);
            }
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
        String formattedValue = decimalFormat.format(_mem/126 * 100);
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
        chart.getAxisLeft().setAxisMaximum(126f);
        chart.getAxisRight().setAxisMinimum(0f);
        chart.getAxisRight().setAxisMaximum(126f);
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
            @SuppressLint("DefaultLocale") String label = String.format("设备内存使用情况(已使用:%.1f kb  空闲:%.1f kb)", memValue, 126 - memValue);
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
        chart.getAxisLeft().setAxisMaximum(32f);//左侧Y轴最大值
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
        // 先重置，清掉上一个类型的残留状态
        carChart.fitScreen();
        carChart.resetZoom();

        XAxis xAxis = carChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);

        YAxis leftAxis = carChart.getAxisLeft();
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                return String.format("%.2f", value);
            }
        });
        carChart.getAxisRight().setEnabled(false);
        carChart.getAxisRight().setDrawGridLines(false);

        xAxis.setGridColor(0x26808080);
        leftAxis.setGridColor(0x26808080);

        BarData barData = getBarData(barChart, label);
        barData.setValueTextSize(8f);
        barData.setValueTypeface(Typeface.DEFAULT_BOLD);
        barData.setValueTextColor(Color.DKGRAY);
        barData.setValueFormatter(new DefaultValueFormatter(2));

        carChart.getDescription().setText(des);
        carChart.getDescription().setTextSize(9f);
        carChart.setDoubleTapToZoomEnabled(false);
        carChart.setScaleYEnabled(false);

        int count = barData.getEntryCount();

        switch (type) {
            case "小时":
                xAxis.setAxisMinimum(-0.5f);
                xAxis.setAxisMaximum(23.5f);
                leftAxis.setAxisMinimum(0f);

                barData.setBarWidth(0.8f);

                // 清掉月份/年份可能留下的 X 范围锁
                carChart.setVisibleXRangeMinimum(1f);
                carChart.setVisibleXRangeMaximum(16f);

                carChart.setScaleXEnabled(false);
                carChart.setDragEnabled(true);

                if (count > 16) {
                    int x = count - 16;
                    carChart.moveViewToX(carChart.getLowestVisibleX() + x + 1);
                }
                break;

            case "日期":
                xAxis.setAxisMinimum(1 - 0.5f);
                xAxis.setAxisMaximum(date_num + 0.5f);
                leftAxis.setAxisMinimum(0f);

                barData.setBarWidth(0.8f);

                carChart.setVisibleXRangeMinimum(1f);
                carChart.setVisibleXRangeMaximum(16f);

                carChart.setScaleXEnabled(false);
                carChart.setDragEnabled(true);

                if (count > 16) {
                    int x = count - 16;
                    carChart.moveViewToX(carChart.getLowestVisibleX() + x + 1);
                }
                break;

            case "月份":
                xAxis.setAxisMinimum(0.5f);
                xAxis.setAxisMaximum(12.5f);
                leftAxis.setAxisMinimum(0f);

                barData.setBarWidth(0.8f);

                carChart.setVisibleXRangeMinimum(12f);
                carChart.setVisibleXRangeMaximum(12f);

                carChart.setScaleXEnabled(false);
                carChart.setDragEnabled(false);

                carChart.moveViewToX(0.5f);
                break;

            case "年份":
                xAxis.setAxisMinimum(2025 - 0.5f);
                xAxis.setAxisMaximum(2037 - 0.5f); // 2025~2036 共12年，上限是 2036+0.5=2036.5
                leftAxis.setAxisMinimum(0f);

                barData.setBarWidth(0.8f);

                carChart.setVisibleXRangeMinimum(12f);
                carChart.setVisibleXRangeMaximum(12f);

                carChart.setScaleXEnabled(false);
                carChart.setDragEnabled(false);

                carChart.moveViewToX(2025 - 0.5f); // 移到2025起始位置
                break;
        }
        carChart.setData(barData);
        carChart.notifyDataSetChanged();
        carChart.invalidate();
        carChart.setOnTouchListener(new View.OnTouchListener() {
            private float startX;
            private float startY;
            private boolean lockHorizontal;
            private boolean hasSentCancelToChart;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getRawX();
                        startY = event.getRawY();
                        lockHorizontal = false;
                        hasSentCancelToChart = false;

                        // 初始不要锁父布局，让页面能正常上下滑
                        requestParentDisallow(v, false);
                        break;

                    case MotionEvent.ACTION_MOVE:
                        float dx = Math.abs(event.getRawX() - startX);
                        float dy = Math.abs(event.getRawY() - startY);

                        // 阈值别太大，10~15够用
                        float slop = 12f;

                        if (!lockHorizontal) {
                            if (dx > dy && dx > slop) {
                                // 明确横向：交给图表
                                lockHorizontal = true;
                                requestParentDisallow(v, true);
                                hasSentCancelToChart = false;
                            } else if (dy > slop) {
                                // 明确竖向：必须让页面滚，别给图表
                                requestParentDisallow(v, false);
                                lockHorizontal = false;

                                // 关键：告诉 Chart 取消当前手势，避免它继续占着
                                if (!hasSentCancelToChart) {
                                    sendCancelToChart(carChart);
                                    hasSentCancelToChart = true;
                                }
                                // 这里返回 true，表示这波竖向不给 Chart 继续吃
                                return true;
                            }
                        } else {
                            // 已经锁横向后，仍要检测“变成明显竖向”
                            if (dy > dx && dy > slop * 2) {
                                lockHorizontal = false;
                                requestParentDisallow(v, false);

                                if (!hasSentCancelToChart) {
                                    sendCancelToChart(carChart);
                                    hasSentCancelToChart = true;
                                }
                                return true;
                            }
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        lockHorizontal = false;
                        requestParentDisallow(v, false);
                        break;
                }

                // 没明确竖向时，让 Chart 自己处理点击/横向拖
                return false;
            }
        });
    }
    private void requestParentDisallow(View v, boolean disallow) {
        ViewParent p = v.getParent();
        while (p != null) {
            p.requestDisallowInterceptTouchEvent(disallow);
            p = p.getParent();
        }
    }

    private void sendCancelToChart(BarChart chart) {
        if (chart == null) return;
        MotionEvent cancel = MotionEvent.obtain(
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                MotionEvent.ACTION_CANCEL,
                0, 0, 0
        );
        chart.onTouchEvent(cancel);
        cancel.recycle();
    }
    @NonNull
    private static BarData getBarData(ArrayList<BarEntry> barChart, String label) {
        BarDataSet dataSet = new BarDataSet(barChart, label);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getBarLabel(BarEntry barEntry) {
                // 💡 这里的 barEntry.getY() 才是具体的柱状图数值
                return String.format(Locale.getDefault(), "%.2f", barEntry.getY());
            }
        });
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
                    layout_mode = viewSwitcher.getDisplayedChild();

                    getSharedPreferences("ui", MODE_PRIVATE)
                            .edit()
                            .putInt("mode", layout_mode)
                            .apply();

                    viewSwitcher.post(() -> {
                        if (layout_mode == 0) {
                            // 切到了经典布局（child 0）
                            resetLastValues();
                            display_data(); //显示数据
                            originFluidView.bindIconCoords(origin_solarIcon, origin_houseIcon);
                        } else {
                            // 切到了卡片布局（child 1）
                            resetLastValues();
                            display_data(); //显示数据
                            cardFluidView.bindIconCoords(card_solarIcon, card_houseIcon);
                        }
                    });
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
            e.printStackTrace();
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

// 💡 关键修改点 1：将 implements IAxisValueFormatter 改为 extends ValueFormatter
class ExamModelOneXValueFormatter extends ValueFormatter {
    private final ArrayList<String> list;

    public ExamModelOneXValueFormatter(ArrayList<String> list) {
        this.list = list;
    }

    // 💡 关键修改点 2：将 @Override 的方法名改为 getAxisLabel
    @Override
    public String getAxisLabel(float value, AxisBase axis) {
        int values = (int) value;
        // 建议增加一个 values < 0 的越界保护（原代码是 <= 0，如果是第0个元素可能会显示为空，根据你的需求决定是否保留 = 号）
        if (values < 0 || values >= list.size()) {
            return "";
        }
        return list.get(values);
    }
}

/*数据值格式化器*/
class NoValueFormatter extends ValueFormatter {

    // 💡 新版版本统一使用这个重载方法
    @Override
    public String getPointLabel(Entry entry) {
        return ""; // 返回空字符串，不显示任何值
    }

    // 如果上面那个不生效，也可以同时重写这个旧版对应的方法
    @Override
    public String getFormattedValue(float value) {
        return "";
    }
}
