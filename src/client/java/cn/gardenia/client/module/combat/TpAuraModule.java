package cn.gardenia.client.module.combat;

import cn.gardenia.client.event.events.player.PlayerTickEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.ModuleManager;
import cn.gardenia.client.module.Setting;
import cn.gardenia.client.module.misc.ClientFriendModule;
import cn.gardenia.client.module.misc.Target;
import cn.gardenia.client.module.misc.Teams;
import cn.gardenia.client.social.FriendManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class TpAuraModule extends Module {
    private final Setting<Double> range = rangedDouble("Range", "Teleport attack range", 100.0D, 3.0D, 100.0D, 1.0D);
    private final Setting<Double> cps = rangedDouble("CPS", "Attacks per second", 1.0D, 1.0D, 20.0D, 1.0D);
    private final Setting<Boolean> switchTarget = new Setting<>("Switch", "Cycle through targets", true);
    private final Setting<Double> attackDistance = rangedDouble("Attack Distance", "Distance to stand from target before attacking", 2.8D, 1.0D, 4.0D, 0.1D);
    private final Setting<Boolean> swing = new Setting<>("Swing", "Swing main hand after attacking", true);

    private final List<Entity> targets = new ArrayList<>();
    private final Consumer<PlayerTickEvent> tickListener = this::onTick;

    private long lastAttackTime;
    private int index;

    public TpAuraModule() {
        super("TpAura", "Teleports to attack targets in a long range", Category.COMBAT);
        addSetting(range);
        addSetting(cps);
        addSetting(switchTarget);
        addSetting(attackDistance);
        addSetting(swing);
    }

    @Override
    public void onEnable() {
        targets.clear();
        lastAttackTime = 0L;
        index = 0;
        subscribe(PlayerTickEvent.class, tickListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerTickEvent.class, tickListener);
        targets.clear();
    }

    public String getSuffix() {
        return targets.size() + " Targets";
    }

    private void onTick(PlayerTickEvent event) {
        if (!event.isPre() || mc.player == null || mc.world == null || mc.interactionManager == null || shouldPause()) {
            return;
        }

        updateTargets();
        if (targets.isEmpty()) {
            return;
        }

        if (index >= targets.size()) {
            index = 0;
        }

        Entity target = switchTarget.getBoolean() ? targets.get(index) : targets.get(0);
        long delay = (long) (1000.0D / Math.max(1.0D, cps.getDouble()));
        long now = System.currentTimeMillis();
        if (now - lastAttackTime < delay) {
            return;
        }

        attack(target);
        lastAttackTime = now;
        if (switchTarget.getBoolean()) {
            index++;
        }
    }

    private boolean shouldPause() {
        Module scaffold = ModuleManager.INSTANCE.getByName("Scaffold");
        Module blink = ModuleManager.INSTANCE.getByName("Blink");
        CrystalAuraModule crystalAura = ModuleManager.INSTANCE.getByClass(CrystalAuraModule.class);
        return scaffold != null && scaffold.isEnabled()
                || blink != null && blink.isEnabled()
                || crystalAura != null && crystalAura.hasTargetCrystal();
    }

    private void updateTargets() {
        targets.clear();
        double currentRange = range.getDouble();
        double rangeSq = currentRange * currentRange;
        Box searchBox = mc.player.getBoundingBox().expand(currentRange);

        targets.addAll(mc.world.getOtherEntities(
                mc.player,
                searchBox,
                entity -> isValidTarget(entity) && mc.player.squaredDistanceTo(entity) <= rangeSq
        ));
        targets.sort(Comparator.comparingDouble(entity -> mc.player.squaredDistanceTo(entity)));
    }

    private boolean isValidTarget(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }

        return entity != mc.player
                && entity.isAlive()
                && living.isAlive()
                && !entity.isSpectator()
                && !AntiBots.isBot(entity)
                && !AntiBots.isBedWarsBot(entity)
                && isTargetEnabled(entity)
                && !Teams.isSameTeam(entity)
                && !FriendManager.isFriend(entity)
                && !ClientFriendModule.isUser(entity);
    }

    private boolean isTargetEnabled(Entity entity) {
        Target targetModule = ModuleManager.INSTANCE.getByClass(Target.class);
        return targetModule == null || targetModule.isTarget(entity);
    }

    private void attack(Entity target) {
        Vec3d original = mc.player.getPos();
        Vec3d targetPos = target.getPos();
        Vec3d direction = original.subtract(targetPos);
        if (direction.lengthSquared() < 0.01D) {
            direction = new Vec3d(1.0D, 0.0D, 0.0D);
        }

        Vec3d attackPos = targetPos.add(direction.normalize().multiply(attackDistance.getDouble()));
        sendPosition(attackPos.x, target.getY(), attackPos.z);
        mc.interactionManager.attackEntity(mc.player, target);
        if (swing.getBoolean()) {
            mc.player.swingHand(Hand.MAIN_HAND);
        }
        sendPosition(original.x, original.y, original.z);
    }

    private void sendPosition(double x, double y, double z) {
        if (mc.getNetworkHandler() == null || mc.player == null) {
            return;
        }
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                x,
                y,
                z,
                mc.player.isOnGround(),
                mc.player.horizontalCollision
        ));
    }

    private static Setting<Double> rangedDouble(String name, String description, double defaultValue, double min, double max, double step) {
        Setting<Double> setting = new Setting<>(name, description, defaultValue);
        setting.setRange(min, max, step);
        return setting;
    }
}
