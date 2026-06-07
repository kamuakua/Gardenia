package cn.gardenia.mixin.client.gui.hud;

import cn.gardenia.client.event.EventBus;
import cn.gardenia.client.event.events.render.RenderHudEvent;
import cn.gardenia.client.event.events.render.RenderOverlayEvent;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        EventBus.INSTANCE.post(new RenderHudEvent(context, tickCounter.getTickDelta(false)));
    }

    @Inject(method = "renderMiscOverlays", at = @At("HEAD"))
    private void onRenderOverlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        EventBus.INSTANCE.post(new RenderOverlayEvent(context, tickCounter.getTickDelta(false)));
    }
}
