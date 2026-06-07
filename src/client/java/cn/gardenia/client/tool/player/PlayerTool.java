package cn.gardenia.client.tool.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.*;
import net.minecraft.util.math.*;

public class PlayerTool {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public void init() {}

    public ClientPlayerEntity get() {
        return mc.player;
    }

    public boolean exists() {
        return mc.player != null;
    }

    public Vec3d pos() {
        if (mc.player == null) return Vec3d.ZERO;
        return mc.player.getPos();
    }

    public Vec3d prevPos() {
        if (mc.player == null) return Vec3d.ZERO;
        return new Vec3d(mc.player.prevX, mc.player.prevY, mc.player.prevZ);
    }

    public Vec3d motion() {
        if (mc.player == null) return Vec3d.ZERO;
        return mc.player.getVelocity();
    }

    public Vec2f rotation() {
        if (mc.player == null) return new Vec2f(0, 0);
        return new Vec2f(mc.player.getYaw(), mc.player.getPitch());
    }

    public float yaw() {
        if (mc.player == null) return 0;
        return mc.player.getYaw();
    }

    public float pitch() {
        if (mc.player == null) return 0;
        return mc.player.getPitch();
    }

    public float bodyYaw() {
        if (mc.player == null) return 0;
        return mc.player.bodyYaw;
    }

    public float headYaw() {
        if (mc.player == null) return 0;
        return mc.player.headYaw;
    }

    public float health() {
        if (mc.player == null) return 0;
        return mc.player.getHealth();
    }

    public float maxHealth() {
        if (mc.player == null) return 0;
        return mc.player.getMaxHealth();
    }

    public float absorption() {
        if (mc.player == null) return 0;
        return mc.player.getAbsorptionAmount();
    }

    public float effectiveHealth() {
        return health() + absorption();
    }

    public float healthPercent() {
        if (mc.player == null) return 0;
        return mc.player.getHealth() / mc.player.getMaxHealth();
    }

    public int hunger() {
        if (mc.player == null) return 0;
        return mc.player.getHungerManager().getFoodLevel();
    }

    public float saturation() {
        if (mc.player == null) return 0;
        return mc.player.getHungerManager().getSaturationLevel();
    }

    public float xpProgress() {
        if (mc.player == null) return 0;
        return mc.player.experienceProgress;
    }

    public int xpLevel() {
        if (mc.player == null) return 0;
        return mc.player.experienceLevel;
    }

    public int totalXp() {
        if (mc.player == null) return 0;
        return mc.player.totalExperience;
    }

    public float fallDistance() {
        if (mc.player == null) return 0;
        return mc.player.fallDistance;
    }

    public boolean onGround() {
        return mc.player != null && mc.player.isOnGround();
    }

    public boolean inWater() {
        return mc.player != null && mc.player.isTouchingWater();
    }

    public boolean inLava() {
        return mc.player != null && mc.player.isInLava();
    }

    public boolean inFluid() {
        return mc.player != null && mc.player.isSubmergedInWater();
    }

    public boolean isBurning() {
        return mc.player != null && mc.player.isOnFire();
    }

    public boolean isAlive() {
        return mc.player != null && mc.player.isAlive();
    }

    public boolean isSprinting() {
        return mc.player != null && mc.player.isSprinting();
    }

    public boolean isSneaking() {
        return mc.player != null && mc.player.isSneaking();
    }

    public boolean isMoving() {
        if (mc.player == null) return false;
        return mc.player.forwardSpeed != 0 || mc.player.sidewaysSpeed != 0;
    }

    public boolean isJumping() {
        if (mc.player == null) return false;
        return mc.options.jumpKey.isPressed();
    }

    public float forwardSpeed() {
        if (mc.player == null) return 0;
        return mc.player.forwardSpeed;
    }

    public float sidewaysSpeed() {
        if (mc.player == null) return 0;
        return mc.player.sidewaysSpeed;
    }

    public boolean hasVehicle() {
        return mc.player != null && mc.player.hasVehicle();
    }

    public Entity getVehicle() {
        if (mc.player == null) return null;
        return mc.player.getVehicle();
    }

    public boolean isRiding() {
        return hasVehicle();
    }

    public int airTicks() {
        if (mc.player == null) return 0;
        return mc.player.getAir();
    }

    public double distanceTo(Vec3d target) {
        if (mc.player == null) return Double.MAX_VALUE;
        return mc.player.getPos().distanceTo(target);
    }

    public double distanceTo(Entity entity) {
        if (mc.player == null || entity == null) return Double.MAX_VALUE;
        return mc.player.distanceTo(entity);
    }

    public double horizontalDistanceTo(Entity entity) {
        if (mc.player == null || entity == null) return Double.MAX_VALUE;
        Vec3d dp = mc.player.getPos().subtract(entity.getPos());
        return Math.sqrt(dp.x * dp.x + dp.z * dp.z);
    }

    public int heldSlot() {
        if (mc.player == null) return 0;
        return mc.player.getInventory().selectedSlot;
    }

    public float getCooldown() {
        if (mc.player == null) return 1.0f;
        return mc.player.getAttackCooldownProgress(0);
    }

    public boolean isHoldingTool() {
        if (mc.player == null) return false;
        var stack = mc.player.getMainHandStack();
        return stack.getItem() instanceof net.minecraft.item.MiningToolItem
                || stack.getItem() instanceof net.minecraft.item.SwordItem;
    }

    public int getItemUseTicks() {
        if (mc.player == null) return 0;
        return mc.player.getItemUseTime();
    }

    public boolean isUsingItem() {
        return mc.player != null && mc.player.isUsingItem();
    }

    public Vec3d forwardVec() {
        if (mc.player == null) return Vec3d.ZERO;
        float yawRad = (float)Math.toRadians(mc.player.getYaw());
        return new Vec3d(-Math.sin(yawRad), 0, Math.cos(yawRad));
    }

    public Vec3d rightVec() {
        if (mc.player == null) return Vec3d.ZERO;
        float yawRad = (float)Math.toRadians(mc.player.getYaw());
        return new Vec3d(Math.cos(yawRad), 0, Math.sin(yawRad));
    }
}
