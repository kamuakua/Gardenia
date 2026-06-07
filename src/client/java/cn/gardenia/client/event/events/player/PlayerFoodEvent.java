package cn.gardenia.client.event.events.player;

import cn.gardenia.client.event.Event;

public class PlayerFoodEvent extends Event {
    private final int foodLevel;
    private final float saturation;

    public PlayerFoodEvent(int foodLevel, float saturation) {
        this.foodLevel = foodLevel;
        this.saturation = saturation;
    }

    public int getFoodLevel() { return foodLevel; }
    public float getSaturation() { return saturation; }
}
