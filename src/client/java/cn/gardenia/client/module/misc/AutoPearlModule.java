package cn.gardenia.client.module.misc;

import cn.gardenia.client.event.events.movement.PlayerMotionEvent;
import cn.gardenia.client.event.events.player.PlayerTickEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.Setting;
import cn.gardenia.client.tool.ToolManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.fluid.Fluids;
import net.minecraft.entity.effect.StatusEffects;

import java.util.Set;
import java.util.function.Consumer;

public class AutoPearlModule extends Module {
    private static final double PEARL_SPEED = 1.5D;
    private static final double PEARL_GRAVITY = 0.03D;
    private static final int MAX_PEARL_TICKS = 80;
    private static final double TARGET_TOLERANCE_SQ = 2.25D;
    private static final float AIM_TOLERANCE = 6.0F;
    private static final int MAX_SIMULATED_TARGETS = 768;
    private static final Set<Block> DANGER_BLOCKS = Set.of(
            Blocks.LAVA,
            Blocks.FIRE,
            Blocks.SOUL_FIRE,
            Blocks.CAMPFIRE,
            Blocks.SOUL_CAMPFIRE,
            Blocks.CACTUS,
            Blocks.MAGMA_BLOCK,
            Blocks.POWDER_SNOW,
            Blocks.SWEET_BERRY_BUSH
    );

    private final Setting<Double> fallDistance = rangedDouble("FallDistance", "Minimum fall distance", 12.0D, 3.0D, 80.0D, 1.0D);
    private final Setting<Double> searchRadius = rangedDouble("Search Radius", "Safe landing search radius", 32.0D, 8.0D, 96.0D, 1.0D);
    private final Setting<Double> cooldownTicks = rangedDouble("Cooldown Ticks", "Throw cooldown in ticks", 40.0D, 5.0D, 200.0D, 1.0D);
    private final Setting<Double> prepareTicks = rangedDouble("Prepare Ticks", "Ticks to hold rotation before throwing", 2.0D, 1.0D, 8.0D, 1.0D);
    private final Setting<Double> rotateSpeed = rangedDouble("Pearl Rotate Speed", "Rotation speed for pearl aiming", 180.0D, 30.0D, 360.0D, 5.0D);

    private final Consumer<PlayerMotionEvent> motionListener = this::onMotion;
    private final Consumer<PlayerTickEvent> tickListener = this::onTick;

    private SafeTarget target;
    private ThrowPlan pendingPlan;
    private int cooldown;
    private int preparedTicks;
    private boolean throwQueued;

    public AutoPearlModule() {
        super("AutoPearl", "Automatically throws ender pearls after long falls", Category.MISC);
        addSetting(fallDistance);
        addSetting(searchRadius);
        addSetting(cooldownTicks);
        addSetting(prepareTicks);
        addSetting(rotateSpeed);
    }

    @Override
    public void onEnable() {
        reset();
        cooldown = 0;
        subscribe(PlayerMotionEvent.class, motionListener);
        subscribe(PlayerTickEvent.class, tickListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerMotionEvent.class, motionListener);
        unsubscribe(PlayerTickEvent.class, tickListener);
        reset();
        cooldown = 0;
        ToolManager.INSTANCE.ROTATION.clearServerRotation();
    }

