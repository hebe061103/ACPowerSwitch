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
import android.view.animation.LinearInterpolator;
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

    // ==================== 核心参数 ====================
    // 电流→比例，用 pow 让低电流也能看到效果
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

        float targetHouseX = width - dp2px(45f);
        float targetHouseY = height - dp2px(55f);

        // 噪声
        smoothPhase1 += 0.017f + (noiseRandom.nextFloat() * 0.006f - 0.003f);
        smoothPhase2 += 0.023f + (noiseRandom.nextFloat() * 0.006f - 0.003f);
        smoothPhase3 += 0.009f + (noiseRandom.nextFloat() * 0.004f - 0.002f);
        smoothAmp += (noiseRandom.nextFloat() * 0.02f - 0.01f);
        smoothAmp = Math.max(0.92f, Math.min(1.08f, smoothAmp));

        // ===== 1. 充电粒子 =====
        float cRatio = chargeRatio();
        if (chargeCurrent > 0.1f) {
            // 40A → 间隔 60ms, 同屏 30颗; 1A → 间隔 800ms, 同屏 3颗
            long interval = (long) (800 - 740 * cRatio);
            int maxCount = 3 + (int) (27 * cRatio);

            int count = 0;
            for (ChargeParticle p : chargeParticles) { if (!p.dead) count++; }

            if (now - lastChargeSpawn > interval && count < maxCount) {
                ChargeParticle p = new ChargeParticle();
                // 太阳能板中心坐标（外部设置，没设置就默认左上区域）`
                float targetSolarX = dp2px(50f);
                float targetSolarY = dp2px(50f);
                // 从太阳能板中心附近随机散开出生
                p.x = targetSolarX + (random.nextFloat() - 0.5f) * dp2px(20f);
                p.y = targetSolarY + (random.nextFloat() - 0.5f) * dp2px(20f);

                p.maxRadius = dp2px(6.5f) + cRatio * dp2px(8.0f);
                p.radius = p.maxRadius * 0.2f;
                p.growPhase = 0f;
                p.absorbPhase = 0f;

                // 充电粒子速度基数
                // dp2px(0.4f) 最小底速（1A时）0.4 想更慢改小，想更快改大
                // dp2px(2.5f)电流加成系数（40A时）2.5 想高速拉满改到 3.5~4.0
                float baseSpeed = dp2px(0.1f) + cRatio * dp2px(2.5f);
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
                // 接近 → 凝聚阶段
                p.absorbPhase += 0.05f;
                float absorbProgress = Math.min(1f, p.absorbPhase);

                // 减速
                p.speedX *= 0.88f;
                p.speedY *= 0.88f;

                // 体积膨胀（凝聚感）
                p.radius = p.maxRadius * (0.4f + 0.8f * absorbProgress);

                // 被球面吸附
                float pullStrength = 1f - (distToBall / dp2px(22f));
                p.x += (targetX - p.x) * pullStrength * 0.25f;
                p.y += (cy - p.y) * pullStrength * 0.12f;

                // 凝聚光晕
                p.growPhase = absorbProgress;

                if (distToBall <= dp2px(2f)) {
                    p.dead = true;
                }
            } else if (p.x > cx + ringRadius) {
                ci.remove();
            }
        }

        // ===== 2. 放电液滴 =====
        float dRatio = dischargeRatio();
        if (dischargeCurrent > 0.1f) {
            // 40A → 间隔 60ms, 同屏 25颗
            long interval = (long) (800 - 740 * dRatio);
            int maxCount = 2 + (int) (23 * dRatio);

            int count = 0;
            for (DischargeDrop p : dischargeDrops) { if (p.state != 3) count++; }

            if (now - lastDischargeSpawn > interval && count < maxCount) {
                DischargeDrop drop = new DischargeDrop();
                float angle = (float) (Math.PI * 0.5 + (random.nextFloat() - 0.5f) * 0.35f);
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

        // 放电液滴运动
        Iterator<DischargeDrop> di = dischargeDrops.iterator();
        while (di.hasNext()) {
            DischargeDrop p = di.next();
            if (p.state == 3) { di.remove(); continue; }

            if (p.state == 0) {
                // ===== 凝聚：水滴从球面慢慢挤出 =====
                p.growTimer += 0.035f;
                float growProgress = Math.min(1f, p.growTimer);

                p.radius = p.maxRadius * (0.15f + 0.85f * growProgress);
                p.tailLength = growProgress * p.maxRadius * 2.2f;
                p.neckLength = growProgress * p.maxRadius * 1.2f;

                // 重力下垂
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
                // ===== 飞行：追踪房子 =====
                float flySpeed = dp2px(0.1f) + dRatio * dp2px(2.5f);
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

                // ✅ 到房子中心附近才消失（6dp）
                if (dist <= dp2px(6f)) {
                    p.state = 2;
                    p.shrinkTimer = 0f;
                }

            } else if (p.state == 2) {
                // ===== 撞击凝聚：在房子处扩散消失 =====
                p.shrinkTimer += 0.05f;
                float shrinkProgress = Math.min(1f, p.shrinkTimer);
                p.radius = p.maxRadius * (1f - shrinkProgress * 0.8f);

                if (shrinkProgress >= 1f) {
                    p.state = 3;
                }
            }

            // 兜底
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


        // ===== 1. 外圈流体环 =====
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

        // ===== 2. 内部液面 =====
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

        // ===== 3. 充电粒子 =====
        for (ChargeParticle p : chargeParticles) {
            if (p.radius <= 0.3f || p.dead) continue;

            if (p.absorbPhase > 0) {
                // 凝聚阶段：画光晕 + 拉伸贴附
                float ap = Math.min(1f, p.absorbPhase);

                // 外层光晕（大）
                glowPaint.setColor(fluidColor);
                glowPaint.setAlpha((int) (50 * ap));
                float glowR = p.radius * (1.5f + ap * 1.5f);
                bubbleRect.set(p.x - glowR, p.y - glowR, p.x + glowR, p.y + glowR);
                canvas.drawOval(bubbleRect, glowPaint);

                // 中层光晕
                glowPaint.setAlpha((int) (100 * ap));
                glowR = p.radius * (1.2f + ap * 0.8f);
                bubbleRect.set(p.x - glowR, p.y - glowR, p.x + glowR, p.y + glowR);
                canvas.drawOval(bubbleRect, glowPaint);

                glowPaint.setAlpha(255);
            }

            // 主体
            float drawW = p.radius * (p.absorbPhase > 0 ? 1.3f : 1.15f);
            float drawH = p.radius * (p.absorbPhase > 0 ? 1.1f : 1.0f);
            bubbleRect.set(p.x - drawW, p.y - drawH, p.x + drawW * 1.2f, p.y + drawH);
            canvas.drawOval(bubbleRect, fluidPaint);
        }

        // ===== 4. 放电液滴 =====
        for (DischargeDrop p : dischargeDrops) {
            if (p.state == 3 || p.radius <= 0.3f) continue;

            if (p.state == 0) {
                // 凝聚中：画水滴形（颈部连接球面）
                dropPath.reset();
                // 主体圆
                float bodyTop = p.y - p.radius;
                float bodyBottom = p.y + p.radius;
                float bodyLeft = p.x - p.radius * 0.8f;
                float bodyRight = p.x + p.radius * 0.8f;

                dropPath.addOval(bodyLeft, bodyTop, bodyRight, bodyBottom, Path.Direction.CW);

                // 颈部（连接回球面）
                if (p.neckLength > 0) {
                    dropPath.moveTo(p.x - p.radius * 0.4f, bodyTop);
                    dropPath.lineTo(p.x - p.radius * 0.3f, bodyTop - p.neckLength);
                    dropPath.lineTo(p.x + p.radius * 0.3f, bodyTop - p.neckLength);
                    dropPath.lineTo(p.x + p.radius * 0.4f, bodyTop);
                    dropPath.close();
                }

                // 尾部拉伸
                if (p.tailLength > 0) {
                    dropPath.moveTo(p.x - p.radius * 0.5f, bodyBottom);
                    dropPath.lineTo(p.x - p.radius * 0.3f, bodyBottom + p.tailLength);
                    dropPath.lineTo(p.x + p.radius * 0.3f, bodyBottom + p.tailLength);
                    dropPath.lineTo(p.x + p.radius * 0.5f, bodyBottom);
                    dropPath.close();
                }

                canvas.drawPath(dropPath, fluidPaint);

            } else if (p.state == 1) {
                // 飞行中：椭圆拉伸
                float stretch = (float) Math.sqrt(p.speedX * p.speedX + p.speedY * p.speedY);
                float drawW = p.radius + stretch * 0.35f;
                float drawH = p.radius + stretch * 0.15f;
                bubbleRect.set(p.x - drawW, p.y - drawH, p.x + drawW, p.y + drawH);
                canvas.drawOval(bubbleRect, fluidPaint);

            } else if (p.state == 2) {
                // 撞击：扩散涟漪
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
                // 核心
                bubbleRect.set(p.x - p.radius * 0.7f, p.y - p.radius * 0.7f,
                        p.x + p.radius * 0.7f, p.y + p.radius * 0.7f);
                canvas.drawOval(bubbleRect, fluidPaint);
            }
        }

        // ===== 5. 中心文字 =====
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textY = cy + (fm.bottom - fm.top) / 2f - fm.bottom;
        textPaint.setTextSize(dp2px(10f));
        textPaint.setColor(Color.parseColor("#1A1A1A"));
        canvas.drawText(
                String.format(java.util.Locale.getDefault(), "%.1f%%", progress),
                cx, textY, textPaint
        );
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