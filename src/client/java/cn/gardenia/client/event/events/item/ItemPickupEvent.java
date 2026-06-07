package cn.gardenia.client.event.events.item;

import cn.gardenia.client.event.Event;
import net.minecraft.item.ItemStack;

public class ItemPickupEvent extends Event {
    private final ItemStack stack;

    public ItemPickupEvent(ItemStack stack) {
        this.stack = stack;
    }

    public ItemStack getStack() { return stack; }
}
