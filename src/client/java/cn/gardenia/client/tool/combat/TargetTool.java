package cn.gardenia.client.tool.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class TargetTool {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public enum SortMode {
        DISTANCE,
        HEALTH,
        HURT_TIME,
        ARMOR,
        HYBRID
    }

    public enum FilterMode {
        ALL,
        PLAYERS_ONLY,
        MOBS_ONLY,
        HOSTILE_ONLY,
        PASSIVE_ONLY
    }

    private SortMode sortMode = SortMode.DISTANCE;
    private FilterMode filterMode = FilterMode.ALL;
    private double range = 6.0;
    private boolean walls = false;
    private boolean invisible = false;
    private boolean dead = false;

    public void init() {}

    public void setSortMode(SortMode mode) { this.sortMode = mode; }
    public void setFilterMode(FilterMode mode) { this.filterMode = mode; }
    public void setRange(double range) { this.range = range; }
    public void setCheckWalls(boolean walls) { this.walls = walls; }
    public void setIncludeInvisible(boolean invisible) { this.invisible = invisible; }
    public void setIncludeDead(boolean dead) { this.dead = dead; }

    public Entity getClosest() {
        return getSorted().stream().findFirst().orElse(null);
    }

    public Entity getClosest(double range) {
        this.range = range;
        return getClosest();
    }

    public Entity getBest() {
        List<Entity> sorted = getSorted();
        if (sorted.isEmpty()) return null;

        return switch (sortMode) {
            case HEALTH -> sorted.stream()
                    .min(Comparator.comparingDouble(e -> e instanceof LivingEntity ? ((LivingEntity)e).getHealth() : 999))
                    .orElse(null);
            case HURT_TIME -> sorted.stream()
                    .min(Comparator.comparingDouble(e -> e instanceof LivingEntity ? ((LivingEntity)e).hurtTime : 999))
                    .orElse(null);
            case ARMOR -> sorted.stream()
                    .min(Comparator.comparingDouble(e -> e instanceof LivingEntity ? ((LivingEntity)e).getArmor() : 999))
                    .orElse(null);
            case HYBRID -> sorted.stream()
                    .min(Comparator.comparingDouble(this::hybridScore))
                    .orElse(null);
            default -> getClosest();
        };
    }

    public List<Entity> getTargets() {
        return getSorted();
    }

    public List<Entity> getTargets(int max) {
        return getSorted().stream().limit(max).collect(Collectors.toList());
    }

    public List<Entity> getSorted() {
        if (mc.world == null || mc.player == null) return List.of();

        return StreamSupport.stream(mc.world.getEntities().spliterator(), false)
                .filter(this::filterEntity)
                .sorted(getComparator())
                .collect(Collectors.toList());
    }

    public List<PlayerEntity> getPlayers() {
        if (mc.world == null || mc.player == null) return List.of();
        return mc.world.getPlayers().stream()
                .filter(p -> p != mc.player && p.isAlive())
                .sorted(getComparator())
                .collect(Collectors.toList());
    }

    public List<LivingEntity> getLiving() {
        if (mc.world == null || mc.player == null) return List.of();
        Box searchBox = mc.player.getBoundingBox().expand(range);
        return mc.world.getEntitiesByClass(LivingEntity.class, searchBox, e ->
                e != mc.player && filterEntity(e) && e.isAlive()
        ).stream().sorted(getComparator()).collect(Collectors.toList());
    }

    public Entity getCrosshairTarget(double range) {
        if (mc.player == null || mc.world == null) return null;
        Entity targeted = mc.targetedEntity;
        if (targeted != null && mc.player.distanceTo(targeted) <= range) {
            return targeted;
        }
        return null;
    }

    public Entity getFovTarget(float fov) {
        if (mc.player == null) return null;
        return getSorted().stream()
                .filter(e -> isInFov(e, fov))
                .findFirst().orElse(null);
    }

    private boolean filterEntity(Entity e) {
        if (e == mc.player) return false;
        if (!dead && !e.isAlive()) return false;
        if (!invisible && e.isInvisible()) return false;
        if (e instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) e;
            if (living.getHealth() <= 0) return false;
        }

        double dist = mc.player.distanceTo(e);
        if (dist > range) return false;

        if (walls && !hasLineOfSight(e)) return false;

        return switch (filterMode) {
            case PLAYERS_ONLY -> e instanceof PlayerEntity;
            case MOBS_ONLY -> e instanceof LivingEntity && !(e instanceof PlayerEntity);
            case HOSTILE_ONLY -> isHostile(e);
            case PASSIVE_ONLY -> e instanceof LivingEntity && !(e instanceof PlayerEntity) && !isHostile(e);
            case ALL -> true;
        };
    }

    private boolean isInFov(Entity entity, float fov) {
        if (mc.player == null) return true;
        Vec3d pos = mc.player.getEyePos();
        Vec3d targetPos = entity.getPos().add(0, entity.getHeight() * 0.5, 0);
        Vec3d toTarget = targetPos.subtract(pos).normalize();

        float yawRad = (float) Math.toRadians(mc.player.getYaw());
        Vec3d forward = new Vec3d(-Math.sin(yawRad), 0, Math.cos(yawRad));

        double dot = forward.dotProduct(new Vec3d(toTarget.x, 0, toTarget.z).normalize());
        double angle = Math.toDegrees(Math.acos(Math.clamp(dot, -1.0, 1.0)));

        return angle <= fov / 2.0;
    }

    private boolean hasLineOfSight(Entity target) {
        if (mc.world == null || mc.player == null) return false;
        Vec3d start = mc.player.getEyePos();
        Vec3d end = target.getPos().add(0, target.getHeight() * 0.5, 0);
        return mc.world.raycast(new RaycastContext(
                start, end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        )).getType() == net.minecraft.util.hit.HitResult.Type.MISS;
    }

    private boolean isHostile(Entity entity) {
        return entity.getType().getSpawnGroup() == net.minecraft.entity.SpawnGroup.MONSTER;
    }

    private Comparator<Entity> getComparator() {
        return switch (sortMode) {
            case HEALTH -> Comparator.comparingDouble(e -> e instanceof LivingEntity ? ((LivingEntity)e).getHealth() : 999);
            case HURT_TIME -> Comparator.comparingDouble(e -> e instanceof LivingEntity ? ((LivingEntity)e).hurtTime : 999);
            case ARMOR -> Comparator.comparingDouble(e -> e instanceof LivingEntity ? ((LivingEntity)e).getArmor() : 999);
            case HYBRID -> Comparator.comparingDouble(this::hybridScore);
            default -> Comparator.comparingDouble(e -> mc.player != null ? mc.player.distanceTo(e) : 999);
        };
    }

    private double hybridScore(Entity e) {
        if (mc.player == null) return 999;
        double dist = mc.player.distanceTo(e);
        double health = e instanceof LivingEntity ? ((LivingEntity)e).getHealth() : 20;
        return dist * 0.6 + health * 0.4;
    }
}
