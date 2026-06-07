package cn.gardenia.client.module.combat;

import cn.gardenia.client.event.events.network.PacketReceiveEvent;
import cn.gardenia.client.event.events.player.PlayerRespawnEvent;
import cn.gardenia.client.event.events.player.PlayerTickEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.ModuleManager;
import cn.gardenia.client.module.Setting;
import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.ints.IntIterator;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.world.GameMode;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class AntiBots extends Module {
    private static final Map<UUID, String> uuidDisplayNames = new ConcurrentHashMap<>();
    private static final Map<Integer, String> entityIdDisplayNames = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> uuids = new ConcurrentHashMap<>();
    private static final Set<Integer> ids = new HashSet<>();
    private static final Map<UUID, Long> respawnTime = new ConcurrentHashMap<>();

    private final Setting<Double> respawnTimeValue = rangedDouble("Respawn Time", "BedWars bot respawn time", 2500.0, 0.0, 10000.0, 100.0);

    private final Consumer<PacketReceiveEvent> packetListener = this::onPacket;
    private final Consumer<PlayerRespawnEvent> respawnListener = this::onRespawn;
    private final Consumer<PlayerTickEvent> tickListener = this::onTickEvent;

    public AntiBots() {
        super("AntiBots", "Prevents bots from attacking you", Category.COMBAT);
        addSetting(respawnTimeValue);
    }

    public static boolean isBedWarsBot(Entity entity) {
        AntiBots module = ModuleManager.INSTANCE.getByClass(AntiBots.class);
        if (module == null || module.respawnTimeValue.getDouble() < 1.0F) {
            return false;
        }
        Long time = respawnTime.get(entity.getUuid());
        return time != null && System.currentTimeMillis() - time < module.respawnTimeValue.getDouble();
    }

    public static boolean isBot(Entity entity) {
        AntiBots module = ModuleManager.INSTANCE.getByClass(AntiBots.class);
        return module != null
                && module.isEnabled()
                && mc.getNetworkHandler() != null
                && (ids.contains(entity.getId()) || !mc.getNetworkHandler().getPlayerUuids().contains(entity.getUuid()));
    }

    @Override
    public void onEnable() {
        subscribe(PacketReceiveEvent.class, packetListener);
        subscribe(PlayerRespawnEvent.class, respawnListener);
        subscribe(PlayerTickEvent.class, tickListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PacketReceiveEvent.class, packetListener);
        unsubscribe(PlayerRespawnEvent.class, respawnListener);
        unsubscribe(PlayerTickEvent.class, tickListener);
    }

    private void onRespawn(PlayerRespawnEvent event) {
        uuidDisplayNames.clear();
        entityIdDisplayNames.clear();
        ids.clear();
        uuids.clear();
    }

    private void onTickEvent(PlayerTickEvent event) {
        if (!event.isPre()) {
            return;
        }
        for (Map.Entry<UUID, Long> entry : uuids.entrySet()) {
            if (System.currentTimeMillis() - entry.getValue() > 500L) {
                uuids.remove(entry.getKey());
            }
        }
    }

    private void onPacket(PacketReceiveEvent event) {
        if (mc.world == null) {
            return;
        }

        Packet<?> packet = event.getPacket();
        if (packet instanceof PlayerListS2CPacket playerList) {
            if (playerList.getActions().contains(PlayerListS2CPacket.Action.ADD_PLAYER)) {
                for (PlayerListS2CPacket.Entry entry : playerList.getEntries()) {
                    GameProfile profile = entry.profile();
                    UUID id = profile.getId();
                    respawnTime.put(id, System.currentTimeMillis());

                    if (entry.displayName() != null && entry.displayName().getSiblings().isEmpty() && entry.gameMode() == GameMode.SURVIVAL) {
                        uuids.put(id, System.currentTimeMillis());
                        uuidDisplayNames.put(id, entry.displayName().getString());
                    }
                }
            }
        } else if (packet instanceof EntityAnimationS2CPacket animation) {
            Entity entity = mc.world.getEntityById(animation.getEntityId());
            if (entity != null && animation.getAnimationId() == EntityAnimationS2CPacket.SWING_MAIN_HAND) {
                respawnTime.remove(entity.getUuid());
            }
        } else if (packet instanceof EntitySpawnS2CPacket spawn) {
            if (uuids.containsKey(spawn.getUuid())) {
                String displayName = uuidDisplayNames.get(spawn.getUuid());
                entityIdDisplayNames.put(spawn.getEntityId(), displayName);
                uuids.remove(spawn.getUuid());
                ids.add(spawn.getEntityId());
            }
        } else if (packet instanceof EntitiesDestroyS2CPacket destroy) {
            IntIterator iterator = destroy.getEntityIds().iterator();
            while (iterator.hasNext()) {
                int entityId = iterator.nextInt();
                if (ids.contains(entityId)) {
                    entityIdDisplayNames.remove(entityId);
                    ids.remove(entityId);
                }
            }
        }
    }

    private static Setting<Double> rangedDouble(String name, String description, double defaultValue, double min, double max, double step) {
        Setting<Double> setting = new Setting<>(name, description, defaultValue);
        setting.setRange(min, max, step);
        return setting;
    }
}
