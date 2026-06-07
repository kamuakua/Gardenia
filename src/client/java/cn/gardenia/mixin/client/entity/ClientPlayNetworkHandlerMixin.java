package cn.gardenia.mixin.client.entity;

import cn.gardenia.client.event.EventBus;
import cn.gardenia.client.event.events.game.GameJoinEvent;
import cn.gardenia.client.event.events.network.PacketReceiveEvent;
import cn.gardenia.client.event.events.network.ServerSetPositionEvent;
import cn.gardenia.client.event.events.player.PlayerDeathEvent;
import cn.gardenia.client.event.events.player.PlayerExperienceEvent;
import cn.gardenia.client.event.events.player.PlayerFoodEvent;
import cn.gardenia.client.event.events.player.PlayerHealthEvent;
import cn.gardenia.client.event.events.player.PlayerRespawnEvent;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExperienceBarUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.LookAtS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onPlayerPositionLook", at = @At("HEAD"), cancellable = true)
    private void onPacketPlayerPositionLook(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
        if (postPacketReceive(packet)) ci.cancel();
    }

    @Redirect(
            method = "onPlayerPositionLook",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;send(Lnet/minecraft/network/packet/Packet;)V", ordinal = 1)
    )
    private void onServerSetPositionReply(ClientConnection connection, Packet<?> packet) {
        ServerSetPositionEvent event = new ServerSetPositionEvent(packet);
        EventBus.INSTANCE.post(event);
        if (event.getPacket() != null) {
            connection.send(event.getPacket());
        }
    }

    @Inject(method = "onLookAt", at = @At("HEAD"), cancellable = true)
    private void onPacketLookAt(LookAtS2CPacket packet, CallbackInfo ci) {
        if (postPacketReceive(packet)) ci.cancel();
    }

    @Inject(method = "onEntityVelocityUpdate", at = @At("HEAD"), cancellable = true)
    private void onPacketEntityVelocity(EntityVelocityUpdateS2CPacket packet, CallbackInfo ci) {
        if (postPacketReceive(packet)) ci.cancel();
    }

    @Inject(method = "onEntity", at = @At("HEAD"), cancellable = true)
    private void onPacketEntity(EntityS2CPacket packet, CallbackInfo ci) {
        if (postPacketReceive(packet)) ci.cancel();
    }

    @Inject(method = "onEntityPositionSync", at = @At("HEAD"), cancellable = true)
    private void onPacketEntityPositionSync(EntityPositionSyncS2CPacket packet, CallbackInfo ci) {
        if (postPacketReceive(packet)) ci.cancel();
    }

    @Inject(method = "onEntityPosition", at = @At("HEAD"), cancellable = true)
    private void onPacketEntityPosition(EntityPositionS2CPacket packet, CallbackInfo ci) {
        if (postPacketReceive(packet)) ci.cancel();
    }

    @Inject(method = "onPlayerList", at = @At("HEAD"), cancellable = true)
    private void onPacketPlayerList(PlayerListS2CPacket packet, CallbackInfo ci) {
        if (postPacketReceive(packet)) ci.cancel();
    }

    @Inject(method = "onEntitySpawn", at = @At("HEAD"), cancellable = true)
    private void onPacketEntitySpawn(EntitySpawnS2CPacket packet, CallbackInfo ci) {
        if (postPacketReceive(packet)) ci.cancel();
    }

    @Inject(method = "onEntitiesDestroy", at = @At("HEAD"), cancellable = true)
    private void onPacketEntitiesDestroy(EntitiesDestroyS2CPacket packet, CallbackInfo ci) {
        if (postPacketReceive(packet)) ci.cancel();
    }

    @Inject(method = "onEntityAnimation", at = @At("HEAD"), cancellable = true)
    private void onPacketEntityAnimation(EntityAnimationS2CPacket packet, CallbackInfo ci) {
        if (postPacketReceive(packet)) ci.cancel();
    }

    @Inject(method = "onPlayerRespawn", at = @At("HEAD"), cancellable = true)
    private void onPacketPlayerRespawn(PlayerRespawnS2CPacket packet, CallbackInfo ci) {
        if (postPacketReceive(packet)) ci.cancel();
    }

    @Inject(method = "onGameJoin", at = @At("TAIL"))
    private void onGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
        EventBus.INSTANCE.post(new GameJoinEvent("", false));
    }

    @Inject(method = "onPlayerRespawn", at = @At("TAIL"))
    private void onPlayerRespawn(PlayerRespawnS2CPacket packet, CallbackInfo ci) {
        EventBus.INSTANCE.post(new PlayerRespawnEvent());
    }

    @Inject(method = "onHealthUpdate", at = @At("TAIL"))
    private void onHealthUpdate(HealthUpdateS2CPacket packet, CallbackInfo ci) {
        EventBus.INSTANCE.post(new PlayerHealthEvent(packet.getHealth(), 20, packet.getSaturation()));
        EventBus.INSTANCE.post(new PlayerFoodEvent(packet.getFood(), packet.getSaturation()));
    }

    @Inject(method = "onExperienceBarUpdate", at = @At("TAIL"))
    private void onExperienceBarUpdate(ExperienceBarUpdateS2CPacket packet, CallbackInfo ci) {
        EventBus.INSTANCE.post(new PlayerExperienceEvent(
            packet.getExperienceLevel(),
            packet.getBarProgress(),
            packet.getExperience()
        ));
    }

    @Inject(method = "onDeathMessage", at = @At("HEAD"))
    private void onDeathMessage(DeathMessageS2CPacket packet, CallbackInfo ci) {
        EventBus.INSTANCE.post(new PlayerDeathEvent(null));
    }

    private boolean postPacketReceive(Packet<?> packet) {
        PacketReceiveEvent event = new PacketReceiveEvent(packet);
        EventBus.INSTANCE.post(event);
        return event.isCancelled();
    }
}
