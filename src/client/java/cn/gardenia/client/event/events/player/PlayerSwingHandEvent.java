package cn.gardenia.client.event.events.player;

import cn.gardenia.client.event.Event;

public class PlayerSwingHandEvent extends Event {
    private final boolean isOffHand;

    public PlayerSwingHandEvent(boolean isOffHand) {
        this.isOffHand = isOffHand;
    }

    public boolean isOffHand() { return isOffHand; }
    public boolean isMainHand() { return !isOffHand; }
}