    private void onMotion(PlayerMotionEvent event) {
        if (!inGame() || mc.currentScreen != null) {
            reset();
            return;
        }

        if (event.isPost()) {
            runQueuedThrow();
            return;
        }

        if (!event.isPre()) {
            return;
        }

        throwQueued = false;
        ClientPlayerEntity player = mc.player;
        if (!shouldTrigger(player)) {
            reset();
            return;
        }

        ThrowPlan plan = findPearlPlan(player);
        if (plan == null) {
            reset();
            return;
        }

        if (target == null || !isTargetStillUsable(player, target)) {
            target = findSafeTarget(player);
            preparedTicks = 0;
        }
        if (target == null) {
            reset();
            return;
        }

        pendingPlan = plan;
        Vec2f wanted = target.rotation();
        ToolManager.INSTANCE.ROTATION.setServerRotation(wanted, rotateSpeed.getDouble());
        Vec2f current = ToolManager.INSTANCE.ROTATION.getServerRotation();
        event.setYaw(current.x);
        event.setPitch(current.y);

        int requiredPrepareTicks = Math.max(1, (int) Math.round(prepareTicks.getDouble()));
        if (preparedTicks < requiredPrepareTicks) {
            preparedTicks++;
            return;
        }

        if (!isAimed(current, wanted)) {
            return;
        }
        if (!isPlanStillValid(player, pendingPlan)) {
            reset();
            return;
        }
        if (!pearlPathCanReach(player, current, target)) {
            preparedTicks = 0;
            return;
        }

        throwQueued = true;
    }

    private void onTick(PlayerTickEvent event) {
        if (event.isPre() && cooldown > 0) {
            cooldown--;
        }
    }

    private void runQueuedThrow() {
        if (!throwQueued) {
            return;
        }
        throwQueued = false;

        ClientPlayerEntity player = mc.player;
        if (pendingPlan != null && target != null && shouldTrigger(player) && isPlanStillValid(player, pendingPlan)) {
            throwPearl(pendingPlan);
            cooldown = Math.max(1, (int) Math.round(cooldownTicks.getDouble()));
        }

        ToolManager.INSTANCE.ROTATION.clearServerRotation();
        reset();
    }

    private boolean shouldTrigger(ClientPlayerEntity player) {
        return player != null
                && cooldown <= 0
                && !player.isOnGround()
                && !player.isTouchingWater()
                && !player.isInLava()
                && !player.getAbilities().flying
                && !player.hasStatusEffect(StatusEffects.SLOW_FALLING)
                && !player.isCreative()
                && player.getVelocity().y < -0.05D
                && player.fallDistance >= fallDistance.getDouble();
    }

    private SafeTarget findSafeTarget(ClientPlayerEntity player) {
        int radius = Math.max(1, (int) Math.round(searchRadius.getDouble()));
        int verticalDown = Math.min(64, Math.max(12, radius));
        int verticalUp = Math.min(16, Math.max(4, radius / 2));
        BlockPos origin = player.getBlockPos();
        SafeTarget best = null;
        double bestScore = Double.MAX_VALUE;
        int simulatedTargets = 0;

        for (int ring = 0; ring <= radius; ring++) {
            for (int x = -ring; x <= ring; x++) {
                for (int z = -ring; z <= ring; z++) {
                    if (Math.max(Math.abs(x), Math.abs(z)) != ring) {
                        continue;
                    }

                    for (int y = verticalUp; y >= -verticalDown; y--) {
                        BlockPos feetPos = origin.add(x, y, z);
                        if (!isSafeStandPos(feetPos)) {
                            continue;
                        }

                        Vec3d aim = aimPoint(feetPos);
                        Vec2f rotation = rotationTo(player, aim);
                        SafeTarget candidate = new SafeTarget(feetPos.toImmutable(), aim, rotation, horizontalDistanceSq(player.getPos(), aim));
                        double score = scoreTarget(player, candidate);
                        if (best != null && score >= bestScore) {
                            continue;
                        }

                        if (!hasLineToTarget(player, aim) || !pearlPathCanReach(player, rotation, candidate)) {
                            if (++simulatedTargets >= MAX_SIMULATED_TARGETS && best != null) {
                                return best;
                            }
                            continue;
                        }

                        simulatedTargets++;
                        if (score < bestScore) {
                            best = candidate;
                            bestScore = score;
                        }
                    }
                }
            }

            if (best != null && ring * ring > best.horizontalDistanceSq() + 9.0D) {
                break;
            }
        }

        return best;
    }

