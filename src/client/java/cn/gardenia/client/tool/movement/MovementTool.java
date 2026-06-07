package cn.gardenia.client.tool.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

public class MovementTool {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public void init() {}

    public void setSprint(boolean sprint) {
        if (mc.player == null) return;
        mc.player.setSprinting(sprint);
        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(
                mc.player,
                sprint ? ClientCommandC2SPacket.Mode.START_SPRINTING
                        : ClientCommandC2SPacket.Mode.STOP_SPRINTING
        ));
    }

    public void setSneak(boolean sneak) {
        if (mc.player == null) return;
        mc.player.setSneaking(sneak);
        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(
                mc.player,
                sneak ? ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY
                        : ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY
        ));
    }

    public void jump() {
        if (mc.player == null) return;
        mc.player.jump();
    }

    public void jumpSilent() {
        if (mc.player == null || !mc.player.isOnGround()) return;
        mc.player.setVelocity(mc.player.getVelocity().x, 0.42f, mc.player.getVelocity().z);
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                mc.player.getX(), mc.player.getY() + 0.42, mc.player.getZ(), false, false
        ));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                mc.player.getX(), mc.player.getY() + 0.42, mc.player.getZ(), true, false
        ));
    }

    public void strafe(float forward, float sideways) {
        if (mc.player == null) return;
        mc.player.input.movementForward = forward;
        mc.player.input.movementSideways = sideways;
    }

    public void strafeWithSpeed(float speed) {
        if (mc.player == null) return;
        Vec3d vel = mc.player.getVelocity();
        float yaw = mc.player.getYaw();
        float forward = mc.player.input.movementForward;
        float sideways = mc.player.input.movementSideways;

        if (forward == 0 && sideways == 0) return;

        double rad = Math.toRadians(yaw);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);

        double moveX = forward * cos - sideways * sin;
        double moveZ = forward * sin + sideways * cos;

        mc.player.setVelocity(moveX * speed, vel.y, moveZ * speed);
    }

    public void stop() {
        if (mc.player == null) return;
        mc.player.input.movementForward = 0;
        mc.player.input.movementSideways = 0;
        mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
    }

    public void stopHorizontal() {
        if (mc.player == null) return;
        mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
    }

    public void setForward(float forward) {
        if (mc.player == null) return;
        mc.player.input.movementForward = forward;
    }

    public void setSideways(float sideways) {
        if (mc.player == null) return;
        mc.player.input.movementSideways = sideways;
    }

    public void setJumping(boolean jumping) {
        if (mc.player == null) return;
        if (jumping) mc.player.jump();
    }

    public void setSneaking(boolean sneaking) {
        if (mc.player == null) return;
        mc.options.sneakKey.setPressed(sneaking);
    }

    public float speed() {
        if (mc.player == null) return 0;
        Vec3d vel = mc.player.getVelocity();
        return (float)Math.sqrt(vel.x * vel.x + vel.z * vel.z);
    }

    public float baseSpeed() {
        if (mc.player == null) return 0.0f;
        float base = 0.1f;
        if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            int amp = mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier() + 1;
            base *= 1.0 + 0.2 * amp;
        }
        if (mc.player.hasStatusEffect(StatusEffects.SLOWNESS)) {
            int amp = mc.player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() + 1;
            base *= 1.0 - 0.15 * amp;
        }
        return base;
    }

    public void setPos(Vec3d pos) {
        if (mc.player == null) return;
        mc.player.setPosition(pos);
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                pos.x, pos.y, pos.z, mc.player.isOnGround(), mc.player.horizontalCollision
        ));
    }

    public void sendMovePacket(Vec3d pos, float yaw, float pitch, boolean onGround) {
        if (mc.getNetworkHandler() == null) return;
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(pos.x, pos.y, pos.z, yaw, pitch, onGround, false));
    }

    public void sendLookPacket(float yaw, float pitch, boolean onGround) {
        if (mc.getNetworkHandler() == null) return;
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, onGround, false));
    }

    public void sendGroundPacket(boolean onGround) {
        if (mc.getNetworkHandler() == null) return;
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(onGround, false));
    }
}
