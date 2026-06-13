package cn.gardenia.mixin.client.network;

import cn.gardenia.client.event.EventBus;
import cn.gardenia.client.event.events.game.GameLeaveEvent;
import cn.gardenia.client.event.events.movement.PlayerMotionContext;
import cn.gardenia.client.event.events.movement.PlayerMotionEvent;
import cn.gardenia.client.event.events.network.PacketReceiveEvent;
import cn.gardenia.client.event.events.network.PacketSendEvent;
import cn.gardenia.client.tool.ToolManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonNetworkHandler.class)
public class ClientCommonNetworkHandlerMixin {
    @Shadow protected ClientConnection connection;
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    // 服务端旋转空间的状态机：跟踪最近一次实际发出的 yaw/pitch（服务端旋转），
    // 用来在服务端旋转空间里重新判定 changesLook —— 复刻 Naven sendPosition 的 flag2 = (yaw-yRotLast)!=0。
    // vanilla 自身的 changesLook 跑在真实相机空间，直行搭路（不动鼠标）时恒为 false，
    // 导致 scaffold 的服务端旋转永远不进姿态包 → Simulation/place 旋转缺失。
    private float gardenia$lastServerYaw = Float.NaN;
    private float gardenia$lastServerPitch = Float.NaN;

    @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
    private void onPacketSend(Packet<?> packet, CallbackInfo ci) {
        PacketSendEvent event = new PacketSendEvent(packet);
        EventBus.INSTANCE.post(event);
        if (event.isCancelled()) {
            ci.cancel();
            return;
        }

        if (packet instanceof PlayerMoveC2SPacket movePacket) {
            PlayerMoveC2SPacket rewritten = rewriteMovePacket(movePacket);
            if (rewritten != movePacket) {
                if (rewritten.changesLook() && mc.player != null) {
                    ToolManager.INSTANCE.ROTATION.confirmSentRotation(
                            rewritten.getYaw(mc.player.getYaw()),
                            rewritten.getPitch(mc.player.getPitch())
                    );
                }
                connection.send(rewritten);
                ci.cancel();
            }
        }
    }

    @Inject(method = "onPing", at = @At("HEAD"), cancellable = true)
    private void onPacketPing(CommonPingS2CPacket packet, CallbackInfo ci) {
        if (postPacketReceive(packet)) ci.cancel();
    }

    @Inject(method = "onDisconnect", at = @At("HEAD"), cancellable = true)
    private void onPacketDisconnect(DisconnectS2CPacket packet, CallbackInfo ci) {
        if (postPacketReceive(packet)) ci.cancel();
    }

    @Inject(method = "onDisconnected", at = @At("HEAD"))
    private void onDisconnected(DisconnectionInfo info, CallbackInfo ci) {
        EventBus.INSTANCE.post(new GameLeaveEvent());
    }

    private boolean postPacketReceive(Packet<?> packet) {
        PacketReceiveEvent event = new PacketReceiveEvent(packet);
        EventBus.INSTANCE.post(event);
        return event.isCancelled();
    }

    private PlayerMoveC2SPacket rewriteMovePacket(PlayerMoveC2SPacket packet) {
        PlayerMotionEvent motionEvent = PlayerMotionContext.getActiveEvent();
        if (motionEvent != null) {
            // changesPosition 沿用 vanilla 决定（位置始终是真实坐标，状态机一致，不能改）。
            boolean changesPosition = packet.changesPosition();
            // changesLook 在服务端旋转空间重新判定：与上次实际发出的服务端 yaw/pitch 比较。
            // 这样直行搭路时只要 scaffold 旋转每 tick 在变，就一定发出 look，且包内旋转与判定一致 —— 对齐 Naven。
            boolean lookChanged = Float.isNaN(gardenia$lastServerYaw)
                    || motionEvent.getYaw() != gardenia$lastServerYaw
                    || motionEvent.getPitch() != gardenia$lastServerPitch;
            boolean changesLook = packet.changesLook() || lookChanged;

            PlayerMoveC2SPacket rewritten = buildMovePacket(
                    changesPosition,
                    changesLook,
                    motionEvent.getX(),
                    motionEvent.getY(),
                    motionEvent.getZ(),
                    motionEvent.getYaw(),
                    motionEvent.getPitch(),
                    motionEvent.isOnGround(),
                    packet.horizontalCollision()
            );
            if (changesLook) {
                gardenia$lastServerYaw = motionEvent.getYaw();
                gardenia$lastServerPitch = motionEvent.getPitch();
            }
            return rewritten;
        }
        if (ToolManager.INSTANCE.ROTATION.isServerRotationActive()) {
            return ToolManager.INSTANCE.ROTATION.applyServerRotation(packet);
        }
        return packet;
    }

    private PlayerMoveC2SPacket buildMovePacket(boolean changesPosition, boolean changesLook, double x, double y, double z, float yaw, float pitch, boolean onGround, boolean horizontalCollision) {
        if (changesPosition && changesLook) {
            return new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, onGround, horizontalCollision);
        }
        if (changesPosition) {
            return new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround, horizontalCollision);
        }
        if (changesLook) {
            return new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, onGround, horizontalCollision);
        }
        return new PlayerMoveC2SPacket.OnGroundOnly(onGround, horizontalCollision);
    }
}
