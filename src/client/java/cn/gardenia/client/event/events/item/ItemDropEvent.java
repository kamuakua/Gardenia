package cn.gardenia.client.event.events.item;

import cn.gardenia.client.event.Event;
import net.minecraft.item.ItemStack;

public class ItemDropEvent extends Event {
    private final ItemStack stack;

    public ItemDropEvent(ItemStack stack) {
        this.stack = stack;
    }

    public ItemStack getStack() { return stack; }
}
