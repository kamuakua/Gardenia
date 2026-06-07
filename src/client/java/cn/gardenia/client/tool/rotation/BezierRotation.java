package cn.gardenia.client.tool.rotation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class BezierRotation {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private Vec2f startRotation;
    private Vec2f endRotation;
    private Vec2f controlYaw;
    private Vec2f controlPitch;
    private int duration;
    private int elapsed;
    private Runnable callback;

    public void init() {}

    public static class BezierConfig {
        public float yawControlOffset = 30.0f;
        public float pitchControlOffset = 15.0f;
        public int durationTicks = 5;
        public EasingType easing = EasingType.NONE;

        public enum EasingType {
            NONE,
            EASE_IN,
            EASE_OUT,
            EASE_IN_OUT
        }
    }

    private BezierConfig config = new BezierConfig();

    public void setConfig(BezierConfig config) {
        this.config = config;
    }

    public boolean isRunning() {
        return elapsed < duration;
    }

    public void start(Vec2f target, int ticks, Runnable onComplete) {
        if (mc.player == null) return;
        startRotation = new Vec2f(mc.player.getYaw(), mc.player.getPitch());
        endRotation = target;
        float yawDiff = target.x - startRotation.x;
        float pitchDiff = target.y - startRotation.y;
        controlYaw = new Vec2f(
                startRotation.x + yawDiff * 0.33f + config.yawControlOffset,
                startRotation.y + pitchDiff * 0.66f + config.pitchControlOffset
        );
        controlPitch = new Vec2f(
                startRotation.x + yawDiff * 0.66f - config.yawControlOffset,
                startRotation.y + pitchDiff * 0.33f - config.pitchControlOffset
        );
        duration = ticks;
        elapsed = 0;
        callback = onComplete;
    }

    public void start(Vec2f target, int ticks) {
        start(target, ticks, null);
    }

    public void startEntity(net.minecraft.entity.Entity target, int ticks, Runnable onComplete) {
        if (target == null) return;
        Vec2f rot = calculateRotation(target);
        start(rot, ticks, onComplete);
    }

    private Vec2f calculateRotation(net.minecraft.entity.Entity target) {
        if (mc.player == null) return new Vec2f(0, 0);
        Vec3d pos = mc.player.getEyePos();
        Vec3d targetPos = target.getPos().add(0, target.getHeight() * 0.75, 0);
        double dx = targetPos.x - pos.x;
        double dy = targetPos.y - pos.y;
        double dz = targetPos.z - pos.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        return new Vec2f(yaw, Math.clamp(pitch, -90.0f, 90.0f));
    }

    public void tick() {
        if (elapsed >= duration || mc.player == null) return;

        float t = (float) elapsed / duration;
        t = applyEasing(t);

        float yaw = cubicBezier(
                startRotation.x, controlYaw.x, controlPitch.x, endRotation.x, t
        );
        float pitch = cubicBezier(
                startRotation.y, controlYaw.y, controlPitch.y, endRotation.y, t
        );

        mc.player.setYaw(yaw);
        mc.player.setPitch(Math.clamp(pitch, -90.0f, 90.0f));

        mc.player.headYaw = yaw;

        elapsed++;

        if (elapsed >= duration && callback != null) {
            callback.run();
            callback = null;
        }
    }

    public Vec2f calculatePoint(Vec2f start, Vec2f cp1, Vec2f cp2, Vec2f end, float t) {
        t = applyEasing(t);
        float yaw = cubicBezier(start.x, cp1.x, cp2.x, end.x, t);
        float pitch = cubicBezier(start.y, cp1.y, cp2.y, end.y, t);
        return new Vec2f(yaw, Math.clamp(pitch, -90.0f, 90.0f));
    }

    public void reset() {
        elapsed = duration;
        callback = null;
    }

    public void cancel() {
        elapsed = duration;
        callback = null;
    }

    public int getProgress() {
        return duration > 0 ? elapsed * 100 / duration : 100;
    }

    public float getProgressFloat() {
        return duration > 0 ? (float) elapsed / duration : 1.0f;
    }

    private float cubicBezier(float p0, float p1, float p2, float p3, float t) {
        float mt = 1.0f - t;
        return mt * mt * mt * p0 + 3 * mt * mt * t * p1 + 3 * mt * t * t * p2 + t * t * t * p3;
    }

    private float applyEasing(float t) {
        return switch (config.easing) {
            case EASE_IN -> t * t;
            case EASE_OUT -> t * (2.0f - t);
            case EASE_IN_OUT -> t < 0.5f ? 2.0f * t * t : -1.0f + (4.0f - 2.0f * t) * t;
            case NONE -> t;
        };
    }
}
