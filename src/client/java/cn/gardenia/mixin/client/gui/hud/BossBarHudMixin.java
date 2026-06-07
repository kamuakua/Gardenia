package cn.gardenia.mixin.client.gui.hud;

import cn.gardenia.client.event.EventBus;
import cn.gardenia.client.event.events.render.RenderBossBarEvent;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossBarHud.class)
public class BossBarHudMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(DrawContext context, CallbackInfo ci) {
        EventBus.INSTANCE.post(new RenderBossBarEvent(context));
    }
}
