package cn.gardenia.client.module.combat;

import cn.gardenia.client.event.events.input.AttackClickEvent;
import cn.gardenia.client.event.events.player.PlayerTickEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.tool.ToolManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec2f;

import java.util.Comparator;
import java.util.function.Consumer;

public class CrystalAuraModule extends Module {
    private static final double SEARCH_RANGE = 4.0D;
    private static final double RAYCAST_RANGE = 3.0D;

    private final Consumer<PlayerTickEvent> tickListener = this::onTick;
    private final Consumer<AttackClickEvent> clickListener = this::onClick;
    private Entity targetCrystal;

    public CrystalAuraModule() {
        super("CrystalAura", "Automatically attacks end crystals", Category.COMBAT);
    }

    public boolean hasTargetCrystal() {
        return targetCrystal != null;
    }

    @Override
    public void onEnable() {
        targetCrystal = null;
        subscribe(PlayerTickEvent.class, tickListener);
        subscribe(AttackClickEvent.class, clickListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerTickEvent.class, tickListener);
        unsubscribe(AttackClickEvent.class, clickListener);
        targetCrystal = null;
    }

    private void onTick(PlayerTickEvent event) {
        if (!event.isPre() || mc.player == null || mc.world == null) {
            return;
        }

        Entity closestCrystal = findClosestCrystal();
        if (closestCrystal == null) {
            return;
        }

        Vec2f rotation = ToolManager.INSTANCE.ROTATION.calculate(closestCrystal);
        HitResult hitResult = ToolManager.INSTANCE.RAY_CAST.rayCast(rotation, RAYCAST_RANGE, 0.0F);
        if (!(hitResult instanceof EntityHitResult entityHitResult) || !entityHitResult.getEntity().equals(closestCrystal)) {
            return;
        }

        ToolManager.INSTANCE.ROTATION.setServerRotation(rotation, 180.0D);
        targetCrystal = closestCrystal;
    }

    private void onClick(AttackClickEvent event) {
        if (targetCrystal == null || mc.player == null || mc.interactionManager == null) {
            return;
        }

        HitResult hitResult = mc.crosshairTarget;
        if (!(hitResult instanceof EntityHitResult entityHitResult) || !entityHitResult.getEntity().equals(targetCrystal)) {
            return;
        }

        mc.interactionManager.attackEntity(mc.player, targetCrystal);
        mc.player.swingHand(Hand.MAIN_HAND);
        targetCrystal = null;
        event.cancel();
    }

    private Entity findClosestCrystal() {
        Box searchBox = mc.player.getBoundingBox().expand(SEARCH_RANGE);
        double maxDistanceSq = SEARCH_RANGE * SEARCH_RANGE;
        return mc.world.getEntitiesByClass(EndCrystalEntity.class, searchBox, Entity::isAlive)
                .stream()
                .filter(crystal -> mc.player.squaredDistanceTo(crystal) <= maxDistanceSq)
                .min(Comparator.comparingDouble(crystal -> mc.player.squaredDistanceTo(crystal)))
                .orElse(null);
    }
}
