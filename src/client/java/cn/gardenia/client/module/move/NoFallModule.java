package cn.gardenia.client.module.move;

import cn.gardenia.client.event.events.input.ClickEvent;
import cn.gardenia.client.event.events.movement.PlayerMotionEvent;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.Setting;
import cn.gardenia.client.tool.ToolManager;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.shape.VoxelShape;

import java.util.function.Consumer;

public class NoFallModule extends Module {
    private static final long PLACE_DELAY_MS = 50L;

    private final Setting<String> fallDistanceMode = new Setting<>("FallDistance Mode", "Fall distance mode", "Calc", new String[]{"Calc", "Custom"});
    private final Setting<Double> fallDistanceValue = rangedDouble("FallDistance", "Custom fall distance", 10.0D, 2.0D, 100.0D, 1.0D);
    private final Setting<Double> rotationSpeed = rangedDouble("Rotation Speed", "Water place rotation speed", 180.0D, 1.0D, 180.0D, 1.0D);
    private final Setting<Boolean> retrieve = new Setting<>("Retrieve Water", "Pick water back up after landing", true);
    private final Consumer<PlayerMotionEvent> motionListener = this::onMotion;
    private final Consumer<ClickEvent> clickListener = this::onClick;

    private boolean retrieveFlag;
    private boolean retrievePending;
    private boolean rotating;
    private boolean placeWater;
    private int timeout;
    private int originalSlot = -1;
    private long lastPlaceAttempt;
    private BlockPos above;
    private Vec2f targetRotation;

    public NoFallModule() {
        super("NoFall", "Prevents fall damage with water", Category.MOVEMENT);
        addSetting(fallDistanceMode);
        addSetting(fallDistanceValue);
        addSetting(rotationSpeed);
        addSetting(retrieve);
    }

    @Override
    public void onEnable() {
        resetState(true);
        lastPlaceAttempt = 0L;
        subscribe(PlayerMotionEvent.class, motionListener);
        subscribe(ClickEvent.class, clickListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerMotionEvent.class, motionListener);
        unsubscribe(ClickEvent.class, clickListener);
        resetState(true);
    }

    private void onMotion(PlayerMotionEvent event) {
        if (!event.isPre() || mc.player == null || mc.world == null) {
            return;
        }

        if (retrieveFlag && mc.player.isOnGround()) {
            retrievePending = true;
            if (above != null) {
                targetRotation = ToolManager.INSTANCE.ROTATION.calculateBlock(above, Direction.UP);
            }
        }

        if (rotating || placeWater || retrievePending) {
            Vec2f current = targetRotation == null ? new Vec2f(mc.player.getYaw(), 90.0F) : targetRotation;
            event.setYaw(current.x);
            event.setPitch(current.y);
            ToolManager.INSTANCE.ROTATION.setServerRotation(current, rotationSpeed.getDouble());
        }

        if (rotating) {
            if (shouldPlaceNow()) {
                placeWater = true;
            }
            if (timeout > 0) {
                timeout--;
            } else {
                resetState(true);
            }
            return;
        }

        if (isFalling()) {
            tryPrepareWater();
        }
    }

    private void onClick(ClickEvent event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            return;
        }

        if (placeWater) {
            long now = System.currentTimeMillis();
            if (now - lastPlaceAttempt < PLACE_DELAY_MS) {
                return;
            }
            lastPlaceAttempt = now;
            placeWater = false;

            HitResult hitResult = getRaycastResult();
            if (hitResult instanceof BlockHitResult blockHit) {
                Direction side = blockHit.getSide();
                boolean lowHeight = mc.player.fallDistance >= 2.0F && mc.player.fallDistance < 4.0F;
                BlockPos hitPos = blockHit.getBlockPos();
                if (side == Direction.UP || (lowHeight && side != Direction.DOWN)) {
                    above = hitPos.up();
                    useItem(blockHit);
                    retrieveFlag = retrieve.getBoolean();
                    rotating = false;
                    event.cancel();
                    return;
                }
            }
            rotating = false;
            return;
        }

