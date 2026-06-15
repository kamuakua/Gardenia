package cn.gardenia.mixin.client.entity;

import cn.gardenia.client.event.EventBus;
import cn.gardenia.client.event.events.render.UpdateFovEvent;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {
    @Inject(method = "getFovMultiplier", at = @At("RETURN"), cancellable = true)
    private void hookFov(boolean firstPerson, float fovEffectScale, CallbackInfoReturnable<Float> cir) {
        UpdateFovEvent event = new UpdateFovEvent(cir.getReturnValue());
        EventBus.INSTANCE.post(event);
        cir.setReturnValue(event.getFov());
    }
}
