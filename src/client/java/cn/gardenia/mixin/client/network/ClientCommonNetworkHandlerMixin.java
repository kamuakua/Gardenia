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
            boolean changesPosition = packet.changesPosition()
                    || mc.player != null && (packet.getX(mc.player.getX()) != motionEvent.getX()
                    || packet.getY(mc.player.getY()) != motionEvent.getY()
                    || packet.getZ(mc.player.getZ()) != motionEvent.getZ());
            boolean changesLook = packet.changesLook()
                    || mc.player != null && (packet.getYaw(mc.player.getYaw()) != motionEvent.getYaw()
                    || packet.getPitch(mc.player.getPitch()) != motionEvent.getPitch());
            return buildMovePacket(
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
