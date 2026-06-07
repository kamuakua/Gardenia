package cn.gardenia.client.event.events.movement;

import cn.gardenia.client.event.Event;

public class StayingOnGroundSurfaceEvent extends Event {
    private boolean stay;

    public StayingOnGroundSurfaceEvent(boolean stay) {
        this.stay = stay;
    }

    public boolean shouldStay() {
        return stay;
    }

    public void setStay(boolean stay) {
        this.stay = stay;
    }
}
