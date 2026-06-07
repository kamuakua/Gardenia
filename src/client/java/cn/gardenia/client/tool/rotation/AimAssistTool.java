package cn.gardenia.client.tool.rotation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

public class AimAssistTool {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private Entity lockedTarget;
    private float lockRange = 6.0f;
    private float lockFov = 180.0f;
    private boolean targetLocked;

    public void init() {}

    public void lock(Entity target) {
        this.lockedTarget = target;
        this.targetLocked = true;
    }

    public void unlock() {
        this.lockedTarget = null;
        this.targetLocked = false;
    }

    public boolean isLocked() {
        return targetLocked && lockedTarget != null && lockedTarget.isAlive();
    }

    public Entity getLockedTarget() {
        return lockedTarget;
    }

    public void setRange(float range) {
        this.lockRange = range;
    }

    public void setFov(float fov) {
        this.lockFov = fov;
    }

    public void aimTick(float aimSpeed, boolean clickState) {
        if (!isLocked()) return;
        if (mc.player == null) return;

        if (mc.player.distanceTo(lockedTarget) > lockRange || !lockedTarget.isAlive()) {
            unlock();
            return;
        }

        Vec3d pos = mc.player.getEyePos();
        Vec3d targetPos = lockedTarget.getPos().add(0, lockedTarget.getHeight() * 0.75, 0);
        double dx = targetPos.x - pos.x;
        double dy = targetPos.y - pos.y;
        double dz = targetPos.z - pos.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        targetPitch = Math.clamp(targetPitch, -90.0f, 90.0f);

        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        float diffYaw = net.minecraft.util.math.MathHelper.wrapDegrees(targetYaw - currentYaw);
        float diffPitch = targetYaw - currentPitch;

        float stepYaw = Math.clamp(diffYaw * aimSpeed, -aimSpeed, aimSpeed);
        float stepPitch = Math.clamp(diffPitch * aimSpeed, -aimSpeed, aimSpeed);

        mc.player.setYaw(currentYaw + stepYaw);
        mc.player.setPitch(Math.clamp(currentPitch + stepPitch, -90.0f, 90.0f));
        mc.player.headYaw = mc.player.getYaw();
    }

    public void aimAt(Vec3d target, float speed) {
        if (mc.player == null) return;
        Vec3d pos = mc.player.getEyePos();
        double dx = target.x - pos.x;
        double dy = target.y - pos.y;
        double dz = target.z - pos.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        targetPitch = Math.clamp(targetPitch, -90.0f, 90.0f);

        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        float diffYaw = net.minecraft.util.math.MathHelper.wrapDegrees(targetYaw - currentYaw);
        float diffPitch = targetPitch - currentPitch;

        float stepYaw = Math.clamp(diffYaw * speed, -speed, speed);
        float stepPitch = Math.clamp(diffPitch * speed, -speed, speed);

        mc.player.setYaw(currentYaw + stepYaw);
        mc.player.setPitch(Math.clamp(currentPitch + stepPitch, -90.0f, 90.0f));
        mc.player.headYaw = mc.player.getYaw();
    }

    public boolean inFov(Entity entity) {
        if (mc.player == null || entity == null) return true;
        if (lockFov >= 180.0f) return true;

        Vec3d pos = mc.player.getEyePos();
        Vec3d targetPos = entity.getPos().add(0, entity.getHeight() * 0.5, 0);
        Vec3d toTarget = targetPos.subtract(pos).normalize();

        float yawRad = (float) Math.toRadians(mc.player.getYaw());
        Vec3d forward = new Vec3d(-Math.sin(yawRad), 0, Math.cos(yawRad));

        double dot = forward.dotProduct(new Vec3d(toTarget.x, 0, toTarget.z).normalize());
        double angle = Math.toDegrees(Math.acos(Math.clamp(dot, -1.0, 1.0)));

        return angle <= lockFov / 2.0;
    }

    public boolean inRange(Entity entity) {
        if (mc.player == null || entity == null) return false;
        return mc.player.distanceTo(entity) <= lockRange;
    }

    public Entity findBestTarget(float range, float fov) {
        if (mc.world == null || mc.player == null) return null;

        List<Entity> entities = new ArrayList<>();
        mc.world.getEntities().forEach(entities::add);
        Entity best = null;
        double bestScore = Double.MAX_VALUE;

        for (Entity e : entities) {
            if (!(e instanceof LivingEntity) || e == mc.player || !e.isAlive()) continue;

            double dist = mc.player.distanceTo(e);
            if (dist > range) continue;

            Vec3d pos = mc.player.getEyePos();
            Vec3d targetPos = e.getPos().add(0, e.getHeight() * 0.5, 0);
            Vec3d toTarget = targetPos.subtract(pos).normalize();

            float yawRad = (float) Math.toRadians(mc.player.getYaw());
            Vec3d forward = new Vec3d(-Math.sin(yawRad), 0, Math.cos(yawRad));
            double dot = forward.dotProduct(new Vec3d(toTarget.x, 0, toTarget.z).normalize());
            double angle = Math.toDegrees(Math.acos(Math.clamp(dot, -1.0, 1.0)));

            if (angle > fov / 2.0) continue;

            double score = dist * 1.0 + angle * 0.1;
            if (score < bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }

    public boolean triggerReady() {
        if (lockedTarget == null || mc.player == null) return false;
        Vec3d pos = mc.player.getEyePos();
        Vec3d targetPos = lockedTarget.getPos().add(0, lockedTarget.getHeight() * 0.75, 0);
        double dx = targetPos.x - pos.x;
        double dy = targetPos.y - pos.y;
        double dz = targetPos.z - pos.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        Vec2f rotation = new Vec2f(
                (float) Math.toDegrees(Math.atan2(-dx, dz)),
                (float) -Math.toDegrees(Math.atan2(dy, horizontal))
        );

        float diffYaw = Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(rotation.x - mc.player.getYaw()));
        float diffPitch = Math.abs(rotation.y - mc.player.getPitch());

        return diffYaw < 5.0f && diffPitch < 5.0f;
    }
}