        if (retrievePending && above != null) {
            HitResult hitResult = getRaycastResult();
            if (hitResult instanceof BlockHitResult blockHit) {
                BlockPos hitAbove = blockHit.getBlockPos().up();
                if (hitAbove.equals(above)) {
                    useItem(blockHit);
                    retrievePending = false;
                    retrieveFlag = false;
                    above = null;
                    rotating = false;
                    if (originalSlot != -1) {
                        mc.player.getInventory().selectedSlot = originalSlot;
                    }
                    originalSlot = -1;
                    event.cancel();
                }
            }
        }
    }

    private boolean isFalling() {
        if (mc.player == null || mc.world == null || mc.player.isGliding()) {
            return false;
        }

        if ("Custom".equals(fallDistanceMode.getString())) {
            return mc.player.fallDistance > fallDistanceValue.getDouble();
        }
        return (((mc.player.fallDistance - 3.0F) / 2.0F) + 3.5F) > mc.player.getHealth() / 3.0F;
    }

    private void tryPrepareWater() {
        int waterSlot = findHotbar(Items.WATER_BUCKET);
        if (waterSlot == -1) {
            return;
        }

        float fallDistance = mc.player.fallDistance;
        boolean lowHeight = fallDistance >= 2.0F && fallDistance < 4.0F;
        double groundCheckMultiplier = lowHeight ? 1.5D : 2.0D;
        double motionY = mc.player.getVelocity().y;
        if (!isOnGround(motionY * groundCheckMultiplier) && !(lowHeight && isOnGround(motionY * 1.2D))) {
            return;
        }

        if (originalSlot == -1) {
            originalSlot = mc.player.getInventory().selectedSlot;
        }
        mc.player.getInventory().selectedSlot = waterSlot;
        BlockPos below = mc.player.getBlockPos().down();
        targetRotation = ToolManager.INSTANCE.ROTATION.calculateBlock(below, Direction.UP);
        rotating = true;
        timeout = lowHeight ? 8 : 5;
    }

    private boolean shouldPlaceNow() {
        float fallDistance = mc.player.fallDistance;
        boolean lowHeight = fallDistance >= 2.0F && fallDistance < 4.0F;
        double motionY = mc.player.getVelocity().y;
        return isOnGround(motionY) || (lowHeight && isOnGround(motionY * 0.8D));
    }

    private boolean isOnGround(double height) {
        Iterable<VoxelShape> collisions = mc.world.getCollisions(mc.player, mc.player.getBoundingBox().offset(0.0D, height, 0.0D));
        return collisions.iterator().hasNext();
    }

    private void resetState(boolean switchBack) {
        rotating = false;
        placeWater = false;
        timeout = 0;
        retrieveFlag = false;
        retrievePending = false;
        above = null;
        targetRotation = null;
        if (switchBack && originalSlot != -1 && mc.player != null) {
            mc.player.getInventory().selectedSlot = originalSlot;
        }
        originalSlot = -1;
    }

    private void useItem(BlockHitResult hitResult) {
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private HitResult getRaycastResult() {
        if (targetRotation != null) {
            HitResult result = ToolManager.INSTANCE.RAY_CAST.rayCast(targetRotation, 4.5D, 0.0F);
            if (result != null) {
                return result;
            }
        }
        return mc.crosshairTarget;
    }

    private int findHotbar(net.minecraft.item.Item item) {
        for (int slot = 0; slot < 9; slot++) {
            if (mc.player.getInventory().getStack(slot).isOf(item)) {
                return slot;
            }
        }
        return -1;
    }

    private static Setting<Double> rangedDouble(String name, String description, double defaultValue, double min, double max, double step) {
        Setting<Double> setting = new Setting<>(name, description, defaultValue);
        setting.setRange(min, max, step);
        return setting;
    }
}
