package cn.gardenia.client.module.misc;

import cn.gardenia.client.event.events.player.PlayerTickEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.Setting;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ExperienceDroppingBlock;
import net.minecraft.block.RedstoneOreBlock;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.function.Consumer;

public class AutoToolModule extends Module {
    private final Setting<Boolean> autoWeapon = new Setting<>("AutoWeapon", "Switch to a weapon before KillAura attacks", false);
    private final Setting<Boolean> maceStealerFall = new Setting<>("Mace StealerModule Fall", "Prefer mace while falling", true);
    private final Setting<Double> maceFallDistance = rangedDouble("Mace Fall Distance", "Fall distance required to prefer mace", 1.5D, 0.0D, 10.0D, 0.1D);
    private final Setting<Boolean> checkSword = new Setting<>("Check Sword", "Do not switch away from swords", false);
    private final Setting<Boolean> switchBack = new Setting<>("Switch Back", "Switch back after mining", true);
    private final Consumer<PlayerTickEvent> tickListener = this::onTick;

    private int originSlot = -1;

    public AutoToolModule() {
        super("AutoTools", "Automatically switches to the best tool for the job", Category.MISC);
        addSetting(autoWeapon);
        addSetting(maceStealerFall);
        addSetting(maceFallDistance);
        addSetting(checkSword);
        addSetting(switchBack);
    }

    @Override
    public void onEnable() {
        originSlot = -1;
        subscribe(PlayerTickEvent.class, tickListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerTickEvent.class, tickListener);
        switchBack();
    }

    private void onTick(PlayerTickEvent event) {
        if (!event.isPre() || mc.player == null || mc.world == null || mc.interactionManager == null) {
            return;
        }

        if (mc.interactionManager.isBreakingBlock()) {
            if (checkSword.getBoolean() && mc.player.getMainHandStack().getItem() instanceof SwordItem) {
                return;
            }
            if (!(mc.crosshairTarget instanceof BlockHitResult hitResult) || mc.crosshairTarget.getType() != HitResult.Type.BLOCK) {
                return;
            }

            int bestTool = getBestTool(hitResult.getBlockPos());
            if (bestTool != -1 && bestTool != mc.player.getInventory().selectedSlot) {
                if (originSlot == -1) {
                    originSlot = mc.player.getInventory().selectedSlot;
                }
                selectHotbarSlot(bestTool);
            }
        } else if (switchBack.getBoolean()) {
            switchBack();
        }
    }

    private void switchBack() {
        if (originSlot != -1 && mc.player != null) {
            selectHotbarSlot(originSlot);
            originSlot = -1;
        }
    }

    public void switchToBestWeaponForAttack() {
        if (!autoWeapon.getBoolean() || mc.player == null) {
            return;
        }

        ItemStack currentStack = mc.player.getMainHandStack();
        if (isWeapon(currentStack)) {
            return;
        }

        int bestSlot = -1;
        if (maceStealerFall.getBoolean() && mc.player.fallDistance > maceFallDistance.getDouble()) {
            bestSlot = findHotbarItem(Items.MACE);
        }

        if (bestSlot == -1) {
            bestSlot = getBestWeaponSlot();
        }

        if (bestSlot != -1 && bestSlot != mc.player.getInventory().selectedSlot) {
            selectHotbarSlot(bestSlot);
        }
    }

    private void selectHotbarSlot(int slot) {
        if (mc.player == null || slot < 0 || slot > 8) {
            return;
        }
        mc.player.getInventory().selectedSlot = slot;
        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }
    }

    private int getBestWeaponSlot() {
        if (mc.player == null) {
            return -1;
        }
        int bestSlot = -1;
        double bestScore = 0.0D;
        for (int slot = 0; slot < 9; slot++) {
            double score = weaponScore(mc.player.getInventory().getStack(slot), false);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    private int findHotbarItem(Item item) {
        if (mc.player == null) {
            return -1;
        }
        for (int slot = 0; slot < 9; slot++) {
            if (mc.player.getInventory().getStack(slot).isOf(item)) {
                return slot;
            }
        }
        return -1;
    }

    private boolean isWeapon(ItemStack stack) {
        return weaponScore(stack, true) > 0.0D;
    }

    private double weaponScore(ItemStack stack, boolean includeMace) {
        if (stack == null || stack.isEmpty()) {
            return 0.0D;
        }
        Item item = stack.getItem();
        if (item == Items.NETHERITE_SWORD) return 10.0D;
        if (item == Items.DIAMOND_SWORD) return 9.0D;
        if (item == Items.IRON_SWORD) return 8.0D;
        if (item == Items.STONE_SWORD) return 7.0D;
        if (item == Items.GOLDEN_SWORD) return 6.0D;
        if (item == Items.WOODEN_SWORD) return 5.0D;
        if (includeMace && item == Items.MACE) return 9.5D;
        if (item == Items.NETHERITE_AXE) return 8.5D;
        if (item == Items.DIAMOND_AXE) return 8.0D;
        if (item == Items.IRON_AXE) return 7.0D;
        if (item == Items.STONE_AXE) return 6.0D;
        if (item == Items.GOLDEN_AXE) return 5.0D;
        if (item == Items.WOODEN_AXE) return 4.0D;
        return 0.0D;
    }

    private int getBestTool(net.minecraft.util.math.BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        int slot = 0;
        float bestSpeed = 1.0F;

        for (int index = 0; index < 9; index++) {
            ItemStack stack = mc.player.getInventory().getStack(index);
            if (stack.isEmpty() || state.isAir()) {
                continue;
            }
            if (stack.getItem() instanceof SwordItem && !state.isOf(Blocks.COBWEB)) {
                continue;
            }

            float speed = stack.getMiningSpeedMultiplier(state);
            if (speed > 1.0F
                    && !(state.getBlock() instanceof ExperienceDroppingBlock)
                    && !(state.getBlock() instanceof RedstoneOreBlock)) {
                int efficiency = enchantmentLevel(stack, Enchantments.EFFICIENCY);
                if (efficiency > 0) {
                    speed += efficiency * efficiency + 1.0F;
                }
            }
            if (speed > bestSpeed) {
                slot = index;
                bestSpeed = speed;
            }
        }

        return bestSpeed > 1.0F ? slot : -1;
    }

    private int enchantmentLevel(ItemStack stack, net.minecraft.registry.RegistryKey<Enchantment> enchantment) {
        if (mc.world == null || stack == null || stack.isEmpty()) {
            return 0;
        }
        Enchantment value = mc.world.getRegistryManager()
                .getOrThrow(RegistryKeys.ENCHANTMENT)
                .getValueOrThrow(enchantment);
        RegistryEntry<Enchantment> entry = mc.world.getRegistryManager()
                .getOrThrow(RegistryKeys.ENCHANTMENT)
                .getEntry(value);
        return EnchantmentHelper.getLevel(entry, stack);
    }

    private static Setting<Double> rangedDouble(String name, String description, double defaultValue, double min, double max, double step) {
        Setting<Double> setting = new Setting<>(name, description, defaultValue);
        setting.setRange(min, max, step);
        return setting;
    }
}
