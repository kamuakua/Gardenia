package cn.gardenia.client.event.events.game;

import cn.gardenia.client.event.Event;

public class GameJoinEvent extends Event {
    private final String worldName;
    private final boolean singleplayer;

    public GameJoinEvent(String worldName, boolean singleplayer) {
        this.worldName = worldName;
        this.singleplayer = singleplayer;
    }

    public String getWorldName() { return worldName; }
    public boolean isSingleplayer() { return singleplayer; }
}
