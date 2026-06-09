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
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Consumer;

public class AutoThrowModule extends Module {
    private static final double PROJECTILE_SPEED = 0.6D;
    private static final double PROJECTILE_GRAVITY = 0.006D;

    private final Setting<Double> minDistance = rangedDouble("Min Distance", "Minimum target distance", 5.0D, 3.0D, 30.0D, 1.0D);
    private final Setting<Double> maxDistance = rangedDouble("Max Distance", "Maximum target distance", 10.0D, 3.0D, 30.0D, 1.0D);
    private final Setting<Double> delay = rangedDouble("Delay", "Throw delay", 500.0D, 50.0D, 2000.0D, 50.0D);

    private final Consumer<PlayerTickEvent> tickListener = this::onTick;

    private int rotationTicks;
    private int swapBack = -1;
    private long lastThrowTime;
    private ThrowPlan pendingPlan;

    public AutoThrowModule() {
        super("AutoThrow", "Automatically throw snowballs and eggs", Category.COMBAT);
        addSetting(minDistance);
        addSetting(maxDistance);
        addSetting(delay);
    }

    @Override
    public void onEnable() {
        reset();
        subscribe(PlayerTickEvent.class, tickListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerTickEvent.class, tickListener);
        restoreSlot();
        reset();
    }

    private void reset() {
        rotationTicks = 0;
        swapBack = -1;
        pendingPlan = null;
        lastThrowTime = 0L;
    }

    private void onTick(PlayerTickEvent event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            return;
        }

        if (event.isPost()) {
            restoreSlot();
            return;
        }
        if (!event.isPre()) {
            return;
        }

        ThrowPlan plan = findThrowPlan();
        if (plan == null) {
            pendingPlan = null;
            rotationTicks = 0;
            return;
        }

        if (rotationTicks > 0) {
            rotationTicks--;
            if (rotationTicks == 0 && pendingPlan != null) {
                throwFromPlan(pendingPlan);
                pendingPlan = null;
            }
            return;
        }

