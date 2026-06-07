package cn.gardenia.client.tool.rotation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class SmoothRotation {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private Vec2f target;
    private float speed;
    private boolean active;
    private Vec2f lastApplied;

    public void init() {}

    public void start(float targetYaw, float targetPitch, float speed) {
        this.target = new Vec2f(targetYaw, targetPitch);
        this.speed = speed;
        this.active = true;
        this.lastApplied = null;
    }

    public void start(Vec2f target, float speed) {
        start(target.x, target.y, speed);
    }

    public void startEntity(net.minecraft.entity.Entity target, float speed) {
        if (target == null || mc.player == null) return;
        Vec3d pos = mc.player.getEyePos();
        Vec3d tPos = target.getPos().add(0, target.getHeight() * 0.75, 0);
        double dx = tPos.x - pos.x;
        double dy = tPos.y - pos.y;
        double dz = tPos.z - pos.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        start(yaw, Math.clamp(pitch, -90.0f, 90.0f), speed);
    }

    public void tick() {
        if (!active || mc.player == null) return;

        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        float diffYaw = MathHelper.wrapDegrees(target.x - currentYaw);
        float diffPitch = target.y - currentPitch;

        if (Math.abs(diffYaw) < speed && Math.abs(diffPitch) < speed) {
            mc.player.setYaw(target.x);
            mc.player.setPitch(target.y);
            mc.player.headYaw = target.x;
            active = false;
            return;
        }

        float newYaw = currentYaw + Math.clamp(diffYaw, -speed, speed);
        float newPitch = currentPitch + Math.clamp(diffPitch, -speed, speed);

        mc.player.setYaw(newYaw);
        mc.player.setPitch(Math.clamp(newPitch, -90.0f, 90.0f));
        mc.player.headYaw = newYaw;

        lastApplied = new Vec2f(newYaw, newPitch);
    }

    public void updateTarget(float targetYaw, float targetPitch) {
        this.target = new Vec2f(targetYaw, targetPitch);
    }

    public void updateTarget(Vec2f target) {
        this.target = target;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public boolean isActive() {
        return active;
    }

    public void stop() {
        active = false;
    }

    public Vec2f getTarget() {
        return target;
    }

    public Vec2f getLastApplied() {
        return lastApplied;
    }
}
