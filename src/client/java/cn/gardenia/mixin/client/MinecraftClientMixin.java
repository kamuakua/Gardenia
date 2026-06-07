package cn.gardenia.mixin.client;

import cn.gardenia.client.event.EventBus;
import cn.gardenia.client.event.EventPhase;
import cn.gardenia.client.event.events.game.GameTickEvent;
import cn.gardenia.client.event.events.game.ScreenCloseEvent;
import cn.gardenia.client.event.events.game.ScreenOpenEvent;
import cn.gardenia.client.event.events.input.ClickEvent;
import cn.gardenia.client.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Overlay;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Shadow public Screen currentScreen;
    @Shadow private Overlay overlay;
    @Shadow boolean skipGameRender;
    @Shadow private boolean finishedLoading;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        EventBus.INSTANCE.post(new GameTickEvent(EventPhase.PRE));
        ModuleManager.INSTANCE.onTick();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickTail(CallbackInfo ci) {
        EventBus.INSTANCE.post(new GameTickEvent(EventPhase.POST));
    }

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        if (screen != null) {
            EventBus.INSTANCE.post(new ScreenOpenEvent(screen));
        } else {
            EventBus.INSTANCE.post(new ScreenCloseEvent());
        }
    }

    @Inject(
            method = "handleInputEvents",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;doItemUse()V"),
            cancellable = true
    )
    private void onClickInput(CallbackInfo ci) {
        ClickEvent event = new ClickEvent();
        EventBus.INSTANCE.post(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
