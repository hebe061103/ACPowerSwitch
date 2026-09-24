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
            animationPhase = animation.getAnimatedFraction();
            generateAndUpdateBubbles();
            invalidate();
        });
        animator.start();
    }

    /**
     * 外部调用的核心方法：动态更新流体颜色以及【充电状态】
     */
    public void updateConfig(int color, boolean isCharging) {
        boolean needInvalidate = false;
        if (this.fluidColor != color) {
            this.fluidColor = color;
            needInvalidate = true;
        }
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

        // 充电中，且屏幕内水滴不超过 12 颗（密度稍微降低，防止大水滴叠在一起显得杂乱）
        if (isCharging && now - lastBubbleTime > 280 && bubbles.size() < 12) {
            DropBubble b = new DropBubble();
            // 扩散跨度稍微拉大一点点
            b.x = cx + (random.nextFloat() * 34f - 17f);
            b.y = height - 5f;

            // 🔥【修改点 1】拉大半径差距：随机基础半径范围从原来的 5~10 放大到 3.5~12 像素
            // 大水滴能达到小水滴的近 4 倍体量，肉眼看过去分辨极其容易
            b.maxRadius = 3.5f + random.nextFloat() * 8.5f;
            b.radius = 1f;

            // 大水滴重，速度稍慢；小水滴轻，上升稍快
            b.speed = 0.6f + (12f - b.maxRadius) * 0.08f + random.nextFloat() * 0.4f;
            b.wobbleOffset = random.nextFloat() * 100f;

            bubbles.add(b);
            lastBubbleTime = now;
        }

        // 刷新每一颗水滴的状态
        Iterator<DropBubble> iterator = bubbles.iterator();
        while (iterator.hasNext()) {
            DropBubble b = iterator.next();
            b.y -= b.speed;

            // 摇晃弧度也根据大小调整，小水滴晃得更活泼
            float wobbleScale = b.maxRadius > 8f ? 0.15f : 0.3f;
            b.x += (float) Math.sin(animationPhase * 2 * Math.PI + b.wobbleOffset) * wobbleScale;

            float totalDistance = (height - 5f) - targetY;
            float currentDistance = b.y - targetY;

            if (currentDistance < 0) {
                iterator.remove();
                continue;
            }

            // 生命周期内的尺寸过渡
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

        // ==================== 1. 绘制极低的底部液面 ====================
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

        // ==================== 2. 绘制大小错落的精致水滴 ====================
        for (DropBubble b : bubbles) {
            if (b.radius <= 0.5f) continue;

            // 🔥【修改点 2】动态拉伸比例：
            // 大水滴（半径>8）如果拉伸太长会失真，所以给 1.15 倍，让它圆润饱满
            // 小水滴（半径<=8）给 1.35 倍拉伸，看起来像尖尖的小流体，对比非常强烈明显
            float stretchFactor = b.maxRadius > 8f ? 1.15f : 1.35f;

            @SuppressLint("DrawAllocation") RectF bubbleRect = new RectF(b.x - b.radius, b.y - b.radius * stretchFactor,
                    b.x + b.radius, b.y + b.radius * stretchFactor);
            canvas.drawOval(bubbleRect, fluidPaint);

            // 内部发光核心：大小也要自适应水滴本身的半径
            fluidPaint.setColor(Color.parseColor("#E0FFD0"));
            canvas.drawCircle(b.x, b.y - b.radius * 0.25f, b.radius * 0.33f, fluidPaint);
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
