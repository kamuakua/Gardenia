package cn.gardenia.client.module.move;

import cn.gardenia.client.event.EventPhase;
import cn.gardenia.client.event.events.movement.PlayerMotionEvent;
import cn.gardenia.client.event.events.network.GlobalPacketEvent;
import cn.gardenia.client.event.events.player.PlayerRespawnEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class GrimFlyModule extends Module {
    private final Queue<Packet<?>> delayedPackets = new ConcurrentLinkedQueue<>();
    private final Consumer<PlayerMotionEvent> motionListener = this::onMotion;
    private final Consumer<GlobalPacketEvent> packetListener = this::onPacket;
    private final Consumer<PlayerRespawnEvent> respawnListener = event -> {
        if (isEnabled()) {
            setEnabled(false);
        }
        resetState(true);
    };

    private boolean intercepting;
    private boolean shouldStartFallFlying;
    private int ticksUntilFallFlying;
    private boolean replaying;

    public GrimFlyModule() {
        super("GrimFly", "Attempts to desync brief keepalive timing after knockback", Category.MOVEMENT);
    }

    @Override
    public void onEnable() {
        resetState(false);
        subscribe(PlayerMotionEvent.class, motionListener);
        subscribe(GlobalPacketEvent.class, packetListener);
        subscribe(PlayerRespawnEvent.class, respawnListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerMotionEvent.class, motionListener);
        unsubscribe(GlobalPacketEvent.class, packetListener);
        unsubscribe(PlayerRespawnEvent.class, respawnListener);
        flushDelayedPackets();
        resetState(false);
    }

    private void onMotion(PlayerMotionEvent event) {
        if (event.getPhase() != EventPhase.PRE || !shouldStartFallFlying || mc.player == null) {
            return;
        }

        ticksUntilFallFlying++;
        if (ticksUntilFallFlying < 8) {
            return;
        }

        sendNoEvent(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        shouldStartFallFlying = false;
        ticksUntilFallFlying = 0;
    }

    private void onPacket(GlobalPacketEvent event) {
        if (mc.player == null || replaying) {
            return;
        }

        Packet<?> packet = event.getPacket();
        if (event.isSend()) {
            if (intercepting && packet instanceof CommonPongC2SPacket) {
                event.cancel();
                if (delayedPackets.isEmpty()) {
                    shouldStartFallFlying = true;
                    ticksUntilFallFlying = 0;
                }
                delayedPackets.offer(packet);
                if (delayedPackets.size() > 200) {
                    flushAndStopIntercepting();
                }
                return;
            }

            if (packet instanceof PlayerInteractEntityC2SPacket && intercepting && !delayedPackets.isEmpty()) {
                flushAndStopIntercepting();
            }
            return;
        }

        if (event.isReceive()) {
            if (packet instanceof PlayerPositionLookS2CPacket) {
                if (intercepting && !delayedPackets.isEmpty()) {
                    flushAndStopIntercepting();
                }
                return;
            }

            if (packet instanceof EntityVelocityUpdateS2CPacket velocityPacket && velocityPacket.getEntityId() == mc.player.getId()) {
                if (intercepting || !delayedPackets.isEmpty()) {
                    return;
                }
                intercepting = true;
                shouldStartFallFlying = false;
                ticksUntilFallFlying = 0;
                event.cancel();
            }
        }
    }

    private void flushAndStopIntercepting() {
        flushDelayedPackets();
        intercepting = false;
        shouldStartFallFlying = false;
        ticksUntilFallFlying = 0;
    }

    private void flushDelayedPackets() {
        while (!delayedPackets.isEmpty()) {
            sendNoEvent(delayedPackets.poll());
        }
    }

    private void resetState(boolean clearQueue) {
        intercepting = false;
        shouldStartFallFlying = false;
        ticksUntilFallFlying = 0;
        if (clearQueue) {
            delayedPackets.clear();
        }
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
}
