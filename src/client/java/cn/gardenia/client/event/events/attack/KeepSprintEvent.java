package cn.gardenia.client.event.events.attack;

import cn.gardenia.client.event.Event;

public class KeepSprintEvent extends Event {
    private boolean keepSprint;

    public KeepSprintEvent(boolean keepSprint) {
        this.keepSprint = keepSprint;
    }

    public boolean shouldKeepSprint() { return keepSprint; }
    public void setKeepSprint(boolean keepSprint) { this.keepSprint = keepSprint; }
}
