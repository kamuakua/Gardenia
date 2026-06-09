package cn.gardenia.mixin.client;

import cn.gardenia.client.event.EventBus;
import cn.gardenia.client.event.EventPhase;
import cn.gardenia.client.event.events.game.GameTickEvent;
import cn.gardenia.client.event.events.game.ScreenCloseEvent;
import cn.gardenia.client.event.events.game.ScreenOpenEvent;
import cn.gardenia.client.event.events.input.AttackClickEvent;
import cn.gardenia.client.event.events.input.ClickEvent;
import cn.gardenia.client.config.ConfigManager;
import cn.gardenia.client.module.ModuleManager;
import cn.gardenia.client.module.move.NoSlowModule;
import cn.gardenia.client.tool.player.GardeniaTickTimer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Overlay;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Shadow public Screen currentScreen;
    @Shadow private Overlay overlay;
    @Shadow boolean skipGameRender;
    @Shadow private boolean finishedLoading;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        GardeniaTickTimer.resetTransientMultiplier();
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

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void onAttackClick(CallbackInfoReturnable<Boolean> cir) {
        AttackClickEvent event = new AttackClickEvent();
        EventBus.INSTANCE.post(event);
        if (event.isCancelled()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "doItemUse", at = @At("HEAD"))
    private void onDoItemUseHead(CallbackInfo ci) {
        NoSlowModule.onStartUseItemPre((MinecraftClient) (Object) this);
    }

    @Inject(method = "doItemUse", at = @At("TAIL"))
    private void onDoItemUseTail(CallbackInfo ci) {
        NoSlowModule.onStartUseItemPost((MinecraftClient) (Object) this);
    }

    @Inject(method = "stop", at = @At("HEAD"))
    private void onStop(CallbackInfo ci) {
        ConfigManager.INSTANCE.save();
    }
}
