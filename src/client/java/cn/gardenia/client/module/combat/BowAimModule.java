package cn.gardenia.client.module.combat;

import cn.gardenia.client.event.events.player.PlayerTickEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.ModuleManager;
import cn.gardenia.client.module.Setting;
import cn.gardenia.client.module.misc.Target;
import cn.gardenia.client.module.misc.Teams;
import cn.gardenia.client.social.FriendManager;
import cn.gardenia.client.tool.ToolManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BowItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.function.Consumer;

public class BowAimModule extends Module {
    private final Setting<Double> range = rangedDouble("Range", "Bow aim range", 60.0D, 1.0D, 120.0D, 1.0D);
    private final Setting<Double> rotateSpeed = rangedDouble("Rotation Speed", "Rotation speed", 180.0D, 1.0D, 180.0D, 1.0D);
    private final Consumer<PlayerTickEvent> tickListener = this::onTick;

    private LivingEntity target;

    public BowAimModule() {
        super("BowAim", "Aims and releases fully charged bow shots", Category.COMBAT);
        addSetting(range);
        addSetting(rotateSpeed);
    }

    @Override
    public void onEnable() {
        target = null;
        subscribe(PlayerTickEvent.class, tickListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerTickEvent.class, tickListener);
        target = null;
    }

    private void onTick(PlayerTickEvent event) {
        if (!event.isPre() || mc.player == null || mc.world == null || mc.interactionManager == null) {
            return;
        }
        if (!(mc.player.getActiveItem().getItem() instanceof BowItem) || !mc.player.isUsingItem()) {
            target = null;
            return;
        }

        target = findTarget();
        if (target == null) {
            return;
        }

        Vec2f rotation = getBowRotation(target);
        ToolManager.INSTANCE.ROTATION.setServerRotation(rotation, rotateSpeed.getDouble());

        if (isBowFullyCharged() && mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN));
            mc.interactionManager.stopUsingItem(mc.player);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    private LivingEntity findTarget() {
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        double maxRange = range.getDouble();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living) || entity == mc.player || !entity.isAlive() || entity.isSpectator()) {
                continue;
            }
            if (AntiBots.isBot(entity) || Teams.isSameTeam(entity) || FriendManager.isFriend(entity)) {
                continue;
            }
            Target targetModule = ModuleManager.INSTANCE.getByClass(Target.class);
            if (targetModule != null && !targetModule.isTarget(entity)) {
                continue;
            }

            double distance = mc.player.distanceTo(entity);
            if (distance > maxRange || !canSee(living)) {
                continue;
            }
            if (distance < bestDistance) {
                best = living;
                bestDistance = distance;
            }
        }

        return best;
    }

    private boolean canSee(LivingEntity entity) {
        Vec3d from = mc.player.getEyePos();
        Vec3d to = entity.getBoundingBox().getCenter();
        HitResult result = mc.world.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        return result.getType() == HitResult.Type.MISS || result.getPos().squaredDistanceTo(to) < 0.25D;
    }

    private Vec2f getBowRotation(LivingEntity entity) {
        Vec3d from = mc.player.getEyePos();
        Vec3d velocity = entity.getVelocity();
        Vec3d targetPos = entity.getBoundingBox().getCenter();
        double distance = from.distanceTo(targetPos);
        double ticks = MathHelper.clamp(distance / 3.0D, 0.0D, 20.0D);
        Vec3d predicted = targetPos.add(velocity.multiply(ticks));
        double dx = predicted.x - from.x;
        double dz = predicted.z - from.z;
        double dy = predicted.y - from.y;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
        float pitch = getProjectilePitch(horizontal, dy, 3.0D, 0.05D);
        return new Vec2f(yaw, pitch);
    }

    private float getProjectilePitch(double horizontal, double vertical, double velocity, double gravity) {
        double velocitySq = velocity * velocity;
        double root = velocitySq * velocitySq - gravity * (gravity * horizontal * horizontal + 2.0D * vertical * velocitySq);
        if (root < 0.0D) {
            return (float) -Math.toDegrees(Math.atan2(vertical, horizontal));
        }
        return (float) -Math.toDegrees(Math.atan((velocitySq - Math.sqrt(root)) / (gravity * horizontal)));
    }

    private boolean isBowFullyCharged() {
        int useTicks = mc.player.getItemUseTime();
        float charge = useTicks / 20.0F;
        charge = (charge * charge + charge * 2.0F) / 3.0F;
        return charge >= 1.0F;
    }

    private static Setting<Double> rangedDouble(String name, String description, double defaultValue, double min, double max, double step) {
        Setting<Double> setting = new Setting<>(name, description, defaultValue);
        setting.setRange(min, max, step);
        return setting;
    }
}
