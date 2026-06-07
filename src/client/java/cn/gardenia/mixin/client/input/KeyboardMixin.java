package cn.gardenia.mixin.client.input;

import cn.gardenia.client.command.CommandManager;
import cn.gardenia.client.event.EventBus;
import cn.gardenia.client.event.events.input.KeyEvent;
import cn.gardenia.client.module.ModuleManager;
import cn.gardenia.client.module.macro.MacroManager;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    private static final int CHAT_KEY = 84;

    @Inject(method = "onKey", at = @At("HEAD"))
    private void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        EventBus.INSTANCE.post(new KeyEvent(key, scancode, action, modifiers));

        if (action != 1) return;

        if (key == CHAT_KEY && (modifiers & 1) != 0) {
            if (client.currentScreen == null) {
                client.setScreen(new ChatScreen(CommandManager.INSTANCE.getPrefix()));
                return;
            }
        }

        if (client.world == null || client.player == null) {
            return;
        }

        ModuleManager.INSTANCE.onKeyPress(key);
        MacroManager.INSTANCE.onKeyPress(key);
    }
}
