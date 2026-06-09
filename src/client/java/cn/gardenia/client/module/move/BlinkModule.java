package cn.gardenia.client.module.move;

import cn.gardenia.client.event.events.network.GlobalPacketEvent;
import cn.gardenia.client.event.events.player.PlayerTickEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.Setting;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class BlinkModule extends Module {
    private final Setting<Double> releaseOnDamage = rangedDouble("Release Ticks on Damage", "Packets to release after damage", 20.0D, 0.0D, 50.0D, 1.0D);
    private final Setting<Double> releaseSpeed = rangedDouble("Release Speed (Tick)", "Packets to release per tick", 10.0D, 3.0D, 20.0D, 1.0D);
    private final Setting<Double> maxTicks = rangedDouble("Max Ticks", "Maximum movement ticks behind", 20.0D, 10.0D, 200.0D, 1.0D);

    private final Consumer<PlayerTickEvent> tickListener = this::onTick;
    private final Consumer<GlobalPacketEvent> packetListener = this::onPacket;

    private final Queue<Packet<?>> packets = new ConcurrentLinkedQueue<>();
    private boolean disabling;
    private boolean replaying;
    private int shouldReleaseTicks;
    private int releasedTicks;

    public BlinkModule() {
        super("Blink", "Suspends movement packets for teleporting", Category.MOVEMENT);
        addSetting(releaseOnDamage);
        addSetting(releaseSpeed);
        addSetting(maxTicks);
    }

    @Override
    public void onEnable() {
        packets.clear();
        disabling = false;
        replaying = false;
        shouldReleaseTicks = 0;
        releasedTicks = 0;
        subscribe(PlayerTickEvent.class, tickListener);
        subscribe(GlobalPacketEvent.class, packetListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerTickEvent.class, tickListener);
        unsubscribe(GlobalPacketEvent.class, packetListener);
        flushAll();
        packets.clear();
        disabling = false;
        replaying = false;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (mc.player == null) {
            return;
        }
        if (enabled) {
            super.setEnabled(true);
        } else if (!disabling) {
            disabling = true;
        } else if (packets.isEmpty()) {
            super.setEnabled(false);
        }
    }

    public long getBlinkTicks() {
        return packets.stream().filter(packet -> packet instanceof PlayerMoveC2SPacket).count();
    }

    public String getSuffix() {
        return getBlinkTicks() + " Ticks Behind";
    }

    private void onTick(PlayerTickEvent event) {
        if (!event.isPre() || mc.player == null) {
            return;
        }

        releasedTicks = 0;
        if (mc.player.hurtTime == 10) {
            shouldReleaseTicks += releaseOnDamage.getInt();
        }

        while (releasedTicks < releaseSpeed.getInt() && shouldReleaseTicks > 0 && !packets.isEmpty()) {
            releaseTick();
            shouldReleaseTicks--;
        }

        while (releasedTicks < releaseSpeed.getInt() && getBlinkTicks() >= maxTicks.getInt() && !packets.isEmpty()) {
            releaseTick();
        }

        if (disabling) {
            while (releasedTicks < releaseSpeed.getInt() && !packets.isEmpty()) {
                releaseTick();
            }
            if (packets.isEmpty()) {
                super.setEnabled(false);
            }
        }
    }

    private void onPacket(GlobalPacketEvent event) {
        if (!event.isSend() || mc.player == null || replaying) {
            return;
        }
        event.cancel();
        packets.offer(event.getPacket());
    }

    private void releaseTick() {
        Packet<?> packet = packets.poll();
        if (packet == null) {
            return;
        }
        sendNoEvent(packet);
        if (packet instanceof PlayerMoveC2SPacket) {
            releasedTicks++;
        }
    }

    private void flushAll() {
        while (!packets.isEmpty()) {
            sendNoEvent(packets.poll());
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

    private static Setting<Double> rangedDouble(String name, String description, double defaultValue, double min, double max, double step) {
        Setting<Double> setting = new Setting<>(name, description, defaultValue);
        setting.setRange(min, max, step);
        return setting;
    }
}
