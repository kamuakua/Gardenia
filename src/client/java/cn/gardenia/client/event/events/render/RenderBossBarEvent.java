package cn.gardenia.client.event.events.render;

import cn.gardenia.client.event.Event;
import net.minecraft.client.gui.DrawContext;

public class RenderBossBarEvent extends Event {
    private final DrawContext context;

    public RenderBossBarEvent(DrawContext context) {
        this.context = context;
    }

    public DrawContext getContext() { return context; }
}
