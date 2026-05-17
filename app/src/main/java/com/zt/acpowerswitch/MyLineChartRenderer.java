package com.zt.acpowerswitch;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.renderer.LineChartRenderer;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.ViewPortHandler;

public class MyLineChartRenderer extends LineChartRenderer {
    private final Entry highestEntry;
    private final Paint redPaint;

    public MyLineChartRenderer(LineChart chart, ChartAnimator animator, ViewPortHandler viewPortHandler, Entry highestEntry) {
        super(chart, animator, viewPortHandler);
        this.highestEntry = highestEntry;

        // 初始化红点画笔
        redPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        redPaint.setColor(Color.RED);
        redPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    public void drawExtras(Canvas c) {
        super.drawExtras(c);

        // 如果最高点存在，则在它上面绘制红点
        if (highestEntry != null) {
            // 1. 获取对应左轴的坐标转换器
            Transformer transformer = mChart.getTransformer(YAxis.AxisDependency.LEFT);

            // 2. 关键修复：使用库底层标准的 MPPointD 接收转换后的绝对屏幕像素坐标
            com.github.mikephil.charting.utils.MPPointD point = transformer.getPixelForValues(highestEntry.getX(), highestEntry.getY());

            // 3. 绘制红点（由于 Canvas.drawCircle 接收 float，记得将 double 强转为 float）
            c.drawCircle((float) point.x, (float) point.y, 10f, redPaint);

            // 4. 回收 MPPointD 实例以避免内存泄漏
            com.github.mikephil.charting.utils.MPPointD.recycleInstance(point);
        }
    }
}
