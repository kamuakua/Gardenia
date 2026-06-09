package cn.gardenia.client.module.combat;

import cn.gardenia.client.event.events.network.GlobalPacketEvent;
import cn.gardenia.client.event.events.player.PlayerRespawnEvent;
import cn.gardenia.client.event.events.player.PlayerTickEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.ModuleManager;
import cn.gardenia.client.module.Setting;
import cn.gardenia.client.module.misc.Teams;
import cn.gardenia.client.module.move.LongJumpModule;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class NewBackTrackModule extends Module {
    private final Setting<Double> delay = rangedDouble("Delay(ms)", "Packet release delay", 500.0D, 0.0D, 10000.0D, 1.0D);
    private final Setting<Double> maxDistance = rangedDouble("Max Range", "Release if server position moves too far", 3.0D, 0.0D, 10.0D, 0.01D);
    private final Setting<Double> range = rangedDouble("Range", "Target detection range", 3.0D, 0.0D, 10.0D, 0.01D);
    private final Setting<Boolean> onlyKillAura = new Setting<>("OnlyKillaura", "Only work while KillAura is enabled", true);
    private final Setting<Boolean> noScaffold = new Setting<>("No Scaffold", "Disable while Scaffold is enabled", true);

    private final Queue<PacketSnapshot> packets = new ConcurrentLinkedQueue<>();
    private final Map<Integer, Vec3d> serverPositions = new ConcurrentHashMap<>();
    private final Consumer<PlayerTickEvent> tickListener = this::onTick;
    private final Consumer<GlobalPacketEvent> packetListener = this::onPacket;
    private final Consumer<PlayerRespawnEvent> respawnListener = event -> {
        clear();
        alink = false;
    };

    private boolean working;
    private boolean foundTarget;
    private boolean alink;
    private boolean replaying;

    public NewBackTrackModule() {
        super("NewBackTrack", "Delays target movement packets", Category.COMBAT);
        addSetting(delay);
        addSetting(maxDistance);
        addSetting(range);
        addSetting(onlyKillAura);
        addSetting(noScaffold);
    }

    @Override
    public void onEnable() {
        working = false;
        foundTarget = false;
        alink = false;
        replaying = false;
        packets.clear();
        serverPositions.clear();
        subscribe(PlayerTickEvent.class, tickListener);
        subscribe(GlobalPacketEvent.class, packetListener);
        subscribe(PlayerRespawnEvent.class, respawnListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerTickEvent.class, tickListener);
        unsubscribe(GlobalPacketEvent.class, packetListener);
        unsubscribe(PlayerRespawnEvent.class, respawnListener);
        clear();
        alink = false;
        replaying = false;
    }

    public boolean isActive() {
        return working && !packets.isEmpty();
    }

    public boolean isBacktracking() {
        return working || foundTarget;
    }

    private void onTick(PlayerTickEvent event) {
        if (!event.isPre() || mc.player == null || mc.world == null) {
            return;
        }

        releasePackets();

        if (onlyKillAura.getBoolean()) {
            KillAura killAura = ModuleManager.INSTANCE.getByClass(KillAura.class);
            if (killAura == null || !killAura.isEnabled()) {
                clear();
                return;
            }
        }

        if (noScaffold.getBoolean() && isModuleEnabled("Scaffold")) {
            clear();
            return;
        }

        double maxSq = range.getDouble() * range.getDouble();
        boolean found = false;
        Box box = mc.player.getBoundingBox().expand(range.getDouble());
        for (PlayerEntity player : mc.world.getEntitiesByClass(PlayerEntity.class, box, this::isSelectableTarget)) {
            if (mc.player.squaredDistanceTo(player) <= maxSq) {
                found = true;
                break;
            }
        }

        foundTarget = found;
        working = foundTarget;
    }

    private void onPacket(GlobalPacketEvent event) {
        if (mc.player == null || mc.world == null || replaying || !event.isReceive()) {
            return;
        }

        if (!working || isLongJumpEnabled() || alink) {
            clear();
            return;
        }

        Packet<?> packet = event.getPacket();
        Velocity velocity = ModuleManager.INSTANCE.getByClass(Velocity.class);
        if (velocity != null && velocity.shouldStopBacktrack()) {
            clear();
            alink = true;
            return;
        }

        if (packet instanceof HealthUpdateS2CPacket health && health.getHealth() <= 0.0F) {
            clear();
            return;
        }

        if (isResetPacket(packet)) {
            clear();
            return;
        }

        if (updateServerPosition(packet) && shouldReleaseByDistance(packet)) {
            clear();
            return;
        }

        if (shouldDelay(packet)) {
            packets.offer(new PacketSnapshot(packet, System.currentTimeMillis()));
            event.cancel();
        }
    }

    private void releasePackets() {
        if (mc.player == null || mc.getNetworkHandler() == null) {
            return;
        }

        long now = System.currentTimeMillis();
        while (true) {
            PacketSnapshot snapshot = packets.peek();
            if (snapshot == null || now - snapshot.time < delay.getDouble()) {
                break;
            }

            packets.poll();
            updateServerPosOnRelease(snapshot.packet);
            handleReleasedPacket(snapshot.packet);
        }

        if (packets.isEmpty()) {
            serverPositions.clear();
        }
    }

    private boolean updateServerPosition(Packet<?> packet) {
        Entity targetEntity = null;
        Vec3d newServerPos = null;

        if (packet instanceof EntityPositionSyncS2CPacket syncPacket) {
            targetEntity = mc.world.getEntityById(syncPacket.id());
            if (isServerTrackedPlayer(targetEntity)) {
                newServerPos = syncPacket.values().position();
            }
        } else if (packet instanceof EntityPositionS2CPacket positionPacket) {
            targetEntity = mc.world.getEntityById(positionPacket.entityId());
            if (isServerTrackedPlayer(targetEntity)) {
                newServerPos = positionPacket.change().position();
            }
        } else if (packet instanceof EntityS2CPacket movePacket) {
            targetEntity = movePacket.getEntity(mc.world);
            if (isServerTrackedPlayer(targetEntity) && movePacket.isPositionChanged()) {
                Vec3d current = serverPositions.getOrDefault(targetEntity.getId(), targetEntity.getPos());
                newServerPos = current.add(
                        movePacket.getDeltaX() / 4096.0D,
                        movePacket.getDeltaY() / 4096.0D,
                        movePacket.getDeltaZ() / 4096.0D
                );
            }
        }

        if (targetEntity == null || newServerPos == null) {
            return false;
        }
        serverPositions.put(targetEntity.getId(), newServerPos);
        return true;
    }

    private boolean shouldReleaseByDistance(Packet<?> packet) {
        if (mc.player == null) {
            return false;
        }

        int id = entityId(packet);
        if (id == -1) {
            return false;
        }

        Vec3d serverPos = serverPositions.get(id);
        if (serverPos == null) {
            return false;
        }

        Vec3d eyePos = mc.player.getEyePos();
        if (eyePos.distanceTo(serverPos) <= maxDistance.getDouble()) {
            return false;
        }

        Entity entity = mc.world.getEntityById(id);
        if (entity != null && eyePos.distanceTo(entity.getPos()) <= maxDistance.getDouble()) {
            return true;
        }

        serverPositions.remove(id);
        return false;
    }

    private boolean shouldDelay(Packet<?> packet) {
        return packet instanceof PlayerPositionLookS2CPacket
                || packet instanceof EntityS2CPacket
                || packet instanceof EntityPositionS2CPacket
                || packet instanceof EntityPositionSyncS2CPacket
                || packet instanceof CommonPingS2CPacket
                || packet instanceof EntityVelocityUpdateS2CPacket;
    }

    private boolean isResetPacket(Packet<?> packet) {
        return packet instanceof PlayerRespawnS2CPacket
                || packet instanceof GameJoinS2CPacket
                || packet instanceof PlayerPositionLookS2CPacket;
    }

    private void clear() {
        PacketSnapshot snapshot;
        while ((snapshot = packets.poll()) != null) {
            handleReleasedPacket(snapshot.packet);
        }
        serverPositions.clear();
        foundTarget = false;
        working = false;
    }

    private void updateServerPosOnRelease(Packet<?> packet) {
        if (mc.world == null) {
            return;
        }

        Entity targetEntity = null;
        Vec3d newServerPos = null;
        if (packet instanceof EntityPositionSyncS2CPacket syncPacket) {
            targetEntity = mc.world.getEntityById(syncPacket.id());
            if (targetEntity instanceof PlayerEntity) {
                newServerPos = syncPacket.values().position();
            }
        } else if (packet instanceof EntityPositionS2CPacket positionPacket) {
            targetEntity = mc.world.getEntityById(positionPacket.entityId());
            if (targetEntity instanceof PlayerEntity) {
                newServerPos = positionPacket.change().position();
            }
        } else if (packet instanceof EntityS2CPacket movePacket) {
            targetEntity = movePacket.getEntity(mc.world);
            if (targetEntity instanceof PlayerEntity && movePacket.isPositionChanged()) {
                newServerPos = targetEntity.getPos().add(
                        movePacket.getDeltaX() / 4096.0D,
                        movePacket.getDeltaY() / 4096.0D,
                        movePacket.getDeltaZ() / 4096.0D
                );
            }
        }

        if (targetEntity != null && newServerPos != null && serverPositions.containsKey(targetEntity.getId())) {
            serverPositions.put(targetEntity.getId(), newServerPos);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void handleReleasedPacket(Packet<?> packet) {
        if (packet == null || mc.getNetworkHandler() == null) {
            return;
        }
        replaying = true;
        try {
            ((Packet) packet).apply((PacketListener) mc.getNetworkHandler());
        } finally {
            replaying = false;
        }
    }

    private int entityId(Packet<?> packet) {
        if (packet instanceof EntityPositionSyncS2CPacket syncPacket) {
            return syncPacket.id();
        }
        if (packet instanceof EntityPositionS2CPacket positionPacket) {
            return positionPacket.entityId();
        }
        if (packet instanceof EntityS2CPacket movePacket) {
            Entity entity = movePacket.getEntity(mc.world);
            return entity == null ? -1 : entity.getId();
        }
        return -1;
    }

    private boolean isServerTrackedPlayer(Entity entity) {
        return entity instanceof PlayerEntity && entity != mc.player && !Teams.isSameTeam(entity);
    }

    private boolean isSelectableTarget(PlayerEntity player) {
        return player != mc.player
                && player.isAlive()
                && !player.isSpectator()
                && !AntiBots.isBot(player)
                && !AntiBots.isBedWarsBot(player)
                && !Teams.isSameTeam(player);
    }

    private boolean isLongJumpEnabled() {
        LongJumpModule module = ModuleManager.INSTANCE.getByClass(LongJumpModule.class);
        return module != null && module.isEnabled();
    }

    private boolean isModuleEnabled(String name) {
        Module module = ModuleManager.INSTANCE.getByName(name);
        return module != null && module.isEnabled();
    }

    private static Setting<Double> rangedDouble(String name, String description, double defaultValue, double min, double max, double step) {
        Setting<Double> setting = new Setting<>(name, description, defaultValue);
        setting.setRange(min, max, step);
        return setting;
    }

    private record PacketSnapshot(Packet<?> packet, long time) {
    }
}
