package cn.gardenia.client.event.events.render;

import cn.gardenia.client.event.Event;

public class RenderFogEvent extends Event {
    private final float tickDelta;

    public RenderFogEvent(float tickDelta) {
        this.tickDelta = tickDelta;
    }

    public float getTickDelta() { return tickDelta; }
}
