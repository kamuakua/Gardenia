package cn.gardenia.mixin.client.gui.screen;

import cn.gardenia.client.command.CommandManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Shadow protected TextFieldWidget chatField;

    private String[] completionCycle;
    private int completionIndex;

    @Inject(method = "sendMessage", at = @At("HEAD"), cancellable = true)
    private void onSendMessage(String chatText, boolean addToHistory, CallbackInfo ci) {
        if (CommandManager.INSTANCE.dispatch(chatText)) {
            ci.cancel();
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (keyCode == 258) {
            String text = chatField.getText();
            if (text.startsWith(CommandManager.INSTANCE.getPrefix())) {
                doCustomCompletion();
                cir.setReturnValue(true);
            }
        } else if (keyCode == 257 || keyCode == 335) {
            completionCycle = null;
            completionIndex = 0;
        } else {
            completionCycle = null;
            completionIndex = 0;
        }
    }

    private void doCustomCompletion() {
        String text = chatField.getText();
        String prefix = CommandManager.INSTANCE.getPrefix();

        if (completionCycle == null) {
            List<String> suggestions = CommandManager.INSTANCE.getSuggestions(text);
            if (suggestions == null || suggestions.isEmpty()) return;

            String current = text;
            List<String> filtered = new java.util.ArrayList<>();
            for (String s : suggestions) {
                if (s.toLowerCase().startsWith(current.toLowerCase()) && !s.equalsIgnoreCase(current)) {
                    filtered.add(s);
                }
            }

            if (filtered.isEmpty()) return;
            completionCycle = filtered.toArray(new String[0]);
            completionIndex = 0;
        }

        String selected = completionCycle[completionIndex];
        chatField.setText(selected);
        chatField.setCursorToEnd(false);
        completionIndex = (completionIndex + 1) % completionCycle.length;
    }
}
