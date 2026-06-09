package cn.gardenia.client.module.move;

import cn.gardenia.client.event.events.input.ClickEvent;
import cn.gardenia.client.event.events.movement.PlayerMotionEvent;
import cn.gardenia.client.event.events.player.PlayerTickEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.ModuleManager;
import cn.gardenia.client.module.Setting;
import cn.gardenia.client.module.combat.KillAura;
import cn.gardenia.client.module.combat.Velocity;
import cn.gardenia.client.tool.ToolManager;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public final class AutoBucketModule extends Module {
    private final Setting<Boolean> antiPause = new Setting<>("AntiPause", "Pause around aura or velocity", true);
    private final Setting<Double> minDistance = rangedDouble("MinDistance", "Minimum fall distance", 3.0D, 3.0D, 10.0D, 1.0D);
    private final Setting<Boolean> autoMlg = new Setting<>("AutoMLG", "Place water while falling", true);
    private final Setting<Boolean> antiFire = new Setting<>("AntiFire", "Place water while burning", false);
    private final Setting<Boolean> macePriority = new Setting<>("Mace Priority", "Use lower rotation priority when holding mace", true);
    private final Consumer<PlayerMotionEvent> motionListener = this::onMotion;
    private final Consumer<ClickEvent> clickListener = this::onClick;
    private final Consumer<PlayerTickEvent> tickListener = this::onTick;

    private BucketPlacementTarget target;
    private Vec2f targetRotation;
    private boolean pendingUse;
    private boolean shouldPickup;
    private boolean pickupAction;
    private int previousSlot = -1;
    private int pendingTicks;
    private int usePauseTicks;
    private boolean rotationApplied;

    public AutoBucketModule() {
        super("AutoBucket", "Auto place water when falling", Category.MOVEMENT);
        addSetting(antiPause);
        addSetting(minDistance);
        addSetting(autoMlg);
        addSetting(antiFire);
        addSetting(macePriority);
    }

    @Override
    public void onEnable() {
        resetAll();
        subscribe(PlayerMotionEvent.class, motionListener);
        subscribe(ClickEvent.class, clickListener);
        subscribe(PlayerTickEvent.class, tickListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerMotionEvent.class, motionListener);
        unsubscribe(ClickEvent.class, clickListener);
        unsubscribe(PlayerTickEvent.class, tickListener);
        restoreSlot();
        resetAll();
    }

    private void onMotion(PlayerMotionEvent event) {
        if (!event.isPre() || mc.player == null || targetRotation == null || !pendingUse) {
            return;
        }

        event.setYaw(targetRotation.x);
        event.setPitch(targetRotation.y);
        ToolManager.INSTANCE.ROTATION.setServerRotation(targetRotation, rotationPriority());
        rotationApplied = true;
    }

    private void onClick(ClickEvent event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || !pendingUse || !rotationApplied) {
            return;
        }
        if (runPendingUse(mc.player)) {
            event.cancel();
        }
    }

    private void onTick(PlayerTickEvent event) {
        if (!event.isPost() || mc.player == null || mc.world == null || mc.interactionManager == null) {
            return;
        }
        ClientPlayerEntity player = mc.player;
        if (player.age < 3) {
            return;
        }
        if (usePauseTicks > 0) {
            usePauseTicks--;
        }
        if (player.isSpectator() || isLongJumpEnabled()) {
            return;
        }
        if (player.isUsingItem()) {
            if (isPauseUseItem(player.getActiveItem())) {
                usePauseTicks = 10;
            }
            return;
        }
        if (mc.currentScreen != null) {
            return;
        }

        if (pendingUse) {
            if (rotationApplied) {
                runPendingUse(player);
            }
            return;
        }

        boolean doMlg = autoMlg.getBoolean()
                && shouldMlg(player)
                && !player.isTouchingWater()
                && !landingInWater(player);
        boolean doFire = antiFire.getBoolean()
                && player.isOnFire()
                && !player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)
                && !player.isTouchingWater()
                && !antiPauseActive(player)
                && usePauseTicks <= 0;

        if (doMlg || doFire) {
            int waterSlot = findHotbar(Items.WATER_BUCKET);
            if (waterSlot != -1) {
                scheduleWaterUse(player, waterSlot);
                return;
            }
        }

        if (shouldPickup && hasEmptyBucket()) {
            scheduleWaterPickup(player);
        }
    }

    private void scheduleWaterUse(ClientPlayerEntity player, int slot) {
        BucketPlacementTarget placementTarget = findPlacementTarget(player);
        if (placementTarget == null) {
            return;
        }
        previousSlot = player.getInventory().selectedSlot;
        target = placementTarget;
        targetRotation = rotationFor(placementTarget);
        pickupAction = false;
        pendingUse = true;
        pendingTicks = 0;
        rotationApplied = false;
        selectSlot(slot);
        shouldPickup = false;
    }

    private void scheduleWaterPickup(ClientPlayerEntity player) {
        int bucketSlot = findHotbar(Items.BUCKET);
        if (bucketSlot == -1) {
            return;
        }
        BlockPos water = findWaterSource(player);
        if (water == null) {
            return;
        }
        previousSlot = player.getInventory().selectedSlot;
        target = new BucketPlacementTarget(water, new Box(water), false);
        targetRotation = ToolManager.INSTANCE.ROTATION.calculate(Vec3d.ofCenter(water));
        pickupAction = true;
        pendingUse = true;
        pendingTicks = 0;
        rotationApplied = false;
        selectSlot(bucketSlot);
        shouldPickup = false;
    }

    private boolean runPendingUse(ClientPlayerEntity player) {
        pendingTicks++;
        if (pendingTicks > 20 || target == null || targetRotation == null) {
            restoreSlot();
            resetPending();
            return false;
        }
        BlockHitResult hit = raycast(targetRotation, pickupAction);
        if (hit == null) {
            return false;
        }
        if (pickupAction) {
            if (!hit.getBlockPos().equals(target.blockPos()) || !mc.world.getFluidState(target.blockPos()).isStill()) {
                return false;
            }
        } else if (!hit.getBlockPos().equals(target.blockPos())) {
            return false;
        }

        mc.crosshairTarget = hit;
        if (!sendUseItem(player, hit)) {
            return false;
        }
        if (!pickupAction && selectedStack().isOf(Items.BUCKET)) {
            shouldPickup = true;
        }
        restoreSlot();
        resetPending();
        return true;
    }

    private BucketPlacementTarget findPlacementTarget(ClientPlayerEntity player) {
        List<BucketPlacementTarget> targets = new ArrayList<>();
        Box predictedBox = player.getBoundingBox().offset(player.getVelocity());
        int distance = Math.max(3, minDistance.getInt());
        BlockPos playerPos = player.getBlockPos();

        for (int x = -distance; x <= distance; x++) {
            for (int z = -distance; z <= distance; z++) {
                for (int y = 0; y <= distance + 2; y++) {
                    BlockPos pos = playerPos.add(x, -y, z);
                    BlockState state = mc.world.getBlockState(pos);
                    if (!isBucketSupport(pos, state)) {
                        continue;
                    }
                    if (!mc.world.getBlockState(pos.up()).isReplaceable()) {
                        continue;
                    }
                    Box box = new Box(pos).withMaxY(pos.getY() + 3.0D);
                    if (!box.intersects(predictedBox) && player.getEyePos().distanceTo(hitVec(pos)) > 4.5D) {
                        continue;
                    }
                    Vec2f rotation = ToolManager.INSTANCE.ROTATION.calculate(hitVec(pos));
                    BlockHitResult hit = raycast(rotation, false);
                    if (hit == null || !hit.getBlockPos().equals(pos)) {
                        continue;
                    }
                    targets.add(new BucketPlacementTarget(pos.toImmutable(), box, false));
                }
            }
        }

        if (targets.isEmpty()) {
            return fallbackTarget(player, distance);
        }
        targets.sort(Comparator
                .comparingDouble((BucketPlacementTarget target) -> -target.blockPos().getY())
                .thenComparingDouble(target -> target.boundingBox().getCenter().squaredDistanceTo(predictedBox.getCenter()))
                .thenComparing(BucketPlacementTarget::pitchOnly));
        int bestY = targets.get(0).blockPos().getY();
        return targets.stream()
                .filter(target -> target.blockPos().getY() == bestY)
                .min(Comparator.comparingDouble(target -> target.boundingBox().getCenter().squaredDistanceTo(predictedBox.getCenter())))
                .orElse(targets.get(0));
    }

    private BucketPlacementTarget fallbackTarget(ClientPlayerEntity player, int distance) {
        Vec3d from = player.getEyePos();
        Vec3d to = from.add(0.0D, -Math.max(distance + 2.0D, player.fallDistance + 2.0F), 0.0D);
        BlockHitResult hit = mc.world.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) {
            return null;
        }
        BlockPos pos = hit.getBlockPos();
        if (!isBucketSupport(pos, mc.world.getBlockState(pos)) || !mc.world.getBlockState(pos.up()).isReplaceable()) {
            return null;
        }
        return new BucketPlacementTarget(pos.toImmutable(), new Box(pos).withMaxY(pos.getY() + 3.0D), false);
    }

    private BlockPos findWaterSource(ClientPlayerEntity player) {
        int radius = (int) Math.ceil(player.getWidth() + 4.5D);
        BlockPos base = player.getBlockPos();
        List<BlockPos> waters = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = base.add(x, y, z);
                    if (mc.world.getFluidState(pos).isStill()
                            && mc.world.getFluidState(pos).getFluid() == Fluids.WATER) {
                        Vec2f rotation = ToolManager.INSTANCE.ROTATION.calculate(Vec3d.ofCenter(pos));
                        BlockHitResult hit = raycast(rotation, true);
                        if (hit != null && hit.getBlockPos().equals(pos)) {
                            waters.add(pos.toImmutable());
                        }
                    }
                }
            }
        }
        waters.sort(Comparator.comparingDouble(pos -> Vec3d.ofCenter(pos).squaredDistanceTo(player.getEyePos())));
        return waters.isEmpty() ? null : waters.get(0);
    }

    private boolean shouldMlg(ClientPlayerEntity player) {
        double threshold = minDistance.getDouble();
        StatusEffectInstance jumpBoost = player.getStatusEffect(StatusEffects.JUMP_BOOST);
        if (jumpBoost != null) {
            threshold += jumpBoost.getAmplifier() + 1.0D;
        }
        return player.fallDistance > threshold;
    }

    private boolean landingInWater(ClientPlayerEntity player) {
        int distance = Math.max(3, minDistance.getInt());
        Vec3d from = player.getEyePos();
        Vec3d to = from.add(0.0D, -Math.max(distance + 2.0D, player.fallDistance + 2.0F), 0.0D);
        BlockHitResult hit = mc.world.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, player));
        return hit.getType() != HitResult.Type.MISS
                && !mc.world.getFluidState(hit.getBlockPos()).isEmpty()
                && mc.world.getFluidState(hit.getBlockPos()).getFluid() == Fluids.WATER;
    }

    private boolean antiPauseActive(ClientPlayerEntity player) {
        if (!antiPause.getBoolean()) {
            return false;
        }
        KillAura aura = ModuleManager.INSTANCE.getByClass(KillAura.class);
        if (aura != null && aura.getTarget() != null && player.distanceTo(aura.getTarget()) <= 6.0F) {
            return true;
        }
        Velocity velocity = ModuleManager.INSTANCE.getByClass(Velocity.class);
        return velocity != null && velocity.isEnabled();
    }

    private boolean isLongJumpEnabled() {
        Module longJump = ModuleManager.INSTANCE.getByName("LongJump");
        return longJump != null && longJump.isEnabled();
    }

    private Vec2f rotationFor(BucketPlacementTarget target) {
        if (target.pitchOnly()) {
            return new Vec2f(mc.player.getYaw(), 90.0F);
        }
        return ToolManager.INSTANCE.ROTATION.calculate(hitVec(target.blockPos()));
    }

    private BlockHitResult raycast(Vec2f rotation, boolean fluids) {
        Vec3d eye = mc.player.getEyePos();
        Vec3d look = Vec3d.fromPolar(rotation.y, rotation.x);
        RaycastContext.FluidHandling fluidMode = fluids ? RaycastContext.FluidHandling.SOURCE_ONLY : RaycastContext.FluidHandling.NONE;
        BlockHitResult hit = mc.world.raycast(new RaycastContext(
                eye,
                eye.add(look.multiply(4.5D)),
                RaycastContext.ShapeType.OUTLINE,
                fluidMode,
                mc.player
        ));
        return hit.getType() == HitResult.Type.MISS ? null : hit;
    }

    private boolean isBucketSupport(BlockPos pos, BlockState state) {
        return !state.isAir()
                && state.getFluidState().isEmpty()
                && !state.getCollisionShape(mc.world, pos).isEmpty();
    }

    private Vec3d hitVec(BlockPos pos) {
        return new Vec3d(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D);
    }

    private void selectSlot(int slot) {
        if (slot < 0 || slot > 8 || mc.player.getInventory().selectedSlot == slot) {
            return;
        }
        mc.player.getInventory().selectedSlot = slot;
        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }
    }

    private void restoreSlot() {
        if (previousSlot >= 0 && previousSlot < 9 && mc.player != null) {
            selectSlot(previousSlot);
        }
        previousSlot = -1;
    }

    private int findHotbar(Item item) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (!stack.isEmpty() && stack.isOf(item)) {
                return slot;
            }
        }
        return -1;
    }

    private boolean hasEmptyBucket() {
        return findHotbar(Items.BUCKET) != -1;
    }

    private ItemStack selectedStack() {
        return mc.player.getInventory().getStack(mc.player.getInventory().selectedSlot);
    }

    private boolean isPauseUseItem(ItemStack stack) {
        return !stack.isEmpty() && (stack.isOf(Items.GOLDEN_APPLE) || stack.isOf(Items.ENCHANTED_GOLDEN_APPLE));
    }

    private double rotationPriority() {
        if (macePriority.getBoolean() && isHoldingMace() && mc.player.fallDistance > minDistance.getDouble()) {
            return 494.0D;
        }
        return 497.0D;
    }

    private boolean isHoldingMace() {
        return isMace(mc.player.getMainHandStack()) || isMace(mc.player.getOffHandStack());
    }

    private boolean isMace(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Identifier id = Registries.ITEM.getId(stack.getItem());
        return "mace".equals(id.getPath());
    }

    private boolean sendUseItem(ClientPlayerEntity player, BlockHitResult hit) {
        final boolean[] used = {false};
        withRotation(player, targetRotation, () -> {
            ActionResult result = mc.interactionManager.interactBlock(player, Hand.MAIN_HAND, hit);
            if (!result.isAccepted()) {
                result = mc.interactionManager.interactItem(player, Hand.MAIN_HAND);
            }
            if (result.isAccepted()) {
                player.swingHand(Hand.MAIN_HAND);
                used[0] = true;
            }
        });
        return used[0];
    }

    private void withRotation(ClientPlayerEntity player, Vec2f rotation, Runnable action) {
        float oldYaw = player.getYaw();
        float oldPitch = player.getPitch();
        player.setYaw(rotation.x);
        player.setPitch(rotation.y);
        try {
            action.run();
        } finally {
            player.setYaw(oldYaw);
            player.setPitch(oldPitch);
        }
    }

    private void resetPending() {
        target = null;
        targetRotation = null;
        pendingUse = false;
        pickupAction = false;
        pendingTicks = 0;
        rotationApplied = false;
    }

    private void resetAll() {
        resetPending();
        shouldPickup = false;
        previousSlot = -1;
        usePauseTicks = 0;
    }

    private static Setting<Double> rangedDouble(String name, String description, double defaultValue, double min, double max, double step) {
        Setting<Double> setting = new Setting<>(name, description, defaultValue);
        setting.setRange(min, max, step);
        return setting;
    }

    private record BucketPlacementTarget(BlockPos blockPos, Box boundingBox, boolean pitchOnly) {
    }
}