    private double scoreTarget(ClientPlayerEntity player, SafeTarget target) {
        double vertical = target.aim().y - player.getY();
        double upwardPenalty = Math.max(0.0D, vertical) * 6.0D;
        double downwardPenalty = Math.max(0.0D, -vertical) * 0.35D;
        return target.horizontalDistanceSq() + upwardPenalty + downwardPenalty - target.pos().getY() * 0.01D;
    }

    private boolean isTargetStillUsable(ClientPlayerEntity player, SafeTarget target) {
        return isSafeStandPos(target.pos())
                && hasLineToTarget(player, target.aim())
                && pearlPathCanReach(player, target.rotation(), target);
    }

    private boolean isSafeStandPos(BlockPos feetPos) {
        BlockPos ground = feetPos.down();
        BlockState groundState = mc.world.getBlockState(ground);
        if (groundState.getCollisionShape(mc.world, ground).isEmpty() || !groundState.getFluidState().isEmpty()) {
            return false;
        }
        if (isDangerous(ground) || isDangerous(feetPos) || isDangerous(feetPos.up())) {
            return false;
        }
        return canStandInside(feetPos) && canStandInside(feetPos.up());
    }

    private boolean canStandInside(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        return state.getCollisionShape(mc.world, pos).isEmpty() && state.getFluidState().isEmpty();
    }

    private boolean isDangerous(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        return DANGER_BLOCKS.contains(state.getBlock()) || state.getFluidState().isOf(Fluids.LAVA);
    }

    private boolean hasLineToTarget(ClientPlayerEntity player, Vec3d target) {
        Vec3d eye = player.getEyePos();
        BlockHitResult hit = mc.world.raycast(new RaycastContext(
                eye,
                target,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                player
        ));
        return hit.getType() == HitResult.Type.MISS || hit.getPos().squaredDistanceTo(target) <= 1.0D;
    }

    private boolean pearlPathCanReach(ClientPlayerEntity player, Vec2f rotation, SafeTarget target) {
        Vec3d position = player.getEyePos();
        Vec3d motion = Vec3d.fromPolar(rotation.y, rotation.x).multiply(PEARL_SPEED);

        for (int tick = 0; tick < MAX_PEARL_TICKS; tick++) {
            Vec3d next = position.add(motion);
            BlockHitResult hit = mc.world.raycast(new RaycastContext(
                    position,
                    next,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    player
            ));
            if (hit.getType() != HitResult.Type.MISS) {
                return hit.getPos().squaredDistanceTo(target.aim()) <= TARGET_TOLERANCE_SQ;
            }
            if (next.squaredDistanceTo(target.aim()) <= TARGET_TOLERANCE_SQ) {
                return true;
            }

            position = next;
            motion = motion.multiply(0.99D).add(0.0D, -PEARL_GRAVITY, 0.0D);
        }

        return false;
    }

    private Vec3d aimPoint(BlockPos feetPos) {
        return new Vec3d(feetPos.getX() + 0.5D, feetPos.getY() + 0.15D, feetPos.getZ() + 0.5D);
    }

    private Vec2f rotationTo(ClientPlayerEntity player, Vec3d point) {
        Vec3d eye = player.getEyePos();
        double x = point.x - eye.x;
        double y = point.y - eye.y;
        double z = point.z - eye.z;
        double horizontal = Math.sqrt(x * x + z * z);
        float yaw = (float) (Math.toDegrees(Math.atan2(z, x)) - 90.0D);
        float pitch = -trajectoryAngleLow(horizontal, y, PEARL_SPEED, PEARL_GRAVITY);
        if (Float.isNaN(pitch) || Float.isInfinite(pitch)) {
            pitch = (float) -Math.toDegrees(Math.atan2(y, horizontal));
        }
        return new Vec2f(yaw, MathHelper.clamp(pitch, -90.0F, 90.0F));
    }

