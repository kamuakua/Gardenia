package cn.gardenia.client.event.events.attack;

import cn.gardenia.client.event.Event;

public class CriticalHitEvent extends Event {
    private float modifier;
    private final boolean vanillaCritical;

    public CriticalHitEvent(float modifier, boolean vanillaCritical) {
        this.modifier = modifier;
        this.vanillaCritical = vanillaCritical;
    }

    public float getModifier() { return modifier; }
    public void setModifier(float modifier) { this.modifier = modifier; }
    public boolean isVanillaCritical() { return vanillaCritical; }
}
