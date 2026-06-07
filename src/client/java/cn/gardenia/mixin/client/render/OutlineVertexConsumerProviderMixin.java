package cn.gardenia.mixin.client.render;

import net.minecraft.client.render.OutlineVertexConsumerProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OutlineVertexConsumerProvider.class)
public class OutlineVertexConsumerProviderMixin {
    @Inject(method = "draw", at = @At("HEAD"))
    private void onDraw(CallbackInfo ci) {
    }
}
