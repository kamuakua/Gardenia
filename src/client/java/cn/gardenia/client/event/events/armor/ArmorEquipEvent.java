package cn.gardenia.client.event.events.armor;

import cn.gardenia.client.event.Event;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.EquipmentSlot;

public class ArmorEquipEvent extends Event {
    private final ItemStack oldArmor;
    private final ItemStack newArmor;
    private final EquipmentSlot slot;

    public ArmorEquipEvent(ItemStack oldArmor, ItemStack newArmor, EquipmentSlot slot) {
        this.oldArmor = oldArmor;
        this.newArmor = newArmor;
        this.slot = slot;
    }

    public ItemStack getOldArmor() { return oldArmor; }
    public ItemStack getNewArmor() { return newArmor; }
    public EquipmentSlot getSlot() { return slot; }
}
