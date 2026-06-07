package cn.gardenia.client.event.events.render;

import cn.gardenia.client.event.Event;

public class RenderWeatherEvent extends Event {
    private final float tickDelta;

    public RenderWeatherEvent(float tickDelta) {
        this.tickDelta = tickDelta;
    }

    public float getTickDelta() { return tickDelta; }
}
