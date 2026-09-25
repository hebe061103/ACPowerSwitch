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
import java.util.Locale;
import java.util.Random;

public class FluidBubbleView extends View {
    private Paint ringPaint;
    private Paint fluidPaint;
    private Paint textPaint;

    private float progress = 0f;
    private int fluidColor = Color.parseColor("#39FF14");

    private float animationPhase = 0f;
    private final List<EnergyDrop> particles = new ArrayList<>();
    private final Random random = new Random();
    private long lastParticleTime = 0;

    private boolean isCharging = true;

    // 复用对象
    private final RectF ringOval = new RectF();
    private final Path circleClipPath = new Path();
    private final Path lightWavePath = new Path();
    private final Path fluidRingPath = new Path();
    private final Path wavePath = new Path();
    private final RectF bubbleRect = new RectF();
    private float chargingCurrent = 0f;
    private boolean isNoDischarge = false;

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

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(2200);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            animationPhase = animation.getAnimatedFraction();
            updateEnergyParticles();
            invalidate();
        });
        animator.start();
    }

    public void updateConfig(float progress, int color, boolean isCharging, float current, boolean isNoDischarge) {
        boolean needInvalidate = false;
        if (this.progress != progress) { this.progress = progress; needInvalidate = true; }
        if (this.fluidColor != color) { this.fluidColor = color; needInvalidate = true; }
        if (this.isCharging != isCharging) { this.isCharging = isCharging; needInvalidate = true; }
        if (this.chargingCurrent != current) { this.chargingCurrent = current; needInvalidate = true; }
        if (this.isNoDischarge != isNoDischarge) { this.isNoDischarge = isNoDischarge; needInvalidate = true; }
        if (needInvalidate) { invalidate(); }
    }

    private void updateEnergyParticles() {
        long now = System.currentTimeMillis();
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) return;

        float ringWidthPx = dp2px(70f);
        float cx = width - (ringWidthPx / 2f) - dp2px(2f);
        float ringRadius = (ringWidthPx - dp2px(12f)) / 2f;
        float cy = ringRadius + dp2px(6f);

        float currentRatio = Math.min(1.0f, Math.max(0f, chargingCurrent / 40f));

        if (isCharging) {
            removeDownParticles();

            currentRatio = Math.min(1.0f, Math.max(0f, chargingCurrent / 40f));
            long dynamicSpawnInterval = (long) (1000 - (900 * currentRatio));
            int dynamicMaxParticles = (int) (2 + (22 * currentRatio));

            if (now - lastParticleTime > dynamicSpawnInterval && particles.size() < dynamicMaxParticles) {
                EnergyDrop p = new EnergyDrop();
                p.x = cx;
                p.y = cy + ringRadius;
                p.maxRadius = dp2px(4.5f) + random.nextFloat() * dp2px(4.0f);
                p.radius = p.maxRadius;

                float baseSpeed = dp2px(0.4f) + (currentRatio * dp2px(2.0f));
                baseSpeed = Math.min(baseSpeed, dp2px(0.8f));
                p.speedX = baseSpeed + random.nextFloat() * dp2px(0.2f);
                p.speedY = (cy - p.y) / ((cx - p.x) / p.speedX);

                p.wobbleOffset = random.nextFloat() * 100f;
                p.isFalling = false;
                particles.add(p);
                lastParticleTime = now;
            }
        } else {
            // ==================== 🏡【放电模式：电池 ➔ 住宅】====================
            removeUpParticles();

            // 放电电流绝对值比例（用于调节水滴大小和数量）
            float dischargeRatio = Math.min(1.0f, Math.max(0f, Math.abs(chargingCurrent) / 40f));

            // ✅ 生成间隔：电流小→慢（1500ms），电流大→快（200ms）
            // 关键：电流越小时，单颗水滴的生命周期越完整、越看得清
            long dropInterval = (long) (1500 - (1300 * dischargeRatio));

            // ✅ 同屏水滴数：电流小→1颗，电流大→最多5颗
            int maxDrops = 1 + (int) (4 * dischargeRatio);

            // ✅ 水滴半径：电流小→小（3dp），电流大→大（9dp）
            float minRadius = dp2px(3.0f);
            float maxRadius = dp2px(5.0f) + dischargeRatio * dp2px(5f);

            // ✅ 凝聚速度：电流小→很慢，电流大→快
            // 这是关键！电流小时凝聚过程拉长，肉眼能看清从小变大的过程
            float condenseSpeed = 0.008f + dischargeRatio * 0.03f;

            // ✅ 拉丝速度：电流小→慢慢被拽长，电流大→干脆利落
            float stretchSpeed = dp2px(0.25f) + dischargeRatio * dp2px(0.8f);

            // ✅ 缩颈速度：电流小→颈部收得很慢很细才断，电流大→快断
            float neckShrink = 0.82f + dischargeRatio * 0.1f;

            if (progress > 0f && !isNoDischarge && now - lastParticleTime > dropInterval && particles.size() < maxDrops) {
                EnergyDrop p = new EnergyDrop();
                // ✅ 出生点：圆环正下方外边缘，带微小随机偏移
                p.x = cx + (random.nextFloat() * dp2px(4f) - dp2px(2f));
                p.y = cy + ringRadius;

                p.maxRadius = minRadius + random.nextFloat() * (maxRadius - minRadius);
                p.radius = 0.1f;
                p.speedX = 0f;
                p.speedY = 0f;
                p.wobbleOffset = random.nextFloat() * 100f;
                p.isFalling = true;

                p.condenseProgress = 0f;
                p.hasDetached = false;

                // 水滴物理参数
                p.stretchY = 0f;
                p.neckRadius = p.maxRadius * 0.55f;
                p.dropPhase = 0;           // 0=凝聚, 1=悬挂拉伸, 2=缩颈断裂, 3=坠落
                p.condenseSpeed = condenseSpeed;
                p.stretchSpeed = stretchSpeed;
                p.neckShrink = neckShrink;

                particles.add(p);
                lastParticleTime = now;
            }
        }

        // 统一拓扑运动刷新
        Iterator<EnergyDrop> iterator = particles.iterator();
        while (iterator.hasNext()) {
            EnergyDrop p = iterator.next();
            if (p.isFalling) {
                // ✅ 四阶段真实水滴物理
                switch (p.dropPhase) {

                    case 0: // ① 凝聚：从极小慢慢胀大到最大体积
                        p.y = cy + ringRadius;
                        p.condenseProgress += p.condenseSpeed;

                        // 用平滑曲线，先快后慢地涨到 1.15 倍最大体积
                        // ease-out：前期涨得快，后期越来越慢，像在"蓄力"
                        float t = Math.min(1f, p.condenseProgress);
                        float easeOut = 1f - (1f - t) * (1f - t);
                        p.radius = p.maxRadius * (0.15f + easeOut);

                        // 凝聚到位 → 进入悬挂拉伸
                        if (p.condenseProgress >= 1.0f) {
                            p.dropPhase = 1;
                        }
                        break;

                    case 1: // ② 悬挂拉伸：水滴被重力慢慢往下拽，形成"拉丝"
                        p.stretchY += p.stretchSpeed;
                        p.y = cy + ringRadius + p.stretchY * 0.5f;

                        // 主体微微缩小（体积被拉长了）
                        float bodyShrink = 1.0f - (p.stretchY / dp2px(28f)) * 0.15f;
                        bodyShrink = Math.max(0.85f, bodyShrink);
                        p.radius = p.maxRadius * bodyShrink;

                        // 颈部也开始变细
                        p.neckRadius = p.maxRadius * 0.55f * (1f - p.stretchY / dp2px(28f));
                        p.neckRadius = Math.max(p.maxRadius * 0.12f, p.neckRadius);

                        // 拉伸到位 → 进入缩颈断裂
                        if (p.stretchY >= dp2px(14f)) {
                            p.dropPhase = 2;
                        }
                        break;

                    case 2: // ③ 缩颈断裂：颈部急剧收缩变细，像细线一样被拉断
                        p.stretchY += p.stretchSpeed * 1.3f;
                        p.y = cy + ringRadius + p.stretchY * 0.6f;

                        // 颈部急剧收缩（电流小→收得更细才断）
                        p.neckRadius *= p.neckShrink;

                        // 主体体积因断裂而骤缩
                        float breakRatio = 0.9f * (p.neckRadius / (p.maxRadius * 0.12f));
                        breakRatio = Math.max(0.55f, Math.min(1.0f, breakRatio));
                        p.radius = p.maxRadius * breakRatio;

                        // 颈部细到极细 → 彻底断裂，进入坠落
                        if (p.neckRadius <= p.maxRadius * 0.04f) {
                            p.dropPhase = 3;
                            p.hasDetached = true;
                            // 断裂初速度：电流大→弹得远，电流小→轻轻落下
                            float dischargeRatio = Math.min(1.0f, Math.abs(chargingCurrent) / 40f);
                            p.speedY = dp2px(0.2f) + dischargeRatio * dp2px(0.5f);
                        }
                        break;

                    case 3: // ④ 自由坠落
                    default:
                        p.speedY += dp2px(0.05f);
                        p.y += p.speedY;

                        // 坠落时恢复圆形
                        float recoveryProgress = Math.min(1f, p.speedY / dp2px(3f));
                        p.radius = p.maxRadius * (0.6f + 0.4f * recoveryProgress);

                        float houseTopY = height - dp2px(110f);
                        if (p.y >= houseTopY) {
                            float fadeRatio = Math.max(0f, (height - dp2px(40f) - p.y) / dp2px(70f));
                            p.radius = p.maxRadius * fadeRatio * Math.min(1f, recoveryProgress + 0.4f);
                        }
                        if (p.y >= height - dp2px(60f)) {
                            iterator.remove();
                        }
                        break;
                }
            } else {
                // ☀️ 充电斜线飞向圆环（不变）
                p.x += p.speedX;
                p.y += p.speedY;

                float dynamicWobble = dp2px(0.12f) - (currentRatio * dp2px(0.10f));
                p.y += (float) Math.sin(animationPhase * 2 * Math.PI + p.wobbleOffset) * dynamicWobble;

                float ringLeftX = cx - ringRadius;
                float distanceToLeftEdge = ringLeftX - p.x;
                float magnetThreshold = dp2px(15f);

                if (distanceToLeftEdge <= magnetThreshold && p.x < cx) {
                    float pullFactor = Math.max(0f, (magnetThreshold - Math.max(0f, distanceToLeftEdge)) / magnetThreshold);
                    p.x += dp2px(1.5f) * pullFactor;
                    p.maxRadius = p.maxRadius * (1.0f + 0.18f * pullFactor);

                    if (p.x >= ringLeftX) {
                        float overlapDistance = p.x - ringLeftX;
                        float absorbRatio = Math.max(0f, (dp2px(10f) - overlapDistance) / dp2px(10f));
                        p.radius = p.maxRadius * absorbRatio;
                    } else {
                        p.radius = p.maxRadius;
                    }

                    if (p.x >= ringLeftX + dp2px(8f)) {
                        iterator.remove();
                    }
                }
            }
        }
    }

    private void removeDownParticles() { particles.removeIf(p -> p.isFalling); }
    private void removeUpParticles() { particles.removeIf(p -> !p.isFalling); }

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
        ringOval.set(cx - ringRadius, cy - ringRadius, cx + ringRadius, cy + ringRadius);
        ringPaint.setColor(fluidColor);
        fluidRingPath.reset();

        int count = 60;
        float sweepAngle = 360f;

        for (int i = 0; i <= count; i++) {
            float angleDeg = -90f + (sweepAngle * i / count);
            float angleRad = (float) Math.toRadians(angleDeg);

            float wave1 = (float) Math.sin(i * 0.4f + animationPhase * 2 * Math.PI) * dp2px(1.6f);
            float wave2 = (float) Math.cos(i * 0.2f - animationPhase * 3 * Math.PI) * dp2px(1.0f);
            float currentWave = (wave1 + wave2) * 0.9f;

            float dynamicRadius = ringRadius + currentWave;
            float px = cx + (float) Math.cos(angleRad) * dynamicRadius;
            float py = cy + (float) Math.sin(angleRad) * dynamicRadius;

            if (i == 0) fluidRingPath.moveTo(px, py);
            else fluidRingPath.lineTo(px, py);
        }
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
            wavePath.quadTo(
                    cx - ringRadius * 0.5f,
                    fluidY + waveBias1 + dp2px(3f),
                    cx,
                    fluidY + waveBias2
            );
            wavePath.quadTo(
                    cx + ringRadius * 0.5f,
                    fluidY + waveBias2 - dp2px(3f),
                    cx + ringRadius,
                    fluidY + waveBias1
            );
            wavePath.lineTo(cx + ringRadius, height);
            wavePath.close();

            fluidPaint.setColor(Color.argb(180,
                    Color.red(fluidColor),
                    Color.green(fluidColor),
                    Color.blue(fluidColor)
            ));
            canvas.drawPath(wavePath, fluidPaint);

            lightWavePath.reset();
            float lightBias1 = (float) Math.cos(-animationPhase * 2 * Math.PI + 1.5f) * dp2px(2.5f);
            float lightBias2 = (float) Math.sin(-animationPhase * 2 * Math.PI) * dp2px(1.5f);

            lightWavePath.moveTo(cx - ringRadius, height);
            lightWavePath.lineTo(cx - ringRadius, fluidY + dp2px(1f) + lightBias1);
            lightWavePath.quadTo(cx, fluidY + lightBias2, cx + ringRadius, fluidY + dp2px(1f) + lightBias1);
            lightWavePath.lineTo(cx + ringRadius, height);
            lightWavePath.close();

            fluidPaint.setColor(Color.argb(60,
                    Color.red(fluidColor),
                    Color.green(fluidColor),
                    Color.blue(fluidColor)
            ));
            canvas.drawPath(lightWavePath, fluidPaint);

            fluidPaint.setColor(fluidColor);
        }

        canvas.restore();

        // ===== 3. 粒子 =====
        for (EnergyDrop p : particles) {
            if (p.radius <= 0.5f) continue;

            if (p.isFalling) {
                // ✅ 水滴形态绘制（根据阶段区分）
                if (p.dropPhase <= 2) {
                    // 凝聚/拉伸/缩颈阶段：画水滴主体 + 颈部拉丝连线

                    // 颈部起点：圆环正下方外边缘
                    float neckTopY = cy + ringRadius - dp2px(1f);

                    // 水滴主体底部位置
                    float bodyBottomY;
                    if (p.dropPhase == 0) {
                        // 凝聚阶段：主体还贴在圆环上
                        bodyBottomY = p.y + p.radius;
                    } else {
                        // 拉伸/缩颈阶段：主体已下垂
                        bodyBottomY = p.y + p.radius + p.stretchY * 0.4f;
                    }

                    // 1) 画颈部拉丝（连接圆环到水滴主体）
                    if (p.dropPhase >= 1 && bodyBottomY > neckTopY) {
                        // 颈部从圆环处开始，逐渐变细到主体顶部
                        float neckTopR = p.neckRadius;           // 圆环处半径（细）
                        float neckBotR = p.radius * 0.85f;      // 主体连接处半径（粗）

                        // 用两层椭圆模拟锥形拉丝
                        int neckAlpha = (int) (255 * Math.min(1f, p.neckRadius / (p.maxRadius * 0.4f)));
                        neckAlpha = Math.max(40, neckAlpha);
                        fluidPaint.setAlpha(neckAlpha);

                        // 拉丝分 4 段画，越往下越粗
                        int seg = 4;
                        for (int i = 0; i < seg; i++) {
                            float f1 = (float) i / seg;
                            float f2 = (float) (i + 1) / seg;
                            float y1 = neckTopY + (bodyBottomY - neckTopY) * f1;
                            float y2 = neckTopY + (bodyBottomY - neckTopY) * f2;
                            float r1 = neckTopR + (neckBotR - neckTopR) * f1;
                            float r2 = neckTopR + (neckBotR - neckTopR) * f2;
                            bubbleRect.set(p.x - r1, y1, p.x + r1, y2 + (r2 - r1));
                            canvas.drawRect(bubbleRect, fluidPaint);
                        }
                        fluidPaint.setAlpha(255);
                    }

                    // 2) 画水滴主体（椭圆，拉伸时变扁长）
                    float bodyHeight;
                    if (p.dropPhase == 0) {
                        bodyHeight = p.radius * 2f;
                    } else {
                        bodyHeight = p.radius * 2f + p.stretchY * 0.4f;
                    }
                    bubbleRect.set(
                            p.x - p.radius,
                            p.y - p.radius,
                            p.x + p.radius,
                            p.y + bodyHeight
                    );
                    canvas.drawOval(bubbleRect, fluidPaint);

                } else {
                    // 坠落阶段：水滴形（头朝下）
                    bubbleRect.set(
                            p.x - p.radius * 0.85f,
                            p.y - p.radius * 0.5f,
                            p.x + p.radius * 0.85f,
                            p.y + p.radius * 1.4f
                    );
                    canvas.drawOval(bubbleRect, fluidPaint);
                }

                // 高光核
                fluidPaint.setColor(Color.parseColor("#E0FFD0"));
                canvas.drawCircle(p.x, p.y + p.radius * 0.15f, p.radius * 0.28f, fluidPaint);
                fluidPaint.setColor(fluidColor);

            } else {
                // 充电粒子（不变）
                bubbleRect.set(p.x - p.radius, p.y - p.radius, p.x + p.radius * 1.35f, p.y + p.radius);
                canvas.drawOval(bubbleRect, fluidPaint);

                if (p.radius > dp2px(3f)) {
                    textPaint.setColor(Color.RED);
                    textPaint.setTextSize(p.radius * 1.2f);
                    Paint.FontMetrics fm = textPaint.getFontMetrics();
                    float offsetY = (fm.bottom - fm.top) / 2f - fm.bottom;
                    canvas.drawText("+", p.x + p.radius * 0.1f, p.y + offsetY, textPaint);
                }
            }
        }

        // ===== 4. 中心文字 =====
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textY = cy + (fm.bottom - fm.top) / 2f - fm.bottom;

        textPaint.setTextSize(dp2px(10f));

        if (fluidY <= textY) {
            textPaint.setColor(Color.WHITE);
        } else {
            textPaint.setColor(Color.parseColor("#1A1A1A"));
        }

        canvas.drawText(
                String.format(Locale.getDefault(), "%.1f%%", progress),
                cx,
                textY,
                textPaint
        );
    }

    // ✅ EnergyDrop 新增水滴物理字段
    private class EnergyDrop {
        float x;
        float y;
        float radius;
        float maxRadius;
        float speedX;
        float speedY;
        float wobbleOffset;
        boolean isFalling;
        float condenseProgress = 0f;
        boolean hasDetached = false;

        // ✅ 水滴悬挂物理
        float stretchY = 0f;          // 纵向拉伸量
        float neckRadius = 0f;        // 颈部半径
        int dropPhase = 0;            // 0=凝聚, 1=拉伸, 2=缩颈, 3=坠落
        float condenseSpeed = 0.02f;  // 凝聚速度
        float stretchSpeed = dp2px(0.5f); // 拉伸速度
        float neckShrink = 0.85f;     // 颈部收缩系数
    }
}
