package cn.gardenia.client.event.events.item;

import cn.gardenia.client.event.Event;
import net.minecraft.item.Item;

public class CooldownEvent extends Event {
    private final Item item;
    private final int cooldown;

    public CooldownEvent(Item item, int cooldown) {
        this.item = item;
        this.cooldown = cooldown;
    }

    public Item getItem() { return item; }
    public int getCooldown() { return cooldown; }
}
