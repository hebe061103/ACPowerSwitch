package com.zt.acpowerswitch;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class FluidBubbleView extends View {
    private Paint ringPaint;
    private Paint fluidPaint;
    private Paint textPaint;
    private Paint glowPaint;

    // ===== 太阳辐射专用画笔（金黄色）=====
    private Paint sunRayPaint;
    private static final int SUN_COLOR = Color.parseColor("#FFD700");

    private float progress = 0f;                  // 电池电量百分比（0~100）
    private int fluidColor = Color.parseColor("#39FF14"); // 流体/粒子颜色

    private float animationPhase = 0f;            // 全局动画相位（0~1循环）
    private final List<ChargeParticle> chargeParticles = new ArrayList<>();     // 充电粒子列表
    private final List<DischargeDrop> dischargeDrops = new ArrayList<>();       // 放电粒子列表
    private final Random random = new Random();
    private long lastChargeSpawn = 0;     // 上次生成充电粒子的时间戳
    private long lastDischargeSpawn = 0;  // 上次生成放电粒子的时间戳

    private float chargeCurrent = 0f;       // 当前充电电流（A）
    private float dischargeCurrent = 0f;    // 当前放电电流（A）
    private float maxchargerCurrent = 0f;   // 最大充电/放电电流上限（A）

    private float smoothPhase1 = 0f;    // 流体环噪声相位1（控制波形1）
    private float smoothPhase2 = 0f;    // 流体环噪声相位2（控制波形2）
    private float smoothPhase3 = 0f;    // 流体环噪声相位3（控制波形3）
    private float smoothAmp = 1.0f;     // 流体环波形振幅平滑值
    private final Random noiseRandom = new Random();

    private final Path circleClipPath = new Path();  // 圆形裁剪路径（用于内部液面）
    private final Path lightWavePath = new Path();   // 高光液面波形路径
    private final Path fluidRingPath = new Path();   // 外圈流体环路径
    private final Path wavePath = new Path();        // 主液面波形路径
    private final Path dropPath = new Path();        // 放电粒子（水滴形）路径
    private final RectF bubbleRect = new RectF();    // 复用矩形（绘制椭圆/光晕用）

    // ===== 坐标绑定（太阳能板、太阳、房子的屏幕坐标）=====
    private float solarX = -1f, solarY = -1f;    // 太阳能板中心
    private float sunX = -1f, sunY = -1f;        // 太阳中心（太阳能板右上）
    private float houseX = -1f, houseY = -1f;    // 房子中心
    private float solarIconSizeDp = 80f;         // 太阳能板图标尺寸（dp）
    private boolean coords_Bound = false;        // 坐标是否已绑定完成

    private float sunPhase = 0f;                 // 太阳辐射旋转相位（0~1）
    private float sunWobblePhase = 0f;           // 太阳光芒抖动相位
    private final Random sunRandom = new Random();

    // ===== 构造函数 =====
    public FluidBubbleView(Context context) { super(context); init(); }
    public FluidBubbleView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public FluidBubbleView(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(); }

    // ===== dp转px工具方法 =====
    private float dp2px(float dp) {
        return dp * getContext().getResources().getDisplayMetrics().density;
    }

    // ===== 初始化所有画笔 + 启动2200ms循环动画 =====
    private void init() {
        ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(dp2px(4.5f));
        ringPaint.setStrokeCap(Paint.Cap.ROUND);

        fluidPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fluidPaint.setStyle(Paint.Style.FILL);

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        // 太阳辐射画笔
        sunRayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sunRayPaint.setStyle(Paint.Style.STROKE);
        sunRayPaint.setStrokeCap(Paint.Cap.ROUND);

        // 启动无限循环动画，每2200ms一轮，驱动粒子运动和重绘
        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(2200);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            animationPhase = animation.getAnimatedFraction();
            updateParticles();   // 每帧更新所有粒子位置
            invalidate();        // 触发onDraw重绘
        });
        animator.start();
    }

    // ===== 外部调用：更新配置参数（电量、颜色、电流值）=====
    public void updateConfig(float progress, int color,
                             float chargeCurrent, float dischargeCurrent, float maxchargerCurrent) {
        boolean needInvalidate = false;
        if (this.progress != progress) { this.progress = progress; needInvalidate = true; }
        if (this.fluidColor != color) { this.fluidColor = color; needInvalidate = true; }
        if (this.chargeCurrent != chargeCurrent) { this.chargeCurrent = chargeCurrent; needInvalidate = true; }
        if (this.dischargeCurrent != dischargeCurrent) { this.dischargeCurrent = dischargeCurrent; needInvalidate = true; }
        if (this.maxchargerCurrent != maxchargerCurrent) { this.maxchargerCurrent = maxchargerCurrent; needInvalidate = true; }
        if (needInvalidate) invalidate();
    }

    // ===== 坐标绑定：从Activity传入太阳能板和房子图标，计算它们在View中的坐标 =====
    public void bindIconCoords(ImageView solarIcon, ImageView houseIcon) {
        if (solarIcon == null || houseIcon == null) return;

        solarIcon.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        // 只执行一次
                        solarIcon.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                        int[] iconLoc = new int[2];
                        int[] myLoc = new int[2];

                        solarIcon.getLocationInWindow(iconLoc);
                        getLocationInWindow(myLoc);

                        float sw = solarIcon.getWidth();
                        float sh = solarIcon.getHeight();

                        solarX = (iconLoc[0] - myLoc[0]) + sw / 2f;
                        solarY = (iconLoc[1] - myLoc[1]) + sh / 2f;

                        sunX = (iconLoc[0] - myLoc[0]) + sw * 0.8f;
                        sunY = (iconLoc[1] - myLoc[1]) + sh * 0.25f;

                        houseIcon.getLocationInWindow(iconLoc);
                        float hw = houseIcon.getWidth();
                        float hh = houseIcon.getHeight();
                        houseX = (iconLoc[0] - myLoc[0]) + hw / 2f;
                        houseY = (iconLoc[1] - myLoc[1]) + hh / 2f;

                        coords_Bound = true;
                        invalidate();
                    }
                });
    }

    // ===== 设置太阳能板图标尺寸（影响太阳辐射大小）=====
    public void setSolarSize(float dp) { this.solarIconSizeDp = dp; }

    // ==================== 电流→速度/数量 映射函数 ====================

    // 充电电流→比率（0~1），用于计算粒子速度、生成间隔、大小
    private float chargeRatio() {
        return (float) Math.pow(Math.min(1.0f, chargeCurrent / 50f), 0.5f);
    }

    // 放电电流→比率（0~1），用于计算粒子速度、生成间隔、大小
    private float dischargeRatio() {
        return (float) Math.pow(Math.min(1.0f, dischargeCurrent / maxchargerCurrent), 0.5f);
    }

    // ==================== 粒子逻辑更新（每帧调用）====================
    private void updateParticles() {
        long now = System.currentTimeMillis();
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) return;

        float ringWidthPx = dp2px(70f);
        float cx = width - (ringWidthPx / 2f) - dp2px(2f);       // 电池环中心X
        float ringRadius = (ringWidthPx - dp2px(12f)) / 2f;        // 电池环半径
        float cy = ringRadius + dp2px(6f);                         // 电池环中心Y

        // 房子目标坐标：优先用绑定坐标，否则 fallback 到硬编码
        float targetHouseX = coords_Bound ? houseX : width - dp2px(45f);
        float targetHouseY = coords_Bound ? houseY : height - dp2px(55f);

        // ===== 流体环噪声更新（让外圈波形有随机抖动感）=====
        smoothPhase1 += 0.017f + (noiseRandom.nextFloat() * 0.006f - 0.003f);
        smoothPhase2 += 0.023f + (noiseRandom.nextFloat() * 0.006f - 0.003f);
        smoothPhase3 += 0.009f + (noiseRandom.nextFloat() * 0.004f - 0.002f);
        smoothAmp += (noiseRandom.nextFloat() * 0.02f - 0.01f);
        smoothAmp = Math.max(0.92f, Math.min(1.08f, smoothAmp));

        // ===== 充电粒子生成与运动 =====
        float cRatio = chargeRatio();
        if (chargeCurrent > 0.1f) {
            long interval = (long) (800 - 740 * cRatio);   // 电流越大生成越快
            int maxCount = 3 + (int) (27 * cRatio);         // 电流越大同时存在越多

            int count = 0;
            for (ChargeParticle p : chargeParticles) { if (!p.dead) count++; }

            // 生成新充电粒子（从太阳能板飞向电池环）
            if (now - lastChargeSpawn > interval && count < maxCount) {
                ChargeParticle p = new ChargeParticle();
                float targetSolarX = coords_Bound ? solarX : dp2px(50f);
                float targetSolarY = coords_Bound ? solarY : dp2px(50f);
                p.x = targetSolarX + (random.nextFloat() - 0.5f) * dp2px(20f);
                p.y = targetSolarY + (random.nextFloat() - 0.5f) * dp2px(20f);

                p.maxRadius = dp2px(6.5f) + cRatio * dp2px(3.0f);
                p.radius = p.maxRadius * 0.2f;
                p.growPhase = 0f;
                p.absorbPhase = 0f;

                float baseSpeed;
                if (cRatio < 0.3f) {
                    // 小电流：速度压到极低，0.05 ~ 0.15 之间
                    baseSpeed = dp2px(0.05f) + (cRatio / 0.3f) * dp2px(0.1f);
                } else {
                    // 大电流：正常线性增长
                    baseSpeed = dp2px(0.15f) + ((cRatio - 0.3f) / 0.7f) * dp2px(2.5f);
                }
                baseSpeed = Math.min(baseSpeed, dp2px(3.0f));
                p.speedX = baseSpeed + random.nextFloat() * dp2px(0.4f);
                p.speedY = (cy - p.y) / ((cx - ringRadius * 0.6f - p.x) / p.speedX);
                p.wobbleOffset = random.nextFloat() * 100f;
                chargeParticles.add(p);
                lastChargeSpawn = now;
            }
        }

        // 充电粒子运动更新
        Iterator<ChargeParticle> ci = chargeParticles.iterator();
        while (ci.hasNext()) {
            ChargeParticle p = ci.next();
            if (p.dead) { ci.remove(); continue; }

            p.x += p.speedX;
            p.y += p.speedY;

            // 正弦摆动（让飞行轨迹有飘动感）
            float wobble = dp2px(0.15f) - cRatio * dp2px(0.12f);
            p.y += (float) Math.sin(animationPhase * 2 * Math.PI + p.wobbleOffset) * wobble;

            float targetX = cx - ringRadius * 0.6f;
            float distToBall = targetX - p.x;

            // 接近电池环时被吸入
            if (distToBall <= dp2px(22f) && distToBall > 0) {
                p.absorbPhase += 0.05f;
                float absorbProgress = Math.min(1f, p.absorbPhase);

                p.speedX *= 0.88f;   // 减速
                p.speedY *= 0.88f;

                p.radius = p.maxRadius * (0.4f + 0.8f * absorbProgress);

                // 被吸入的拉力
                float pullStrength = 1f - (distToBall / dp2px(22f));
                p.x += (targetX - p.x) * pullStrength * 0.25f;
                p.y += (cy - p.y) * pullStrength * 0.12f;

                p.growPhase = absorbProgress;

                if (distToBall <= dp2px(2f)) {
                    p.dead = true;   // 到达中心，标记死亡
                }
            } else if (p.x > cx + ringRadius) {
                ci.remove();   // 飞出边界，移除
            }
        }

        // ===== 放电粒子生成与运动 =====
        float dRatio = dischargeRatio();
        if (dischargeCurrent > 0.1f) {
            long interval = (long) (800 - 740 * dRatio);
            int maxCount = 1 + (int) (10 * dRatio);

            int count = 0;
            for (DischargeDrop p : dischargeDrops) { if (p.state != 3) count++; }

            // 生成新放电粒子（从电池环飞向房子）
            if (now - lastDischargeSpawn > interval && count < maxCount) {
                DischargeDrop drop = new DischargeDrop();
                float angle = (float) (Math.PI * 0.5f + (random.nextFloat() - 0.5f) * 0.35f);
                drop.x = cx + (float) Math.cos(angle) * ringRadius * 0.9f;
                drop.y = cy + (float) Math.sin(angle) * ringRadius * 0.9f;
                drop.maxRadius = dp2px(2.0f) + dRatio * dp2px(1.0f);
                drop.radius = drop.maxRadius * 0.15f;
                drop.state = 0;
                drop.growTimer = 0f;
                drop.shrinkTimer = 0f;
                drop.tailLength = 0f;
                drop.neckLength = 0f;
                drop.wobbleOffset = random.nextFloat() * 100f;
                dischargeDrops.add(drop);
                lastDischargeSpawn = now;
            }
        }

        // 放电粒子状态机更新
        Iterator<DischargeDrop> di = dischargeDrops.iterator();
        while (di.hasNext()) {
            DischargeDrop p = di.next();
            if (p.state == 3) { di.remove(); continue; }

            if (p.state == 0) {
                // 状态0：在电池环上生长（从小变大，长出尾巴和脖子）
                p.growTimer += 0.035f;
                float growProgress = Math.min(1f, p.growTimer);

                p.radius = p.maxRadius * (0.15f + 0.85f * growProgress);
                p.tailLength = growProgress * p.maxRadius * 2.2f;
                p.neckLength = growProgress * p.maxRadius * 1.2f;

                p.y += dp2px(0.25f) * growProgress;

                if (growProgress >= 1f) {
                    // 生长完成，进入飞行状态
                    p.state = 1;
                    float flySpeed = dp2px(0.1f) + dRatio * dp2px(1.5f);
                    flySpeed = Math.min(flySpeed, dp2px(3.0f));
                    float dx = targetHouseX - p.x;
                    float dy = targetHouseY - p.y;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);
                    p.speedX = (dx / dist) * flySpeed;
                    p.speedY = (dy / dist) * flySpeed;
                    p.breakAnim = 0f;
                }

            } else if (p.state == 1) {
                // 状态1：飞向房子
                float flySpeed = dp2px(0.2f) + dRatio * dp2px(2.5f);
                flySpeed = Math.min(flySpeed, dp2px(3.0f));

                float dx = targetHouseX - p.x;
                float dy = targetHouseY - p.y;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                if (dist > 0) {
                    p.speedX = (dx / dist) * flySpeed;
                    p.speedY = (dy / dist) * flySpeed;
                }

                p.x += p.speedX;
                p.y += p.speedY;

                p.tailLength *= 0.92f;    // 尾巴逐渐缩短
                p.neckLength *= 0.9f;     // 脖子逐渐缩短

                if (dist <= dp2px(18f)) {
                    p.state = 2;           // 到达房子附近，进入收缩消失
                    p.shrinkTimer = 0f;
                }

            } else if (p.state == 2) {
                // 状态2：到达房子后，先炸出金色光爆再消失
                p.shrinkTimer += 0.04f;
                float shrinkProgress = Math.min(1f, p.shrinkTimer);

                // 向内收缩效果
                float pullStrength = shrinkProgress * 0.3f;
                p.x += (targetHouseX - p.x) * pullStrength * 0.15f;
                p.y += (targetHouseY - p.y) * pullStrength * 0.15f;

                if (shrinkProgress >= 1f) {
                    p.state = 3;   // 消失
                }
            }
            // 飞出屏幕边界则移除
            if (p.x > width + dp2px(30f) || p.y > height + dp2px(30f) ||
                    p.x < -dp2px(30f) || p.y < -dp2px(30f)) {
                p.state = 3;
            }
        }
        // ===== 太阳辐射旋转相位更新（噪声驱动，电流越大转越快）=====
        float sunPhaseSpeed = 0.05f + 0.1f * chargeRatio();
        float speedNoise = (sunRandom.nextFloat() - 0.5f) * 0.3f;
        sunPhase += (sunPhaseSpeed + speedNoise) / 60f;
        if (sunPhase > 1f) sunPhase -= 1f;

        sunWobblePhase += 0.02f + sunRandom.nextFloat() * 0.01f;
    }

    // ==================== 绘制（每帧调用）====================
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) return;

        float ringWidthPx = dp2px(70f);
        float cx = width - (ringWidthPx / 2f) - dp2px(2f);
        float ringRadius = (ringWidthPx - dp2px(12f)) / 2f;
        float cy = ringRadius + dp2px(6f);

        // ===== 1. 太阳辐射（金黄色光芒，仅叠加在太阳能板右上角）=====
        drawSolarRadiation(canvas);

        // ===== 2. 外圈流体环（带噪声波形）=====
        ringPaint.setColor(fluidColor);
        ringPaint.setAntiAlias(true);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeCap(Paint.Cap.ROUND);
        ringPaint.setStrokeJoin(Paint.Join.ROUND);

        fluidRingPath.reset();
        int count = 120;
        float sweepAngle = 360f;

        for (int i = 0; i <= count; i++) {
            float t = (float) i / count;
            float angleDeg = -90f + (sweepAngle * t);
            float angleRad = (float) Math.toRadians(angleDeg);

            // 三层噪声叠加让环有有机波动感
            float noise1 = (float) Math.sin(t * Math.PI * 2 * 3 + smoothPhase1) * dp2px(1.4f);
            float noise2 = (float) Math.cos(t * Math.PI * 2 * 5 - smoothPhase2) * dp2px(0.8f);
            float noise3 = (float) Math.sin(t * Math.PI * 2 * 2 + smoothPhase3) * dp2px(0.6f);
            float currentWave = (noise1 + noise2 + noise3) * smoothAmp * 0.9f;

            float dynamicRadius = ringRadius + currentWave;
            float px = cx + (float) Math.cos(angleRad) * dynamicRadius;
            float py = cy + (float) Math.sin(angleRad) * dynamicRadius;

            if (i == 0) fluidRingPath.moveTo(px, py);
            else fluidRingPath.lineTo(px, py);
        }
        fluidRingPath.close();
        canvas.drawPath(fluidRingPath, ringPaint);

        // ===== 3. 内部液面（裁剪到流体环内）=====
        canvas.save();
        circleClipPath.reset();
        circleClipPath.addPath(fluidRingPath);
        canvas.clipPath(circleClipPath);

        float fluidY = cy + ringRadius - (2f * ringRadius * (progress / 100f));

        if (progress > 0f) {
            // 主液面波形
            wavePath.reset();
            float waveBias1 = (float) Math.sin(animationPhase * 2 * Math.PI) * dp2px(3f);
            float waveBias2 = (float) Math.cos(animationPhase * 2 * Math.PI) * dp2px(2f);

            wavePath.moveTo(cx - ringRadius, height);
            wavePath.lineTo(cx - ringRadius, fluidY + waveBias1);
            wavePath.quadTo(cx - ringRadius * 0.5f, fluidY + waveBias1 + dp2px(3f), cx, fluidY + waveBias2);
            wavePath.quadTo(cx + ringRadius * 0.5f, fluidY + waveBias2 - dp2px(3f), cx + ringRadius, fluidY + waveBias1);
            wavePath.lineTo(cx + ringRadius, height);
            wavePath.close();

            fluidPaint.setColor(Color.argb(180, Color.red(fluidColor), Color.green(fluidColor), Color.blue(fluidColor)));
            canvas.drawPath(wavePath, fluidPaint);

            // 高光液面（更透明，偏移量不同）
            lightWavePath.reset();
            float lightBias1 = (float) Math.cos(-animationPhase * 2 * Math.PI + 1.5f) * dp2px(2.5f);
            float lightBias2 = (float) Math.sin(-animationPhase * 2 * Math.PI) * dp2px(1.5f);

            lightWavePath.moveTo(cx - ringRadius, height);
            lightWavePath.lineTo(cx - ringRadius, fluidY + dp2px(1f) + lightBias1);
            lightWavePath.quadTo(cx, fluidY + lightBias2, cx + ringRadius, fluidY + dp2px(1f) + lightBias1);
            lightWavePath.lineTo(cx + ringRadius, height);
            lightWavePath.close();

            fluidPaint.setColor(Color.argb(60, Color.red(fluidColor), Color.green(fluidColor), Color.blue(fluidColor)));
            canvas.drawPath(lightWavePath, fluidPaint);

            fluidPaint.setColor(fluidColor);
        }
        canvas.restore();

        // ===== 4. 充电粒子绘制（从太阳能板飞向电池环）=====
        for (ChargeParticle p : chargeParticles) {
            if (p.radius <= 0.3f || p.dead) continue;

            // 萤火虫发光效果：电流越小越亮（低电流时粒子移动慢，加光晕）
            if (chargeCurrent > 0f && chargeCurrent < 10f) {
                float glowIntensity = 1f - (chargeCurrent / 15f);
                glowIntensity = glowIntensity * glowIntensity;

                float breath = 0.6f + 0.4f * (float) Math.sin(animationPhase * Math.PI * 6 + p.wobbleOffset);

                int layers = 3;
                for (int g = 0; g < layers; g++) {
                    float layerRatio = (g + 1f) / layers;
                    float layerRadius = p.radius * (2.5f + 3.5f * glowIntensity * layerRatio);
                    int alpha = (int) (70 * glowIntensity * (1f - layerRatio) * breath);

                    glowPaint.setColor(fluidColor);
                    glowPaint.setAlpha(Math.max(0, Math.min(255, alpha)));
                    bubbleRect.set(p.x - layerRadius, p.y - layerRadius,
                            p.x + layerRadius, p.y + layerRadius);
                    canvas.drawOval(bubbleRect, glowPaint);
                }
                glowPaint.setAlpha(255);
            }

            // 被吸入时的光晕扩散
            if (p.absorbPhase > 0) {
                float ap = Math.min(1f, p.absorbPhase);

                glowPaint.setColor(fluidColor);
                glowPaint.setAlpha((int) (50 * ap));
                float glowR = p.radius * (1.5f + ap * 1.5f);
                bubbleRect.set(p.x - glowR, p.y - glowR, p.x + glowR, p.y + glowR);
                canvas.drawOval(bubbleRect, glowPaint);

                glowPaint.setAlpha((int) (100 * ap));
                glowR = p.radius * (1.2f + ap * 0.8f);
                bubbleRect.set(p.x - glowR, p.y - glowR, p.x + glowR, p.y + glowR);
                canvas.drawOval(bubbleRect, glowPaint);

                glowPaint.setAlpha(255);
            }

            // 粒子本体
            float drawW = p.radius * (p.absorbPhase > 0 ? 1.3f : 1.15f);
            float drawH = p.radius * (p.absorbPhase > 0 ? 1.1f : 1.0f);
            bubbleRect.set(p.x - drawW, p.y - drawH, p.x + drawW * 1.2f, p.y + drawH);
            canvas.drawOval(bubbleRect, fluidPaint);
        }

        // ===== 5. 放电粒子绘制（从电池环飞向房子）=====
        for (DischargeDrop p : dischargeDrops) {
            if (p.state == 3 || p.radius <= 0.3f) continue;

            if (p.state == 0) {
                // 生长状态：画水滴形（身体+脖子+尾巴）
                dropPath.reset();
                float bodyTop = p.y - p.radius;
                float bodyBottom = p.y + p.radius;
                float bodyLeft = p.x - p.radius * 0.8f;
                float bodyRight = p.x + p.radius * 0.8f;

                dropPath.addOval(bodyLeft, bodyTop, bodyRight, bodyBottom, Path.Direction.CW);

                if (p.neckLength > 0) {
                    dropPath.moveTo(p.x - p.radius * 0.4f, bodyTop);
                    dropPath.lineTo(p.x - p.radius * 0.3f, bodyTop - p.neckLength);
                    dropPath.lineTo(p.x + p.radius * 0.3f, bodyTop - p.neckLength);
                    dropPath.lineTo(p.x + p.radius * 0.4f, bodyTop);
                    dropPath.close();
                }

                if (p.tailLength > 0) {
                    dropPath.moveTo(p.x - p.radius * 0.5f, bodyBottom);
                    dropPath.lineTo(p.x - p.radius * 0.3f, bodyBottom + p.tailLength);
                    dropPath.lineTo(p.x + p.radius * 0.3f, bodyBottom + p.tailLength);
                    dropPath.lineTo(p.x + p.radius * 0.5f, bodyBottom);
                    dropPath.close();
                }

                canvas.drawPath(dropPath, fluidPaint);

            } else if (p.state == 1) {
                // 飞行状态：拉伸椭圆 + 萤火虫发光（电流越小越亮）
                if (dischargeCurrent > 0f && dischargeCurrent < maxchargerCurrent * 0.1f) {
                    float glowIntensity = 1f - (dischargeCurrent / (maxchargerCurrent * 0.4f));
                    glowIntensity = glowIntensity * glowIntensity;

                    float breath = 0.6f + 0.4f * (float) Math.sin(animationPhase * Math.PI * 6 + p.wobbleOffset);

                    int layers = 3;
                    for (int g = 0; g < layers; g++) {
                        float layerRatio = (g + 1f) / layers;
                        float layerRadius = p.radius * (2f + 3f * glowIntensity * layerRatio);
                        int alpha = (int) (60 * glowIntensity * (1f - layerRatio) * breath);

                        glowPaint.setColor(fluidColor);
                        glowPaint.setAlpha(Math.max(0, Math.min(255, alpha)));
                        bubbleRect.set(p.x - layerRadius, p.y - layerRadius,
                                p.x + layerRadius, p.y + layerRadius);
                        canvas.drawOval(bubbleRect, glowPaint);
                    }
                    glowPaint.setAlpha(255);
                }

                // 飞行拉伸本体
                float stretch = (float) Math.sqrt(p.speedX * p.speedX + p.speedY * p.speedY);
                float drawW = p.radius + stretch * 0.35f;
                float drawH = p.radius + stretch * 0.15f;
                bubbleRect.set(p.x - drawW, p.y - drawH, p.x + drawW, p.y + drawH);
                canvas.drawOval(bubbleRect, fluidPaint);

            } else if (p.state == 2) {
                // 收缩消失状态：光晕扩散 + 本体淡出
                float sp = Math.min(1f, p.shrinkTimer);

                int glowAlpha = (int) (180 * (1f - sp));
                if (glowAlpha > 0) {
                    glowPaint.setColor(fluidColor);
                    glowPaint.setAlpha(glowAlpha);
                    float rippleR = p.radius * (1f + sp * 2.5f);
                    bubbleRect.set(p.x - rippleR, p.y - rippleR, p.x + rippleR, p.y + rippleR);
                    canvas.drawOval(bubbleRect, glowPaint);
                    glowPaint.setAlpha(255);
                }

                int bodyAlpha = (int) (255 * (1f - sp * 0.7f));
                if (bodyAlpha > 0) {
                    fluidPaint.setAlpha(bodyAlpha);
                    bubbleRect.set(p.x - p.radius * 0.8f, p.y - p.radius * 0.8f,
                            p.x + p.radius * 0.8f, p.y + p.radius * 0.8f);
                    canvas.drawOval(bubbleRect, fluidPaint);
                    fluidPaint.setAlpha(255);
                }
            }
        }

        // ===== 6. 中心电量文字 =====
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textY = cy + (fm.bottom - fm.top) / 2f - fm.bottom;
        textPaint.setTextSize(dp2px(10f));
        textPaint.setColor(Color.parseColor("#1A1A1A"));
        canvas.drawText(
                String.format(java.util.Locale.getDefault(), "%.1f%%", progress),
                cx, textY, textPaint
        );
    }

    // ===== 太阳辐射绘制（太阳图标右上角的旋转光芒+光晕）=====
    private void drawSolarRadiation(Canvas canvas) {
        if (!coords_Bound || chargeCurrent < 0.1f) return;

        float cRatio = Math.min(chargeCurrent / 40f, 1f);
        float sunR = dp2px(solarIconSizeDp) * 0.18f;
        if (sunR < dp2px(6f)) sunR = dp2px(6f);

        // 旋转角度（噪声驱动）
        float baseAngle = 360f * sunPhase;

        // 光晕（带随机脉冲呼吸）
        float breath = 0.85f + 0.15f * ((sunRandom.nextFloat() - 0.5f) * 2f);
        float glowAlpha = (50 * cRatio) * breath;
        glowPaint.setColor(SUN_COLOR);
        glowPaint.setAlpha((int) Math.min(255, glowAlpha));
        canvas.drawCircle(sunX, sunY, sunR * (2.5f + 1.5f * cRatio), glowPaint);

        // 长芒 8根（角度加随机抖动）
        sunRayPaint.setColor(SUN_COLOR);
        sunRayPaint.setStrokeWidth(dp2px(1.5f));
        sunRayPaint.setAlpha((int) (200 * cRatio));
        float longRayLen = sunR * (1.2f + 0.8f * cRatio);

        for (int i = 0; i < 8; i++) {
            float jitter = (float) Math.sin(sunWobblePhase + i * 1.7f) * 3f * cRatio;
            float a = baseAngle + 45f * i + jitter;
            double rad = Math.toRadians(a);

            float sx = sunX + (float) Math.cos(rad) * sunR;
            float sy = sunY + (float) Math.sin(rad) * sunR;
            float ex = sunX + (float) Math.cos(rad) * (sunR + longRayLen);
            float ey = sunY + (float) Math.sin(rad) * (sunR + longRayLen);
            canvas.drawLine(sx, sy, ex, ey, sunRayPaint);
        }

        // 短芒 4~12根（反向旋转）
        int shortRayCount = 4 + (int) (8 * cRatio);
        float shortRayLen = sunR * (0.6f + 0.4f * cRatio);
        float revAngle = -baseAngle * 0.6f;

        sunRayPaint.setStrokeWidth(dp2px(1.8f));
        float pulse = 0.7f + 0.3f * ((sunRandom.nextFloat() - 0.5f) * 2f);
        sunRayPaint.setAlpha((int) (120 + 100 * cRatio * pulse));

        for (int i = 0; i < shortRayCount; i++) {
            float jitter = (float) Math.cos(sunWobblePhase * 0.7f + i * 2.3f) * 2f * cRatio;
            float a = revAngle + (360f / shortRayCount) * i + jitter;
            double rad = Math.toRadians(a);

            float sx = sunX + (float) Math.cos(rad) * sunR * 0.85f;
            float sy = sunY + (float) Math.sin(rad) * sunR * 0.85f;
            float ex = sunX + (float) Math.cos(rad) * (sunR + shortRayLen);
            float ey = sunY + (float) Math.sin(rad) * (sunR + shortRayLen);
            canvas.drawLine(sx, sy, ex, ey, sunRayPaint);
        }

        sunRayPaint.setAlpha(255);
    }

    // ==================== 内部粒子类 ====================

    // 充电粒子：从太阳能板飞向电池环，到达后被吸入消失
    private static class ChargeParticle {
        float x, y;
        float radius;
        float maxRadius;
        float speedX, speedY;
        float wobbleOffset;       // 摆动相位偏移
        float growPhase = 0f;     // 生长进度（预留）
        float absorbPhase = 0f;   // 被吸入进度（0~1）
        boolean dead = false;     // 是否死亡（到达目标后标记）
    }

    // 放电粒子：从电池环飞向房子，有生长→飞行→收缩消失 三阶段
    private static class DischargeDrop {
        float x, y;
        float radius;
        float maxRadius;
        float speedX, speedY;
        float wobbleOffset;
        float growTimer = 0f;     // 生长计时器
        float shrinkTimer = 0f;   // 收缩计时器
        float tailLength = 0f;    // 尾巴长度
        float neckLength = 0f;    // 脖子长度
        float breakAnim = 0f;     // 破碎动画（预留）
        int state = 0;            // 0=生长 1=飞行 2=收缩消失 3=已死亡
    }
}