    private float trajectoryAngleLow(double distance, double height, double velocity, double gravity) {
        double velocitySq = velocity * velocity;
        double root = velocitySq * velocitySq - gravity * (gravity * distance * distance + 2.0D * height * velocitySq);
        if (root <= 0.0D) {
            return (float) Math.toDegrees(Math.atan2(height, distance));
        }
        return (float) Math.toDegrees(Math.atan((velocitySq - Math.sqrt(root)) / (gravity * distance)));
    }

    private ThrowPlan findPearlPlan(ClientPlayerEntity player) {
        if (player.getOffHandStack().isOf(Items.ENDER_PEARL)) {
            return new ThrowPlan(Hand.OFF_HAND, -1);
        }

        int selected = player.getInventory().selectedSlot;
        if (isPearl(player.getInventory().getStack(selected))) {
            return new ThrowPlan(Hand.MAIN_HAND, selected);
        }

        for (int hotbar = 0; hotbar < 9; hotbar++) {
            if (isPearl(player.getInventory().getStack(hotbar))) {
                return new ThrowPlan(Hand.MAIN_HAND, hotbar);
            }
        }

        return null;
    }

    private boolean isPlanStillValid(ClientPlayerEntity player, ThrowPlan plan) {
        if (player == null || plan == null) {
            return false;
        }
        if (plan.hand() == Hand.OFF_HAND) {
            return player.getOffHandStack().isOf(Items.ENDER_PEARL);
        }
        return plan.hotbarSlot() >= 0
                && plan.hotbarSlot() < 9
                && isPearl(player.getInventory().getStack(plan.hotbarSlot()));
    }

    private boolean isPearl(ItemStack stack) {
        return !stack.isEmpty() && stack.isOf(Items.ENDER_PEARL);
    }

    private void throwPearl(ThrowPlan plan) {
        if (mc.player == null || mc.interactionManager == null) {
            return;
        }

        int previous = mc.player.getInventory().selectedSlot;
        if (plan.hand() == Hand.MAIN_HAND && previous != plan.hotbarSlot()) {
            selectSlot(plan.hotbarSlot());
        }

        ActionResult result = mc.interactionManager.interactItem(mc.player, plan.hand());
        if (result.isAccepted()) {
            mc.player.swingHand(plan.hand());
        }

        if (plan.hand() == Hand.MAIN_HAND && previous != plan.hotbarSlot()) {
            selectSlot(previous);
        }
    }

    private void selectSlot(int slot) {
        if (slot < 0 || slot > 8 || mc.player == null) {
            return;
        }
        mc.player.getInventory().selectedSlot = slot;
        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }
    }

    private double horizontalDistanceSq(Vec3d from, Vec3d to) {
        double x = from.x - to.x;
        double z = from.z - to.z;
        return x * x + z * z;
    }

    private boolean inGame() {
        return mc.player != null && mc.world != null && mc.interactionManager != null && mc.getNetworkHandler() != null;
    }

    private void reset() {
        target = null;
        pendingPlan = null;
        preparedTicks = 0;
        throwQueued = false;
    }

    private boolean isAimed(Vec2f current, Vec2f target) {
        float yawDelta = Math.abs(MathHelper.wrapDegrees(target.x - current.x));
        float pitchDelta = Math.abs(target.y - current.y);
        return yawDelta <= AIM_TOLERANCE && pitchDelta <= AIM_TOLERANCE;
    }

    private static Setting<Double> rangedDouble(String name, String description, double defaultValue, double min, double max, double step) {
        Setting<Double> setting = new Setting<>(name, description, defaultValue);
        setting.setRange(min, max, step);
        return setting;
    }

    private record SafeTarget(BlockPos pos, Vec3d aim, Vec2f rotation, double horizontalDistanceSq) {
    }

    private record ThrowPlan(Hand hand, int hotbarSlot) {
    }
}
