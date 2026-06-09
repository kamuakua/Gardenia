package cn.gardenia.client.module.misc;

import net.minecraft.block.Blocks;
import net.minecraft.block.FlowerBlock;
import net.minecraft.block.PressurePlateBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.FireworkRocketItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.PlayerHeadItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class GardeniaInventoryUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private GardeniaInventoryUtil() {
    }

    static List<ItemStack> getAllItems() {
        ArrayList<ItemStack> items = new ArrayList<>(41);
        if (mc.player == null) {
            return items;
        }
        for (int slot = 0; slot < mc.player.getInventory().size(); slot++) {
            items.add(mc.player.getInventory().getStack(slot));
        }
        items.add(mc.player.getOffHandStack());
        return items;
    }

    static boolean shouldDisableFeatures() {
        for (ItemStack stack : getAllItems()) {
            if (stack.isEmpty()) {
                continue;
            }
            String name = stack.getName().getString();
            if (name.contains("长按点击") || name.contains("点击使用") || name.contains("离开游戏")
                    || name.contains("选择一个队伍") || name.contains("再来一局")) {
                return true;
            }
        }
        return false;
    }

    static boolean isGodItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.getItem() == Items.TOTEM_OF_UNDYING
                || stack.getItem() == Items.END_CRYSTAL
                || isKBBall(stack)
                || isKBStick(stack)
                || isGodAxe(stack);
    }

    static boolean isGodAxe(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() == Items.GOLDEN_AXE
                && enchantmentLevel(stack, Enchantments.SHARPNESS) > 100;
    }

    static boolean isSharpnessAxe(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof AxeItem)) {
            return false;
        }
        int level = enchantmentLevel(stack, Enchantments.SHARPNESS);
        return level >= 8 && level < 50;
    }

    static boolean isKBBall(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() == Items.SLIME_BALL
                && enchantmentLevel(stack, Enchantments.KNOCKBACK) > 1;
    }

    static boolean isKBStick(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() == Items.STICK
                && enchantmentLevel(stack, Enchantments.KNOCKBACK) > 1;
    }

    static int getItemSlot(Item item) {
        if (mc.player == null) {
            return -1;
        }
        for (int slot = 0; slot < 36; slot++) {
            if (mc.player.getInventory().getStack(slot).isOf(item)) {
                return slot;
            }
        }
        return -1;
    }

    static int getItemStackSlot(ItemStack stack) {
        if (mc.player == null || stack == null) {
            return -1;
        }
        for (int slot = 0; slot < 36; slot++) {
            if (mc.player.getInventory().getStack(slot) == stack) {
                return slot;
            }
        }
        return -1;
    }

    static boolean hasItem(Item item) {
        return getItemCount(item) > 0;
    }

    static int getItemCount(Item item) {
        int count = 0;
        for (ItemStack stack : getAllItems()) {
            if (!stack.isEmpty() && stack.isOf(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    static int getBlockCountInInventory() {
        int count = 0;
        for (ItemStack stack : getAllItems()) {
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem && isValidBlockStack(stack) && isItemValid(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    static boolean isValidBlockStack(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        return blockItem.getBlock() != Blocks.ENCHANTING_TABLE
                && blockItem.getBlock() != Blocks.COBWEB
                && !(blockItem.getBlock() instanceof SlabBlock)
                && !(blockItem.getBlock() instanceof FlowerBlock)
                && !(blockItem.getBlock() instanceof PressurePlateBlock)
                && !isStairItem(stack.getItem());
    }

    static ItemStack getBestSword() {
        return getAllItems().stream()
                .filter(stack -> !stack.isEmpty() && stack.getItem() instanceof SwordItem && isItemValid(stack))
                .max(Comparator.comparingDouble(GardeniaInventoryUtil::getSwordDamage))
                .orElse(null);
    }

    static ItemStack getBestPickaxe() {
        return getAllItems().stream()
                .filter(stack -> !stack.isEmpty() && stack.getItem() instanceof PickaxeItem && isItemValid(stack))
                .max(Comparator.comparingDouble(GardeniaInventoryUtil::getToolScore))
                .orElse(null);
    }

    static ItemStack getBestAxe() {
        return getAllItems().stream()
                .filter(stack -> !stack.isEmpty() && stack.getItem() instanceof AxeItem && !isSharpnessAxe(stack) && isItemValid(stack))
                .max(Comparator.comparingDouble(GardeniaInventoryUtil::getToolScore))
                .orElse(null);
    }

    static ItemStack getBestSharpnessAxe() {
        return getAllItems().stream()
                .filter(stack -> !stack.isEmpty() && stack.getItem() instanceof AxeItem && isSharpnessAxe(stack) && isItemValid(stack) && !isGodAxe(stack))
                .max(Comparator.comparingDouble(GardeniaInventoryUtil::getAxeDamage))
                .orElse(null);
    }

    static ItemStack getBestShovel() {
        return getAllItems().stream()
                .filter(stack -> !stack.isEmpty() && stack.getItem() instanceof ShovelItem && isItemValid(stack))
                .max(Comparator.comparingDouble(GardeniaInventoryUtil::getToolScore))
                .orElse(null);
    }

    static ItemStack getBestCrossbow() {
        return getAllItems().stream()
                .filter(stack -> !stack.isEmpty() && stack.getItem() instanceof CrossbowItem && isItemValid(stack))
                .max(Comparator.comparingDouble(GardeniaInventoryUtil::getCrossbowScore))
                .orElse(null);
    }

    static ItemStack getBestPunchBow() {
        return getAllItems().stream()
                .filter(stack -> !stack.isEmpty() && stack.getItem() instanceof BowItem && isPunchBow(stack) && isItemValid(stack))
                .max(Comparator.comparingDouble(GardeniaInventoryUtil::getPunchBowScore))
                .orElse(null);
    }

    static ItemStack getBestPowerBow() {
        return getAllItems().stream()
                .filter(stack -> !stack.isEmpty() && stack.getItem() instanceof BowItem && isPowerBow(stack) && isItemValid(stack))
                .max(Comparator.comparingDouble(GardeniaInventoryUtil::getPowerBowScore))
                .orElse(null);
    }

    static ItemStack getBestProjectile() {
        return getAllItems().stream()
                .filter(stack -> !stack.isEmpty() && (stack.isOf(Items.EGG) || stack.isOf(Items.SNOWBALL)) && isItemValid(stack))
                .max(Comparator.comparingInt(ItemStack::getCount))
                .orElse(null);
    }

    static ItemStack getFishingRod() {
        return getAllItems().stream()
                .filter(stack -> !stack.isEmpty() && stack.getItem() instanceof FishingRodItem && isItemValid(stack))
                .findFirst()
                .orElse(null);
    }

    static ItemStack getBestBlock() {
        return getAllItems().stream()
                .filter(stack -> !stack.isEmpty() && stack.getItem() instanceof BlockItem && isValidBlockStack(stack) && isItemValid(stack))
                .max(Comparator.comparingInt(ItemStack::getCount))
                .orElse(null);
    }

    static ItemStack getWorstBlock() {
        return getAllItems().stream()
                .filter(stack -> !stack.isEmpty() && stack.getItem() instanceof BlockItem && isValidBlockStack(stack) && isItemValid(stack))
                .min(Comparator.comparingInt(ItemStack::getCount))
                .orElse(null);
    }

    static ItemStack getWorstProjectile() {
        return getAllItems().stream()
                .filter(stack -> !stack.isEmpty() && (stack.isOf(Items.EGG) || stack.isOf(Items.SNOWBALL)))
                .min(Comparator.comparingInt(ItemStack::getCount))
                .orElse(null);
    }

    static ItemStack getWorstArrow() {
        return getAllItems().stream()
                .filter(stack -> !stack.isEmpty() && stack.isOf(Items.ARROW) && isItemValid(stack))
                .min(Comparator.comparingInt(ItemStack::getCount))
                .orElse(null);
    }

    static float getBestArmorScore(EquipmentSlot slot) {
        return getAllItems().stream()
                .filter(stack -> !stack.isEmpty() && stack.getItem() instanceof ArmorItem && armorSlot(stack) == slot)
                .map(GardeniaInventoryUtil::getProtection)
                .max(Float::compareTo)
                .orElse(0.0F);
    }

    static float getCurrentArmorScore(EquipmentSlot slot) {
        if (mc.player == null) {
            return 0.0F;
        }
        return switch (slot) {
            case HEAD -> getProtection(mc.player.getInventory().getArmorStack(3));
            case CHEST -> getProtection(mc.player.getInventory().getArmorStack(2));
            case LEGS -> getProtection(mc.player.getInventory().getArmorStack(1));
            case FEET -> getProtection(mc.player.getInventory().getArmorStack(0));
            default -> 0.0F;
        };
    }

    static EquipmentSlot armorSlot(ItemStack stack) {
        Item item = stack.getItem();
        if (item == Items.LEATHER_HELMET || item == Items.CHAINMAIL_HELMET || item == Items.IRON_HELMET
                || item == Items.GOLDEN_HELMET || item == Items.DIAMOND_HELMET || item == Items.NETHERITE_HELMET
                || item == Items.TURTLE_HELMET) {
            return EquipmentSlot.HEAD;
        }
        if (item == Items.LEATHER_CHESTPLATE || item == Items.CHAINMAIL_CHESTPLATE || item == Items.IRON_CHESTPLATE
                || item == Items.GOLDEN_CHESTPLATE || item == Items.DIAMOND_CHESTPLATE || item == Items.NETHERITE_CHESTPLATE) {
            return EquipmentSlot.CHEST;
        }
        if (item == Items.LEATHER_LEGGINGS || item == Items.CHAINMAIL_LEGGINGS || item == Items.IRON_LEGGINGS
                || item == Items.GOLDEN_LEGGINGS || item == Items.DIAMOND_LEGGINGS || item == Items.NETHERITE_LEGGINGS) {
            return EquipmentSlot.LEGS;
        }
        if (item == Items.LEATHER_BOOTS || item == Items.CHAINMAIL_BOOTS || item == Items.IRON_BOOTS
                || item == Items.GOLDEN_BOOTS || item == Items.DIAMOND_BOOTS || item == Items.NETHERITE_BOOTS) {
            return EquipmentSlot.FEET;
        }
        return null;
    }

    static float getProtection(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ArmorItem)) {
            return 0.0F;
        }
        return armorBaseScore(stack.getItem()) + enchantmentLevel(stack, Enchantments.PROTECTION);
    }

    static float getSwordDamage(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0.0F;
        }
        return swordBaseDamage(stack.getItem()) + enchantmentLevel(stack, Enchantments.SHARPNESS) * 1.25F;
    }

    static float getAxeDamage(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0.0F;
        }
        return axeBaseDamage(stack.getItem()) + enchantmentLevel(stack, Enchantments.SHARPNESS) * 1.25F;
    }

    static float getToolScore(ItemStack stack) {
        if (stack == null || stack.isEmpty() || isGodItem(stack) || isSharpnessAxe(stack)) {
            return 0.0F;
        }
        float score;
        if (stack.getItem() instanceof PickaxeItem) {
            score = stack.getMiningSpeedMultiplier(Blocks.STONE.getDefaultState());
        } else if (stack.getItem() instanceof AxeItem) {
            score = stack.getMiningSpeedMultiplier(Blocks.OAK_LOG.getDefaultState());
        } else if (stack.getItem() instanceof ShovelItem) {
            score = stack.getMiningSpeedMultiplier(Blocks.DIRT.getDefaultState());
        } else if (stack.getItem() instanceof MiningToolItem) {
            score = 1.0F;
        } else {
            return 0.0F;
        }
        return score + enchantmentLevel(stack, Enchantments.EFFICIENCY) * 0.0075F;
    }

    static boolean isPunchBow(ItemStack stack) {
        return getPunchBowScore(stack) > 10.0F && isItemValid(stack);
    }

    static boolean isPowerBow(ItemStack stack) {
        return getPowerBowScore(stack) > 10.0F && isItemValid(stack);
    }

    static float getPunchBowScore(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof BowItem)) {
            return 0.0F;
        }
        return 10.0F
                + enchantmentLevel(stack, Enchantments.PUNCH)
                + enchantmentLevel(stack, Enchantments.INFINITY)
                + enchantmentLevel(stack, Enchantments.FLAME)
                + enchantmentLevel(stack, Enchantments.POWER) / 10.0F
                + durabilityFraction(stack);
    }

    static float getPowerBowScore(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof BowItem)) {
            return 0.0F;
        }
        return 10.0F
                + enchantmentLevel(stack, Enchantments.PUNCH) / 10.0F
                + enchantmentLevel(stack, Enchantments.INFINITY)
                + enchantmentLevel(stack, Enchantments.FLAME)
                + enchantmentLevel(stack, Enchantments.POWER)
                + durabilityFraction(stack);
    }

    static float getCrossbowScore(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof CrossbowItem)) {
            return 0.0F;
        }
        return enchantmentLevel(stack, Enchantments.QUICK_CHARGE)
                + enchantmentLevel(stack, Enchantments.MULTISHOT)
                + enchantmentLevel(stack, Enchantments.PIERCING);
    }

    static boolean isItemValid(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return true;
        }
        if (stack.getItem() instanceof PlayerHeadItem) {
            return false;
        }
        String name = stack.getName().getString();
        return !name.contains("Click")
                && !name.contains("Right")
                && !name.contains("点击")
                && !name.contains("Teleport")
                && !name.contains("使用")
                && !name.contains("传送")
                && !name.contains("再来");
    }

    static boolean isCommonItemUseful(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        Item item = stack.getItem();
        if (item instanceof BlockItem block) {
            return block.getBlock() != Blocks.ENCHANTING_TABLE && block.getBlock() != Blocks.COBWEB;
        }
        return item != Items.BOOK
                && item != Items.WRITABLE_BOOK
                && item != Items.WRITTEN_BOOK
                && !(item instanceof ExperienceBottleItem)
                && !(item instanceof FireworkRocketItem)
                && item != Items.WHEAT_SEEDS
                && item != Items.BEETROOT_SEEDS
                && item != Items.MELON_SEEDS
                && item != Items.PUMPKIN_SEEDS
                && item != Items.FLINT_AND_STEEL;
    }

    private static int enchantmentLevel(ItemStack stack, net.minecraft.registry.RegistryKey<Enchantment> enchantment) {
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

    private static boolean isStairItem(Item item) {
        return item == Items.OAK_STAIRS
                || item == Items.SPRUCE_STAIRS
                || item == Items.BIRCH_STAIRS
                || item == Items.JUNGLE_STAIRS
                || item == Items.ACACIA_STAIRS
                || item == Items.DARK_OAK_STAIRS
                || item == Items.MANGROVE_STAIRS
                || item == Items.CHERRY_STAIRS
                || item == Items.BAMBOO_STAIRS
                || item == Items.CRIMSON_STAIRS
                || item == Items.WARPED_STAIRS
                || item == Items.STONE_STAIRS
                || item == Items.COBBLESTONE_STAIRS
                || item == Items.MOSSY_COBBLESTONE_STAIRS
                || item == Items.STONE_BRICK_STAIRS
                || item == Items.MOSSY_STONE_BRICK_STAIRS
                || item == Items.BRICK_STAIRS
                || item == Items.SANDSTONE_STAIRS
                || item == Items.SMOOTH_SANDSTONE_STAIRS
                || item == Items.RED_SANDSTONE_STAIRS
                || item == Items.SMOOTH_RED_SANDSTONE_STAIRS
                || item == Items.QUARTZ_STAIRS
                || item == Items.SMOOTH_QUARTZ_STAIRS
                || item == Items.PURPUR_STAIRS
                || item == Items.PRISMARINE_STAIRS
                || item == Items.PRISMARINE_BRICK_STAIRS
                || item == Items.DARK_PRISMARINE_STAIRS
                || item == Items.NETHER_BRICK_STAIRS
                || item == Items.RED_NETHER_BRICK_STAIRS
                || item == Items.BLACKSTONE_STAIRS
                || item == Items.POLISHED_BLACKSTONE_STAIRS
                || item == Items.POLISHED_BLACKSTONE_BRICK_STAIRS
                || item == Items.END_STONE_BRICK_STAIRS
                || item == Items.GRANITE_STAIRS
                || item == Items.POLISHED_GRANITE_STAIRS
                || item == Items.DIORITE_STAIRS
                || item == Items.POLISHED_DIORITE_STAIRS
                || item == Items.ANDESITE_STAIRS
                || item == Items.POLISHED_ANDESITE_STAIRS
                || item == Items.CUT_COPPER_STAIRS
                || item == Items.EXPOSED_CUT_COPPER_STAIRS
                || item == Items.WEATHERED_CUT_COPPER_STAIRS
                || item == Items.OXIDIZED_CUT_COPPER_STAIRS
                || item == Items.WAXED_CUT_COPPER_STAIRS
                || item == Items.WAXED_EXPOSED_CUT_COPPER_STAIRS
                || item == Items.WAXED_WEATHERED_CUT_COPPER_STAIRS
                || item == Items.WAXED_OXIDIZED_CUT_COPPER_STAIRS
                || item == Items.TUFF_STAIRS
                || item == Items.POLISHED_TUFF_STAIRS
                || item == Items.TUFF_BRICK_STAIRS
                || item == Items.DEEPSLATE_BRICK_STAIRS
                || item == Items.DEEPSLATE_TILE_STAIRS
                || item == Items.COBBLED_DEEPSLATE_STAIRS
                || item == Items.POLISHED_DEEPSLATE_STAIRS
                || item == Items.MUD_BRICK_STAIRS;
    }

    private static float durabilityFraction(ItemStack stack) {
        if (!stack.isDamageable() || stack.getMaxDamage() <= 0) {
            return 0.0F;
        }
        return (float) stack.getDamage() / (float) stack.getMaxDamage();
    }

    private static float armorBaseScore(Item item) {
        if (item == Items.LEATHER_HELMET || item == Items.LEATHER_CHESTPLATE || item == Items.LEATHER_LEGGINGS || item == Items.LEATHER_BOOTS) {
            return 100.0F;
        }
        if (item == Items.CHAINMAIL_HELMET || item == Items.CHAINMAIL_CHESTPLATE || item == Items.CHAINMAIL_LEGGINGS || item == Items.CHAINMAIL_BOOTS) {
            return 200.0F;
        }
        if (item == Items.GOLDEN_HELMET || item == Items.GOLDEN_CHESTPLATE || item == Items.GOLDEN_LEGGINGS || item == Items.GOLDEN_BOOTS) {
            return 300.0F;
        }
        if (item == Items.IRON_HELMET || item == Items.IRON_CHESTPLATE || item == Items.IRON_LEGGINGS || item == Items.IRON_BOOTS) {
            return 400.0F;
        }
        if (item == Items.DIAMOND_HELMET || item == Items.DIAMOND_CHESTPLATE || item == Items.DIAMOND_LEGGINGS || item == Items.DIAMOND_BOOTS) {
            return 500.0F;
        }
        if (item == Items.NETHERITE_HELMET || item == Items.NETHERITE_CHESTPLATE || item == Items.NETHERITE_LEGGINGS || item == Items.NETHERITE_BOOTS) {
            return 600.0F;
        }
        return 0.0F;
    }

    private static float swordBaseDamage(Item item) {
        if (item == Items.WOODEN_SWORD) return 5.0F;
        if (item == Items.GOLDEN_SWORD) return 5.0F;
        if (item == Items.STONE_SWORD) return 6.0F;
        if (item == Items.IRON_SWORD) return 7.0F;
        if (item == Items.DIAMOND_SWORD) return 8.0F;
        if (item == Items.NETHERITE_SWORD) return 9.0F;
        return 0.0F;
    }

    private static float axeBaseDamage(Item item) {
        if (item == Items.WOODEN_AXE) return 4.0F;
        if (item == Items.GOLDEN_AXE) return 4.0F;
        if (item == Items.STONE_AXE) return 5.0F;
        if (item == Items.IRON_AXE) return 6.0F;
        if (item == Items.DIAMOND_AXE) return 7.0F;
        if (item == Items.NETHERITE_AXE) return 8.0F;
        return 0.0F;
    }
}
