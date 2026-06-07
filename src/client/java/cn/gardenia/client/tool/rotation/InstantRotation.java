package cn.gardenia.client.tool.rotation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class InstantRotation {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public void init() {}

    public void look(float yaw, float pitch) {
        if (mc.player == null) return;
        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
        mc.player.headYaw = yaw;
        mc.player.bodyYaw = yaw;
    }

    public void look(Vec2f rotation) {
        look(rotation.x, rotation.y);
    }

    public void lookPacket(float yaw, float pitch) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                yaw, pitch, mc.player.isOnGround(), mc.player.horizontalCollision
        ));
    }

    public void lookSilent(float yaw, float pitch) {
        if (mc.player == null) return;
        ToolManagerAccess.setServerRotation(yaw, pitch);
    }

    public void lookSilent(Vec2f rotation) {
        lookSilent(rotation.x, rotation.y);
    }

    public void lookSilentPacket(float yaw, float pitch) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        ToolManagerAccess.setServerRotation(yaw, pitch);
    }

    public void lookSilentPacket(Vec2f rotation) {
        lookSilentPacket(rotation.x, rotation.y);
    }

    public void applyHead(float yaw) {
        if (mc.player == null) return;
        mc.player.headYaw = yaw;
    }

    public void applyBody(float yaw) {
        if (mc.player == null) return;
        mc.player.bodyYaw = yaw;
    }

    public void lookAt(Vec3d target) {
        if (mc.player == null) return;
        Vec3d pos = mc.player.getEyePos();
        double dx = target.x - pos.x;
        double dy = target.y - pos.y;
        double dz = target.z - pos.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        look(yaw, Math.clamp(pitch, -90.0f, 90.0f));
    }

    public void lookAtSilent(Vec3d target) {
        if (mc.player == null) return;
        Vec3d pos = mc.player.getEyePos();
        double dx = target.x - pos.x;
        double dy = target.y - pos.y;
        double dz = target.z - pos.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        lookSilent(yaw, Math.clamp(pitch, -90.0f, 90.0f));
    }

    public void lookAt(net.minecraft.entity.Entity target) {
        if (target == null) return;
        lookAt(target.getPos().add(0, target.getHeight() * 0.75, 0));
    }

    public void lookAt(net.minecraft.entity.Entity target, Vec3d offset) {
        if (target == null) return;
        lookAt(target.getPos().add(offset));
    }

    public void lookAtPacket(Vec3d target) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        Vec3d pos = mc.player.getEyePos();
        double dx = target.x - pos.x;
        double dy = target.y - pos.y;
        double dz = target.z - pos.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        lookPacket(yaw, Math.clamp(pitch, -90.0f, 90.0f));
    }

    public void lookAtBlock(net.minecraft.util.math.BlockPos pos) {
        lookAt(Vec3d.ofCenter(pos));
    }

    public void lookAtBlock(net.minecraft.util.math.BlockPos pos, net.minecraft.util.math.Direction side) {
        Vec3d target = Vec3d.ofCenter(pos).add(
                side.getOffsetX() * 0.5,
                side.getOffsetY() * 0.5,
                side.getOffsetZ() * 0.5
        );
        lookAt(target);
    }

    private static final class ToolManagerAccess {
        private static void setServerRotation(float yaw, float pitch) {
            cn.gardenia.client.tool.ToolManager.INSTANCE.ROTATION.setServerRotation(new Vec2f(yaw, pitch));
        }
    }
}