        Optional<LivingEntity> target = getTarget();
        long now = System.currentTimeMillis();
        if (target.isPresent() && now - lastThrowTime >= delay.getDouble() && canRotate(plan.hand())) {
            Vec2f rotation = getRotationToEntity(target.get());
            ToolManager.INSTANCE.ROTATION.setServerRotation(rotation, 180.0D);
            rotationTicks = 2;
            pendingPlan = plan;
            lastThrowTime = now;
        }
    }

    private void throwFromPlan(ThrowPlan plan) {
        if (mc.player == null || mc.interactionManager == null) {
            return;
        }
        if (plan.hand() == Hand.MAIN_HAND) {
            int originalHotbar = mc.player.getInventory().selectedSlot;
            if (originalHotbar != plan.hotbarSlot()) {
                mc.player.getInventory().selectedSlot = plan.hotbarSlot();
                if (mc.getNetworkHandler() != null) {
                    mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(plan.hotbarSlot()));
                }
                swapBack = originalHotbar;
            }
        }
        mc.interactionManager.interactItem(mc.player, plan.hand());
        mc.player.swingHand(plan.hand());
    }

    private void restoreSlot() {
        if (swapBack != -1 && mc.player != null) {
            mc.player.getInventory().selectedSlot = swapBack;
            if (mc.getNetworkHandler() != null) {
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(swapBack));
            }
            swapBack = -1;
        }
    }

    private ThrowPlan findThrowPlan() {
        if (mc.player == null) {
            return null;
        }
        if (isThrowable(mc.player.getOffHandStack())) {
            return new ThrowPlan(Hand.OFF_HAND, -1);
        }

        int selected = mc.player.getInventory().selectedSlot;
        if (isThrowable(mc.player.getInventory().getStack(selected))) {
            return new ThrowPlan(Hand.MAIN_HAND, selected);
        }

        for (int hotbar = 0; hotbar < 9; hotbar++) {
            if (isThrowable(mc.player.getInventory().getStack(hotbar))) {
                return new ThrowPlan(Hand.MAIN_HAND, hotbar);
            }
        }
        return null;
    }

    private boolean canRotate(Hand hand) {
        if (mc.player.isUsingItem()) {
            return false;
        }
        ItemStack stack = hand == Hand.MAIN_HAND ? mc.player.getMainHandStack() : mc.player.getOffHandStack();
        if (stack.isEmpty()) {
            return true;
        }
        Item item = stack.getItem();
        return item != Items.ENDER_PEARL && !(item instanceof BowItem) && !(item instanceof PotionItem);
    }

    private Optional<LivingEntity> getTarget() {
        Target targetModule = ModuleManager.INSTANCE.getByClass(Target.class);
        return mc.world.getPlayers().stream()
                .filter(player -> player != mc.player)
                .filter(LivingEntity::isAlive)
                .filter(player -> !player.isSpectator())
                .filter(player -> !AntiBots.isBot(player))
                .filter(player -> targetModule == null || targetModule.isTarget(player))
                .filter(player -> !Teams.isSameTeam(player))
                .filter(player -> !FriendManager.isFriend(player))
                .filter(mc.player::canSee)
                .filter(player -> !player.isInvisibleTo(mc.player))
                .filter(player -> {
                    double distance = getHorizontalDistance(player);
                    return distance <= maxDistance.getDouble() && distance >= minDistance.getDouble();
                })
                .map(player -> (LivingEntity) player)
                .min(Comparator.comparingDouble(player -> mc.player.distanceTo(player)));
    }

    private Vec2f getRotationToEntity(LivingEntity target) {
        Vec3d velocity = target.getVelocity();
        double targetX = target.getX();
        double targetY = target.getY() + target.getHeight() * 0.6D;
        double targetZ = target.getZ();

        double time = 0.0D;
        for (int i = 0; i < 3; i++) {
            double predictX = targetX + velocity.x * time;
            double predictZ = targetZ + velocity.z * time;
            double dx = predictX - mc.player.getX();
            double dz = predictZ - mc.player.getZ();
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            time = horizontal / PROJECTILE_SPEED;
        }

        double predictX = targetX + velocity.x * time;
        double predictY = targetY + velocity.y * time;
        double predictZ = targetZ + velocity.z * time;

        double x = predictX - mc.player.getX();
        double z = predictZ - mc.player.getZ();
        double h = predictY - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double horizontal = Math.sqrt(x * x + z * z);

        float yaw = (float) (Math.toDegrees(Math.atan2(z, x)) - 90.0D);
        float pitch = -getTrajectoryAngleLow((float) horizontal, (float) h, (float) PROJECTILE_SPEED, (float) PROJECTILE_GRAVITY);
        return new Vec2f(yaw, MathHelper.clamp(pitch, -90.0F, 90.0F));
    }

    private float getTrajectoryAngleLow(float distance, float height, float velocity, float gravity) {
        float velocitySq = velocity * velocity;
        float under = velocitySq * velocitySq - gravity * (gravity * distance * distance + 2.0F * height * velocitySq);
        if (under <= 0.0F) {
            return (float) Math.toDegrees(Math.atan2(height, distance));
        }
        return (float) Math.toDegrees(Math.atan((velocitySq - Math.sqrt(under)) / (gravity * distance)));
    }

    private double getHorizontalDistance(LivingEntity entity) {
        double dx = entity.getX() - mc.player.getX();
        double dz = entity.getZ() - mc.player.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private boolean isThrowable(ItemStack stack) {
        return !stack.isEmpty() && (stack.isOf(Items.EGG) || stack.isOf(Items.SNOWBALL));
    }

    private static Setting<Double> rangedDouble(String name, String description, double defaultValue, double min, double max, double step) {
        Setting<Double> setting = new Setting<>(name, description, defaultValue);
        setting.setRange(min, max, step);
        return setting;
    }

    private record ThrowPlan(Hand hand, int hotbarSlot) {
    }
}
