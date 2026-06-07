package cn.gardenia.client.event.events.item;

import cn.gardenia.client.event.Event;
import net.minecraft.item.ItemStack;

public class InventoryUpdateEvent extends Event {
    private final int slot;
    private final ItemStack stack;

    public InventoryUpdateEvent(int slot, ItemStack stack) {
        this.slot = slot;
        this.stack = stack;
    }

    public int getSlot() { return slot; }
    public ItemStack getStack() { return stack; }
}
