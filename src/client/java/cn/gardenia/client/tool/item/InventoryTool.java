package cn.gardenia.client.tool.item;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

public class InventoryTool {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public void init() {}

    public PlayerInventory getInventory() {
        if (mc.player == null) return null;
        return mc.player.getInventory();
    }

    public int getSelectedSlot() {
        if (mc.player == null) return 0;
        return mc.player.getInventory().selectedSlot;
    }

    public void setSelectedSlot(int slot) {
        if (mc.player == null) return;
        if (slot >= 0 && slot <= 8) {
            mc.player.getInventory().selectedSlot = slot;
        }
    }

    public int getTotalSlots() {
        if (mc.player == null) return 0;
        return mc.player.getInventory().size();
    }

    public int getHotbarSize() {
        return 9;
    }

    public int getMainSize() {
        return 27;
    }

    public int getArmorSize() {
        return 4;
    }

    public int getOffhandSlot() {
        return 40;
    }

    public int findItemInHotbar(Item item) {
        if (mc.player == null) return -1;
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).getItem() == item) return i;
        }
        return -1;
    }

    public int findItemInInventory(Item item) {
        if (mc.player == null) return -1;
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 9; i < 36; i++) {
            if (inv.getStack(i).getItem() == item) return i;
        }
        return -1;
    }

    public int findEmptySlot() {
        if (mc.player == null) return -1;
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 0; i < 36; i++) {
            if (inv.getStack(i).isEmpty()) return i;
        }
        return -1;
    }

    public int findEmptyHotbarSlot() {
        if (mc.player == null) return -1;
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).isEmpty()) return i;
        }
        return -1;
    }

    public int totalItemCount(Item item) {
        if (mc.player == null) return 0;
        return mc.player.getInventory().count(item);
    }

    public int totalItemCount(Item item, boolean includeOffhand) {
        int count = totalItemCount(item);
        if (includeOffhand && mc.player != null) {
            if (mc.player.getOffHandStack().getItem() == item) {
                count += mc.player.getOffHandStack().getCount();
            }
        }
        return count;
    }

    public boolean hasItem(Item item) {
        return totalItemCount(item) > 0;
    }

    public boolean hasSpace() {
        return findEmptySlot() != -1;
    }

    public boolean hasSpaceInHotbar() {
        return findEmptyHotbarSlot() != -1;
    }

    public int getFirstBlockSlot() {
        if (mc.player == null) return -1;
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).getItem() instanceof net.minecraft.item.BlockItem) {
                return i;
            }
        }
        return -1;
    }

    public int getFirstFoodSlot() {
        if (mc.player == null) return -1;
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.get(net.minecraft.component.DataComponentTypes.FOOD) != null) {
                return i;
            }
        }
        return -1;
    }

    public boolean swap(int slotA, int slotB) {
        if (mc.player == null || mc.interactionManager == null) return false;
        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                slotA,
                slotB,
                SlotActionType.SWAP,
                mc.player
        );
        return true;
    }

    public boolean quickMove(int slot) {
        if (mc.player == null || mc.interactionManager == null) return false;
        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                slot,
                0,
                SlotActionType.QUICK_MOVE,
                mc.player
        );
        return true;
    }

    public boolean dropSlot(int slot) {
        if (mc.player == null || mc.interactionManager == null) return false;
        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                slot,
                1,
                SlotActionType.THROW,
                mc.player
        );
        return true;
    }

    public boolean closeScreen() {
        if (mc.player == null) return false;
        mc.player.closeHandledScreen();
        return true;
    }

    public boolean isContainerOpen() {
        if (mc.player == null) return false;
        return mc.player.currentScreenHandler != mc.player.playerScreenHandler;
    }

    public void moveToHotbar(int inventorySlot) {
        if (mc.player == null || mc.interactionManager == null) return;
        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                inventorySlot,
                0,
                SlotActionType.QUICK_MOVE,
                mc.player
        );
    }

    public void pickUpSlot(int slot) {
        if (mc.player == null || mc.interactionManager == null) return;
        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                slot,
                0,
                SlotActionType.PICKUP,
                mc.player
        );
    }

    public void quickDrop(int slot) {
        if (mc.player == null || mc.interactionManager == null) return;
        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                slot,
                0,
                SlotActionType.THROW,
                mc.player
        );
    }

    public void sendSlotPacket(int slot, ItemStack stack) {
        if (mc.getNetworkHandler() == null) return;
        Int2ObjectMap<ItemStack> stacks = new Int2ObjectOpenHashMap<>();
        stacks.put(slot, stack);
        mc.getNetworkHandler().sendPacket(new net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket(
                mc.player.currentScreenHandler.syncId,
                mc.player.currentScreenHandler.getRevision(),
                slot,
                0,
                SlotActionType.PICKUP,
                stack,
                stacks
        ));
    }
}
