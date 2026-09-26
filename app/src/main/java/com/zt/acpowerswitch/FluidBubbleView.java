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

    // ===== 坐标绑定 =====
    private float solarX = -1f, solarY = -1f;    // 太阳能板中心
    private float sunX = -1f, sunY = -1f;        // 太阳中心（太阳能板右上）
    private float houseX = -1f, houseY = -1f;    // 房子中心
    private float solarIconSizeDp = 80f;
    private boolean coordsBound = false;

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

        // 太阳辐射画笔
        sunRayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sunRayPaint.setStyle(Paint.Style.STROKE);
        sunRayPaint.setStrokeCap(Paint.Cap.ROUND);

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

    public void updateConfig(float progress, int color,
                             float chargeCurrent, float dischargeCurrent) {
        boolean needInvalidate = false;
        if (this.progress != progress) { this.progress = progress; needInvalidate = true; }
        if (this.fluidColor != color) { this.fluidColor = color; needInvalidate = true; }
        if (this.chargeCurrent != chargeCurrent) { this.chargeCurrent = chargeCurrent; needInvalidate = true; }
        if (this.dischargeCurrent != dischargeCurrent) { this.dischargeCurrent = dischargeCurrent; needInvalidate = true; }
        if (needInvalidate) invalidate();
    }

    // ===== 坐标绑定（Activity 中调用）=====
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

                        coordsBound = true;
                        invalidate();
                    }
                });
    }

    public void setSolarSize(float dp) { this.solarIconSizeDp = dp; }

    // ==================== 核心参数 ====================
    private float chargeRatio() {
        return (float) Math.pow(Math.min(1.0f, chargeCurrent / 40f), 0.5f);
    }
    private float dischargeRatio() {
        return (float) Math.pow(Math.min(1.0f, dischargeCurrent / 40f), 0.5f);
    }

    private void updateParticles() {
        long now = System.currentTimeMillis();
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) return;

        float ringWidthPx = dp2px(70f);
        float cx = width - (ringWidthPx / 2f) - dp2px(2f);
        float ringRadius = (ringWidthPx - dp2px(12f)) / 2f;
        float cy = ringRadius + dp2px(6f);

        // ✅ 房子目标坐标：优先用绑定坐标，否则 fallback 到硬编码
        float targetHouseX = coordsBound ? houseX : width - dp2px(45f);
        float targetHouseY = coordsBound ? houseY : height - dp2px(55f);

        // 噪声
        smoothPhase1 += 0.017f + (noiseRandom.nextFloat() * 0.006f - 0.003f);
        smoothPhase2 += 0.023f + (noiseRandom.nextFloat() * 0.006f - 0.003f);
        smoothPhase3 += 0.009f + (noiseRandom.nextFloat() * 0.004f - 0.002f);
        smoothAmp += (noiseRandom.nextFloat() * 0.02f - 0.01f);
        smoothAmp = Math.max(0.92f, Math.min(1.08f, smoothAmp));

        // ===== 充电粒子 =====
        float cRatio = chargeRatio();
        if (chargeCurrent > 0.1f) {
            long interval = (long) (800 - 740 * cRatio);
            int maxCount = 3 + (int) (27 * cRatio);

            int count = 0;
            for (ChargeParticle p : chargeParticles) { if (!p.dead) count++; }

            if (now - lastChargeSpawn > interval && count < maxCount) {
                ChargeParticle p = new ChargeParticle();
                // ✅ 出生点：优先用太阳能板中心坐标
                float targetSolarX = coordsBound ? solarX : dp2px(50f);
                float targetSolarY = coordsBound ? solarY : dp2px(50f);
                p.x = targetSolarX + (random.nextFloat() - 0.5f) * dp2px(20f);
                p.y = targetSolarY + (random.nextFloat() - 0.5f) * dp2px(20f);

                p.maxRadius = dp2px(6.5f) + cRatio * dp2px(8.0f);
                p.radius = p.maxRadius * 0.2f;
                p.growPhase = 0f;
                p.absorbPhase = 0f;

                float baseSpeed = dp2px(0.2f) + cRatio * dp2px(2.5f);
                baseSpeed = Math.min(baseSpeed, dp2px(3.0f));
                p.speedX = baseSpeed + random.nextFloat() * dp2px(0.4f);
                p.speedY = (cy - p.y) / ((cx - ringRadius * 0.6f - p.x) / p.speedX);
                p.wobbleOffset = random.nextFloat() * 100f;
                chargeParticles.add(p);
                lastChargeSpawn = now;
            }
        }

        // 充电粒子运动
        Iterator<ChargeParticle> ci = chargeParticles.iterator();
        while (ci.hasNext()) {
            ChargeParticle p = ci.next();
            if (p.dead) { ci.remove(); continue; }

            p.x += p.speedX;
            p.y += p.speedY;

            float wobble = dp2px(0.15f) - cRatio * dp2px(0.12f);
            p.y += (float) Math.sin(animationPhase * 2 * Math.PI + p.wobbleOffset) * wobble;

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

                if (distToBall <= dp2px(2f)) {
                    p.dead = true;
                }
            } else if (p.x > cx + ringRadius) {
                ci.remove();
            }
        }

        // ===== 放电粒子 =====
        float dRatio = dischargeRatio();
        if (dischargeCurrent > 0.1f) {
            long interval = (long) (800 - 740 * dRatio);
            int maxCount = 2 + (int) (23 * dRatio);

            int count = 0;
            for (DischargeDrop p : dischargeDrops) { if (p.state != 3) count++; }

            if (now - lastDischargeSpawn > interval && count < maxCount) {
                DischargeDrop drop = new DischargeDrop();
                float angle = (float) (Math.PI * 0.5f + (random.nextFloat() - 0.5f) * 0.35f);
                drop.x = cx + (float) Math.cos(angle) * ringRadius * 0.9f;
                drop.y = cy + (float) Math.sin(angle) * ringRadius * 0.9f;
                drop.maxRadius = dp2px(2.0f) + dRatio * dp2px(3.5f);
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
                    float flySpeed = dp2px(0.2f) + dRatio * dp2px(2.5f);
                    flySpeed = Math.min(flySpeed, dp2px(3.0f));
                    float dx = targetHouseX - p.x;
                    float dy = targetHouseY - p.y;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);
                    p.speedX = (dx / dist) * flySpeed;
                    p.speedY = (dy / dist) * flySpeed;
                    p.breakAnim = 0f;
                }

            } else if (p.state == 1) {
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

                p.tailLength *= 0.92f;
                p.neckLength *= 0.9f;

                if (dist <= dp2px(6f)) {
                    p.state = 2;
                    p.shrinkTimer = 0f;
                }

            } else if (p.state == 2) {
                p.shrinkTimer += 0.05f;
                float shrinkProgress = Math.min(1f, p.shrinkTimer);
                p.radius = p.maxRadius * (1f - shrinkProgress * 0.8f);

                if (shrinkProgress >= 1f) {
                    p.state = 3;
                }
            }

            if (p.x > width + dp2px(30f) || p.y > height + dp2px(30f) ||
                    p.x < -dp2px(30f) || p.y < -dp2px(30f)) {
                p.state = 3;
            }
        }
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

        // ===== 太阳辐射（金黄色光芒，仅叠加）=====
        drawSolarRadiation(canvas);

        // ===== 外圈流体环 =====
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

        // ===== 内部液面 =====
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

        // ===== 充电粒子 =====
        for (ChargeParticle p : chargeParticles) {
            if (p.radius <= 0.3f || p.dead) continue;

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

        // ===== 放电粒子 =====
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
                float stretch = (float) Math.sqrt(p.speedX * p.speedX + p.speedY * p.speedY);
                float drawW = p.radius + stretch * 0.35f;
                float drawH = p.radius + stretch * 0.15f;
                bubbleRect.set(p.x - drawW, p.y - drawH, p.x + drawW, p.y + drawH);
                canvas.drawOval(bubbleRect, fluidPaint);

            } else if (p.state == 2) {
                float sp = Math.min(1f, p.shrinkTimer);
                int alpha = (int) (200 * (1f - sp));
                if (alpha > 0) {
                    glowPaint.setColor(fluidColor);
                    glowPaint.setAlpha(alpha);
                    float rippleR = p.radius * (1f + sp * 3f);
                    bubbleRect.set(p.x - rippleR, p.y - rippleR, p.x + rippleR, p.y + rippleR);
                    canvas.drawOval(bubbleRect, glowPaint);
                    glowPaint.setAlpha(255);
                }
                bubbleRect.set(p.x - p.radius * 0.7f, p.y - p.radius * 0.7f,
                        p.x + p.radius * 0.7f, p.y + p.radius * 0.7f);
                canvas.drawOval(bubbleRect, fluidPaint);
            }
        }

        // ===== 中心文字 =====
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textY = cy + (fm.bottom - fm.top) / 2f - fm.bottom;
        textPaint.setTextSize(dp2px(10f));
        textPaint.setColor(Color.parseColor("#1A1A1A"));
        canvas.drawText(
                String.format(java.util.Locale.getDefault(), "%.1f%%", progress),
                cx, textY, textPaint
        );
    }

    // ===== 太阳辐射 =====
    private void drawSolarRadiation(Canvas canvas) {
        if (!coordsBound || chargeCurrent < 0.1f) return;

        float cRatio = Math.min(chargeCurrent / 40f, 1f);
        float sunR = dp2px(solarIconSizeDp) * 0.18f;
        if (sunR < dp2px(6f)) sunR = dp2px(6f);

        float animPhase = (System.currentTimeMillis() % 3000) / 3000f;
        float baseAngle = 360f * animPhase * (0.3f + 0.7f * cRatio);

        // 光晕
        glowPaint.setColor(SUN_COLOR);
        glowPaint.setAlpha((int) (50 * cRatio));
        canvas.drawCircle(sunX, sunY, sunR * (2.5f + 3f * cRatio), glowPaint);

        // 长芒 8根
        sunRayPaint.setColor(SUN_COLOR);
        sunRayPaint.setStrokeWidth(dp2px(1.5f));
        sunRayPaint.setAlpha((int) (200 * cRatio));
        float longRayLen = sunR * (1.2f + 2.5f * cRatio);
        for (int i = 0; i < 8; i++) {
            float a = baseAngle + 45f * i;
            double rad = Math.toRadians(a);
            float sx = sunX + (float) Math.cos(rad) * sunR;
            float sy = sunY + (float) Math.sin(rad) * sunR;
            float ex = sunX + (float) Math.cos(rad) * (sunR + longRayLen);
            float ey = sunY + (float) Math.sin(rad) * (sunR + longRayLen);
            canvas.drawLine(sx, sy, ex, ey, sunRayPaint);
        }

        // 短芒 4~12根 反向
        int shortRayCount = 4 + (int) (8 * cRatio);
        float shortRayLen = sunR * (0.6f + 1.2f * cRatio);
        float revAngle = -baseAngle * 0.6f;
        sunRayPaint.setStrokeWidth(dp2px(1.8f));
        float pulse = (float) Math.sin(animPhase * 2 * Math.PI + 1) * 0.5f + 0.5f;
        sunRayPaint.setAlpha((int) (120 + 100 * cRatio * pulse));
        for (int i = 0; i < shortRayCount; i++) {
            float a = revAngle + (360f / shortRayCount) * i;
            double rad = Math.toRadians(a);
            float sx = sunX + (float) Math.cos(rad) * sunR * 0.85f;
            float sy = sunY + (float) Math.sin(rad) * sunR * 0.85f;
            float ex = sunX + (float) Math.cos(rad) * (sunR + shortRayLen);
            float ey = sunY + (float) Math.sin(rad) * (sunR + shortRayLen);
            canvas.drawLine(sx, sy, ex, ey, sunRayPaint);
        }
        sunRayPaint.setAlpha(255);
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