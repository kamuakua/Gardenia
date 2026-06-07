package cn.gardenia.client.event.events.player;

import cn.gardenia.client.event.Event;
import cn.gardenia.client.event.EventPhase;

public class PlayerTickEvent extends Event {
    private final EventPhase phase;

    public PlayerTickEvent(EventPhase phase) {
        this.phase = phase;
    }

    public EventPhase getPhase() { return phase; }
    public boolean isPre() { return phase == EventPhase.PRE; }
    public boolean isPost() { return phase == EventPhase.POST; }
}
