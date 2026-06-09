package cn.gardenia.client.module.misc;

import cn.gardenia.client.event.events.attack.AttackEntityEvent;
import cn.gardenia.client.event.events.network.GlobalPacketEvent;
import cn.gardenia.client.event.events.player.PlayerRespawnEvent;
import cn.gardenia.client.event.events.player.PlayerTickEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.Setting;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;

import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

public class KillSayModule extends Module {
    private static final String EXCLUDED_PREFIX = "CIT-";
    private static final String[] MESSAGES = {
            "你知道吗Gardenia Client是无敌的",
            "你知道吗Gardenia是麦迪",
            "你知道吗Gardenia是牛逼",
            "你知道吗我是无敌的",
            "艾露你个非无能干嘛"
    };

    private final Setting<Double> delay = rangedDouble("Delay", "Delay before sending kill message", 1000.0D, 0.0D, 15000.0D, 100.0D);

    private final Consumer<AttackEntityEvent> attackListener = this::onAttack;
    private final Consumer<GlobalPacketEvent> packetListener = this::onPacket;
    private final Consumer<PlayerTickEvent> tickListener = this::onTick;
    private final Consumer<PlayerRespawnEvent> respawnListener = event -> clearState();

    private final Set<UUID> attackedPlayers = new CopyOnWriteArraySet<>();
    private final Queue<String> messageQueue = new ConcurrentLinkedQueue<>();
    private final Random random = new Random();
    private long lastMessageTime;

    public KillSayModule() {
        super("KillSay", "Automatically sends a message after a kill", Category.MISC);
        addSetting(delay);
    }

    @Override
    public void onEnable() {
        clearState();
        subscribe(AttackEntityEvent.class, attackListener);
        subscribe(GlobalPacketEvent.class, packetListener);
        subscribe(PlayerTickEvent.class, tickListener);
        subscribe(PlayerRespawnEvent.class, respawnListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(AttackEntityEvent.class, attackListener);
        unsubscribe(GlobalPacketEvent.class, packetListener);
        unsubscribe(PlayerTickEvent.class, tickListener);
        unsubscribe(PlayerRespawnEvent.class, respawnListener);
        clearState();
    }

    private void onAttack(AttackEntityEvent event) {
        if (!event.isPre() || mc.player == null) {
            return;
        }
        if (event.getTarget() instanceof PlayerEntity player && player != mc.player && !isExcluded(player.getGameProfile().getName())) {
            attackedPlayers.add(player.getUuid());
        }
    }

    private void onPacket(GlobalPacketEvent event) {
        if (!event.isReceive() || mc.player == null || mc.world == null || mc.getNetworkHandler() == null) {
            return;
        }
        if (event.getPacket() instanceof PlayerRemoveS2CPacket packet) {
            queueKillMessages(packet);
        }
    }

    private void onTick(PlayerTickEvent event) {
        if (!event.isPre() || mc.player == null || mc.getNetworkHandler() == null) {
            return;
        }
        if (mc.player.age < 10) {
            clearState();
            return;
        }
        long now = System.currentTimeMillis();
        if (!messageQueue.isEmpty() && now - lastMessageTime >= delay.getDouble()) {
            mc.getNetworkHandler().sendChatMessage(messageQueue.poll());
            lastMessageTime = now;
        }
    }

    private void queueKillMessages(PlayerRemoveS2CPacket packet) {
        for (UUID uuid : packet.profileIds()) {
            if (!attackedPlayers.remove(uuid)) {
                continue;
            }

            PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(uuid);
            if (entry == null) {
                continue;
            }

            String playerName = entry.getProfile().getName();
            if (!isExcluded(playerName)) {
                messageQueue.offer(MESSAGES[random.nextInt(MESSAGES.length)]);
            }
        }
    }

    private boolean isExcluded(String playerName) {
        return playerName != null && playerName.startsWith(EXCLUDED_PREFIX);
    }

    private void clearState() {
        attackedPlayers.clear();
        messageQueue.clear();
        lastMessageTime = System.currentTimeMillis();
    }

    private static Setting<Double> rangedDouble(String name, String description, double defaultValue, double min, double max, double step) {
        Setting<Double> setting = new Setting<>(name, description, defaultValue);
        setting.setRange(min, max, step);
        return setting;
    }
}
