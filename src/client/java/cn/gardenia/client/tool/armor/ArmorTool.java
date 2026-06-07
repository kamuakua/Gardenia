package cn.gardenia.client.tool.armor;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.*;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.entry.RegistryEntry;
import java.util.ArrayList;
import java.util.List;

public class ArmorTool {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public void init() {}

    public ItemStack getHelmet() {
        if (mc.player == null) return ItemStack.EMPTY;
        return mc.player.getInventory().getArmorStack(3);
    }

    public ItemStack getChestplate() {
        if (mc.player == null) return ItemStack.EMPTY;
        return mc.player.getInventory().getArmorStack(2);
    }

    public ItemStack getLeggings() {
        if (mc.player == null) return ItemStack.EMPTY;
        return mc.player.getInventory().getArmorStack(1);
    }

    public ItemStack getBoots() {
        if (mc.player == null) return ItemStack.EMPTY;
        return mc.player.getInventory().getArmorStack(0);
    }

    public ItemStack getArmorPiece(EquipmentSlot slot) {
        if (mc.player == null) return ItemStack.EMPTY;
        return switch (slot) {
            case HEAD -> getHelmet();
            case CHEST -> getChestplate();
            case LEGS -> getLeggings();
            case FEET -> getBoots();
            default -> ItemStack.EMPTY;
        };
    }

    public int getTotalProtection() {
        return 0;
    }

    public int getTotalToughness() {
        return 0;
    }

    public List<ItemStack> getArmorStacks() {
        List<ItemStack> list = new ArrayList<>();
        if (mc.player == null) return list;
        PlayerInventory inv = mc.player.getInventory();
        list.add(inv.getArmorStack(3));
        list.add(inv.getArmorStack(2));
        list.add(inv.getArmorStack(1));
        list.add(inv.getArmorStack(0));
        return list;
    }

    public boolean isWearingFullSet() {
        return !getHelmet().isEmpty()
                && !getChestplate().isEmpty()
                && !getLeggings().isEmpty()
                && !getBoots().isEmpty();
    }

    public boolean isWearingAny() {
        return !getHelmet().isEmpty()
                || !getChestplate().isEmpty()
                || !getLeggings().isEmpty()
                || !getBoots().isEmpty();
    }

    public boolean hasEmptySlot() {
        return getHelmet().isEmpty()
                || getChestplate().isEmpty()
                || getLeggings().isEmpty()
                || getBoots().isEmpty();
    }

    public boolean canEquip(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem;
    }

    public EquipmentSlot getSlotFor(ItemStack stack) {
        if (!(stack.getItem() instanceof ArmorItem)) return null;
        if (stack == getHelmet()) return EquipmentSlot.HEAD;
        if (stack == getChestplate()) return EquipmentSlot.CHEST;
        if (stack == getLeggings()) return EquipmentSlot.LEGS;
        if (stack == getBoots()) return EquipmentSlot.FEET;
        return null;
    }

    public int getAverageDurability() {
        int total = 0;
        int count = 0;
        for (ItemStack armor : getArmorStacks()) {
            if (armor.isEmpty() || !armor.isDamageable()) continue;
            total += (armor.getMaxDamage() - armor.getDamage()) * 100 / armor.getMaxDamage();
            count++;
        }
        return count > 0 ? total / count : 0;
    }

    public int getDurability(EquipmentSlot slot) {
        ItemStack armor = getArmorPiece(slot);
        if (armor.isEmpty() || !armor.isDamageable()) return -1;
        return (armor.getMaxDamage() - armor.getDamage()) * 100 / armor.getMaxDamage();
    }

    public boolean isLowDurability(EquipmentSlot slot, int thresholdPercent) {
        int dur = getDurability(slot);
        return dur >= 0 && dur <= thresholdPercent;
    }

    public boolean hasEnchantment(EquipmentSlot slot, RegistryEntry<net.minecraft.enchantment.Enchantment> enchantment) {
        ItemStack armor = getArmorPiece(slot);
        if (armor.isEmpty()) return false;
        return net.minecraft.enchantment.EnchantmentHelper.getLevel(enchantment, armor) > 0;
    }

    public int getEnchantmentLevel(EquipmentSlot slot, RegistryEntry<net.minecraft.enchantment.Enchantment> enchantment) {
        ItemStack armor = getArmorPiece(slot);
        if (armor.isEmpty()) return 0;
        return net.minecraft.enchantment.EnchantmentHelper.getLevel(enchantment, armor);
    }

    public int findBestArmorSlot(EquipmentSlot slot) {
        if (mc.player == null) return -1;
        PlayerInventory inv = mc.player.getInventory();
        int bestSlot = -1;
        int bestProt = -1;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.getItem() instanceof ArmorItem) {
                if (slotToArmorIndex(slot) == armorSlotForIndex(i)) {
                    bestSlot = i;
                    bestProt = 1;
                }
            }
        }
        return bestSlot;
    }

    private int slotToArmorIndex(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> 3;
            case CHEST -> 2;
            case LEGS -> 1;
            case FEET -> 0;
            default -> -1;
        };
    }

    private int armorSlotForIndex(int invIndex) {
        if (invIndex >= 36 && invIndex < 40) return invIndex - 36;
        return -1;
    }

    public boolean equipBest(EquipmentSlot slot) {
        int bestSlot = findBestArmorSlot(slot);
        if (bestSlot < 0) return false;

        int armorSlot = switch (slot) {
            case HEAD -> 39;
            case CHEST -> 38;
            case LEGS -> 37;
            case FEET -> 36;
            default -> -1;
        };

        if (armorSlot < 0) return false;
        if (mc.player == null || mc.interactionManager == null) return false;

        if (bestSlot <= 8) {
            mc.player.getInventory().selectedSlot = bestSlot;
            mc.interactionManager.interactItem(mc.player, net.minecraft.util.Hand.MAIN_HAND);
        } else {
            mc.interactionManager.clickSlot(
                    mc.player.currentScreenHandler.syncId,
                    bestSlot,
                    0,
                    net.minecraft.screen.slot.SlotActionType.QUICK_MOVE,
                    mc.player
            );
        }
        return true;
    }

    public boolean equipBestAll() {
        return equipBest(EquipmentSlot.HEAD)
                & equipBest(EquipmentSlot.CHEST)
                & equipBest(EquipmentSlot.LEGS)
                & equipBest(EquipmentSlot.FEET);
    }

    public double getDamageReduction() {
        int prot = getTotalProtection();
        return prot * 4.0 / 100.0;
    }

    public double getDamageReductionMax() {
        int prot = Math.min(getTotalProtection(), 20);
        return prot * 4.0 / 100.0;
    }
}
