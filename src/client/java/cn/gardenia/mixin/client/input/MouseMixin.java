package cn.gardenia.mixin.client.input;

import cn.gardenia.client.event.EventBus;
import cn.gardenia.client.event.events.input.MouseDragEvent;
import cn.gardenia.client.event.events.input.MouseEvent;
import cn.gardenia.client.event.events.input.MouseScrollEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    @Shadow private double cursorDeltaX;
    @Shadow private double cursorDeltaY;
    @Shadow private double x;
    @Shadow private double y;
    @Shadow private boolean leftButtonClicked;
    @Shadow private boolean rightButtonClicked;
    @Shadow private boolean middleButtonClicked;

    private double lastDragX = 0;
    private double lastDragY = 0;
    private boolean isDragging = false;

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void onMouseButton(long window, int button, int action, int modifiers, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        double mouseX = mc.mouse.getX() / mc.getWindow().getScaleFactor();
        double mouseY = mc.mouse.getY() / mc.getWindow().getScaleFactor();
        EventBus.INSTANCE.post(new MouseEvent(button, action, modifiers, mouseX, mouseY));
        
        if (action == 1 && button == 0) {
            isDragging = true;
            lastDragX = mouseX;
            lastDragY = mouseY;
        } else if (action == 0 && button == 0) {
            isDragging = false;
        }
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"))
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        double mouseX = mc.mouse.getX() / mc.getWindow().getScaleFactor();
        double mouseY = mc.mouse.getY() / mc.getWindow().getScaleFactor();
        EventBus.INSTANCE.post(new MouseScrollEvent(horizontal, vertical, mouseX, mouseY));
    }

    @Inject(method = "onCursorPos", at = @At("TAIL"))
    private void onCursorPos(long window, double x, double y, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (isDragging) {
            double mouseX = mc.mouse.getX() / mc.getWindow().getScaleFactor();
            double mouseY = mc.mouse.getY() / mc.getWindow().getScaleFactor();
            double deltaX = mouseX - lastDragX;
            double deltaY = mouseY - lastDragY;
            lastDragX = mouseX;
            lastDragY = mouseY;
            EventBus.INSTANCE.post(new MouseDragEvent(0, mouseX, mouseY, deltaX, deltaY));
        }
    }
}
