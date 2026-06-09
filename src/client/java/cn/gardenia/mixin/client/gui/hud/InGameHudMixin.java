package cn.gardenia.mixin.client.gui.hud;

import cn.gardenia.client.event.EventBus;
import cn.gardenia.client.event.events.render.RenderHudEvent;
import cn.gardenia.client.event.events.render.RenderOverlayEvent;
import cn.gardenia.client.event.events.render.RenderSkiaEvent;
import cn.gardenia.client.tool.render.skia.SkiaContext;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.MinecraftClient;
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

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderSkia(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) {
            return;
        }

        float scaleFactor = (float) client.getWindow().getScaleFactor();
        SkiaContext.draw(canvas -> {
            canvas.save();
            canvas.scale(scaleFactor, scaleFactor);
            EventBus.INSTANCE.post(new RenderSkiaEvent(
                    canvas,
                    tickCounter.getTickDelta(false),
                    client.getWindow().getScaledWidth(),
                    client.getWindow().getScaledHeight(),
                    scaleFactor
            ));
            canvas.restore();
        });
    }

    @Inject(method = "renderMiscOverlays", at = @At("HEAD"))
    private void onRenderOverlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        EventBus.INSTANCE.post(new RenderOverlayEvent(context, tickCounter.getTickDelta(false)));
    }
}
