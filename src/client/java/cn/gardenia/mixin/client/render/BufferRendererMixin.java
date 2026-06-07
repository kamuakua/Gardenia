package cn.gardenia.mixin.client.render;

import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BufferRenderer.class)
public class BufferRendererMixin {
    @Inject(method = "draw", at = @At("HEAD"))
    private static void onDraw(BuiltBuffer buffer, CallbackInfo ci) {
    }
}
