package cn.gardenia.client.event.events.attack;

import cn.gardenia.client.event.Event;

public class AttackSlowdownEvent extends Event {
    private Double reduce;
    private boolean sprint;

    public AttackSlowdownEvent(Double reduce, boolean sprint) {
        this.reduce = reduce;
        this.sprint = sprint;
    }

    public Double getReduce() {
        return reduce;
    }

    public void setReduce(Double reduce) {
        this.reduce = reduce;
    }

    public boolean isSprint() {
        return sprint;
    }

    public void setSprint(boolean sprint) {
        this.sprint = sprint;
    }
}
