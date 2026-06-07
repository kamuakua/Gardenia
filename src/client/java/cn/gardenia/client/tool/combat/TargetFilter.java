package cn.gardenia.client.tool.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class TargetFilter {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public void init() {}

    public boolean isPlayer(Entity entity) {
        return entity instanceof PlayerEntity;
    }

    public boolean isMonster(Entity entity) {
        return entity instanceof Monster;
    }

    public boolean isAnimal(Entity entity) {
        return entity instanceof PassiveEntity || entity instanceof AnimalEntity;
    }

    public boolean isGolem(Entity entity) {
        return entity instanceof GolemEntity;
    }

    public boolean isLiving(Entity entity) {
        return entity instanceof LivingEntity;
    }

    public boolean isHostile(Entity entity) {
        if (entity instanceof Monster) return true;
        if (entity instanceof net.minecraft.entity.mob.PiglinEntity) return true;
        if (entity instanceof net.minecraft.entity.mob.PiglinBruteEntity) return true;
        if (entity instanceof net.minecraft.entity.mob.HoglinEntity) return true;
        if (entity instanceof net.minecraft.entity.mob.ZoglinEntity) return true;
        if (entity instanceof net.minecraft.entity.mob.EndermanEntity) return true;
        if (entity instanceof net.minecraft.entity.mob.SpiderEntity) return true;
        if (entity instanceof net.minecraft.entity.mob.CaveSpiderEntity) return true;
        if (entity instanceof net.minecraft.entity.passive.WolfEntity wolf && wolf.isAttacking()) return true;
        return false;
    }

    public boolean isPassive(Entity entity) {
        return isLiving(entity) && !isHostile(entity) && !(entity instanceof PlayerEntity);
    }

    public boolean isNeutral(Entity entity) {
        return entity instanceof net.minecraft.entity.mob.EndermanEntity
                || entity instanceof net.minecraft.entity.passive.WolfEntity
                || entity instanceof net.minecraft.entity.passive.BeeEntity
                || entity instanceof net.minecraft.entity.passive.IronGolemEntity
                || entity instanceof net.minecraft.entity.mob.PiglinEntity;
    }

    public boolean isTamed(Entity entity) {
        if (entity instanceof TameableEntity tameable) {
            return tameable.isTamed();
        }
        return false;
    }

    public boolean isOwnTamed(Entity entity) {
        if (entity instanceof TameableEntity tameable) {
            return tameable.isTamed() && tameable.getOwnerUuid() != null
                    && tameable.getOwnerUuid().equals(mc.player != null ? mc.player.getUuid() : null);
        }
        return false;
    }

    public boolean sameTeam(Entity entity) {
        if (mc.player == null) return false;
        AbstractTeam playerTeam = mc.player.getScoreboardTeam();
        AbstractTeam entityTeam = entity.getScoreboardTeam();
        return playerTeam != null && entityTeam != null && playerTeam == entityTeam;
    }

    public boolean sameTeam(PlayerEntity a, PlayerEntity b) {
        AbstractTeam teamA = a.getScoreboardTeam();
        AbstractTeam teamB = b.getScoreboardTeam();
        return teamA != null && teamB != null && teamA == teamB;
    }

    public boolean inRange(Entity target, double range) {
        if (mc.player == null || target == null) return false;
        return mc.player.distanceTo(target) <= range;
    }

    public boolean inFov(Entity target, float fov) {
        if (mc.player == null || target == null) return true;
        Vec3d pos = mc.player.getEyePos();
        Vec3d targetPos = target.getPos().add(0, target.getHeight() * 0.5, 0);
        Vec3d toTarget = targetPos.subtract(pos).normalize();

        float yawRad = (float) Math.toRadians(mc.player.getYaw());
        Vec3d forward = new Vec3d(-Math.sin(yawRad), 0, Math.cos(yawRad));

        double dot = forward.dotProduct(new Vec3d(toTarget.x, 0, toTarget.z).normalize());
        double angle = Math.toDegrees(Math.acos(Math.clamp(dot, -1.0, 1.0)));

        return angle <= fov / 2.0;
    }

    public boolean hasLineOfSight(Entity target) {
        if (mc.world == null || mc.player == null || target == null) return false;
        Vec3d start = mc.player.getEyePos();
        Vec3d end = target.getPos().add(0, target.getHeight() * 0.5, 0);
        return mc.world.raycast(new RaycastContext(
                start, end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        )).getType() == net.minecraft.util.hit.HitResult.Type.MISS;
    }

    public boolean isInvisible(Entity entity) {
        return entity.isInvisible();
    }

    public boolean isDead(Entity entity) {
        return !entity.isAlive();
    }

    public boolean isVulnerable(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return false;
        return living.hurtTime <= 0;
    }

    public float getHealth(LivingEntity entity) {
        return entity.getHealth();
    }

    public float getMaxHealth(LivingEntity entity) {
        return entity.getMaxHealth();
    }

    public float getHealthPercent(LivingEntity entity) {
        return entity.getHealth() / entity.getMaxHealth();
    }

    public boolean isArmored(LivingEntity entity) {
        return entity.getArmor() > 0;
    }

    public int getArmor(LivingEntity entity) {
        return entity.getArmor();
    }
}
