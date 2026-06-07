package cn.gardenia.client.event.events.movement;

import cn.gardenia.client.event.Event;

public class StepEvent extends Event {
    private float stepHeight;

    public StepEvent(float stepHeight) {
        this.stepHeight = stepHeight;
    }

    public float getStepHeight() { return stepHeight; }
    public void setStepHeight(float stepHeight) { this.stepHeight = stepHeight; }
}
