package cn.gardenia.client.event.events.render;

import cn.gardenia.client.event.Event;

public class UpdateFovEvent extends Event {
    private float fov;

    public UpdateFovEvent(float fov) {
        this.fov = fov;
    }

    public float getFov() {
        return fov;
    }

    public void setFov(float fov) {
        this.fov = fov;
    }
}
