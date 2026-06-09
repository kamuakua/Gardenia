package cn.gardenia.client.module.combat;

import cn.gardenia.client.event.events.player.PlayerTickEvent;
import cn.gardenia.client.event.events.render.Render3DEvent;
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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.SwordItem;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class AimAssistModule extends Module {
    private final Setting<Double> speed = rangedDouble("Speed", "Aim speed", 2.0D, 1.0D, 10.0D, 0.1D);
    private final Setting<Boolean> aimPitch = new Setting<>("Aim Pitch", "Also adjust pitch", false);
    private final Setting<Boolean> click = new Setting<>("Require Swinging", "Only aim while attack is held", true);
    private final Setting<Boolean> sticky = new Setting<>("Sticky", "Increase aim speed on selected target", false);
    private final Setting<Boolean> mouseMovement = new Setting<>("Require Mouse Movement", "Only aim while mouse moves", false);
    private final Setting<Boolean> limitItems = new Setting<>("Limit Items", "Only aim while holding a sword", false);
    private final Setting<Boolean> aimWhilstOnTarget = new Setting<>("Aim Whilst on Target", "Keep aiming while already on target", false);
    private final Setting<Double> onTargetSpeed = rangedDouble("On Target Speed", "Aim speed while crosshair is on an entity", 1.0D, 1.0D, 10.0D, 0.1D);
    private final Setting<Double> fov = rangedDouble("FOV", "Aim assist field of view", 90.0D, 0.0D, 180.0D, 1.0D);

    private final Consumer<PlayerTickEvent> tickListener = this::onTick;
    private final Consumer<Render3DEvent> renderListener = this::onRender;

    private LivingEntity target;
    private float moveYaw;
    private float movePitch;
    private double lastMouseX;
    private double lastMouseY;

    public AimAssistModule() {
        super("AimAssist", "Automatically aims at targets", Category.COMBAT);
        addSetting(speed);
        addSetting(aimPitch);
        addSetting(click);
        addSetting(sticky);
        addSetting(mouseMovement);
        addSetting(limitItems);
        addSetting(aimWhilstOnTarget);
        addSetting(onTargetSpeed);
        addSetting(fov);
    }

    @Override
    public void onEnable() {
        reset();
        subscribe(PlayerTickEvent.class, tickListener);
        subscribe(Render3DEvent.class, renderListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerTickEvent.class, tickListener);
        unsubscribe(Render3DEvent.class, renderListener);
        reset();
    }

    private void reset() {
        target = null;
        moveYaw = 0.0F;
        movePitch = 0.0F;
    }

    private void onTick(PlayerTickEvent event) {
        if (!event.isPre() || mc.player == null || mc.world == null) {
            return;
        }

        reset();
        List<LivingEntity> targets = getTargets(4.0D);
        if (targets.isEmpty()) {
            return;
        }

        target = targets.get(0);
        Vec2f needed = calculateRotation(target);
        float yawDiff = Math.abs(MathHelper.wrapDegrees(needed.x - mc.player.getYaw()));
        if (yawDiff > fov.getDouble()) {
            reset();
            return;
        }

        if (limitItems.getBoolean() && !(mc.player.getMainHandStack().getItem() instanceof SwordItem)) {
            reset();
            return;
        }

        double speedValue = speed.getDouble();
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            speedValue = onTargetSpeed.getDouble();
        }
        if (sticky.getBoolean()) {
            speedValue *= 10.0D;
        }

        double adjustedSpeed = speedValue / Math.max(1, mc.getCurrentFps()) * 100.0D;
        Vec2f delta = move(new Vec2f(mc.player.getYaw(), mc.player.getPitch()), needed, adjustedSpeed);
        moveYaw = delta.x;
        movePitch = delta.y;
    }

    private void onRender(Render3DEvent event) {
        if (mc.player == null || target == null || moveYaw == 0.0F && movePitch == 0.0F) {
            return;
        }

        double currentMouseX = mc.mouse.getX();
        double currentMouseY = mc.mouse.getY();
        boolean movingMouse = currentMouseX != lastMouseX || currentMouseY != lastMouseY;
        lastMouseX = currentMouseX;
        lastMouseY = currentMouseY;

        if (mouseMovement.getBoolean() && !movingMouse) {
            return;
        }
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY && !aimWhilstOnTarget.getBoolean()) {
            return;
        }
        if (click.getBoolean() && !mc.options.attackKey.isPressed()) {
            return;
        }

        double sensitivity = mc.options.getMouseSensitivity().getValue() * 0.6D + 0.2D;
        double gcd = sensitivity * sensitivity * sensitivity * 8.0D;
        double sensitivityStep = gcd * 0.15D;
        float fixedYaw = (float) (Math.round(moveYaw / sensitivityStep) * sensitivityStep);
        float fixedPitch = (float) (Math.round(movePitch / sensitivityStep) * sensitivityStep);

        boolean applyYaw = Math.abs(fixedYaw) >= sensitivityStep;
        boolean applyPitch = aimPitch.getBoolean() && Math.abs(fixedPitch) >= sensitivityStep;
        if (applyYaw || applyPitch) {
            mc.player.changeLookDirection(applyYaw ? fixedYaw : 0.0D, applyPitch ? fixedPitch : 0.0D);
        }
    }

    private List<LivingEntity> getTargets(double range) {
        List<LivingEntity> targets = new ArrayList<>();
        if (mc.world == null || mc.player == null) {
            return targets;
        }

        Box searchBox = mc.player.getBoundingBox().expand(range);
        for (Entity entity : mc.world.getOtherEntities(mc.player, searchBox)) {
            if (entity instanceof LivingEntity living && isValidTarget(living) && mc.player.distanceTo(living) <= range) {
                Vec2f needed = calculateRotation(living);
                float yawDiff = Math.abs(MathHelper.wrapDegrees(needed.x - mc.player.getYaw()));
                if (yawDiff <= fov.getDouble()) {
                    targets.add(living);
                }
            }
        }

        targets.sort(Comparator.comparingDouble(entity -> {
            Vec2f rotation = calculateRotation(entity);
            float yawDiff = Math.abs(MathHelper.wrapDegrees(rotation.x - mc.player.getYaw()));
            float pitchDiff = Math.abs(rotation.y - mc.player.getPitch());
            return Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
        }));
        return targets;
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity == mc.player || !entity.isAlive() || !mc.player.canSee(entity)) {
            return false;
        }
        if (AntiBots.isBot(entity) || Teams.isSameTeam(entity) || FriendManager.isFriend(entity)) {
            return false;
        }

        Target targetModule = ModuleManager.INSTANCE.getByClass(Target.class);
        if (targetModule != null) {
            return targetModule.isTarget(entity);
        }
        return entity instanceof PlayerEntity;
    }

    private Vec2f calculateRotation(Entity entity) {
        Vec3d eye = mc.player.getEyePos();
        Box box = entity.getBoundingBox();
        Vec3d targetPoint = new Vec3d(
                (box.minX + box.maxX) * 0.5D,
                box.minY + Math.max(0.0D, Math.min(mc.player.getY() - entity.getY() + mc.player.getEyeHeight(entity.getPose()), entity.getHeight() * 0.9D)),
                (box.minZ + box.maxZ) * 0.5D
        );
        return ToolManager.INSTANCE.ROTATION.calculate(eye, targetPoint);
    }

    private Vec2f move(Vec2f current, Vec2f target, double speed) {
        if (speed <= 0.0D) {
            return new Vec2f(0.0F, 0.0F);
        }
        double deltaYaw = MathHelper.wrapDegrees(target.x - current.x);
        double deltaPitch = target.y - current.y;
        double distance = Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);
        if (distance <= 0.0D) {
            return new Vec2f(0.0F, 0.0F);
        }
        double yawPart = Math.abs(deltaYaw / distance);
        double pitchPart = Math.abs(deltaPitch / distance);
        float yaw = (float) Math.max(Math.min(deltaYaw, speed * yawPart), -speed * yawPart);
        float pitch = (float) Math.max(Math.min(deltaPitch, speed * pitchPart), -speed * pitchPart);
        return new Vec2f(yaw, pitch);
    }

    private static Setting<Double> rangedDouble(String name, String description, double defaultValue, double min, double max, double step) {
        Setting<Double> setting = new Setting<>(name, description, defaultValue);
        setting.setRange(min, max, step);
        return setting;
    }
}
