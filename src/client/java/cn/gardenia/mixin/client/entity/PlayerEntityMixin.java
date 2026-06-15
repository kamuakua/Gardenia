package cn.gardenia.mixin.client.entity;

import cn.gardenia.client.event.EventBus;
import cn.gardenia.client.event.events.attack.AttackSlowdownEvent;
import cn.gardenia.client.event.events.entity.EntityRemoveEvent;
import cn.gardenia.client.event.events.movement.StayingOnGroundSurfaceEvent;
import cn.gardenia.client.module.combat.Velocity;
import cn.gardenia.client.tool.ToolManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Inject(method = "attack", at = @At("HEAD"))
    private void onAttackPre(Entity target, CallbackInfo ci) {
        EventBus.INSTANCE.post(new EntityRemoveEvent(false, target));
    }

    @Inject(method = "attack", at = @At("TAIL"))
    private void onAttackPost(Entity target, CallbackInfo ci) {
        EventBus.INSTANCE.post(new EntityRemoveEvent(true, target));
    }

    @Redirect(
            method = "attack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getYaw()F")
    )
    private float onAttackGetYaw(PlayerEntity instance) {
        if (instance == net.minecraft.client.MinecraftClient.getInstance().player && ToolManager.INSTANCE.ROTATION.isServerRotationActive()) {
            return ToolManager.INSTANCE.ROTATION.getServerRotation().x;
        }
        return instance.getYaw();
    }

    @Redirect(
            method = "attack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V")
    )
    private void onAttackSetVelocity(PlayerEntity instance, Vec3d velocity) {
        AttackSlowdownEvent event = new AttackSlowdownEvent(0.6D, instance.isSprinting());
        EventBus.INSTANCE.post(event);
        Vec3d currentVelocity = instance.getVelocity();
        double reduce = event.getReduce() == null ? 0.6D : event.getReduce();
        instance.setVelocity(currentVelocity.x * reduce, velocity.y, currentVelocity.z * reduce);
    }

    @Redirect(
            method = "attack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setSprinting(Z)V")
    )
    private void onAttackSetSprinting(PlayerEntity instance, boolean sprinting) {
        AttackSlowdownEvent event = new AttackSlowdownEvent(0.6D, sprinting);
        EventBus.INSTANCE.post(event);
        instance.setSprinting(event.isSprint());
    }

    @Inject(method = "clipAtLedge", at = @At("RETURN"), cancellable = true)
    private void onClipAtLedge(CallbackInfoReturnable<Boolean> cir) {
        StayingOnGroundSurfaceEvent event = new StayingOnGroundSurfaceEvent(cir.getReturnValue());
        EventBus.INSTANCE.post(event);
        cir.setReturnValue(Velocity.handleStayingOnGroundSurface(event.shouldStay()));
    }
}
