package cn.gardenia.mixin.client.gui.screen;

import cn.gardenia.client.event.EventBus;
import cn.gardenia.client.event.events.render.RenderScreenEvent;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderScreen(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        EventBus.INSTANCE.post(new RenderScreenEvent(screen, context, mouseX, mouseY, delta));
    }
}
