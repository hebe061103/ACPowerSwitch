package com.zt.acpowerswitch;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class FluidBubbleView extends View {
    // ==================== 云海粒子 ====================
    private final List<FogParticle> fogParticles = new ArrayList<>();
    private final Random fogRand = new Random();

    // ==================== 雨粒子 ====================
    private final List<RainParticle> rainParticles = new ArrayList<>();
    private final Random rainRand = new Random();

    // ==================== 雪粒子 ====================
    private final List<SnowParticle> snowParticles = new ArrayList<>();
    private final Random snowRand = new Random();

    // ==================== 闪电 ====================
    private float lightningTimer = 0f;
    private float lightningAlpha = 0f;
    // 闪电成员变量
    private float lightningX = -1.0f;
    private float lightningSx = -1.0f;
    private final Random lightningRand = new Random();

    // ==================== Paint ====================
    private final Paint fogPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rainPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint snowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lightningPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint ringPaint;
    private Paint fluidPaint;
    private Paint textPaint;
    private Paint glowPaint;
    private static final int SUN_COLOR = Color.parseColor("#FFD700");

    private float progress = 0f;
    private int fluidColor = Color.parseColor("#39FF14");

    private float animationPhase = 0f;
    private final List<ChargeParticle> chargeParticles = new ArrayList<>();
    private final List<DischargeDrop> dischargeDrops = new ArrayList<>();
    private final Random random = new Random();
    private long lastChargeSpawn = 0;
    private long lastDischargeSpawn = 0;

    private float chargeCurrent = 0f;
    private float dischargeCurrent = 0f;
    private float maxchargerCurrent = 0f;

    private float smoothPhase1 = 0f;
    private float smoothPhase2 = 0f;
    private float smoothPhase3 = 0f;
    private float smoothAmp = 1.0f;
    private final Random noiseRandom = new Random();

    private final Path circleClipPath = new Path();
    private final Path lightWavePath = new Path();
    private final Path fluidRingPath = new Path();
    private final Path wavePath = new Path();
    private final Path dropPath = new Path();
    private final RectF bubbleRect = new RectF();

    private float solarX = -1f, solarY = -1f;
    private float sunX = -1f, sunY = -1f;
    private float houseX = -1f, houseY = -1f;
    private float solarIconSizeDp = 80f;
    private boolean coords_Bound = false;

    private float sunPhase = 0f;
    private float sunWobblePhase = 0f;
    private final Random sunRandom = new Random();

    private float cloudCover = 0f;
    private int weatherCode = 0;
    private float uvIndex = 0f;

    private int sunriseMinute = -1;
    private int sunsetMinute = -1;
    private boolean isNight = false;
    private final float fogBandTopRatio = 0.05f;
    private final float fogBandHeightRatio = 0.22f;
    private float lastW = -1f, lastH = -1f;

    // ===== 云海粒子系统 =====
    private static class FogParticle {
        float x, y, r;
        float speed;
        float alphaBase;
        float phase;
        int layer;
        float densityWeight;
        float stretchX;
        float stretchY;
    }
    private static class RainParticle {
        float x, y, speed, length, alpha;
    }

    private static class SnowParticle {
        float x, y, speed, size, alpha, swingPhase, swingAmp;
    }
    // ===== 月亮 =====
    private final Paint moonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint moonGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public FluidBubbleView(Context context) { super(context); init(); }
    public FluidBubbleView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public FluidBubbleView(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(); }

    private float dp2px(float dp) {
        return dp * getContext().getResources().getDisplayMetrics().density;
    }

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

        Paint sunRayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sunRayPaint.setStyle(Paint.Style.STROKE);
        sunRayPaint.setStrokeCap(Paint.Cap.ROUND);

        moonPaint.setStyle(Paint.Style.FILL);
        moonPaint.setColor(Color.parseColor("#E8E8E0"));
        moonGlowPaint.setStyle(Paint.Style.FILL);

        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(2200);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            animationPhase = animation.getAnimatedFraction();
            updateParticles();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            buildFog(cloudCover, weatherCode);
            initWeatherParticles(w, h);
        }
    }
    public void updateWeather(float cloudCover, int weatherCode, float uv) {
        this.cloudCover = cloudCover;
        this.weatherCode = weatherCode;
        this.uvIndex = uv;
        buildFog(cloudCover, weatherCode);
        lastW = -1f;
        invalidate();
    }

    public void updateSunTimes(int sunriseMinute, int sunsetMinute) {
        if (this.sunriseMinute == sunriseMinute && this.sunsetMinute == sunsetMinute) return;
        this.sunriseMinute = sunriseMinute;
        this.sunsetMinute = sunsetMinute;
        checkDayNight();
    }

    private void checkDayNight() {
        if (sunriseMinute < 0 || sunsetMinute < 0) {
            isNight = false; // 没数据默认白天，月亮不显示
            return;
        }
        Calendar now = Calendar.getInstance();
        int cur = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        isNight = cur < sunriseMinute || cur > sunsetMinute;
    }

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

    public void bindIconCoords(ImageView solarIcon, ImageView houseIcon) {
        if (solarIcon == null || houseIcon == null) return;
        solarIcon.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
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

    public void setSolarSize(float dp) { this.solarIconSizeDp = dp; }

    private float chargeRatio() {
        return (float) Math.pow(Math.min(1.0f, chargeCurrent / 50f), 0.5f);
    }

    private float dischargeRatio() {
        return (float) Math.pow(Math.min(1.0f, dischargeCurrent / maxchargerCurrent), 0.5f);
    }

    // ==================== 云海粒子构建 ====================
    private void buildFog(float cloudCover, int weatherCode) {
        fogParticles.clear();

        float w = getWidth();
        float h = getHeight();
        if (w == 0 || h == 0) return;

        // 云海粒子数：cloudCover 越高越密
        int count = (int) (80 + cloudCover * 200);
        count = Math.max(60, Math.min(count, 300));

        // 云带范围 —— 改这里，drawFog 里重生粒子自动一致
        float bandTop = h * fogBandTopRatio;
        float bandH = h * fogBandHeightRatio;

        for (int i = 0; i < count; i++) {
            FogParticle p = new FogParticle();
            p.layer = fogRand.nextInt(3);
            p.r = dp2px(fogRand.nextFloat() * 14f + 10f);
            if (p.layer == 0) p.r *= 1.5f;
            else if (p.layer == 2) p.r *= 0.8f;

            p.x = fogRand.nextFloat() * w;
            p.y = bandTop + fogRand.nextFloat() * bandH;
            p.speed = dp2px((p.layer == 0 ? 0.04f : p.layer == 1 ? 0.08f : 0.14f)
                    * (0.6f + fogRand.nextFloat() * 0.8f));
            p.alphaBase = (p.layer == 0 ? 80 : p.layer == 1 ? 120 : 160);
            p.phase = fogRand.nextFloat() * (float) (Math.PI * 2);
            p.densityWeight = 0.3f + fogRand.nextFloat() * 1.2f; // 厚薄随机
            p.stretchX = 1.4f + fogRand.nextFloat() * 1.2f; // 横向拉伸
            p.stretchY = 0.55f + fogRand.nextFloat() * 0.35f; // 纵向压扁
            fogParticles.add(p);
        }
    }

    private int getFogColor(int weatherCode, float cloudCover) {
        if (weatherCode >= 95) return Color.rgb(60, 60, 70);        // 雷暴：深灰
        if (weatherCode >= 80) return Color.rgb(100, 110, 120);      // 阵雨：灰蓝
        if (weatherCode >= 71) return Color.rgb(200, 200, 210);      // 雪：灰白
        if (weatherCode >= 51) return Color.rgb(120, 130, 140);      // 毛毛雨：灰
        if (weatherCode <= 1) return Color.rgb(205, 215, 228);       // 晴：白
        return Color.rgb(180, 190, 200);                             // 多云/阴：浅灰
    }

    // ==================== 粒子逻辑 ====================
    private void updateParticles() {
        long now = System.currentTimeMillis();
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) return;

        checkDayNight();

        smoothPhase1 += 0.017f + (noiseRandom.nextFloat() * 0.006f - 0.003f);
        smoothPhase2 += 0.023f + (noiseRandom.nextFloat() * 0.006f - 0.003f);
        smoothPhase3 += 0.009f + (noiseRandom.nextFloat() * 0.004f - 0.002f);
        smoothAmp += (noiseRandom.nextFloat() * 0.02f - 0.01f);
        smoothAmp = Math.max(0.92f, Math.min(1.08f, smoothAmp));

        float ringWidthPx = dp2px(70f);
        float cx = width - (ringWidthPx / 2f) - dp2px(2f);
        float ringRadius = (ringWidthPx - dp2px(12f)) / 2f;
        float cy = ringRadius + dp2px(6f);

        float targetHouseX = coords_Bound ? houseX : width - dp2px(45f);
        float targetHouseY = coords_Bound ? houseY : height - dp2px(55f);

        // 充电粒子
        float cRatio = chargeRatio();
        if (chargeCurrent > 0.1f) {
            long interval = (long) (800 - 740 * cRatio);
            int maxCount = 3 + (int) (27 * cRatio);
            int count = 0;
            for (ChargeParticle p : chargeParticles) { if (!p.dead) count++; }
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
                float baseSpeed = Math.min(dp2px(3.0f), dp2px(0.15f) + ((cRatio - 0.3f) / 0.7f) * dp2px(2.5f));
                p.speedX = baseSpeed + random.nextFloat() * dp2px(0.4f);
                p.speedY = (cy - p.y) / ((cx - ringRadius * 0.6f - p.x) / p.speedX);
                p.wobbleOffset = random.nextFloat() * 100f;
                chargeParticles.add(p);
                lastChargeSpawn = now;
            }
        }

        Iterator<ChargeParticle> ci = chargeParticles.iterator();
        while (ci.hasNext()) {
            ChargeParticle p = ci.next();
            if (p.dead) { ci.remove(); continue; }
            p.x += p.speedX;
            p.y += p.speedY;
            p.y += (float) Math.sin(animationPhase * 2 * Math.PI + p.wobbleOffset) * (dp2px(0.15f) - cRatio * dp2px(0.12f));
            float targetX = cx - ringRadius * 0.6f;
            float distToBall = targetX - p.x;
            if (distToBall <= dp2px(22f) && distToBall > 0) {
                p.absorbPhase += 0.05f;
                float absorbProgress = Math.min(1f, p.absorbPhase);
                p.speedX *= 0.88f;
                p.speedY *= 0.88f;
                p.radius = p.maxRadius * (0.4f + 0.8f * absorbProgress);
                float pullStrength = 1f - (distToBall / dp2px(22f));
                p.x += (targetX - p.x) * pullStrength * 0.25f;
                p.y += (cy - p.y) * pullStrength * 0.12f;
                p.growPhase = absorbProgress;
                if (distToBall <= dp2px(2f)) p.dead = true;
            } else if (p.x > cx + ringRadius) {
                ci.remove();
            }
        }

        // 放电粒子
        float dRatio = dischargeRatio();
        if (dischargeCurrent > 0.1f) {
            long interval = (long) (800 - 740 * dRatio);
            int maxCount = 1 + (int) (10 * dRatio);
            int count = 0;
            for (DischargeDrop p : dischargeDrops) { if (p.state != 3) count++; }
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

        Iterator<DischargeDrop> di = dischargeDrops.iterator();
        while (di.hasNext()) {
            DischargeDrop p = di.next();
            if (p.state == 3) { di.remove(); continue; }
            if (p.state == 0) {
                p.growTimer += 0.035f;
                float growProgress = Math.min(1f, p.growTimer);
                p.radius = p.maxRadius * (0.15f + 0.85f * growProgress);
                p.tailLength = growProgress * p.maxRadius * 2.2f;
                p.neckLength = growProgress * p.maxRadius * 1.2f;
                p.y += dp2px(0.25f) * growProgress;
                if (growProgress >= 1f) {
                    p.state = 1;
                    float flySpeed = Math.min(dp2px(3.0f), dp2px(0.1f) + dRatio * dp2px(1.5f));
                    float dx = targetHouseX - p.x;
                    float dy = targetHouseY - p.y;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);
                    p.speedX = (dx / dist) * flySpeed;
                    p.speedY = (dy / dist) * flySpeed;
                    p.breakAnim = 0f;
                }
            } else if (p.state == 1) {
                float flySpeed = Math.min(dp2px(3.0f), dp2px(0.2f) + dRatio * dp2px(2.5f));
                float dx = targetHouseX - p.x;
                float dy = targetHouseY - p.y;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist > 0) {
                    p.speedX = (dx / dist) * flySpeed;
                    p.speedY = (dy / dist) * flySpeed;
                }
                p.x += p.speedX;
                p.y += p.speedY;
                p.tailLength *= 0.92f;
                p.neckLength *= 0.9f;
                if (dist <= dp2px(18f)) { p.state = 2; p.shrinkTimer = 0f; }
            } else if (p.state == 2) {
                p.shrinkTimer += 0.04f;
                float shrinkProgress = Math.min(1f, p.shrinkTimer);
                float pullStrength = shrinkProgress * 0.3f;
                p.x += (targetHouseX - p.x) * pullStrength * 0.15f;
                p.y += (targetHouseY - p.y) * pullStrength * 0.15f;
                if (shrinkProgress >= 1f) p.state = 3;
            }
            if (p.x > width + dp2px(30f) || p.y > height + dp2px(30f) || p.x < -dp2px(30f) || p.y < -dp2px(30f)) p.state = 3;
        }

        float sunPhaseSpeed = 0.05f + 0.1f * chargeRatio();
        sunPhase += (sunPhaseSpeed + (sunRandom.nextFloat() - 0.5f) * 0.3f) / 60f;
        if (sunPhase > 1f) sunPhase -= 1f;
        sunWobblePhase += 0.02f + sunRandom.nextFloat() * 0.01f;
    }

    // ==================== 绘制 ====================
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

        // 0. 太阳辐射或月亮（被云层遮挡）
        if (isNight) drawMoon(canvas);
        else drawSolarRadiation(canvas);

        // 1. 云海（在太阳/月亮之前，自然遮挡）
        drawFog(canvas, cloudCover, weatherCode);

        // 2. 外圈流体环
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

        // 3. 内部液面
        canvas.save();
        circleClipPath.reset();
        circleClipPath.addPath(fluidRingPath);
        canvas.clipPath(circleClipPath);

        float fluidY = cy + ringRadius - (2f * ringRadius * (progress / 100f));
        if (progress > 0f) {
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

        // 4. 充电粒子
        for (ChargeParticle p : chargeParticles) {
            if (p.radius <= 0.3f || p.dead) continue;
            if (chargeCurrent > 0f && chargeCurrent < 10f) {
                float glowIntensity = 1f - (chargeCurrent / 15f);
                glowIntensity = glowIntensity * glowIntensity;
                float breath = 0.6f + 0.4f * (float) Math.sin(animationPhase * Math.PI * 6 + p.wobbleOffset);
                for (int g = 0; g < 3; g++) {
                    float layerRatio = (g + 1f) / 3f;
                    float layerRadius = p.radius * (2.5f + 3.5f * glowIntensity * layerRatio);
                    int alpha = (int) (70 * glowIntensity * (1f - layerRatio) * breath);
                    glowPaint.setColor(fluidColor);
                    glowPaint.setAlpha(Math.max(0, Math.min(255, alpha)));
                    bubbleRect.set(p.x - layerRadius, p.y - layerRadius, p.x + layerRadius, p.y + layerRadius);
                    canvas.drawOval(bubbleRect, glowPaint);
                }
                glowPaint.setAlpha(255);
            }
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
            float drawW = p.radius * (p.absorbPhase > 0 ? 1.3f : 1.15f);
            float drawH = p.radius * (p.absorbPhase > 0 ? 1.1f : 1.0f);
            bubbleRect.set(p.x - drawW, p.y - drawH, p.x + drawW * 1.2f, p.y + drawH);
            canvas.drawOval(bubbleRect, fluidPaint);
        }

        // 5. 放电粒子
        for (DischargeDrop p : dischargeDrops) {
            if (p.state == 3 || p.radius <= 0.3f) continue;
            if (p.state == 0) {
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
                if (dischargeCurrent > 0f && dischargeCurrent < maxchargerCurrent * 0.1f) {
                    float glowIntensity = 1f - (dischargeCurrent / (maxchargerCurrent * 0.4f));
                    glowIntensity = glowIntensity * glowIntensity;
                    float breath = 0.6f + 0.4f * (float) Math.sin(animationPhase * Math.PI * 6 + p.wobbleOffset);
                    for (int g = 0; g < 3; g++) {
                        float layerRatio = (g + 1f) / 3f;
                        float layerRadius = p.radius * (2f + 3f * glowIntensity * layerRatio);
                        int alpha = (int) (60 * glowIntensity * (1f - layerRatio) * breath);
                        glowPaint.setColor(fluidColor);
                        glowPaint.setAlpha(Math.max(0, Math.min(255, alpha)));
                        bubbleRect.set(p.x - layerRadius, p.y - layerRadius, p.x + layerRadius, p.y + layerRadius);
                        canvas.drawOval(bubbleRect, glowPaint);
                    }
                    glowPaint.setAlpha(255);
                }
                float stretch = (float) Math.sqrt(p.speedX * p.speedX + p.speedY * p.speedY);
                float drawW = p.radius + stretch * 0.35f;
                float drawH = p.radius + stretch * 0.15f;
                bubbleRect.set(p.x - drawW, p.y - drawH, p.x + drawW, p.y + drawH);
                canvas.drawOval(bubbleRect, fluidPaint);
            } else if (p.state == 2) {
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
                    bubbleRect.set(p.x - p.radius * 0.8f, p.y - p.radius * 0.8f, p.x + p.radius * 0.8f, p.y + p.radius * 0.8f);
                    canvas.drawOval(bubbleRect, fluidPaint);
                    fluidPaint.setAlpha(255);
                }
            }
        }

        // 6. 电量文字
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textY = cy + (fm.bottom - fm.top) / 2f - fm.bottom;
        textPaint.setTextSize(dp2px(10f));
        textPaint.setColor(Color.parseColor("#1A1A1A"));
        canvas.drawText(String.format(java.util.Locale.getDefault(), "%.1f%%", progress), cx, textY, textPaint);
    }

    // ==================== 雾状云海 ====================
    private void drawFog(Canvas canvas, float cloudCover, int weatherCode) {
        if (!coords_Bound) return;

        float w = getWidth();
        float h = getHeight();
        int fogColor = getFogColor(weatherCode, cloudCover);

        float bandTop = h * fogBandTopRatio;
        float bandH = h * fogBandHeightRatio;
        float cloudFactor = 0.3f + cloudCover * 0.7f;

        float uvFactor = 1.0f - Math.min(uvIndex, 11f) / 11f * 0.8f;
        float coverAlpha = 0.35f + cloudCover * 0.65f;

        for (int layer = 0; layer < 3; layer++) {
            for (FogParticle p : fogParticles) {
                if (p.layer != layer) continue;
                p.x -= p.speed;
                if (p.x + p.r * 2f < 0f) {
                    p.x = w + p.r * 2f + fogRand.nextFloat() * w * 0.15f;
                    p.y += (fogRand.nextFloat() - 0.5f) * dp2px(3f);
                    if (p.y < bandTop) p.y = bandTop + fogRand.nextFloat() * bandH * 0.3f;
                    if (p.y > bandTop + bandH) p.y = bandTop + bandH * 0.7f + fogRand.nextFloat() * bandH * 0.3f;
                }

                float cloudClump = 0.85f + 0.15f * (float) Math.sin(p.x * 0.002f + p.phase * 0.5f);
                float wobble = (float) Math.sin(System.currentTimeMillis() * 0.00015f + p.phase) * dp2px(2f);
                float cx2 = p.x;
                float cy2 = p.y + wobble;

                float breath = 0.92f + 0.08f * (float) Math.sin(
                        System.currentTimeMillis() * 0.0002f + p.phase * 0.3f);
                int a = (int) Math.max(0, Math.min(255,
                        p.alphaBase * uvFactor * coverAlpha * p.densityWeight * cloudClump * breath
                ));

                RadialGradient rg = new RadialGradient(
                        0, 0, p.r,
                        Color.argb(a, Color.red(fogColor), Color.green(fogColor), Color.blue(fogColor)),
                        Color.argb(0, Color.red(fogColor), Color.green(fogColor), Color.blue(fogColor)),
                        Shader.TileMode.CLAMP
                );
                fogPaint.setShader(rg);
                canvas.save();
                canvas.translate(cx2, cy2);
                canvas.scale(p.stretchX, p.stretchY);
                canvas.drawCircle(0, 0, p.r, fogPaint);
                canvas.restore();
            }
        }
        fogPaint.setShader(null);

        // 顶部遮罩
        LinearGradient topMask = new LinearGradient(
                0, 0, 0, bandTop + bandH,
                Color.argb((int) (200 * uvFactor), Color.red(fogColor), Color.green(fogColor), Color.blue(fogColor)),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
        );
        fogPaint.setShader(topMask);
        canvas.drawRect(0, 0, w, bandTop + bandH, fogPaint);
        fogPaint.setShader(null);

        // 雪
        if (weatherCode == 71 || weatherCode == 73 || weatherCode == 75 || weatherCode == 77
                || weatherCode == 85 || weatherCode == 86) {
            updateAndDrawSnow(canvas, w, h,weatherCode);
        }

        // 毛毛雨
        if (weatherCode >= 51 && weatherCode <= 57) {
            updateAndDrawRain(canvas, w, h, weatherCode);
        }

        // 雨+阵雨
        if ((weatherCode >= 61 && weatherCode <= 67) || (weatherCode >= 80 && weatherCode <= 82)) {
            updateAndDrawRain(canvas, w, h, weatherCode);
        }

        // 雷暴
        if (weatherCode >= 95) {
            updateAndDrawLightning(canvas, w, h, cloudCover);
            updateAndDrawRain(canvas, w, h, weatherCode); // 雷暴伴雨
        }
    }
    private void initWeatherParticles(float w, float h) {
        if (w == lastW && h == lastH) return;
        lastW = w; lastH = h;
        rainParticles.clear();
        snowParticles.clear();
    }
    // 更新和画雨
    private void updateAndDrawRain(Canvas canvas, float w, float h, int weatherCode) {
        float bandTop = h * fogBandTopRatio;
        float bandH   = h * fogBandHeightRatio;
        float rainStartY = bandTop + bandH * 0.6f;

        // ===== 按 weatherCode 分级 =====
        boolean isDrizzle = weatherCode >= 51 && weatherCode <= 57;
        boolean isStorm   = weatherCode >= 95;
        boolean isShower  = weatherCode >= 80 && weatherCode <= 82;

        float density, lineLenMin, lineLenMax, speedMin, speedMax, alphaMin, alphaMax, strokeW;

        if (isDrizzle) {
            // 毛毛雨：细密短雨线，低密度
            density   = 0.35f;
            lineLenMin = dp2px(6f);   lineLenMax = dp2px(10f);
            speedMin  = 2.5f;         speedMax  = 4f;
            alphaMin  = 45;           alphaMax  = 80;
            strokeW   = dp2px(0.7f);
        } else if (isStorm) {
            // 雷暴：暴雨级，长线快落高密度
            density   = 0.9f;
            lineLenMin = dp2px(18f);  lineLenMax = dp2px(20f);
            speedMin  = 7f;           speedMax  = 11f;
            alphaMin  = 120;          alphaMax  = 200;
            strokeW   = dp2px(1.2f);
        } else if (isShower) {
            // 阵雨：中等偏上
            density   = 0.65f;
            lineLenMin = dp2px(12f);  lineLenMax = dp2px(16f);
            speedMin  = 5f;           speedMax  = 8f;
            alphaMin  = 90;           alphaMax  = 160;
            strokeW   = dp2px(1f);
        } else {
            // 普通雨/冻雨 61-67：中等
            density   = 0.5f;
            lineLenMin = dp2px(10f);  lineLenMax = dp2px(13f);
            speedMin  = 3.5f;         speedMax  = 6f;
            alphaMin  = 70;           alphaMax  = 130;
            strokeW   = dp2px(0.9f);
        }

        // ===== 动态调整粒子数量（只改一次，不每帧 clear）=====
        int targetCount = (int) (rainParticles.size() * density / 0.5f);
        targetCount = Math.max(30, Math.min(targetCount, 120));
        while (rainParticles.size() < targetCount) {
            RainParticle p = new RainParticle();
            p.x = rainRand.nextFloat() * w;
            p.y = rainStartY - dp2px(rainRand.nextFloat() * h * 0.8f); // 从屏幕顶部外排队
            p.speed = speedMin + rainRand.nextFloat() * (speedMax - speedMin);
            p.length = lineLenMin + rainRand.nextFloat() * (lineLenMax - lineLenMin);
            p.alpha = 0;
            rainParticles.add(p);
        }
        while (rainParticles.size() > targetCount) {
            rainParticles.remove(rainParticles.size() - 1);
        }

        rainPaint.setStrokeWidth(strokeW);
        for (int i = rainParticles.size() - 1; i >= 0; i--) {
            RainParticle p = rainParticles.get(i);
            p.y += p.speed;
            if (p.y > h) {
                p.y = rainStartY - dp2px(rainRand.nextFloat() * h * 0.8f);
                p.x = rainRand.nextFloat() * w;
                p.alpha = 0;
            }
            if (p.y >= rainStartY) {
                // 淡入
                p.alpha = Math.min(p.alpha + 8f, alphaMax);
                if (p.alpha < alphaMin) p.alpha = alphaMin;
                rainPaint.setColor(Color.argb((int) p.alpha, 174, 194, 224));
                canvas.drawLine(p.x, p.y, p.x, p.y + p.length, rainPaint);
            }
        }
    }
    // 更新和画雪
    private void updateAndDrawSnow(Canvas canvas, float w, float h, int weatherCode) {
        float bandTop = h * fogBandTopRatio;
        float bandH   = h * fogBandHeightRatio;
        float snowStartY = bandTop + bandH * 0.6f;

        // ===== 按 weatherCode 分级 =====
        boolean isLight   = weatherCode == 71 || weatherCode == 85; // 小雪/小阵雪
        boolean isHeavy   = weatherCode == 75 || weatherCode == 86; // 大雪/大阵雪
        boolean isGrains  = weatherCode == 77; // 雪粒/米雪

        float density, sizeMin, sizeMax, speedMin, speedMax, alphaMin, alphaMax, swingMin, swingMax;

        if (isLight) {
            // 小雪：稀疏、小、慢、半透明
            density   = 0.4f;
            sizeMin   = dp2px(0.8f);  sizeMax   = dp2px(1.5f);
            speedMin  = 0.4f;         speedMax  = 0.9f;
            alphaMin  = 70;           alphaMax  = 120;
            swingMin  = 0.3f;         swingMax  = 0.6f;
        } else if (isHeavy) {
            // 大雪：密集、大、快
            density   = 1.5f;
            sizeMin   = dp2px(2.0f);  sizeMax   = dp2px(4.5f);
            speedMin  = 0.8f;         speedMax  = 3.0f;
            alphaMin  = 180;          alphaMax  = 255;
            swingMin  = 0.8f;         swingMax  = 1.5f;
        } else if (isGrains) {
            // 雪粒：小、快、几乎不飘
            density   = 0.6f;
            sizeMin   = dp2px(0.6f);  sizeMax   = dp2px(1.2f);
            speedMin  = 1.2f;         speedMax  = 2.5f;
            alphaMin  = 100;          alphaMax  = 160;
            swingMin  = 0.1f;         swingMax  = 0.3f;
        } else {
            // 中雪 73：中等
            density   = 1.0f;
            sizeMin   = dp2px(1.5f);  sizeMax   = dp2px(3.5f);
            speedMin  = 1.0f;         speedMax  = 2.0f;
            alphaMin  = 150;           alphaMax  = 200;
            swingMin  = 0.6f;         swingMax  = 1.2f;
        }

        // ===== 动态调整粒子数量（只改差值，不重建）=====
        int targetCount = (int) (50 * density);
        while (snowParticles.size() < targetCount) {
            SnowParticle p = new SnowParticle();
            p.x = snowRand.nextFloat() * w;
            p.y = snowStartY - dp2px(snowRand.nextFloat() * h * 0.8f); // 从顶部外排队
            p.size = sizeMin + snowRand.nextFloat() * (sizeMax - sizeMin);
            p.speed = speedMin + snowRand.nextFloat() * (speedMax - speedMin);
            p.alpha = 0;
            p.swingAmp = swingMin + snowRand.nextFloat() * (swingMax - swingMin);
            p.swingPhase = snowRand.nextFloat() * 6.28f;
            snowParticles.add(p);
        }
        while (snowParticles.size() > targetCount) {
            snowParticles.remove(snowParticles.size() - 1);
        }

        snowPaint.setAntiAlias(true);
        snowPaint.setStyle(Paint.Style.FILL);
        for (int i = snowParticles.size() - 1; i >= 0; i--) {
            SnowParticle p = snowParticles.get(i);
            p.y += p.speed;
            p.x += (float) Math.sin(animationPhase * 0.4f + p.swingPhase) * p.swingAmp;
            if (p.y > h) {
                p.y = snowStartY - dp2px(snowRand.nextFloat() * 30f);
                p.x = snowRand.nextFloat() * w;
            }
            if (p.x < 0) p.x = w;
            if (p.x > w) p.x = 0;
            if (p.y >= snowStartY) {
                p.alpha = Math.min(p.alpha + 6f, alphaMax);
                if (p.alpha < alphaMin) p.alpha = alphaMin;
                snowPaint.setColor(Color.argb((int) p.alpha, 255, 255, 255));
                canvas.drawCircle(p.x, p.y, p.size, snowPaint);
            }
        }
    }
    // 更新和画闪电
    private void updateAndDrawLightning(Canvas canvas, float w, float h, float cloudCover) {
        lightningTimer += 1f / 60f;
        float interval = 10f + lightningRand.nextFloat() * 30f;

        if (lightningTimer > interval) {
            lightningTimer = 0f;
            lightningAlpha = 210f + fogRand.nextFloat() * 45f;

            // ===== 只在触发时随机一次位置 =====
            float bandTop = h * fogBandTopRatio;
            float bandH   = h * fogBandHeightRatio;
            lightningX = w * (0.22f + fogRand.nextFloat() * 0.56f);
            float lightningY = bandTop + bandH * (0.4f + fogRand.nextFloat() * 0.2f);
            lightningSx = dp2px(18f) * (0.85f + fogRand.nextFloat() * 0.3f);
        }

        lightningAlpha *= 0.93f;
        if (lightningAlpha < 1f) lightningAlpha = 0f;
        if (lightningAlpha <= 0f) return;

        float bandTop = h * fogBandTopRatio;
        float bandH   = h * fogBandHeightRatio;
        float lightTop = bandTop + bandH * 0.55f;
        float lightBottom = bandTop + bandH * 0.82f;
        float centerX = w * (0.15f + lightningRand.nextFloat() * 0.7f);
        float centerY = (lightTop + lightBottom) * 0.5f;
        float halfH = (lightBottom - lightTop) * 0.5f;

        // 用锁定的位置
        float cx = lightningX;
        float sy = halfH * 2f;

        Path bolt = new Path();
        bolt.moveTo(cx + lightningSx * 0.35f, centerY - halfH);
        bolt.lineTo(cx + lightningSx * 0.95f, centerY - halfH * 0.85f);
        bolt.lineTo(cx - lightningSx * 0.05f, centerY - halfH * 0.05f);
        bolt.lineTo(cx + lightningSx * 0.75f, centerY + halfH * 0.1f);
        bolt.lineTo(cx - lightningSx * 0.35f, centerY + halfH);
        bolt.lineTo(cx - lightningSx * 0.15f, centerY + halfH * 0.15f);
        bolt.lineTo(cx + lightningSx * 0.55f, centerY + halfH * 0.02f);
        bolt.lineTo(cx - lightningSx * 0.25f, centerY - halfH * 0.55f);
        bolt.close();

        // 外发光
        Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
        glow.setStyle(Paint.Style.STROKE);
        glow.setStrokeJoin(Paint.Join.ROUND);
        glow.setStrokeCap(Paint.Cap.ROUND);
        glow.setColor(Color.argb((int)(lightningAlpha * 0.18f), 255, 250, 210));
        glow.setStrokeWidth(dp2px(14f));
        canvas.drawPath(bolt, glow);
        glow.setColor(Color.argb((int)(lightningAlpha * 0.32f), 255, 245, 180));
        glow.setStrokeWidth(dp2px(7f));
        canvas.drawPath(bolt, glow);

        float radius = Math.max(lightningSx * 1.8f, halfH * 2.2f);
        RadialGradient halo = new RadialGradient(
                cx, centerY, radius,
                Color.argb((int)(lightningAlpha * 0.35f), 255, 240, 200),
                Color.argb(0, 255, 240, 200),
                Shader.TileMode.CLAMP);
        Paint haloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        haloPaint.setShader(halo);
        canvas.drawCircle(cx, centerY, radius, haloPaint);

        // 本体
        Paint core = new Paint(Paint.ANTI_ALIAS_FLAG);
        core.setStyle(Paint.Style.FILL);
        core.setColor(Color.argb((int) lightningAlpha, 255, 255, 245));
        canvas.drawPath(bolt, core);
    }

    // ==================== 月亮 ====================
    private void drawMoon(Canvas canvas) {
        if (!coords_Bound) return;
        float moonR = dp2px(15f);
        float glowAlpha = 40 + (int) (20 * Math.sin(animationPhase * Math.PI * 2));
        moonGlowPaint.setColor(Color.parseColor("#FFF8DC"));
        moonGlowPaint.setAlpha((int) Math.max(0, Math.min(255, glowAlpha)));
        canvas.drawCircle(sunX, sunY, moonR * 1.6f, moonGlowPaint);
        moonGlowPaint.setAlpha((int) Math.max(0, Math.min(255, glowAlpha / 2)));
        canvas.drawCircle(sunX, sunY, moonR * 2.2f, moonGlowPaint);
        canvas.drawCircle(sunX, sunY, moonR, moonPaint);

        Paint craterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        craterPaint.setStyle(Paint.Style.FILL);
        craterPaint.setColor(Color.parseColor("#D0D0C8"));
        craterPaint.setAlpha(120);
        canvas.drawCircle(sunX - moonR * 0.3f, sunY - moonR * 0.2f, moonR * 0.25f, craterPaint);
        canvas.drawCircle(sunX + moonR * 0.25f, sunY + moonR * 0.1f, moonR * 0.2f, craterPaint);
        canvas.drawCircle(sunX - moonR * 0.1f, sunY + moonR * 0.3f, moonR * 0.18f, craterPaint);
        moonGlowPaint.setAlpha(255);
    }

    // ==================== 太阳辐射 ====================
    private void drawSolarRadiation(Canvas canvas) {
        if (!coords_Bound || chargeCurrent < 0.1f) return;
        // ===== 充电流强度（0~1），电流越大光晕越强 =====
        float cRatio = Math.min(chargeCurrent / 40f, 1f);
        // ===== 太阳基础半径，solarIconSizeDp 是图标大小 =====
        float sunR = dp2px(solarIconSizeDp) * 0.18f;
        if (sunR < dp2px(6f)) sunR = dp2px(6f); // 最小半径兜底
        // ===== 呼吸波动，0.85~1.0 之间随机抖动 =====
        float breath = 0.85f + 0.15f * ((sunRandom.nextFloat() - 0.5f) * 2f);
        // ===== 光晕基础透明度，50 是基数，乘 cRatio 和 breath =====
        // 改 50 → 更大值让光晕更亮，更小值更暗
        float glowAlpha = (50 * cRatio) * breath;
        glowPaint.setColor(0xFFFFD700);  // 金黄色，改这里换色
        glowPaint.setAlpha((int) Math.min(255, glowAlpha));
        // ===== 光晕半径，2.5 是基础扩散，1.5 是电流加成 =====
        // 改 2.5 → 更大光晕范围，改 1.5 → 电流对光晕的影响幅度
        canvas.drawCircle(sunX, sunY, sunR * (1.2f + 1.2f * cRatio), glowPaint);
    }

    private static class ChargeParticle {
        float x, y;
        float radius;
        float maxRadius;
        float speedX, speedY;
        float wobbleOffset;
        float growPhase = 0f;
        float absorbPhase = 0f;
        boolean dead = false;
    }

    private static class DischargeDrop {
        float x, y;
        float radius;
        float maxRadius;
        float speedX, speedY;
        float wobbleOffset;
        float growTimer = 0f;
        float shrinkTimer = 0f;
        float tailLength = 0f;
        float neckLength = 0f;
        float breakAnim = 0f;
        int state = 0;
    }
}
