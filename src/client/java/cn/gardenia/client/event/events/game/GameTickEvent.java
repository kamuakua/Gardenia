package cn.gardenia.client.event.events.game;

import cn.gardenia.client.event.Event;
import cn.gardenia.client.event.EventPhase;

public class GameTickEvent extends Event {
    private final EventPhase phase;

    public GameTickEvent() {
        this(EventPhase.PRE);
    }

    public GameTickEvent(EventPhase phase) {
        this.phase = phase;
    }

    public EventPhase getPhase() {
        return phase;
    }

    public boolean isPre() {
        return phase == EventPhase.PRE;
    }

    public boolean isPost() {
        return phase == EventPhase.POST;
    }
}
