package cn.gardenia.client.tool.rotation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class IncrementalRotation {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private Vec2f target;
    private float maxYawStep;
    private float maxPitchStep;
    private boolean active;
    private boolean yawFirst = true;

    public void init() {}

    public void start(float targetYaw, float targetPitch, float maxStep) {
        this.target = new Vec2f(targetYaw, targetPitch);
        this.maxYawStep = maxStep;
        this.maxPitchStep = maxStep;
        this.active = true;
    }

    public void start(Vec2f target, float maxStep) {
        start(target.x, target.y, maxStep);
    }

    public void start(float targetYaw, float targetPitch, float maxYawStep, float maxPitchStep) {
        this.target = new Vec2f(targetYaw, targetPitch);
        this.maxYawStep = maxYawStep;
        this.maxPitchStep = maxPitchStep;
        this.active = true;
    }

    public void startEntity(net.minecraft.entity.Entity target, float maxStep) {
        if (target == null || mc.player == null) return;
        Vec3d pos = mc.player.getEyePos();
        Vec3d tPos = target.getPos().add(0, target.getHeight() * 0.75, 0);
        double dx = tPos.x - pos.x;
        double dy = tPos.y - pos.y;
        double dz = tPos.z - pos.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        start(yaw, Math.clamp(pitch, -90.0f, 90.0f), maxStep);
    }

    public void setYawFirst(boolean yawFirst) {
        this.yawFirst = yawFirst;
    }

    public void tick() {
        if (!active || mc.player == null) return;

        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        float diffYaw = MathHelper.wrapDegrees(target.x - currentYaw);
        float diffPitch = target.y - currentPitch;

        boolean yawDone = Math.abs(diffYaw) <= maxYawStep;
        boolean pitchDone = Math.abs(diffPitch) <= maxPitchStep;

        if (yawDone && pitchDone) {
            mc.player.setYaw(target.x);
            mc.player.setPitch(target.y);
            mc.player.headYaw = target.x;
            active = false;
            return;
        }

        float newYaw = currentYaw;
        float newPitch = currentPitch;

        if (yawFirst) {
            if (!yawDone) {
                newYaw += Math.clamp(diffYaw, -maxYawStep, maxYawStep);
            }
            if (yawDone || Math.abs(diffYaw) < maxYawStep * 2) {
                if (!pitchDone) {
                    newPitch += Math.clamp(diffPitch, -maxPitchStep, maxPitchStep);
                }
            }
        } else {
            if (!pitchDone) {
                newPitch += Math.clamp(diffPitch, -maxPitchStep, maxPitchStep);
            }
            if (pitchDone || Math.abs(diffPitch) < maxPitchStep * 2) {
                if (!yawDone) {
                    newYaw += Math.clamp(diffYaw, -maxYawStep, maxYawStep);
                }
            }
        }

        mc.player.setYaw(newYaw);
        mc.player.setPitch(Math.clamp(newPitch, -90.0f, 90.0f));
        mc.player.headYaw = newYaw;
    }

    public void updateTarget(float targetYaw, float targetPitch) {
        this.target = new Vec2f(targetYaw, targetPitch);
    }

    public void updateTarget(Vec2f target) {
        this.target = target;
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
}
