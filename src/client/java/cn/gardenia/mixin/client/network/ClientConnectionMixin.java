package cn.gardenia.mixin.client.network;

import cn.gardenia.client.event.EventBus;
import cn.gardenia.client.event.events.network.GlobalPacketEvent;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin {
    @Shadow
    private static <T extends PacketListener> void handlePacket(Packet<T> packet, PacketListener listener) {
    }

    @Shadow
    private void sendInternal(Packet<?> packet, PacketCallbacks callbacks, boolean flush) {
    }

    @Redirect(
            method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;handlePacket(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;)V")
    )
    private void onHandlePacket(Packet<?> packet, PacketListener listener) {
        GlobalPacketEvent event = new GlobalPacketEvent(GlobalPacketEvent.Type.RECEIVE, packet);
        EventBus.INSTANCE.post(event);
        if (!event.isCancelled() && event.getPacket() != null) {
            handle(event.getPacket(), listener);
        }
    }

    @Redirect(
            method = "sendImmediately",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;sendInternal(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;Z)V")
    )
    private void onSendInternal(ClientConnection connection, Packet<?> packet, PacketCallbacks callbacks, boolean flush) {
        GlobalPacketEvent event = new GlobalPacketEvent(GlobalPacketEvent.Type.SEND, packet);
        EventBus.INSTANCE.post(event);
        if (!event.isCancelled() && event.getPacket() != null) {
            sendInternal(event.getPacket(), callbacks, flush);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void handle(Packet<?> packet, PacketListener listener) {
        handlePacket((Packet) packet, listener);
    }
}
