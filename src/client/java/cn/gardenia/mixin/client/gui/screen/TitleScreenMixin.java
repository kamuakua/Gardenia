package cn.gardenia.mixin.client.gui.screen;

import cn.gardenia.client.gui.menu.CustomMainMenu;
import cn.gardenia.client.gui.screen.GardeniaSplashScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    private static boolean firstLoad = true;

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void onInit(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            if (firstLoad) {
                firstLoad = false;
                client.setScreen(new GardeniaSplashScreen());
            } else {
                client.setScreen(new CustomMainMenu());
            }
            ci.cancel();
        }
    }
}
