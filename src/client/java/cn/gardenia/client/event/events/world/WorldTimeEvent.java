package cn.gardenia.client.event.events.world;

import cn.gardenia.client.event.Event;

public class WorldTimeEvent extends Event {
    private final long timeOfDay;
    private final long dayTime;

    public WorldTimeEvent(long timeOfDay, long dayTime) {
        this.timeOfDay = timeOfDay;
        this.dayTime = dayTime;
    }

    public long getTimeOfDay() { return timeOfDay; }
    public long getDayTime() { return dayTime; }
}
