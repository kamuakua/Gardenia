package cn.gardenia.client.event.events.render;

import cn.gardenia.client.event.Event;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

public class RenderScreenEvent extends Event {
    private final Screen screen;
    private final DrawContext context;
    private final int mouseX;
    private final int mouseY;
    private final float tickDelta;

    public RenderScreenEvent(Screen screen, DrawContext context, int mouseX, int mouseY, float tickDelta) {
        this.screen = screen;
        this.context = context;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.tickDelta = tickDelta;
    }

    public Screen getScreen() { return screen; }
    public DrawContext getContext() { return context; }
    public int getMouseX() { return mouseX; }
    public int getMouseY() { return mouseY; }
    public float getTickDelta() { return tickDelta; }
}
