package cn.gardenia.client.module.move;

import cn.gardenia.client.event.EventPhase;
import cn.gardenia.client.event.events.movement.PlayerMotionEvent;
import cn.gardenia.client.event.events.network.GlobalPacketEvent;
import cn.gardenia.client.event.events.player.PlayerRespawnEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.tool.ToolManager;
import net.minecraft.item.BowItem;
import net.minecraft.item.EggItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SnowballItem;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.item.TridentItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.PlayerInput;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class StuckModule extends Module {
    public static StuckModule INSTANCE;

    private final Consumer<PlayerMotionEvent> motionListener = this::onMotion;
    private final Consumer<GlobalPacketEvent> packetListener = this::onPacket;
    private final Consumer<PlayerRespawnEvent> respawnListener = event -> forceDisable();

    private int stage;
    private Packet<?> delayedPacket;
    private float interceptedYaw;
    private float interceptedPitch;
    private float lastYaw;
    private float lastPitch;
    private boolean tryDisable;
    private boolean replaying;
    private final Queue<CommonPongC2SPacket> pongPackets = new ConcurrentLinkedQueue<>();

    public StuckModule() {
        super("Stuck", "Stuck in air", Category.MOVEMENT);
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        INSTANCE = this;
        reset();
        if (mc.player != null) {
            if (ToolManager.INSTANCE.ROTATION.isServerRotationActive()) {
                lastYaw = ToolManager.INSTANCE.ROTATION.getServerRotation().x;
                lastPitch = ToolManager.INSTANCE.ROTATION.getServerRotation().y;
            } else {
                lastYaw = mc.player.getYaw();
                lastPitch = mc.player.getPitch();
            }
        }
        subscribe(PlayerMotionEvent.class, motionListener);
        subscribe(GlobalPacketEvent.class, packetListener);
        subscribe(PlayerRespawnEvent.class, respawnListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerMotionEvent.class, motionListener);
        unsubscribe(GlobalPacketEvent.class, packetListener);
        unsubscribe(PlayerRespawnEvent.class, respawnListener);
        flushPongs();
        reset();
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (mc.player == null) {
            return;
        }
        if (enabled) {
            super.setEnabled(true);
        } else if (stage != 3) {
            tryDisable = true;
        } else {
            super.setEnabled(false);
        }
    }

    public static PlayerInput handleMoveInput(PlayerInput input) {
        if (INSTANCE == null || !INSTANCE.isEnabled()) {
            return input;
        }
        return new PlayerInput(false, false, false, false, false, false, false);
    }

    private void reset() {
        stage = 0;
        delayedPacket = null;
        interceptedYaw = 0.0F;
        interceptedPitch = 0.0F;
        tryDisable = false;
        replaying = false;
        pongPackets.clear();
    }

    private void onMotion(PlayerMotionEvent event) {
        if (mc.player == null || event.getPhase() != EventPhase.PRE) {
            return;
        }

        mc.player.setVelocity(0.0D, 0.0D, 0.0D);

        if (stage == 1) {
            stage = 2;
            if (!shouldRotate() || lastYaw != interceptedYaw || lastPitch != interceptedPitch) {
                sendNoEvent(new PlayerMoveC2SPacket.LookAndOnGround(
                        interceptedYaw,
                        interceptedPitch,
                        mc.player.isOnGround(),
                        mc.player.horizontalCollision
                ));
                flushPongs();
                lastYaw = interceptedYaw;
                lastPitch = interceptedPitch;
            }

            if (delayedPacket != null) {
                sendNoEvent(delayedPacket);
                delayedPacket = null;
            }
        }

        if (tryDisable) {
            sendNoEvent(new PlayerMoveC2SPacket.PositionAndOnGround(
                    mc.player.getX() + 1337.0D,
                    mc.player.getY(),
                    mc.player.getZ() + 1337.0D,
                    mc.player.isOnGround(),
                    mc.player.horizontalCollision
            ));
            flushPongs();
            tryDisable = false;
            stage = 3;
            super.setEnabled(false);
        }
    }

    private void onPacket(GlobalPacketEvent event) {
        if (mc.player == null || replaying) {
            return;
        }

        Packet<?> packet = event.getPacket();
        if (event.isSend()) {
            if (packet instanceof PlayerMoveC2SPacket) {
                event.cancel();
            } else if (packet instanceof CommonPongC2SPacket pongPacket) {
                pongPackets.offer(pongPacket);
                event.cancel();
            } else if (packet instanceof PlayerInteractItemC2SPacket
                    || packet instanceof PlayerInteractBlockC2SPacket
                    || packet instanceof PlayerActionC2SPacket) {
                delayedPacket = packet;
                stage = 1;
                interceptedYaw = ToolManager.INSTANCE.ROTATION.isServerRotationActive()
                        ? ToolManager.INSTANCE.ROTATION.getServerRotation().x
                        : mc.player.getYaw();
                interceptedPitch = ToolManager.INSTANCE.ROTATION.isServerRotationActive()
                        ? ToolManager.INSTANCE.ROTATION.getServerRotation().y
                        : mc.player.getPitch();
                event.cancel();
            }
        } else if (packet instanceof PlayerPositionLookS2CPacket) {
            flushPongs();
            stage = 3;
            super.setEnabled(false);
        }
    }

    private boolean shouldRotate() {
        if (mc.player == null || delayedPacket == null) {
            return false;
        }

        if (delayedPacket instanceof PlayerInteractItemC2SPacket useItemPacket) {
            ItemStack item = mc.player.getStackInHand(useItemPacket.getHand());
            if (isProjectile(item)) {
                return false;
            }
            Item stackItem = item.getItem();
            return stackItem != Items.MUSHROOM_STEW && !(stackItem instanceof BowItem);
        }

        if (delayedPacket instanceof PlayerInteractBlockC2SPacket) {
            return true;
        }

        return delayedPacket instanceof PlayerActionC2SPacket actionPacket
                && actionPacket.getAction() == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM
                && mc.player.getActiveItem().getItem() instanceof BowItem;
    }

    private boolean isProjectile(ItemStack item) {
        Item stackItem = item.getItem();
        return stackItem instanceof EnderPearlItem
                || stackItem instanceof SnowballItem
                || stackItem instanceof EggItem
                || stackItem instanceof SplashPotionItem
                || stackItem instanceof ExperienceBottleItem
                || stackItem instanceof TridentItem;
    }

    private void flushPongs() {
        while (!pongPackets.isEmpty()) {
            sendNoEvent(pongPackets.poll());
        }
    }

    private void sendNoEvent(Packet<?> packet) {
        if (packet == null || mc.getNetworkHandler() == null) {
            return;
        }
        replaying = true;
        try {
            mc.getNetworkHandler().sendPacket(packet);
        } finally {
            replaying = false;
        }
    }

    private void forceDisable() {
        stage = 3;
        delayedPacket = null;
        super.setEnabled(false);
    }
}
