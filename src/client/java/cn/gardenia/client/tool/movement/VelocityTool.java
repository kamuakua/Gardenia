package cn.gardenia.client.tool.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

public class VelocityTool {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public void init() {}

    public void set(Vec3d velocity) {
        if (mc.player == null) return;
        mc.player.setVelocity(velocity);
    }

    public void set(double x, double y, double z) {
        if (mc.player == null) return;
        mc.player.setVelocity(x, y, z);
    }

    public void add(Vec3d velocity) {
        if (mc.player == null) return;
        mc.player.addVelocity(velocity.x, velocity.y, velocity.z);
    }

    public void add(double x, double y, double z) {
        if (mc.player == null) return;
        mc.player.addVelocity(x, y, z);
    }

    public void multiply(double factor) {
        if (mc.player == null) return;
        Vec3d vel = mc.player.getVelocity();
        mc.player.setVelocity(vel.x * factor, vel.y * factor, vel.z * factor);
    }

    public void multiplyHorizontal(double factor) {
        if (mc.player == null) return;
        Vec3d vel = mc.player.getVelocity();
        mc.player.setVelocity(vel.x * factor, vel.y, vel.z * factor);
    }

    public void multiplyVertical(double factor) {
        if (mc.player == null) return;
        Vec3d vel = mc.player.getVelocity();
        mc.player.setVelocity(vel.x, vel.y * factor, vel.z);
    }

    public void setVertical(double y) {
        if (mc.player == null) return;
        Vec3d vel = mc.player.getVelocity();
        mc.player.setVelocity(vel.x, y, vel.z);
    }

    public void setHorizontal(double speed) {
        if (mc.player == null) return;
        Vec3d vel = mc.player.getVelocity();
        double currentH = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        if (currentH == 0) return;
        double scale = speed / currentH;
        mc.player.setVelocity(vel.x * scale, vel.y, vel.z * scale);
    }

    public void strafe() {
        if (mc.player == null) return;
        Vec3d vel = mc.player.getVelocity();
        float yaw = mc.player.getYaw();
        float forward = mc.player.input.movementForward;
        float sideways = mc.player.input.movementSideways;

        if (forward == 0 && sideways == 0) return;

        double rad = Math.toRadians(yaw);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);

        double speed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        double moveX = forward * cos - sideways * sin;
        double moveZ = forward * sin + sideways * cos;

        mc.player.setVelocity(moveX * speed, vel.y, moveZ * speed);
    }

    public void airStrafe() {
        if (mc.player == null) return;
        if (mc.player.isOnGround()) return;

        Vec3d vel = mc.player.getVelocity();
        float yaw = mc.player.getYaw();
        float forward = mc.player.input.movementForward;
        float sideways = mc.player.input.movementSideways;

        if (forward == 0 && sideways == 0) return;

        double speed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);

        double rad = Math.toRadians(yaw);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);
        double moveX = forward * cos - sideways * sin;
        double moveZ = forward * sin + sideways * cos;

        double maxSpeed = 0.026;
        double newSpeed = speed;
        if (speed < maxSpeed) {
            newSpeed = Math.min(speed + 0.02, maxSpeed);
        }

        mc.player.setVelocity(moveX * newSpeed, vel.y, moveZ * newSpeed);
    }

    public void reset() {
        if (mc.player == null) return;
        mc.player.setVelocity(0, 0, 0);
    }

    public void resetHorizontal() {
        if (mc.player == null) return;
        Vec3d vel = mc.player.getVelocity();
        mc.player.setVelocity(0, vel.y, 0);
    }

    public Vec3d get() {
        if (mc.player == null) return Vec3d.ZERO;
        return mc.player.getVelocity();
    }

    public double horizontalSpeed() {
        if (mc.player == null) return 0;
        Vec3d vel = mc.player.getVelocity();
        return Math.sqrt(vel.x * vel.x + vel.z * vel.z);
    }

    public double verticalSpeed() {
        if (mc.player == null) return 0;
        return mc.player.getVelocity().y;
    }

    public double totalSpeed() {
        if (mc.player == null) return 0;
        Vec3d vel = mc.player.getVelocity();
        return Math.sqrt(vel.x * vel.x + vel.y * vel.y + vel.z * vel.z);
    }

    public void glide(double factor) {
        if (mc.player == null) return;
        Vec3d vel = mc.player.getVelocity();
        if (vel.y < 0) {
            mc.player.setVelocity(vel.x, vel.y * factor, vel.z);
        }
    }

    public void antiKnockback(double horizontal, double vertical) {
        if (mc.player == null) return;
        Vec3d vel = mc.player.getVelocity();
        if (vel.x != 0 || vel.z != 0) {
            mc.player.setVelocity(vel.x * horizontal, vel.y * vertical, vel.z * horizontal);
        }
    }
}
