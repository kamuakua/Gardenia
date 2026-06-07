package cn.gardenia.client.event.events.player;

import cn.gardenia.client.event.Event;

public class PlayerSneakEvent extends Event {
    private final boolean sneaking;

    public PlayerSneakEvent(boolean sneaking) {
        this.sneaking = sneaking;
    }

    public boolean isSneaking() { return sneaking; }
}
