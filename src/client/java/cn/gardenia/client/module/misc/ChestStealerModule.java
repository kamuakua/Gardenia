package cn.gardenia.client.module.misc;

import cn.gardenia.client.event.events.player.PlayerTickEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.Setting;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class ChestStealerModule extends Module {
    private static long lastActionTime;

    private final Setting<Double> delay = rangedDouble("Min Delay (Ticks)", "Minimum steal delay in ticks", 2.0D, 0.0D, 10.0D, 1.0D);
    private final Setting<Double> maxDelay = rangedDouble("Max Delay (Ticks)", "Maximum steal delay in ticks", 6.0D, 0.0D, 10.0D, 1.0D);
    private final Setting<Boolean> pickEnderChest = new Setting<>("Ender Chest", "Also steal from ender chests", false);
    private final Consumer<PlayerTickEvent> tickListener = this::onTick;

    private Screen lastTickScreen;

    public ChestStealerModule() {
        super("ChestStealer", "Automatically steals useful chest items", Category.MISC);
        addSetting(delay);
        addSetting(maxDelay);
        addSetting(pickEnderChest);
    }

    public static boolean isWorking() {
        return System.currentTimeMillis() - lastActionTime < 150L;
    }

    @Override
    public void onEnable() {
        lastTickScreen = null;
        lastActionTime = 0L;
        subscribe(PlayerTickEvent.class, tickListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerTickEvent.class, tickListener);
        lastTickScreen = null;
    }

    private void onTick(PlayerTickEvent event) {
        if (!event.isPre() || mc.player == null || mc.interactionManager == null) {
            return;
        }

        Screen currentScreen = mc.currentScreen;
        if (!(currentScreen instanceof GenericContainerScreen) || !(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler menu)) {
            lastTickScreen = currentScreen;
            return;
        }

        if (currentScreen != lastTickScreen) {
            lastActionTime = System.currentTimeMillis();
            lastTickScreen = currentScreen;
            return;
        }

        if (!isChestTitle(currentScreen.getTitle())) {
            return;
        }

        if (isChestEmpty(menu) && delayPassed()) {
            mc.player.closeHandledScreen();
            markAction();
            return;
        }

        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < menu.getRows() * 9; slot++) {
            slots.add(slot);
        }
        Collections.shuffle(slots);

        for (int slot : slots) {
            ItemStack stack = menu.getSlot(slot).getStack();
            if (isItemUseful(stack) && isBestItemInChest(menu, stack) && delayPassed()) {
                mc.interactionManager.clickSlot(menu.syncId, slot, 0, SlotActionType.QUICK_MOVE, mc.player);
                markAction();
                break;
            }
        }
    }

    private boolean isChestTitle(Text title) {
        String chestTitle = title == null ? "" : title.getString();
        return chestTitle.equals(Text.translatable("container.chest").getString())
                || chestTitle.equals(Text.translatable("container.chestDouble").getString())
                || chestTitle.equals("Chest")
                || pickEnderChest.getBoolean() && chestTitle.equals(Text.translatable("container.enderchest").getString());
    }

    private boolean isChestEmpty(GenericContainerScreenHandler menu) {
        for (int slot = 0; slot < menu.getRows() * 9; slot++) {
            ItemStack stack = menu.getSlot(slot).getStack();
            if (!stack.isEmpty() && isItemUseful(stack) && isBestItemInChest(menu, stack)) {
                return false;
            }
        }
        return true;
    }

    private boolean isBestItemInChest(GenericContainerScreenHandler menu, ItemStack stack) {
        if (GardeniaInventoryUtil.isGodItem(stack) || GardeniaInventoryUtil.isSharpnessAxe(stack)) {
            return true;
        }

        for (int slot = 0; slot < menu.getRows() * 9; slot++) {
            ItemStack checkStack = menu.getSlot(slot).getStack();
            if (checkStack == stack || checkStack.isEmpty()) {
                continue;
            }

            if (stack.getItem() instanceof ArmorItem && checkStack.getItem() instanceof ArmorItem) {
                if (GardeniaInventoryUtil.armorSlot(stack) == GardeniaInventoryUtil.armorSlot(checkStack)
                        && GardeniaInventoryUtil.getProtection(checkStack) > GardeniaInventoryUtil.getProtection(stack)) {
                    return false;
                }
            } else if (stack.getItem() instanceof SwordItem && checkStack.getItem() instanceof SwordItem) {
                if (GardeniaInventoryUtil.getSwordDamage(checkStack) > GardeniaInventoryUtil.getSwordDamage(stack)) {
                    return false;
                }
            } else if (stack.getItem() instanceof PickaxeItem && checkStack.getItem() instanceof PickaxeItem
                    || stack.getItem() instanceof AxeItem && checkStack.getItem() instanceof AxeItem
                    || stack.getItem() instanceof ShovelItem && checkStack.getItem() instanceof ShovelItem) {
                if (GardeniaInventoryUtil.getToolScore(checkStack) > GardeniaInventoryUtil.getToolScore(stack)) {
                    return false;
                }
            }
        }
        return true;
    }

    static boolean isItemUseful(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (GardeniaInventoryUtil.isGodItem(stack) || GardeniaInventoryUtil.isSharpnessAxe(stack)) {
            return true;
        }
        if (stack.getItem() instanceof ArmorItem item) {
            float protection = GardeniaInventoryUtil.getProtection(stack);
            float bestArmor = GardeniaInventoryUtil.getBestArmorScore(GardeniaInventoryUtil.armorSlot(stack));
            return protection > bestArmor || GardeniaInventoryUtil.getCurrentArmorScore(GardeniaInventoryUtil.armorSlot(stack)) < protection;
        }
        if (stack.getItem() instanceof SwordItem) {
            return GardeniaInventoryUtil.getSwordDamage(stack) > GardeniaInventoryUtil.getSwordDamage(GardeniaInventoryUtil.getBestSword());
        }
        if (stack.getItem() instanceof PickaxeItem) {
            return GardeniaInventoryUtil.getToolScore(stack) > GardeniaInventoryUtil.getToolScore(GardeniaInventoryUtil.getBestPickaxe());
        }
        if (stack.getItem() instanceof AxeItem) {
            return GardeniaInventoryUtil.getToolScore(stack) > GardeniaInventoryUtil.getToolScore(GardeniaInventoryUtil.getBestAxe());
        }
        if (stack.getItem() instanceof ShovelItem) {
            return GardeniaInventoryUtil.getToolScore(stack) > GardeniaInventoryUtil.getToolScore(GardeniaInventoryUtil.getBestShovel());
        }
        if (stack.getItem() instanceof CrossbowItem) {
            return GardeniaInventoryUtil.getCrossbowScore(stack) > GardeniaInventoryUtil.getCrossbowScore(GardeniaInventoryUtil.getBestCrossbow());
        }
        if (stack.getItem() instanceof BowItem && GardeniaInventoryUtil.isPunchBow(stack)) {
            return GardeniaInventoryUtil.getPunchBowScore(stack) > GardeniaInventoryUtil.getPunchBowScore(GardeniaInventoryUtil.getBestPunchBow());
        }
        if (stack.getItem() instanceof BowItem && GardeniaInventoryUtil.isPowerBow(stack)) {
            return GardeniaInventoryUtil.getPowerBowScore(stack) > GardeniaInventoryUtil.getPowerBowScore(GardeniaInventoryUtil.getBestPowerBow());
        }
        if (stack.isOf(Items.COMPASS)) {
            return !GardeniaInventoryUtil.hasItem(stack.getItem());
        }
        if (stack.isOf(Items.WATER_BUCKET) && GardeniaInventoryUtil.getItemCount(Items.WATER_BUCKET) >= 1) {
            return false;
        }
        if (stack.isOf(Items.LAVA_BUCKET) && GardeniaInventoryUtil.getItemCount(Items.LAVA_BUCKET) >= 1) {
            return false;
        }
        if (stack.getItem() instanceof BlockItem && GardeniaInventoryUtil.isValidBlockStack(stack)
                && GardeniaInventoryUtil.getBlockCountInInventory() + stack.getCount() >= 256) {
            return false;
        }
        if (stack.isOf(Items.ARROW) && GardeniaInventoryUtil.getItemCount(Items.ARROW) + stack.getCount() >= 256) {
            return false;
        }
        if (stack.getItem() instanceof FishingRodItem && GardeniaInventoryUtil.getItemCount(Items.FISHING_ROD) >= 1) {
            return false;
        }
        if ((stack.isOf(Items.SNOWBALL) || stack.isOf(Items.EGG))
                && GardeniaInventoryUtil.getItemCount(Items.SNOWBALL) + GardeniaInventoryUtil.getItemCount(Items.EGG) + stack.getCount() >= 64) {
            return false;
        }
        return GardeniaInventoryUtil.isCommonItemUseful(stack);
    }

    private boolean delayPassed() {
        double min = Math.min(delay.getDouble(), maxDelay.getDouble());
        double max = Math.max(delay.getDouble(), maxDelay.getDouble());
        long ticks = (long) ThreadLocalRandom.current().nextDouble(min, max + 1.0D);
        return System.currentTimeMillis() - lastActionTime >= ticks * 50L;
    }

    private void markAction() {
        lastActionTime = System.currentTimeMillis();
    }

    private static Setting<Double> rangedDouble(String name, String description, double defaultValue, double min, double max, double step) {
        Setting<Double> setting = new Setting<>(name, description, defaultValue);
        setting.setRange(min, max, step);
        return setting;
    }
}
