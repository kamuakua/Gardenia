package cn.gardenia.client.tool.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.util.Hand;

public class AttackTool {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private boolean attacking;
    private int attackCooldown;
    private int currentTick;

    public void init() {}

    public void attack(Entity target) {
        if (mc.player == null || mc.interactionManager == null) return;
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        currentTick = 0;
        attacking = true;
    }

    public void attackSilent(Entity target) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
        currentTick = 0;
        attacking = true;
    }

    public void attackNoSwing(Entity target) {
        if (mc.player == null || mc.interactionManager == null) return;
        mc.interactionManager.attackEntity(mc.player, target);
        currentTick = 0;
        attacking = true;
    }

    public boolean canAttack(Entity target) {
        if (mc.player == null || target == null) return false;
        if (!target.isAlive()) return false;
        if (target == mc.player) return false;
        if (target instanceof PlayerEntity) {
            if (((PlayerEntity) target).isDead()) return false;
        }
        return mc.player.distanceTo(target) <= getReach();
    }

    public boolean canCrit() {
        if (mc.player == null) return false;
        return mc.player.isOnGround()
                && !mc.player.isInLava()
                && !mc.player.isTouchingWater()
                && !mc.player.hasVehicle()
                && mc.player.fallDistance > 0.0f
                && mc.player.fallDistance < 1.0f;
    }

    public boolean canSweep() {
        if (mc.player == null) return false;
        var stack = mc.player.getMainHandStack();
        return stack.getItem() instanceof net.minecraft.item.SwordItem;
    }

    public void criticalHit(Entity target) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        mc.player.setVelocity(mc.player.getVelocity().x, 0.2, mc.player.getVelocity().z);
        mc.player.fallDistance = 0.2f;
        mc.player.isOnGround();
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                mc.player.getX(), mc.player.getY() + 0.0625, mc.player.getZ(), false, false
        ));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                mc.player.getX(), mc.player.getY(), mc.player.getZ(), false, false
        ));
        attack(target);
    }

    public void criticalHitSilent(Entity target) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                mc.player.getX(), mc.player.getY() + 0.0625, mc.player.getZ(), false, false
        ));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                mc.player.getX(), mc.player.getY(), mc.player.getZ(), false, false
        ));
        attackSilent(target);
    }

    public void swingHand() {
        if (mc.player == null) return;
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    public void swingHand(Hand hand) {
        if (mc.player == null) return;
        mc.player.swingHand(hand);
    }

    public float getCooldownProgress() {
        if (mc.player == null) return 1.0f;
        return mc.player.getAttackCooldownProgress(0);
    }

    public float getCooldownProgressPerTick() {
        if (mc.player == null) return 1.0f;
        return mc.player.getAttackCooldownProgressPerTick();
    }

    public boolean isCooldownComplete() {
        return getCooldownProgress() >= 0.9f;
    }

    public void resetCooldown() {
        if (mc.player == null) return;
        mc.player.resetLastAttackedTicks();
    }

    public boolean isAttacking() {
        return attacking && currentTick < 5;
    }

    public void tick() {
        if (attacking) currentTick++;
        if (currentTick > 10) attacking = false;
        if (attackCooldown > 0) attackCooldown--;
    }

    public double getReach() {
        if (mc.interactionManager == null) return 4.5;
        if (mc.player == null) return 4.5;
        if (mc.interactionManager.getCurrentGameMode() == net.minecraft.world.GameMode.CREATIVE) {
            return 6.0;
        }
        return 4.5;
    }

    public double getBlockReach() {
        if (mc.interactionManager == null) return 5.0;
        if (mc.interactionManager.getCurrentGameMode() == net.minecraft.world.GameMode.CREATIVE) {
            return 6.0;
        }
        return 5.0;
    }

    public int getLastAttackedTicks() {
        return 0;
    }

    public Entity getLastAttackedEntity() {
        return null;
    }
}
