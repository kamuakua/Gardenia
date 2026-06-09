package cn.gardenia.client.module.move;

import cn.gardenia.client.event.EventPhase;
import cn.gardenia.client.event.events.network.GlobalPacketEvent;
import cn.gardenia.client.event.events.player.PlayerTickEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.TridentItem;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.play.DamageTiltS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class NoSlowModule extends Module {
    public static NoSlowModule INSTANCE;

    private enum Step {
        NONE,
        CANCEL_C0F,
        SWAP_HANDS,
        USING
    }

    private static HitResult savedCrosshairTarget;

    private final Setting<Double> s12Delay = rangedDouble("S12 Delay", "Delay C0F flush after hurt", 6.0D, 0.0D, 20.0D, 1.0D);
    private final Consumer<PlayerTickEvent> tickListener = this::onTick;
    private final Consumer<GlobalPacketEvent> packetListener = this::onPacket;

    private final Queue<CommonPongC2SPacket> queuedPongs = new ConcurrentLinkedQueue<>();
    private Step step = Step.NONE;
    private int noUsingItemTicks;
    private int swapWaitTicks;
    private int hurtDelayTicks;
    private boolean swapSent;
    private boolean forcedUseKeyDown;
    private boolean replaying;
    private Hand targetHand = Hand.MAIN_HAND;

    public NoSlowModule() {
        super("NoSlow", "C0F NoSlow", Category.MOVEMENT);
        INSTANCE = this;
        addSetting(s12Delay);
    }

    @Override
    public void onEnable() {
        INSTANCE = this;
        resetState();
        hurtDelayTicks = 0;
        queuedPongs.clear();
        subscribe(PlayerTickEvent.class, tickListener);
        subscribe(GlobalPacketEvent.class, packetListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerTickEvent.class, tickListener);
        unsubscribe(GlobalPacketEvent.class, packetListener);
        release(true);
        hurtDelayTicks = 0;
    }

    public static boolean shouldPreventInputSlowdown() {
        return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.step == Step.USING && INSTANCE.isUsingSlowingItem();
    }

    public static void onStartUseItemPre(MinecraftClient minecraft) {
        if (INSTANCE == null || !INSTANCE.isBlockingInternal(minecraft)) {
            return;
        }
        savedCrosshairTarget = minecraft.crosshairTarget;
        Vec3d location = savedCrosshairTarget.getPos();
        minecraft.crosshairTarget = BlockHitResult.createMissed(location, Direction.DOWN, BlockPos.ofFloored(location));
    }

    public static void onStartUseItemPost(MinecraftClient minecraft) {
        if (savedCrosshairTarget != null) {
            minecraft.crosshairTarget = savedCrosshairTarget;
            savedCrosshairTarget = null;
        }
    }

    private void onTick(PlayerTickEvent event) {
        if (event.getPhase() != EventPhase.PRE || mc.player == null) {
            return;
        }

        if (hurtDelayTicks > 0) {
            hurtDelayTicks--;
            if (hurtDelayTicks == 0) {
                flushQueuedPongs();
            }
            if (forcedUseKeyDown && mc.options != null) {
                mc.options.useKey.setPressed(false);
            }
            return;
        }

        if (isUsingSlowingItem()) {
            if (step == Step.NONE) {
                startC0f();
            }
            if (mc.options != null && step != Step.USING) {
                mc.options.useKey.setPressed(false);
                forcedUseKeyDown = true;
            }
        }

        if (step == Step.SWAP_HANDS) {
            swapWaitTicks++;
            if (swapWaitTicks >= 20) {
                release(true);
            }
            return;
        }

        if (step != Step.USING) {
            noUsingItemTicks = 0;
            return;
        }

        if (isUsingSlowingItem()) {
            noUsingItemTicks = 0;
            mc.player.setSprinting(true);
            return;
        }

        noUsingItemTicks++;
        if (noUsingItemTicks >= 10) {
            release(true);
        }
    }

    private void onPacket(GlobalPacketEvent event) {
        if (mc.player == null || replaying) {
            return;
        }

        Packet<?> packet = event.getPacket();
        if (event.isReceive()) {
            if (isOwnVelocity(packet) || isOwnDamage(packet) || isOwnHurt(packet)) {
                onHurt();
                return;
            }

            if (step == Step.SWAP_HANDS && (packet instanceof ScreenHandlerSlotUpdateS2CPacket || packet instanceof InventoryS2CPacket)) {
                if (mc.options != null && hurtDelayTicks <= 0) {
                    mc.options.useKey.setPressed(true);
                    forcedUseKeyDown = true;
                }
                step = Step.USING;
                noUsingItemTicks = 0;
                swapWaitTicks = 0;
            }
            return;
        }

        if (!event.isSend()) {
            return;
        }

        if (packet instanceof PlayerInteractItemC2SPacket useItemPacket
                && step == Step.USING
                && swapSent
                && useItemPacket.getHand() != targetHand) {
            event.cancel();
            sendNoEvent(new PlayerInteractItemC2SPacket(targetHand, 0, mc.player.getYaw(), mc.player.getPitch()));
            return;
        }

        if (packet instanceof CommonPongC2SPacket pongPacket && step != Step.NONE) {
            event.cancel();
            queuedPongs.offer(pongPacket);
            if (step == Step.CANCEL_C0F && hurtDelayTicks <= 0) {
                sendSwap();
                step = Step.SWAP_HANDS;
                swapWaitTicks = 0;
            }
            return;
        }

        if (packet instanceof PlayerActionC2SPacket actionPacket
                && actionPacket.getAction() == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM
                && step == Step.USING) {
            release(true);
        }
    }

    private void startC0f() {
        Hand usedHand = mc.player.getActiveHand();
        targetHand = usedHand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
        step = Step.CANCEL_C0F;
        swapWaitTicks = 0;
        noUsingItemTicks = 0;
        if (mc.player.currentScreenHandler != null
                && mc.player.playerScreenHandler != null
                && mc.player.currentScreenHandler.syncId != mc.player.playerScreenHandler.syncId) {
            sendNoEvent(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
        }
    }

    private void onHurt() {
        int delay = s12Delay.getInt();
        if (delay <= 0) {
            return;
        }
        hurtDelayTicks = Math.max(hurtDelayTicks, delay);
        release(false);
        if (forcedUseKeyDown && mc.options != null) {
            mc.options.useKey.setPressed(false);
        }
    }

    private void sendSwap() {
        swapSent = true;
        sendNoEvent(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                BlockPos.ORIGIN,
                Direction.DOWN
        ));
    }

    private void release(boolean flushPongs) {
        if (swapSent) {
            sendNoEvent(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                    BlockPos.ORIGIN,
                    Direction.DOWN
            ));
        }
        if (flushPongs) {
            flushQueuedPongs();
        }
        resetState();
    }

    private void resetState() {
        step = Step.NONE;
        noUsingItemTicks = 0;
        swapWaitTicks = 0;
        swapSent = false;
        targetHand = Hand.MAIN_HAND;
        if (forcedUseKeyDown && mc.options != null) {
            mc.options.useKey.setPressed(false);
        }
        forcedUseKeyDown = false;
    }

    private void flushQueuedPongs() {
        CommonPongC2SPacket pongPacket;
        while ((pongPacket = queuedPongs.poll()) != null) {
            sendNoEvent(pongPacket);
        }
    }

    private boolean isOwnVelocity(Packet<?> packet) {
        return packet instanceof EntityVelocityUpdateS2CPacket motion
                && mc.player != null
                && motion.getEntityId() == mc.player.getId()
                && (motion.getVelocityX() != 0.0D || motion.getVelocityY() != 0.0D || motion.getVelocityZ() != 0.0D);
    }

    private boolean isOwnDamage(Packet<?> packet) {
        return packet instanceof EntityDamageS2CPacket damage
                && mc.player != null
                && damage.entityId() == mc.player.getId();
    }

    private boolean isOwnHurt(Packet<?> packet) {
        return packet instanceof DamageTiltS2CPacket hurt
                && mc.player != null
                && hurt.id() == mc.player.getId();
    }

    private boolean isSlowingUse(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        UseAction action = stack.getUseAction();
        return action == UseAction.EAT
                || action == UseAction.DRINK
                || action == UseAction.BLOCK
                || action == UseAction.BOW
                || action == UseAction.CROSSBOW
                || action == UseAction.SPEAR
                || stack.getItem() instanceof ShieldItem
                || stack.getItem() instanceof BowItem
                || stack.getItem() instanceof CrossbowItem
                || stack.getItem() instanceof TridentItem;
    }

    private boolean isUsingSlowingItem() {
        return mc.player != null
                && mc.player.isUsingItem()
                && mc.player.getItemUseTimeLeft() > 0
                && isSlowingUse(mc.player.getActiveItem())
                && allowNoSlow();
    }

    private boolean allowNoSlow() {
        if (mc.player == null) {
            return false;
        }
        ItemStack main = mc.player.getMainHandStack();
        return !(main.getItem() instanceof BowItem) && !(main.getItem() instanceof CrossbowItem);
    }

    private boolean isBlockingInternal(MinecraftClient minecraft) {
        if (minecraft == null || minecraft.player == null || minecraft.crosshairTarget == null || hurtDelayTicks > 0) {
            return false;
        }
        if (minecraft.crosshairTarget.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        for (Hand hand : Hand.values()) {
            if (isSlowingUse(minecraft.player.getStackInHand(hand))) {
                return true;
            }
        }
        return false;
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

    private static Setting<Double> rangedDouble(String name, String description, double defaultValue, double min, double max, double step) {
        Setting<Double> setting = new Setting<>(name, description, defaultValue);
        setting.setRange(min, max, step);
        return setting;
    }
}
