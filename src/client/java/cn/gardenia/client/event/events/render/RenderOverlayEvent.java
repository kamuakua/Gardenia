package cn.gardenia.client.event.events.render;

import cn.gardenia.client.event.Event;
import net.minecraft.client.gui.DrawContext;

public class RenderOverlayEvent extends Event {
    private final DrawContext context;
    private final float tickDelta;

    public RenderOverlayEvent(DrawContext context, float tickDelta) {
        this.context = context;
        this.tickDelta = tickDelta;
    }

    public DrawContext getContext() { return context; }
    public float getTickDelta() { return tickDelta; }
}
