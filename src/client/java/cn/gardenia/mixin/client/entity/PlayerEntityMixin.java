package cn.gardenia.mixin.client.entity;

import cn.gardenia.client.event.EventBus;
import cn.gardenia.client.event.events.entity.EntityRemoveEvent;
import cn.gardenia.client.module.combat.Velocity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
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

    @Inject(method = "clipAtLedge", at = @At("RETURN"), cancellable = true)
    private void onClipAtLedge(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(Velocity.handleStayingOnGroundSurface(cir.getReturnValue()));
    }
}
