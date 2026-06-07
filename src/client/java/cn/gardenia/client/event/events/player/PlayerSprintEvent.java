package cn.gardenia.client.event.events.player;

import cn.gardenia.client.event.Event;

public class PlayerSprintEvent extends Event {
    private final boolean sprinting;

    public PlayerSprintEvent(boolean sprinting) {
        this.sprinting = sprinting;
    }

    public boolean isSprinting() { return sprinting; }
}
