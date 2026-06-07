package cn.gardenia.client.tool.item;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.*;
import net.minecraft.util.Hand;

public class ItemTool {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public void init() {}

    public ItemStack getMainHand() {
        if (mc.player == null) return ItemStack.EMPTY;
        return mc.player.getMainHandStack();
    }

    public ItemStack getOffHand() {
        if (mc.player == null) return ItemStack.EMPTY;
        return mc.player.getOffHandStack();
    }

    public ItemStack getStackInSlot(int slot) {
        if (mc.player == null) return ItemStack.EMPTY;
        return mc.player.getInventory().getStack(slot);
    }

    public String getItemName(ItemStack stack) {
        if (stack.isEmpty()) return "";
        return stack.getItem().getName().getString();
    }

    public int getItemCount(Item item) {
        if (mc.player == null) return 0;
        return mc.player.getInventory().count(item);
    }

    public boolean hasItem(Item item) {
        return getItemCount(item) > 0;
    }

    public int findItem(Item item) {
        if (mc.player == null) return -1;
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            if (inv.getStack(i).getItem() == item) return i;
        }
        return -1;
    }

    public int findItemFast(Item item) {
        if (mc.player == null) return -1;
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).getItem() == item) return i;
        }
        return -1;
    }

    public int findItem(Class<? extends Item> itemClass) {
        if (mc.player == null) return -1;
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            if (itemClass.isInstance(inv.getStack(i).getItem())) return i;
        }
        return -1;
    }

    public boolean switchTo(int slot) {
        if (mc.player == null) return false;
        if (slot < 0 || slot > 8) return false;
        mc.player.getInventory().selectedSlot = slot;
        return true;
    }

    public boolean switchTo(Item item) {
        int slot = findItemFast(item);
        if (slot < 0) return false;
        return switchTo(slot);
    }

    public boolean useItem() {
        if (mc.player == null || mc.interactionManager == null) return false;
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        return true;
    }

    public boolean useItem(Hand hand) {
        if (mc.player == null || mc.interactionManager == null) return false;
        mc.interactionManager.interactItem(mc.player, hand);
        return true;
    }

    public boolean useItemOffhand() {
        return useItem(Hand.OFF_HAND);
    }

    public void drop() {
        if (mc.player == null) return;
        mc.player.dropSelectedItem(false);
    }

    public void dropAll() {
        if (mc.player == null) return;
        mc.player.dropSelectedItem(true);
    }

    public void dropSlot(int slot) {
        if (mc.player == null || mc.interactionManager == null) return;
        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                slot,
                0,
                net.minecraft.screen.slot.SlotActionType.THROW,
                mc.player
        );
    }

    public float getAttackDamage(ItemStack stack) {
        if (stack.isEmpty()) return 1.0f;
        return 1.0f;
    }

    public float getMiningSpeed(ItemStack stack) {
        if (stack.isEmpty()) return 1.0f;
        return stack.getMiningSpeedMultiplier(null);
    }

    public boolean isTool(ItemStack stack) {
        return stack.getItem() instanceof net.minecraft.item.MiningToolItem;
    }

    public boolean isSword(ItemStack stack) {
        return stack.getItem() instanceof SwordItem;
    }

    public boolean isPickaxe(ItemStack stack) {
        return stack.getItem() instanceof PickaxeItem;
    }

    public boolean isAxe(ItemStack stack) {
        return stack.getItem() instanceof AxeItem;
    }

    public boolean isShovel(ItemStack stack) {
        return stack.getItem() instanceof ShovelItem;
    }

    public boolean isHoe(ItemStack stack) {
        return stack.getItem() instanceof HoeItem;
    }

    public boolean isFood(ItemStack stack) {
        return !stack.isEmpty() && stack.get(net.minecraft.component.DataComponentTypes.FOOD) != null;
    }

    public boolean isBlock(ItemStack stack) {
        return stack.getItem() instanceof BlockItem;
    }

    public boolean isBow(ItemStack stack) {
        return stack.getItem() instanceof BowItem;
    }

    public boolean isCrossbow(ItemStack stack) {
        return stack.getItem() instanceof CrossbowItem;
    }

    public boolean isThrowable(ItemStack stack) {
        return false;
    }

    public boolean isPotion(ItemStack stack) {
        return false;
    }

    public int getItemCooldown(Item item) {
        if (mc.player == null) return 0;
        ItemStack stack = new ItemStack(item);
        return (int)(mc.player.getItemCooldownManager().getCooldownProgress(stack, 0) * 20);
    }

    public boolean isOnCooldown(Item item) {
        if (mc.player == null) return false;
        return mc.player.getItemCooldownManager().isCoolingDown(new ItemStack(item));
    }

    public boolean hasCooldown(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return isOnCooldown(stack.getItem());
    }

    public int getDurability(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageable()) return -1;
        return stack.getMaxDamage() - stack.getDamage();
    }
}
