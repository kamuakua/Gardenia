package cn.gardenia.client.event.events.item;

import cn.gardenia.client.event.Event;
import net.minecraft.item.ItemStack;

public class ItemUseEvent extends Event {
    private final ItemStack stack;
    private final int itemUseCooldown;

    public ItemUseEvent(ItemStack stack, int itemUseCooldown) {
        this.stack = stack;
        this.itemUseCooldown = itemUseCooldown;
    }

    public ItemStack getStack() { return stack; }
    public int getItemUseCooldown() { return itemUseCooldown; }
}
