package cn.gardenia.client.module.combat;

import cn.gardenia.client.event.EventPhase;
import cn.gardenia.client.event.events.attack.AttackEntityEvent;
import cn.gardenia.client.event.events.movement.PlayerMotionEvent;
import cn.gardenia.client.event.events.network.GlobalPacketEvent;
import cn.gardenia.client.event.events.player.PlayerTickEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.ModuleManager;
import cn.gardenia.client.module.Setting;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BowItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class CriticalsModule extends Module {
    public static CriticalsModule INSTANCE;

    private final Setting<String> mode = new Setting<>("Mode", "Critical hit mode", "Packet", new String[]{"Packet", "Grim"});
    private final Setting<Boolean> packetGroundOnly = new Setting<>("GroundOnly", "Only packet crit while on ground", false);
    private final Setting<Integer> grimStartDelayTicks = new Setting<>("Delay", "Grim stuck delay", 0, 0, 20);
    private final Setting<Integer> grimMaxBufferedPackets = new Setting<>("Max Packets", "Maximum buffered pong packets", 10, 5, 50);

    private final Consumer<AttackEntityEvent> attackListener = this::onAttack;
    private final Consumer<PlayerTickEvent> tickListener = this::onTick;
    private final Consumer<PlayerMotionEvent> motionListener = this::onMotion;
    private final Consumer<GlobalPacketEvent> packetListener = this::onPacket;

    private int grimPhase;
    private Packet<?> delayedUsePacket;
    private float lastSpoofYaw;
    private float lastSpoofPitch;
    private boolean pendingUnstuck;
    private final Queue<Packet<?>> delayedTransactionPackets = new ConcurrentLinkedQueue<>();
    private boolean grimStuckActive;
    private int grimCycleCooldown;
    private double jumpStartY;
    private boolean backingOffTarget;
    private boolean replaying;

    public CriticalsModule() {
        super("Criticals", "刀刀暴击", Category.COMBAT);
        INSTANCE = this;
        addSetting(mode);
        addSetting(packetGroundOnly);
        addSetting(grimStartDelayTicks);
        addSetting(grimMaxBufferedPackets);
    }

    @Override
    public void onEnable() {
        INSTANCE = this;
        resetStuck();
        backingOffTarget = false;
        subscribe(AttackEntityEvent.class, attackListener);
        subscribe(PlayerTickEvent.class, tickListener);
        subscribe(PlayerMotionEvent.class, motionListener);
        subscribe(GlobalPacketEvent.class, packetListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(AttackEntityEvent.class, attackListener);
        unsubscribe(PlayerTickEvent.class, tickListener);
        unsubscribe(PlayerMotionEvent.class, motionListener);
        unsubscribe(GlobalPacketEvent.class, packetListener);
        disableStuck();
        resetStuck();
    }

    public String getSuffix() {
        return mode.getString();
    }

    public static PlayerInput handleMoveInput(PlayerInput input) {
        if (INSTANCE == null || !INSTANCE.isEnabled()) {
            return input;
        }
        return INSTANCE.onMoveInput(input);
    }

    private void onAttack(AttackEntityEvent event) {
        if (mc.player == null || mc.world == null || !event.isPre()) {
            return;
        }
        if (!(event.getTarget() instanceof LivingEntity)) {
            return;
        }
        if (!isCriticalHitAvailable() || packetGroundOnly.getBoolean() && !mc.player.isOnGround()) {
            return;
        }
        if (!isBoxEmpty(mc.player.getBoundingBox().offset(0.0D, 0.0625D, 0.0D))) {
            return;
        }
        if ("Packet".equals(mode.getString())) {
            doPacketCriticals();
        }
    }

    private void onTick(PlayerTickEvent event) {
        if (mc.player == null || mc.world == null || !event.isPre() || !"Grim".equals(mode.getString())) {
            return;
        }

        KillAura killAura = ModuleManager.INSTANCE.getByClass(KillAura.class);
        if (!mc.player.isOnGround() && killAura != null && killAura.isEnabled() && killAura.getTarget() != null) {
            mc.player.setSprinting(false);
        }

        if (mc.player.isOnGround()) {
            backingOffTarget = false;
            jumpStartY = mc.player.getY();
        } else if (isMovingBackwards()) {
            if (grimStuckActive) {
                disableStuck();
            }
            return;
        }

        if (backingOffTarget) {
            if (grimStuckActive) {
                disableStuck();
            }
            return;
        }

        if (killAura != null && killAura.isEnabled() && !mc.player.isOnGround()) {
            if (!isBoxEmpty(mc.player.getBoundingBox().offset(0.0D, -0.2D, 0.0D))) {
                if (grimStuckActive) {
                    disableStuck();
                }
                return;
            }

            if (killAura.getTarget() instanceof LivingEntity) {
                mc.player.setSprinting(false);
                if (grimStuckActive || mc.player.getVelocity().y < 0.0D) {
                    if (mc.player.getVelocity().y > 0.0D || isMovingBackwards()) {
                        if (grimStuckActive) {
                            disableStuck();
                        }
                        return;
                    }

                    if (grimCycleCooldown > 0) {
                        grimCycleCooldown--;
                    } else if (grimStuckActive) {
                        disableStuck();
                        grimCycleCooldown = 1;
                    } else {
                        enableStuck();
                        grimCycleCooldown = grimStartDelayTicks.getInt();
                    }
                } else if (grimStuckActive) {
                    disableStuck();
                }
            } else if (grimStuckActive) {
                disableStuck();
            }
        } else if (grimStuckActive) {
            disableStuck();
        }
    }

    private void onMotion(PlayerMotionEvent event) {
        if (mc.player == null || mc.world == null || !"Grim".equals(mode.getString())) {
            return;
        }
        if (!grimStuckActive && !pendingUnstuck) {
            return;
        }

        if (event.getPhase() == EventPhase.PRE) {
            if (grimStuckActive) {
                if (mc.player.getVelocity().y < 0.0D) {
                    mc.player.setVelocity(mc.player.getVelocity().x, 0.0D, mc.player.getVelocity().z);
                }
                mc.player.setSprinting(false);
            }

            if (grimPhase == 1) {
                grimPhase = 2;
                float rotationYaw = mc.player.getYaw();
                float rotationPitch = mc.player.getPitch();
                if (shouldRotate() && (lastSpoofYaw != rotationYaw || lastSpoofPitch != rotationPitch)) {
                    sendPacketNoEvent(new PlayerMoveC2SPacket.LookAndOnGround(rotationYaw, rotationPitch, mc.player.isOnGround(), mc.player.horizontalCollision));
                    flushDelayedTransactions();
                    lastSpoofYaw = rotationYaw;
                    lastSpoofPitch = rotationPitch;
                }

                if (delayedUsePacket != null) {
                    sendPacketNoEvent(delayedUsePacket);
                }
            }

            if (pendingUnstuck) {
                sendPacketNoEvent(new PlayerMoveC2SPacket.PositionAndOnGround(
                        mc.player.getX() + 1337.0D,
                        mc.player.getY(),
                        mc.player.getZ() + 1337.0D,
                        mc.player.isOnGround(),
                        mc.player.horizontalCollision
                ));
                flushDelayedTransactions();
                pendingUnstuck = false;
                grimStuckActive = false;
            }
        }
    }

    private void onPacket(GlobalPacketEvent event) {
        if (mc.player == null || mc.world == null || !"Grim".equals(mode.getString()) || replaying) {
            return;
        }

        Packet<?> packet = event.getPacket();
        if (packet instanceof PlayerPositionLookS2CPacket) {
            flushDelayedTransactions();
            grimPhase = 3;
            disableStuck();
            return;
        }

        if (!grimStuckActive || !event.isSend()) {
            return;
        }

        if (packet instanceof PlayerMoveC2SPacket) {
            event.cancel();
        } else if (packet instanceof CommonPongC2SPacket) {
            if (delayedTransactionPackets.size() < grimMaxBufferedPackets.getInt()) {
                delayedTransactionPackets.offer(packet);
            }
            event.cancel();
        } else if (packet instanceof PlayerInteractItemC2SPacket || packet instanceof PlayerActionC2SPacket) {
            delayedUsePacket = packet;
            grimPhase = 1;
            event.cancel();
        } else if (packet instanceof ClientCommandC2SPacket command && command.getMode() == ClientCommandC2SPacket.Mode.START_SPRINTING) {
            KillAura killAura = ModuleManager.INSTANCE.getByClass(KillAura.class);
            if (killAura != null && killAura.isEnabled() && killAura.getTarget() != null && !mc.player.isOnGround()) {
                event.cancel();
            }
        }
    }

    private PlayerInput onMoveInput(PlayerInput input) {
        if (!"Grim".equals(mode.getString()) || mc.player == null) {
            return input;
        }

        if (grimStuckActive) {
            return new PlayerInput(false, false, false, false, false, false, false);
        }

        if (!mc.player.isOnGround()) {
            KillAura killAura = ModuleManager.INSTANCE.getByClass(KillAura.class);
            if (killAura != null && killAura.isEnabled() && killAura.getTarget() != null) {
                mc.player.setSprinting(false);
                return new PlayerInput(input.forward(), input.backward(), input.left(), input.right(), input.jump(), input.sneak(), false);
            }
        }
        return input;
    }

    private void enableStuck() {
        grimStuckActive = true;
        grimPhase = 0;
        delayedUsePacket = null;
        lastSpoofYaw = mc.player.getYaw();
        lastSpoofPitch = mc.player.getPitch();
        pendingUnstuck = false;
    }

    private void disableStuck() {
        if (grimStuckActive) {
            if (grimPhase == 3) {
                grimStuckActive = false;
            } else {
                pendingUnstuck = true;
            }
        } else {
            flushDelayedTransactions();
            grimStuckActive = false;
        }
    }

    private void resetStuck() {
        grimStuckActive = false;
        grimPhase = 0;
        delayedUsePacket = null;
        pendingUnstuck = false;
        delayedTransactionPackets.clear();
        grimCycleCooldown = 0;
        jumpStartY = 0.0D;
        replaying = false;
    }

    private void flushDelayedTransactions() {
        while (!delayedTransactionPackets.isEmpty()) {
            Packet<?> packet = delayedTransactionPackets.poll();
            if (packet != null) {
                sendPacketNoEvent(packet);
            }
        }
    }

    private void sendPacketNoEvent(Packet<?> packet) {
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

    private boolean shouldRotate() {
        if (delayedUsePacket instanceof PlayerInteractItemC2SPacket) {
            return !(mc.player.getActiveItem().getItem() instanceof BowItem);
        }
        if (delayedUsePacket instanceof PlayerActionC2SPacket playerDigging) {
            return playerDigging.getAction() == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM
                    && mc.player.getActiveItem().getItem() instanceof BowItem;
        }
        return false;
    }

    private void doPacketCriticals() {
        if (mc.player == null || mc.getNetworkHandler() == null) {
            return;
        }

        Vec3d pos = mc.player.getPos();
        boolean ground = mc.player.isOnGround();
        boolean horizontalCollision = mc.player.horizontalCollision;
        sendPacketNoEvent(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y + 0.0625D, pos.z, false, horizontalCollision));
        sendPacketNoEvent(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y + 0.00125D, pos.z, false, horizontalCollision));
        sendPacketNoEvent(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, ground, horizontalCollision));
    }

    private boolean isCriticalHitAvailable() {
        return mc.player != null
                && mc.player.isOnGround()
                && !mc.player.isTouchingWater()
                && !mc.player.isInLava()
                && !mc.player.isClimbing()
                && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
                && !mc.player.hasVehicle();
    }

    private boolean isBoxEmpty(Box box) {
        return mc.world != null && !mc.world.getBlockCollisions(mc.player, box).iterator().hasNext();
    }

    private boolean isMovingBackwards() {
        if (mc.player == null || mc.player.getVelocity().x == 0.0D && mc.player.getVelocity().z == 0.0D) {
            return false;
        }

        float yaw = mc.player.getYaw();
        double motionYaw = Math.toDegrees(Math.atan2(mc.player.getVelocity().z, mc.player.getVelocity().x)) - 90.0D;
        double diff = Math.abs(yaw - motionYaw) % 360.0D;
        if (diff > 180.0D) {
            diff = 360.0D - diff;
        }
        return diff > 135.0D;
    }
}
