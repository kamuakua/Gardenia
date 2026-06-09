package cn.gardenia.client.module.misc;

import cn.gardenia.client.event.events.movement.PlayerMotionEvent;
import cn.gardenia.client.event.events.network.GlobalPacketEvent;
import cn.gardenia.client.event.events.player.PlayerTickEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.ModuleManager;
import cn.gardenia.client.module.Setting;
import cn.gardenia.client.module.combat.KillAura;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.PlayerInput;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class NewInvManagerModule extends Module {
    public static NewInvManagerModule INSTANCE;

    private static int suppressMovementForInventoryAction;

    private final Setting<Double> delay = rangedDouble("Delay", "Inventory action delay in milliseconds", 50.0D, 0.0D, 200.0D, 1.0D);
    private final Setting<String> offhandItems = new Setting<>("Offhand Items", "Preferred offhand item", "None", new String[]{"Golden Apple", "Projectile", "Fishing Rod", "Block", "None"});
    private final Setting<Boolean> autoArmor = new Setting<>("AutoArmor", "Equip better armor automatically", true);
    private final Setting<Boolean> silentManage = new Setting<>("Silent Manage", "Manage player inventory through silent open/click/close packets", false);
    private final Setting<Boolean> inventoryOnly = new Setting<>("Inventory Only", "Only manage while inventory is open", false);
    private final Setting<Boolean> noMove = new Setting<>("No Move", "Suppress movement around inventory actions", false);
    private final Setting<Boolean> fastResprint = new Setting<>("Fast Resprint", "Resume sprint shortly after silent inventory actions", true);
    private final Setting<Double> resprintDelay = rangedDouble("Resprint Delay", "Ticks before resuming sprint after silent inventory actions", 1.0D, 0.0D, 5.0D, 1.0D);
    private final Setting<Boolean> switchSword = new Setting<>("Switch Sword", "Sort best sword into slot", true);
    private final Setting<Double> swordSlot = rangedDouble("Sword Slot", "Target sword hotbar slot", 1.0D, 1.0D, 9.0D, 1.0D);
    private final Setting<Boolean> switchBlock = new Setting<>("Switch Block", "Sort best block into slot", true);
    private final Setting<Double> blockSlot = rangedDouble("Block Slot", "Target block hotbar slot", 2.0D, 1.0D, 9.0D, 1.0D);
    private final Setting<Double> maxBlockSize = rangedDouble("Max Block Size", "Maximum carried block count", 256.0D, 64.0D, 1024.0D, 64.0D);
    private final Setting<Boolean> switchPickaxe = new Setting<>("Switch Pickaxe", "Sort best pickaxe into slot", true);
    private final Setting<Double> pickaxeSlot = rangedDouble("Pickaxe Slot", "Target pickaxe hotbar slot", 3.0D, 1.0D, 9.0D, 1.0D);
    private final Setting<Boolean> switchAxe = new Setting<>("Switch Axe", "Sort best axe into slot", true);
    private final Setting<Double> axeSlot = rangedDouble("Axe Slot", "Target axe hotbar slot", 4.0D, 1.0D, 9.0D, 1.0D);
    private final Setting<Boolean> switchBow = new Setting<>("Switch Bow or Crossbow", "Sort best bow or crossbow into slot", true);
    private final Setting<Double> bowSlot = rangedDouble("Bow Slot", "Target bow hotbar slot", 5.0D, 1.0D, 9.0D, 1.0D);
    private final Setting<String> preferBow = new Setting<>("Bow Priority", "Preferred bow type", "Crossbow", new String[]{"Power Bow", "Punch Bow", "Crossbow"});
    private final Setting<Double> maxArrowSize = rangedDouble("Max Arrow Size", "Maximum carried arrow count", 256.0D, 64.0D, 512.0D, 64.0D);
    private final Setting<Boolean> switchWaterBucket = new Setting<>("Switch Water Bucket", "Sort water bucket into slot", true);
    private final Setting<Double> waterBucketSlot = rangedDouble("Water Bucket Slot", "Target water bucket slot", 6.0D, 1.0D, 9.0D, 1.0D);
    private final Setting<Boolean> switchEnderPearl = new Setting<>("Switch Ender Pearl", "Sort ender pearl into slot", true);
    private final Setting<Double> enderPearlSlot = rangedDouble("Ender Pearl Slot", "Target pearl slot", 7.0D, 1.0D, 9.0D, 1.0D);
    private final Setting<Boolean> switchFireball = new Setting<>("Switch Fireball", "Sort fire charge into slot", true);
    private final Setting<Double> fireballSlot = rangedDouble("Fireball Slot", "Target fireball slot", 8.0D, 1.0D, 9.0D, 1.0D);
    private final Setting<Boolean> switchGoldenApple = new Setting<>("Switch Golden Apple", "Sort golden apple into slot", true);
    private final Setting<Double> goldenAppleSlot = rangedDouble("Golden Apple Slot", "Target golden apple slot", 9.0D, 1.0D, 9.0D, 1.0D);
    private final Setting<Boolean> throwItems = new Setting<>("Throw Items", "Throw useless items", true);
    private final Setting<Double> waterBucketCount = rangedDouble("Keep Water Buckets", "Water bucket keep count", 1.0D, 0.0D, 5.0D, 1.0D);
    private final Setting<Double> lavaBucketCount = rangedDouble("Keep Lava Buckets", "Lava bucket keep count", 1.0D, 0.0D, 5.0D, 1.0D);
    private final Setting<Double> cobwebCount = rangedDouble("Keep Cobweb", "Cobweb keep count", 1.0D, 0.0D, 10.0D, 1.0D);
    private final Setting<Boolean> keepProjectile = new Setting<>("Keep Eggs & Snowballs", "Keep throwable projectiles", true);
    private final Setting<Boolean> switchProjectile = new Setting<>("Switch Eggs & Snowballs", "Sort projectile into slot", false);
    private final Setting<Double> projectileSlot = rangedDouble("Eggs & Snowballs Slot", "Target projectile slot", 9.0D, 1.0D, 9.0D, 1.0D);
    private final Setting<Double> maxProjectileSize = rangedDouble("Max Eggs & Snowballs Size", "Maximum projectile count", 64.0D, 16.0D, 256.0D, 16.0D);
    private final Setting<Boolean> switchRod = new Setting<>("Switch Rod", "Sort fishing rod into slot", false);
    private final Setting<Double> rodSlot = rangedDouble("Rod Slot", "Target fishing rod slot", 9.0D, 1.0D, 9.0D, 1.0D);
    private final Setting<Boolean> autoDisable = new Setting<>("Auto Disable", "Disable after death or respawn", false);
    private final Consumer<PlayerTickEvent> tickListener = this::onTick;
    private final Consumer<PlayerMotionEvent> motionListener = this::onMotion;
    private final Consumer<GlobalPacketEvent> packetListener = this::onPacket;

    private long lastActionTime;
    private boolean clickOffHand;
    private boolean inventoryOpen;
    private boolean cancelNextInventoryOpenPacket;
    private boolean silentInventoryClickPrimed;
    private boolean sendingSilentInventoryPackets;
    private boolean movingSilentAction;
    private boolean wasSprinting;
    private boolean suppressSprint;
    private boolean prevSuppressSprint;
    private boolean replaying;
    private int movingSilentDelayTicks;
    private int pendingSilentThrowSlot = -1;
    private int pendingSilentThrowButton;
    private int pendingSilentThrowTicks;
    private int suppressSprintTicks;
    private int resprintCountdown;
    private final Queue<Packet<?>> pendingMovingSilentPackets = new ConcurrentLinkedQueue<>();

    public NewInvManagerModule() {
        super("NewInvManager", "Inventory manager", Category.MISC);
        INSTANCE = this;
        addSetting(delay);
        addSetting(offhandItems);
        addSetting(autoArmor);
        addSetting(silentManage);
        addSetting(inventoryOnly);
        addSetting(noMove);
        addSetting(fastResprint);
        addSetting(resprintDelay);
        addSetting(switchSword);
        addSetting(swordSlot);
        addSetting(switchBlock);
        addSetting(blockSlot);
        addSetting(maxBlockSize);
        addSetting(switchPickaxe);
        addSetting(pickaxeSlot);
        addSetting(switchAxe);
        addSetting(axeSlot);
        addSetting(switchBow);
        addSetting(bowSlot);
        addSetting(preferBow);
        addSetting(maxArrowSize);
        addSetting(switchWaterBucket);
        addSetting(waterBucketSlot);
        addSetting(switchEnderPearl);
        addSetting(enderPearlSlot);
        addSetting(switchFireball);
        addSetting(fireballSlot);
        addSetting(switchGoldenApple);
        addSetting(goldenAppleSlot);
        addSetting(throwItems);
        addSetting(waterBucketCount);
        addSetting(lavaBucketCount);
        addSetting(cobwebCount);
        addSetting(keepProjectile);
        addSetting(switchProjectile);
        addSetting(projectileSlot);
        addSetting(maxProjectileSize);
        addSetting(switchRod);
        addSetting(rodSlot);
        addSetting(autoDisable);
    }

    public static PlayerInput handleMoveInput(PlayerInput input) {
        if (INSTANCE == null || !INSTANCE.isEnabled() || INSTANCE.mc.player == null) {
            return input;
        }
        if (suppressMovementForInventoryAction <= 0 && !INSTANCE.shouldPauseMovingSilentSprint()) {
            return input;
        }
        if (INSTANCE.mc.options.jumpKey.isPressed() && INSTANCE.mc.player.isOnGround() && !INSTANCE.shouldPauseMovingSilentSprint()) {
            return input;
        }
        INSTANCE.mc.player.setSprinting(false);
        return new PlayerInput(false, false, false, false, false, false, false);
    }

    public static boolean isSuppressingSprint() {
        return INSTANCE != null && INSTANCE.isEnabled()
                && (suppressMovementForInventoryAction > 0 || INSTANCE.suppressSprint || INSTANCE.shouldPauseMovingSilentSprint());
    }

    public static int getMaxBlockSize() {
        return INSTANCE == null ? 256 : INSTANCE.maxBlockSize.getInt();
    }

    public static boolean shouldKeepProjectile() {
        return INSTANCE == null || INSTANCE.keepProjectile.getBoolean();
    }

    public static int getMaxProjectileSize() {
        return INSTANCE == null ? 64 : INSTANCE.maxProjectileSize.getInt();
    }

    public static int getMaxArrowSize() {
        return INSTANCE == null ? 256 : INSTANCE.maxArrowSize.getInt();
    }

    public static int getWaterBucketCount() {
        return INSTANCE == null ? 1 : INSTANCE.waterBucketCount.getInt();
    }

    public static int getLavaBucketCount() {
        return INSTANCE == null ? 1 : INSTANCE.lavaBucketCount.getInt();
    }

    public static int getCobwebCount() {
        return INSTANCE == null ? 1 : INSTANCE.cobwebCount.getInt();
    }

    @Override
    public void onEnable() {
        INSTANCE = this;
        resetState(false);
        subscribe(PlayerTickEvent.class, tickListener);
        subscribe(PlayerMotionEvent.class, motionListener);
        subscribe(GlobalPacketEvent.class, packetListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerTickEvent.class, tickListener);
        unsubscribe(PlayerMotionEvent.class, motionListener);
        unsubscribe(GlobalPacketEvent.class, packetListener);
        resetState(true);
    }

    private void onTick(PlayerTickEvent event) {
        if (event.isPost()) {
            return;
        }
        if (!event.isPre() || mc.player == null || mc.interactionManager == null) {
            return;
        }

        if (!checkConfig()) {
            setEnabled(false);
            return;
        }

        if (autoDisable.getBoolean() && (!mc.player.isAlive() || mc.player.age <= 1)) {
            setEnabled(false);
            return;
        }

        if (GardeniaInventoryUtil.shouldDisableFeatures() || isExternalContainerOpen()) {
            clickOffHand = false;
            resetSilentManageState(false);
            return;
        }

        boolean moving = isMoving();
        if (silentManage.getBoolean() && !inventoryOnly.getBoolean() && !canRunMovingSilentManage()) {
            clickOffHand = false;
            return;
        }
        if (moving && silentManage.getBoolean() && inventoryOnly.getBoolean()) {
            resetSilentManageState(true);
        }
        if (runPendingSilentThrow()) {
            return;
        }
        if (ChestStealerModule.isWorking() || shouldPauseForAction()
                || moving && (!silentManage.getBoolean() || inventoryOnly.getBoolean())
                || !silentManage.getBoolean() && !(mc.currentScreen instanceof InventoryScreen)
                || inventoryOnly.getBoolean() && !(mc.currentScreen instanceof InventoryScreen)) {
            clickOffHand = false;
            return;
        }

        if (autoArmor.getBoolean()) {
            autoArmor();
        }
        handleOffhand();
        sortHotbar();
        throwOverflowItems();
        throwUselessItems();
    }

    private void onMotion(PlayerMotionEvent event) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        if (event.isPost()) {
            flushMovingSilentPackets();
            suppressMovementForInventoryAction = 0;
            return;
        }
        if (!event.isPre()) {
            return;
        }

        boolean busy = shouldSuppressSprint();
        int suppressCap = fastResprint.getBoolean() ? 2 : 8;
        if (busy) {
            suppressSprintTicks = Math.max(suppressSprintTicks, suppressCap);
        } else if (suppressSprintTicks > 0) {
            suppressSprintTicks--;
        }

        boolean nowSuppress = suppressSprintTicks > 0;
        boolean movingSilentBusy = shouldPauseMovingSilentSprint();
        suppressSprint = nowSuppress;
        if (nowSuppress || movingSilentBusy) {
            pauseSprint();
        }

        if (fastResprint.getBoolean()) {
            boolean stillBusy = nowSuppress || movingSilentBusy;
            if (prevSuppressSprint && !stillBusy && resprintCountdown <= 0) {
                resprintCountdown = Math.max(0, resprintDelay.getInt());
                if (resprintCountdown == 0) {
                    tryResumeSprint();
                }
            } else if (resprintCountdown > 0 && !stillBusy) {
                resprintCountdown--;
                if (resprintCountdown == 0) {
                    tryResumeSprint();
                }
            } else if (stillBusy) {
                resprintCountdown = 0;
            }
            prevSuppressSprint = stillBusy;
        } else {
            prevSuppressSprint = false;
            resprintCountdown = 0;
        }
    }

    private void onPacket(GlobalPacketEvent event) {
        if (mc.player == null || !event.isSend() || replaying) {
            return;
        }

        Packet<?> packet = event.getPacket();
        if (handleMovingSilentPacket(event, packet)) {
            return;
        }

        if (inventoryOpen && isActionPacket(packet)) {
            forceSilentClose();
        }

        if (sendingSilentInventoryPackets) {
            if (packet instanceof CloseHandledScreenC2SPacket) {
                inventoryOpen = false;
            }
            return;
        }

        if (cancelNextInventoryOpenPacket
                && packet instanceof ClientCommandC2SPacket command
                && command.getMode() == ClientCommandC2SPacket.Mode.OPEN_INVENTORY) {
            event.cancel();
            cancelNextInventoryOpenPacket = false;
            silentInventoryClickPrimed = true;
            return;
        }

        if (silentInventoryClickPrimed && packet instanceof ClickSlotC2SPacket clickPacket) {
            event.cancel();
            silentInventoryClickPrimed = false;
            sendSilentInventoryPackets(clickPacket);
            return;
        }

        if (packet instanceof CloseHandledScreenC2SPacket) {
            inventoryOpen = false;
        }
    }

    private boolean checkConfig() {
        if (!keepProjectile.getBoolean()) {
            switchProjectile.setValue(false);
        }
        Set<Integer> usedSlots = new HashSet<>();
        return addConfiguredSlot(usedSlots, switchSword, swordSlot)
                && addConfiguredSlot(usedSlots, switchPickaxe, pickaxeSlot)
                && addConfiguredSlot(usedSlots, switchAxe, axeSlot)
                && addConfiguredSlot(usedSlots, switchBow, bowSlot)
                && addConfiguredSlot(usedSlots, switchWaterBucket, waterBucketSlot)
                && addConfiguredSlot(usedSlots, switchEnderPearl, enderPearlSlot)
                && addConfiguredSlot(usedSlots, switchFireball, fireballSlot)
                && (isOffhandMode("Golden Apple") || addConfiguredSlot(usedSlots, switchGoldenApple, goldenAppleSlot))
                && (isOffhandMode("Projectile") || addConfiguredSlot(usedSlots, switchProjectile, projectileSlot))
                && (isOffhandMode("Fishing Rod") || addConfiguredSlot(usedSlots, switchRod, rodSlot))
                && (isOffhandMode("Block") || addConfiguredSlot(usedSlots, switchBlock, blockSlot));
    }

    private boolean addConfiguredSlot(Set<Integer> usedSlots, Setting<Boolean> enabled, Setting<Double> slotSetting) {
        if (!enabled.getBoolean()) {
            return true;
        }
        return usedSlots.add(hotbarSlot(slotSetting));
    }

    private void autoArmor() {
        for (int armorIndex = 0; armorIndex < 4; armorIndex++) {
            ItemStack stack = mc.player.getInventory().getArmorStack(armorIndex);
            EquipmentSlot slot = armorSlotFromInventoryIndex(armorIndex);
            if (!stack.isEmpty() && GardeniaInventoryUtil.getBestArmorScore(slot) > GardeniaInventoryUtil.getProtection(stack)) {
                clickInventory(8 - armorIndex, 1, SlotActionType.THROW);
                return;
            }
        }

        for (int invSlot = 0; invSlot < 36; invSlot++) {
            ItemStack stack = mc.player.getInventory().getStack(invSlot);
            if (!stack.isEmpty() && stack.getItem() instanceof ArmorItem) {
                EquipmentSlot slot = GardeniaInventoryUtil.armorSlot(stack);
                float protection = GardeniaInventoryUtil.getProtection(stack);
                if (GardeniaInventoryUtil.getBestArmorScore(slot) == protection
                        && GardeniaInventoryUtil.getCurrentArmorScore(slot) < protection) {
                    clickInventory(screenSlot(invSlot), 0, SlotActionType.QUICK_MOVE);
                    return;
                }
            }
        }
    }

    private void handleOffhand() {
        if (clickOffHand) {
            clickInventory(45, 0, SlotActionType.PICKUP);
            clickOffHand = false;
            return;
        }

        if (isOffhandMode("Golden Apple")) {
            ItemStack offhand = mc.player.getOffHandStack();
            int slot = GardeniaInventoryUtil.getItemSlot(Items.GOLDEN_APPLE);
            if (slot != -1) {
                if (offhand.isOf(Items.GOLDEN_APPLE)) {
                    ItemStack appleStack = mc.player.getInventory().getStack(slot);
                    if (offhand.getCount() + appleStack.getCount() <= appleStack.getMaxCount()) {
                        clickInventory(screenSlot(slot), 0, SlotActionType.PICKUP);
                        clickOffHand = true;
                    }
                } else {
                    swapOffhand(slot);
                }
            }
        } else if (isOffhandMode("Projectile")) {
            ItemStack projectile = GardeniaInventoryUtil.getBestProjectile();
            if (projectile != null) {
                ItemStack offhand = mc.player.getOffHandStack();
                boolean shouldSwap = !offhand.isOf(Items.EGG) && !offhand.isOf(Items.SNOWBALL)
                        || offhand.getCount() < projectile.getCount();
                if (shouldSwap) {
                    swapOffhand(GardeniaInventoryUtil.getItemStackSlot(projectile));
                }
            }
        } else if (isOffhandMode("Fishing Rod")) {
            int slot = GardeniaInventoryUtil.getItemSlot(Items.FISHING_ROD);
            if (slot != -1 && !mc.player.getOffHandStack().isOf(Items.FISHING_ROD)) {
                swapOffhand(slot);
            }
        } else if (isOffhandMode("Block")) {
            ItemStack bestBlock = GardeniaInventoryUtil.getBestBlock();
            if (bestBlock != null) {
                ItemStack offhand = mc.player.getOffHandStack();
                boolean shouldSwap = !GardeniaInventoryUtil.isValidBlockStack(offhand) || offhand.getCount() < bestBlock.getCount();
                if (shouldSwap) {
                    swapOffhand(GardeniaInventoryUtil.getItemStackSlot(bestBlock));
                }
            }
        }
    }

    private void sortHotbar() {
        if (switchGoldenApple.getBoolean() && !isOffhandMode("Golden Apple")) {
            swapItem(hotbarSlot(goldenAppleSlot), Items.GOLDEN_APPLE);
        }
        if (switchBlock.getBoolean() && !isOffhandMode("Block")) {
            ItemStack bestBlock = GardeniaInventoryUtil.getBestBlock();
            int target = hotbarSlot(blockSlot);
            ItemStack current = mc.player.getInventory().getStack(target);
            if (bestBlock != null && (bestBlock.getCount() > current.getCount() || !GardeniaInventoryUtil.isValidBlockStack(current))) {
                swapItem(target, bestBlock);
            }
        }
        if (switchSword.getBoolean()) {
            ItemStack bestWeapon = bestWeapon();
            if (bestWeapon != null) {
                int target = hotbarSlot(swordSlot);
                ItemStack current = mc.player.getInventory().getStack(target);
                float currentDamage = Math.max(GardeniaInventoryUtil.getSwordDamage(current), GardeniaInventoryUtil.getAxeDamage(current));
                float bestDamage = Math.max(GardeniaInventoryUtil.getSwordDamage(bestWeapon), GardeniaInventoryUtil.getAxeDamage(bestWeapon));
                if (bestDamage > currentDamage) {
                    swapItem(target, bestWeapon);
                }
            }
        }
        if (switchPickaxe.getBoolean()) {
            swapTool(hotbarSlot(pickaxeSlot), GardeniaInventoryUtil.getBestPickaxe(), PickaxeItem.class);
        }
        if (switchAxe.getBoolean()) {
            swapTool(hotbarSlot(axeSlot), GardeniaInventoryUtil.getBestAxe(), AxeItem.class);
        }
        if (switchRod.getBoolean() && !isOffhandMode("Fishing Rod")) {
            ItemStack rod = GardeniaInventoryUtil.getFishingRod();
            if (rod != null && !(mc.player.getInventory().getStack(hotbarSlot(rodSlot)).getItem() instanceof FishingRodItem)) {
                swapItem(hotbarSlot(rodSlot), rod);
            }
        }
        if (switchBow.getBoolean()) {
            sortBow();
        }
        if (switchEnderPearl.getBoolean()) {
            swapItem(hotbarSlot(enderPearlSlot), Items.ENDER_PEARL);
        }
        if (switchWaterBucket.getBoolean()) {
            swapItem(hotbarSlot(waterBucketSlot), Items.WATER_BUCKET);
        }
        if (switchFireball.getBoolean()) {
            swapItem(hotbarSlot(fireballSlot), Items.FIRE_CHARGE);
        }
        if (keepProjectile.getBoolean() && switchProjectile.getBoolean() && !isOffhandMode("Projectile")) {
            int target = hotbarSlot(projectileSlot);
            if (GardeniaInventoryUtil.getItemCount(Items.EGG) > 0) {
                swapItem(target, Items.EGG);
            } else if (GardeniaInventoryUtil.getItemCount(Items.SNOWBALL) > 0) {
                swapItem(target, Items.SNOWBALL);
            }
        }
    }

    private void sortBow() {
        int target = hotbarSlot(bowSlot);
        ItemStack current = mc.player.getInventory().getStack(target);
        ItemStack bestBow;
        float bestScore;
        float currentScore;
        if (isBowMode("Crossbow")) {
            bestBow = GardeniaInventoryUtil.getBestCrossbow();
            bestScore = GardeniaInventoryUtil.getCrossbowScore(bestBow);
            currentScore = GardeniaInventoryUtil.getCrossbowScore(current);
        } else if (isBowMode("Power Bow")) {
            bestBow = GardeniaInventoryUtil.getBestPowerBow();
            bestScore = GardeniaInventoryUtil.getPowerBowScore(bestBow);
            currentScore = GardeniaInventoryUtil.getPowerBowScore(current);
        } else {
            bestBow = GardeniaInventoryUtil.getBestPunchBow();
            bestScore = GardeniaInventoryUtil.getPunchBowScore(bestBow);
            currentScore = GardeniaInventoryUtil.getPunchBowScore(current);
        }

        if (bestBow == null) {
            bestBow = GardeniaInventoryUtil.getBestCrossbow();
            bestScore = GardeniaInventoryUtil.getCrossbowScore(bestBow);
            currentScore = GardeniaInventoryUtil.getCrossbowScore(current);
        }
        if (bestBow == null) {
            bestBow = GardeniaInventoryUtil.getBestPowerBow();
            bestScore = GardeniaInventoryUtil.getPowerBowScore(bestBow);
            currentScore = GardeniaInventoryUtil.getPowerBowScore(current);
        }
        if (bestBow == null) {
            bestBow = GardeniaInventoryUtil.getBestPunchBow();
            bestScore = GardeniaInventoryUtil.getPunchBowScore(bestBow);
            currentScore = GardeniaInventoryUtil.getPunchBowScore(current);
        }
        if (bestBow != null && bestScore > currentScore) {
            swapItem(target, bestBow);
        }
    }

    private void swapTool(int targetSlot, ItemStack bestTool, Class<?> expectedItemType) {
        if (bestTool == null) {
            return;
        }
        ItemStack current = mc.player.getInventory().getStack(targetSlot);
        if (expectedItemType.isInstance(bestTool.getItem())
                && (GardeniaInventoryUtil.getToolScore(bestTool) > GardeniaInventoryUtil.getToolScore(current)
                || !expectedItemType.isInstance(current.getItem()))) {
            swapItem(targetSlot, bestTool);
        }
    }

    private ItemStack bestWeapon() {
        ItemStack bestSword = GardeniaInventoryUtil.getBestSword();
        ItemStack bestSharpAxe = GardeniaInventoryUtil.getBestSharpnessAxe();
        if (GardeniaInventoryUtil.getAxeDamage(bestSharpAxe) > GardeniaInventoryUtil.getSwordDamage(bestSword)) {
            return bestSharpAxe;
        }
        return bestSword;
    }

    private void throwOverflowItems() {
        if (GardeniaInventoryUtil.getBlockCountInInventory() > maxBlockSize.getInt()) {
            throwItem(GardeniaInventoryUtil.getWorstBlock());
        }
        if (GardeniaInventoryUtil.getItemCount(Items.ARROW) > maxArrowSize.getInt()) {
            throwItem(GardeniaInventoryUtil.getWorstArrow());
        }
        if (keepProjectile.getBoolean()
                && GardeniaInventoryUtil.getItemCount(Items.EGG) + GardeniaInventoryUtil.getItemCount(Items.SNOWBALL) > maxProjectileSize.getInt()) {
            throwItem(GardeniaInventoryUtil.getWorstProjectile());
        }
    }

    private void throwUselessItems() {
        if (!throwItems.getBoolean()) {
            return;
        }
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < 36; slot++) {
            slots.add(slot);
        }
        Collections.shuffle(slots);
        for (int slot : slots) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (!stack.isEmpty() && !isItemUseful(stack)) {
                throwItem(stack);
                return;
            }
        }
    }

    private boolean isItemUseful(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.isOf(Items.STICK) || stack.isOf(Items.STRING) || stack.isOf(Items.BOWL)) {
            return false;
        }
        if (GardeniaInventoryUtil.isGodItem(stack) || GardeniaInventoryUtil.isSharpnessAxe(stack)) {
            return true;
        }
        if (stack.getName().getString().contains("点击使用")) {
            return false;
        }
        if (stack.getItem() instanceof ArmorItem) {
            EquipmentSlot slot = GardeniaInventoryUtil.armorSlot(stack);
            float protection = GardeniaInventoryUtil.getProtection(stack);
            return GardeniaInventoryUtil.getCurrentArmorScore(slot) < protection
                    && GardeniaInventoryUtil.getBestArmorScore(slot) <= protection;
        }
        if (stack.getItem() instanceof SwordItem) {
            return GardeniaInventoryUtil.getBestSword() == stack;
        }
        if (stack.getItem() instanceof PickaxeItem) {
            return GardeniaInventoryUtil.getBestPickaxe() == stack;
        }
        if (stack.getItem() instanceof AxeItem && !GardeniaInventoryUtil.isSharpnessAxe(stack)) {
            return GardeniaInventoryUtil.getBestAxe() == stack;
        }
        if (stack.getItem() instanceof ShovelItem) {
            return GardeniaInventoryUtil.getBestShovel() == stack;
        }
        if (stack.getItem() instanceof HoeItem) {
            return false;
        }
        if (stack.getItem() instanceof CrossbowItem) {
            return GardeniaInventoryUtil.getBestCrossbow() == stack;
        }
        if (stack.getItem() instanceof BowItem) {
            return GardeniaInventoryUtil.getBestPunchBow() == stack
                    || GardeniaInventoryUtil.getBestPowerBow() == stack
                    || GardeniaInventoryUtil.getItemCount(Items.BOW) <= 1;
        }
        if (stack.isOf(Items.WATER_BUCKET) && GardeniaInventoryUtil.getItemCount(Items.WATER_BUCKET) > waterBucketCount.getInt()) {
            return false;
        }
        if (stack.isOf(Items.LAVA_BUCKET) && GardeniaInventoryUtil.getItemCount(Items.LAVA_BUCKET) > lavaBucketCount.getInt()) {
            return false;
        }
        if (stack.isOf(Items.COBWEB) && GardeniaInventoryUtil.getItemCount(Items.COBWEB) > cobwebCount.getInt()) {
            return false;
        }
        if (stack.getItem() instanceof FishingRodItem && GardeniaInventoryUtil.getItemCount(Items.FISHING_ROD) > 1) {
            return false;
        }
        if ((stack.isOf(Items.SNOWBALL) || stack.isOf(Items.EGG)) && !keepProjectile.getBoolean()) {
            return false;
        }
        if (stack.isOf(Items.EXPERIENCE_BOTTLE)) {
            return true;
        }
        if (stack.getItem() instanceof BlockItem && !GardeniaInventoryUtil.isValidBlockStack(stack)) {
            return false;
        }
        return GardeniaInventoryUtil.isCommonItemUseful(stack);
    }

    private void swapOffhand(int slot) {
        if (slot != -1) {
            clickInventory(screenSlot(slot), 40, SlotActionType.SWAP);
        }
    }

    private void throwItem(ItemStack item) {
        int itemSlot = GardeniaInventoryUtil.getItemStackSlot(item);
        if (itemSlot != -1 && GardeniaInventoryUtil.isItemValid(item)) {
            clickInventory(screenSlot(itemSlot), 1, SlotActionType.THROW);
        }
    }

    private void swapItem(int targetSlot, ItemStack bestItem) {
        if (bestItem == null || bestItem.isEmpty()) {
            return;
        }
        ItemStack current = mc.player.getInventory().getStack(targetSlot);
        if (!GardeniaInventoryUtil.isItemValid(current) || bestItem == current) {
            return;
        }
        int bestSlot = GardeniaInventoryUtil.getItemStackSlot(bestItem);
        if (bestSlot != -1) {
            clickInventory(screenSlot(bestSlot), targetSlot, SlotActionType.SWAP);
        }
    }

    private void swapItem(int targetSlot, Item item) {
        ItemStack current = mc.player.getInventory().getStack(targetSlot);
        if (!GardeniaInventoryUtil.isItemValid(current)) {
            return;
        }
        int bestSlot = GardeniaInventoryUtil.getItemSlot(item);
        if (bestSlot == -1) {
            return;
        }
        ItemStack bestStack = mc.player.getInventory().getStack(bestSlot);
        if (!current.isOf(item) || current.getCount() < bestStack.getCount()) {
            clickInventory(screenSlot(bestSlot), targetSlot, SlotActionType.SWAP);
        }
    }

    private void clickInventory(int slot, int button, SlotActionType actionType) {
        if (mc.player == null || mc.interactionManager == null || mc.player.playerScreenHandler == null) {
            return;
        }
        if (inventoryOnly.getBoolean() && !(mc.currentScreen instanceof InventoryScreen)) {
            return;
        }
        if (delay.getDouble() != 0.0D && System.currentTimeMillis() - lastActionTime < (long) delay.getDouble()) {
            return;
        }
        PlayerScreenHandler handler = mc.player.playerScreenHandler;
        if (silentManage.getBoolean()) {
            if (canRunMovingSilentManage() && isMoving()) {
                movingSilentAction = true;
                wasSprinting = wasSprinting || mc.player.isSprinting();
                movingSilentDelayTicks = Math.max(movingSilentDelayTicks, wasSprinting ? 4 : 2);
                mc.interactionManager.clickSlot(handler.syncId, slot, button, actionType, mc.player);
                lastActionTime = System.currentTimeMillis();
                return;
            }

            if (actionType == SlotActionType.THROW) {
                queueSilentThrow(slot, button);
                return;
            }

            suppressMovementForInventoryAction = Math.max(suppressMovementForInventoryAction, 8);
            beginSilentInventoryClick(false);
        } else if (noMove.getBoolean() && !isModuleEnabled("Scaffold")) {
            suppressMovementForInventoryAction = Math.max(suppressMovementForInventoryAction, 8);
        }

        mc.interactionManager.clickSlot(handler.syncId, slot, button, actionType, mc.player);
        lastActionTime = System.currentTimeMillis();
        inventoryOpen = true;
        if (!silentManage.getBoolean() && !inventoryOnly.getBoolean() && mc.getNetworkHandler() != null) {
            sendNoEvent(new CloseHandledScreenC2SPacket(handler.syncId));
            inventoryOpen = false;
        }
    }

    private void queueSilentThrow(int slot, int button) {
        suppressMovementForInventoryAction = Math.max(suppressMovementForInventoryAction, 12);
        pendingSilentThrowSlot = slot;
        pendingSilentThrowButton = button;
        pendingSilentThrowTicks = 1;
    }

    private boolean runPendingSilentThrow() {
        if (pendingSilentThrowSlot == -1) {
            return false;
        }
        if (isMoving()) {
            resetSilentManageState(true);
            return true;
        }
        suppressMovementForInventoryAction = Math.max(suppressMovementForInventoryAction, 12);
        if (pendingSilentThrowTicks > 0) {
            pendingSilentThrowTicks--;
            return true;
        }

        int slot = pendingSilentThrowSlot;
        int button = pendingSilentThrowButton;
        pendingSilentThrowSlot = -1;
        pendingSilentThrowButton = 0;
        beginSilentInventoryClick(true);
        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, button, SlotActionType.THROW, mc.player);
        inventoryOpen = true;
        lastActionTime = System.currentTimeMillis();
        return true;
    }

    private boolean handleMovingSilentPacket(GlobalPacketEvent event, Packet<?> packet) {
        if (!shouldUseMovingSilentQueue()) {
            return false;
        }

        if (packet instanceof ClientCommandC2SPacket command) {
            if (command.getMode() == ClientCommandC2SPacket.Mode.START_SPRINTING) {
                wasSprinting = true;
            } else if (command.getMode() == ClientCommandC2SPacket.Mode.STOP_SPRINTING) {
                wasSprinting = false;
            }
        }

        if (isPlayerInventoryPacket(packet)) {
            event.cancel();
            pendingMovingSilentPackets.offer(packet);
            movingSilentAction = true;
            movingSilentDelayTicks = Math.max(movingSilentDelayTicks, wasSprinting || mc.player.isSprinting() ? 4 : 2);
            return true;
        }
        return false;
    }

    private boolean flushMovingSilentPackets() {
        if (!silentManage.getBoolean()
                || inventoryOnly.getBoolean()
                || mc.player == null
                || mc.getNetworkHandler() == null
                || mc.interactionManager == null) {
            return false;
        }

        if (pendingMovingSilentPackets.isEmpty()) {
            movingSilentDelayTicks = 0;
            movingSilentAction = false;
            return false;
        }

        if (!mc.player.isOnGround() || mc.options.jumpKey.isPressed()) {
            pauseSprint();
            return true;
        }

        pauseSprint();
        if (wasSprinting || mc.player.isSprinting()) {
            movingSilentDelayTicks = Math.max(movingSilentDelayTicks, 4);
            wasSprinting = false;
            return true;
        }

        if (movingSilentDelayTicks > 0) {
            movingSilentDelayTicks--;
            return true;
        }

        sendingSilentInventoryPackets = true;
        try {
            Packet<?> packet;
            while ((packet = pendingMovingSilentPackets.poll()) != null) {
                sendNoEvent(packet);
            }
            sendNoEvent(new CloseHandledScreenC2SPacket(mc.player.playerScreenHandler.syncId));
        } finally {
            sendingSilentInventoryPackets = false;
            movingSilentAction = false;
            wasSprinting = false;
            movingSilentDelayTicks = 0;
            inventoryOpen = false;
        }
        return true;
    }

    private boolean shouldUseMovingSilentQueue() {
        return silentManage.getBoolean()
                && !inventoryOnly.getBoolean()
                && !sendingSilentInventoryPackets
                && mc.player != null
                && mc.player.isOnGround()
                && !mc.options.jumpKey.isPressed()
                && mc.getNetworkHandler() != null
                && mc.interactionManager != null
                && !isExternalContainerOpen()
                && isSafeForSilentManage();
    }

    private boolean canRunMovingSilentManage() {
        return silentManage.getBoolean()
                && !inventoryOnly.getBoolean()
                && isSafeForSilentManage();
    }

    private boolean shouldPauseMovingSilentSprint() {
        return silentManage.getBoolean()
                && !inventoryOnly.getBoolean()
                && (movingSilentAction || !pendingMovingSilentPackets.isEmpty() || sendingSilentInventoryPackets);
    }

    private boolean shouldSuppressSprint() {
        return suppressMovementForInventoryAction > 0
                || pendingSilentThrowSlot != -1
                || cancelNextInventoryOpenPacket
                || silentInventoryClickPrimed
                || sendingSilentInventoryPackets;
    }

    private boolean isPlayerInventoryPacket(Packet<?> packet) {
        if (mc.player == null || packet == null) {
            return false;
        }
        int inventorySyncId = mc.player.playerScreenHandler.syncId;
        if (packet instanceof ClickSlotC2SPacket clickPacket) {
            return clickPacket.getSyncId() == inventorySyncId;
        }
        if (packet instanceof CloseHandledScreenC2SPacket closePacket) {
            return closePacket.getSyncId() == inventorySyncId;
        }
        return false;
    }

    private boolean isActionPacket(Packet<?> packet) {
        return packet instanceof PlayerInteractBlockC2SPacket
                || packet instanceof PlayerInteractItemC2SPacket
                || packet instanceof PlayerInteractEntityC2SPacket
                || packet instanceof PlayerActionC2SPacket;
    }

    private boolean isSafeForSilentManage() {
        if (mc.player == null || mc.world == null) {
            return false;
        }
        return !mc.player.isTouchingWater()
                && !mc.player.isSubmergedInWater()
                && !mc.player.isInLava()
                && !mc.player.isCrawling()
                && !mc.world.getBlockState(mc.player.getBlockPos()).isOf(Blocks.SLIME_BLOCK)
                && !mc.world.getBlockState(mc.player.getBlockPos().down()).isOf(Blocks.SLIME_BLOCK)
                && !mc.world.getBlockState(mc.player.getBlockPos()).isOf(Blocks.HONEY_BLOCK)
                && !mc.world.getBlockState(mc.player.getBlockPos().down()).isOf(Blocks.HONEY_BLOCK);
    }

    private void beginSilentInventoryClick(boolean keepSprintStopped) {
        if (mc.player == null || mc.getNetworkHandler() == null) {
            return;
        }
        if (!keepSprintStopped) {
            suppressSprint = false;
        }
        cancelNextInventoryOpenPacket = true;
        silentInventoryClickPrimed = false;
        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.OPEN_INVENTORY));
    }

    private void sendSilentInventoryPackets(ClickSlotC2SPacket clickPacket) {
        boolean throwingItem = clickPacket.getActionType() == SlotActionType.THROW;
        suppressMovementForInventoryAction = Math.max(suppressMovementForInventoryAction, throwingItem ? 12 : 8);
        sendingSilentInventoryPackets = true;
        try {
            sendNoEvent(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.OPEN_INVENTORY));
            sendNoEvent(clickPacket);
            sendNoEvent(new CloseHandledScreenC2SPacket(clickPacket.getSyncId()));
            inventoryOpen = false;
        } finally {
            sendingSilentInventoryPackets = false;
        }
    }

    private void forceSilentClose() {
        if (!inventoryOpen || mc.player == null || mc.getNetworkHandler() == null) {
            return;
        }
        if (silentManage.getBoolean()) {
            cancelNextInventoryOpenPacket = false;
            silentInventoryClickPrimed = false;
            sendingSilentInventoryPackets = false;
            sendNoEvent(new CloseHandledScreenC2SPacket(mc.player.playerScreenHandler.syncId));
        }
        inventoryOpen = false;
    }

    private void resetSilentManageState(boolean closeInventory) {
        pendingSilentThrowSlot = -1;
        pendingSilentThrowButton = 0;
        pendingSilentThrowTicks = 0;
        cancelNextInventoryOpenPacket = false;
        silentInventoryClickPrimed = false;
        sendingSilentInventoryPackets = false;
        pendingMovingSilentPackets.clear();
        movingSilentAction = false;
        wasSprinting = false;
        movingSilentDelayTicks = 0;
        if (closeInventory && inventoryOpen) {
            forceSilentClose();
        } else if (!closeInventory) {
            inventoryOpen = false;
        }
    }

    private void pauseSprint() {
        if (mc.player == null || mc.options == null) {
            return;
        }
        mc.player.setSprinting(false);
        mc.options.sprintKey.setPressed(false);
    }

    private void tryResumeSprint() {
        if (mc.player == null || mc.options == null || !isMoving()) {
            return;
        }
        mc.options.sprintKey.setPressed(true);
    }

    private void resetState(boolean closeInventory) {
        suppressMovementForInventoryAction = 0;
        suppressSprintTicks = 0;
        suppressSprint = false;
        prevSuppressSprint = false;
        resprintCountdown = 0;
        clickOffHand = false;
        lastActionTime = 0L;
        replaying = false;
        resetSilentManageState(closeInventory);
    }

    private void sendNoEvent(Packet<?> packet) {
        if (packet == null || mc.getNetworkHandler() == null) {
            return;
        }
        replaying = true;
        try {
            mc.getNetworkHandler().getConnection().send(packet);
        } finally {
            replaying = false;
        }
    }

    private boolean shouldPauseForAction() {
        if (isModuleEnabled("Scaffold")) {
            return true;
        }
        KillAura killAura = ModuleManager.INSTANCE.getByClass(KillAura.class);
        return killAura != null && killAura.isEnabled() && killAura.getTarget() != null;
    }

    private boolean isExternalContainerOpen() {
        return mc.player != null
                && mc.player.currentScreenHandler != null
                && mc.player.playerScreenHandler != null
                && mc.player.currentScreenHandler.syncId != mc.player.playerScreenHandler.syncId
                && mc.currentScreen instanceof HandledScreen<?>;
    }

    private boolean isModuleEnabled(String name) {
        Module module = ModuleManager.INSTANCE.getByName(name);
        return module != null && module.isEnabled();
    }

    private boolean isMoving() {
        return mc.player != null && (mc.options.forwardKey.isPressed()
                || mc.options.backKey.isPressed()
                || mc.options.leftKey.isPressed()
                || mc.options.rightKey.isPressed()
                || Math.abs(mc.player.getVelocity().x) > 0.01D
                || Math.abs(mc.player.getVelocity().z) > 0.01D);
    }

    private boolean isOffhandMode(String mode) {
        return mode.equals(offhandItems.getString());
    }

    private boolean isBowMode(String mode) {
        return mode.equals(preferBow.getString());
    }

    private int hotbarSlot(Setting<Double> setting) {
        return Math.max(0, Math.min(8, setting.getInt() - 1));
    }

    private int screenSlot(int inventorySlot) {
        return inventorySlot < 9 ? inventorySlot + 36 : inventorySlot;
    }

    private EquipmentSlot armorSlotFromInventoryIndex(int armorIndex) {
        return switch (armorIndex) {
            case 0 -> EquipmentSlot.FEET;
            case 1 -> EquipmentSlot.LEGS;
            case 2 -> EquipmentSlot.CHEST;
            case 3 -> EquipmentSlot.HEAD;
            default -> EquipmentSlot.MAINHAND;
        };
    }

    private static Setting<Double> rangedDouble(String name, String description, double defaultValue, double min, double max, double step) {
        Setting<Double> setting = new Setting<>(name, description, defaultValue);
        setting.setRange(min, max, step);
        return setting;
    }
}
