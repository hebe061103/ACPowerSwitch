package com.zt.acpowerswitch;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class FluidBubbleView extends View {
    private Paint fluidPaint;
    private int fluidColor = Color.parseColor("#39FF14"); // 科技荧光绿

    private float animationPhase = 0f;
    private final List<DropBubble> bubbles = new ArrayList<>();
    private final Random random = new Random();
    private long lastBubbleTime = 0;

    // 控制是否正在充电的状态开关
    private boolean isCharging = true;

    public FluidBubbleView(Context context) { super(context); init(); }
    public FluidBubbleView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public FluidBubbleView(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(); }

    private void init() {
        fluidPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fluidPaint.setStyle(Paint.Style.FILL);

        // 全局驱动动画
        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(2600);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            // 【核心修改点】无论充不充电，全局动画都必须不断刷新，以驱动残留水滴的移动和液面的呼吸
            animationPhase = animation.getAnimatedFraction();
            generateAndUpdateBubbles();
            invalidate();
        });
        animator.start();
    }

    /**
     * 外部调用的核心方法：动态更新流体颜色、电量以及【充电状态】
     */
    public void updateConfig(int color, boolean isCharging) {
        boolean needInvalidate = false;

        // 如果颜色变了，更新颜色并准备重绘
        if (this.fluidColor != color) {
            this.fluidColor = color;
            needInvalidate = true;
        }

        // 如果充电状态变了，更新状态并准备重绘
        if (this.isCharging != isCharging) {
            this.isCharging = isCharging;
            needInvalidate = true;
        }

        if (needInvalidate) {
            invalidate();
        }
    }

    /** 核心逻辑：管理水滴的产生、上升和消亡 */
    private void generateAndUpdateBubbles() {
        long now = System.currentTimeMillis();
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) return;

        float cx = width / 2f;
        float targetY = 15f;

        // 【核心修改点1】只有在 isCharging 为 true（充电中）时，才会往列表里泵入新水滴
        if (isCharging) {
            if (now - lastBubbleTime > 250 && bubbles.size() < 16) {
                DropBubble b = new DropBubble();
                b.x = cx + (random.nextFloat() * 30f - 15f);
                b.y = height - 5f;
                b.maxRadius = 5f + random.nextFloat() * 5f;
                b.radius = 1f;
                b.speed = 0.6f + random.nextFloat();
                b.wobbleOffset = random.nextFloat() * 100f;
                bubbles.add(b);
                lastBubbleTime = now;
            }
        }

        // 【核心修改点2】无论充不充电，只要屏幕里还有旧水滴，就继续让它们向上漂流直至自然消融淡出
        Iterator<DropBubble> iterator = bubbles.iterator();
        while (iterator.hasNext()) {
            DropBubble b = iterator.next();
            b.y -= b.speed; // 现存水滴保持丝滑升华移动

            b.x += (float) Math.sin(animationPhase * 2 * Math.PI + b.wobbleOffset) * 0.2f;

            float totalDistance = (height - 5f) - targetY;
            float currentDistance = b.y - targetY;

            if (currentDistance < 0) {
                iterator.remove();
                continue;
            }

            // 平滑控制尺寸变化（两头小、中间大，快到顶部时无缝淡出缩小）
            if (currentDistance > totalDistance - 25f) {
                float birthRatio = (totalDistance - currentDistance) / 25f;
                b.radius = b.maxRadius * birthRatio;
            } else if (currentDistance < 80f) {
                float absorbRatio = currentDistance / 80f;
                b.radius = b.maxRadius * absorbRatio;
            } else {
                b.radius = b.maxRadius;
            }

            if (b.y <= targetY) {
                iterator.remove();
            }
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) return;

        float cx = width / 2f;

        // ==================== 1. 绘制极低的底部液面（不充电时依然呼吸长存） ====================
        @SuppressLint("DrawAllocation") Path poolPath = new Path();
        float poolBaseY = height - 5f;
        float poolWave = (float) Math.cos(animationPhase * 2 * Math.PI) * 1.5f;

        poolPath.moveTo(0, height);
        poolPath.lineTo(width, height);
        poolPath.lineTo(width, poolBaseY + poolWave);
        poolPath.cubicTo(width * 0.7f, poolBaseY + poolWave,
                cx + 15f, poolBaseY - 3f + poolWave,
                cx, poolBaseY - 5f + poolWave);
        poolPath.cubicTo(cx - 15f, poolBaseY - 3f + poolWave,
                width * 0.3f, poolBaseY + poolWave,
                0, poolBaseY + poolWave);
        poolPath.close();
        fluidPaint.setColor(fluidColor);
        canvas.drawPath(poolPath, fluidPaint);

        // ==================== 2. 绘制水滴（断电后，现有的水滴会优雅地向上漂完并自然淡出消融） ====================
        for (DropBubble b : bubbles) {
            if (b.radius <= 0.5f) continue;

            @SuppressLint("DrawAllocation") RectF bubbleRect = new RectF(b.x - b.radius, b.y - b.radius * 1.2f,
                    b.x + b.radius, b.y + b.radius * 1.2f);
            canvas.drawOval(bubbleRect, fluidPaint);

            // 内部发光核心
            fluidPaint.setColor(Color.parseColor("#E0FFD0"));
            canvas.drawCircle(b.x, b.y - b.radius * 0.25f, b.radius * 0.35f, fluidPaint);
            fluidPaint.setColor(fluidColor);
        }
    }

    private static class DropBubble {
        float x;
        float y;
        float radius;
        float maxRadius;
        float speed;
        float wobbleOffset;
    }
}
