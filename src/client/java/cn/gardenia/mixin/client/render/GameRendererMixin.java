package cn.gardenia.mixin.client.render;

import cn.gardenia.client.event.EventBus;
import cn.gardenia.client.event.events.render.Render3DEvent;
import cn.gardenia.client.module.FullBrightModule;
import cn.gardenia.client.module.ModuleManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "getNightVisionStrength", at = @At("HEAD"), cancellable = true)
    private static void onGetNightVisionStrength(LivingEntity entity, float tickDelta, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Float> cir) {
        FullBrightModule module = ModuleManager.INSTANCE.getByClass(FullBrightModule.class);
        if (module != null && module.isEnabled()) {
            cir.setReturnValue(module.getBrightness());
        }
    }

    @Inject(method = "renderWorld", at = @At("TAIL"))
    private void onRenderWorld(RenderTickCounter tickCounter, CallbackInfo ci) {
        EventBus.INSTANCE.post(new Render3DEvent(tickCounter.getTickDelta(false), null));
    }
}
