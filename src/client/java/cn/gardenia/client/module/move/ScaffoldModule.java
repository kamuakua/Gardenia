package cn.gardenia.client.module.move;

import cn.gardenia.client.event.events.movement.PlayerMotionEvent;
import cn.gardenia.client.event.events.network.GlobalPacketEvent;
import cn.gardenia.client.event.events.player.PlayerTickEvent;
import cn.gardenia.client.event.events.render.Render3DEvent;
import cn.gardenia.client.event.events.render.RenderHudEvent;
import cn.gardenia.client.event.events.render.UpdateFovEvent;
import cn.gardenia.client.gui.nanovg.NanoVGManager;
import cn.gardenia.client.gui.nanovg.NanoVGRender;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.ModuleManager;
import cn.gardenia.client.module.Setting;
import cn.gardenia.client.tool.ToolManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.*;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class ScaffoldModule extends Module {
    private static final int CLUTCH_TIMEOUT_TICKS = 35;
    private static final int CLUTCH_RELEASE_DELAY_TICKS = 2;
    private static final double CLUTCH_REACH = 4.5D;
    private static final double SOUTH_SIDE_CLUTCH_SAFE_DISTANCE = 4.5D;
    private static final int CLUTCH_VERTICAL_SCAN = 6;
    private static final int CLUTCH_HORIZONTAL_SCAN = 4;
    private static final Direction[] CLUTCH_SUPPORT_DIRECTIONS = new Direction[]{
            Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP
    };

    private static final List<Block> BLOCK_BLACKLIST = Arrays.asList(
            Blocks.AIR, Blocks.WATER, Blocks.LAVA, Blocks.ENCHANTING_TABLE, Blocks.GLASS_PANE, Blocks.GLASS_PANE,
            Blocks.IRON_BARS, Blocks.SNOW, Blocks.COAL_ORE, Blocks.DIAMOND_ORE, Blocks.EMERALD_ORE,
            Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.TORCH, Blocks.ANVIL, Blocks.TRAPPED_CHEST, Blocks.NOTE_BLOCK,
            Blocks.JUKEBOX, Blocks.TNT, Blocks.GOLD_ORE, Blocks.IRON_ORE, Blocks.LAPIS_ORE,
            Blocks.STONE_PRESSURE_PLATE, Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE,
            Blocks.STONE_BUTTON, Blocks.LEVER, Blocks.TALL_GRASS, Blocks.TRIPWIRE, Blocks.TRIPWIRE_HOOK,
            Blocks.RAIL, Blocks.CORNFLOWER, Blocks.RED_MUSHROOM, Blocks.BROWN_MUSHROOM, Blocks.VINE,
            Blocks.SUNFLOWER, Blocks.LADDER, Blocks.FURNACE, Blocks.SAND, Blocks.CACTUS,
            Blocks.DISPENSER, Blocks.DROPPER, Blocks.CRAFTING_TABLE, Blocks.COBWEB, Blocks.PUMPKIN,
            Blocks.COBBLESTONE_WALL, Blocks.OAK_FENCE, Blocks.REDSTONE_TORCH, Blocks.FLOWER_POT
    );

    private final Setting<String> mode = new Setting<>("Mode", "Placement mode", "Heypixel", new String[]{"Heypixel", "Legit"});
    private final Setting<String> heypixelMode = new Setting<>("Heypixel Mode", "Heypixel placement variant", "Telly Bridge", new String[]{"Telly Bridge"});
    private final Setting<Boolean> snap = new Setting<>("Snap", "Snap rotations while legit", false);
    private final Setting<Integer> rotateSpeed = rangedInt("Rotation Speed", "Placement rotation speed", 180, 1, 180, 1);
    private final Setting<Integer> rotateBackSpeed = rangedInt("Rotation Back Speed", "Return rotation speed", 180, 1, 180, 1);
    private final Setting<Integer> tellyTick = rangedInt("Telly Ticks", "Air ticks before placing", 1, 0, 6, 1);
    private final Setting<Integer> upTellyRotateSpeed = rangedInt("UpTelly Rotation Speed", "Rising rotation speed", 95, 1, 180, 1);
    private final Setting<Integer> upTellyPlaceTicks = rangedInt("UpTelly Place Ticks", "Rising ticks before place", 2, 1, 6, 1);
    private final Setting<Boolean> smartUpTellyRotations = new Setting<>("Smart UpTelly Rotations", "Stage rising rotations", true);
    private final Setting<Boolean> tellyKeepFov = new Setting<>("Keep Fov", "Keep normal FOV while Telly bridging", true);
    private final Setting<Boolean> safeMode = new Setting<>("Safe Mode", "Use NavenAlpha SouthSide-safe target selection", false);
    private final Setting<Boolean> testOnGround = new Setting<>("Test OnGround", "Use NavenAlpha SouthSide on-ground rotation test", false);
    private final Setting<Boolean> onGround = new Setting<>("OnGround", "Use NavenAlpha on-ground UpTelly rotations", false);
    private final Setting<Boolean> clutch = new Setting<>("Clutch", "Try to clutch while falling", true);
    private final Setting<Boolean> clutchAutoStuck = new Setting<>("Clutch Auto Stuck", "Enable Stuck during clutch", true);
    private final Setting<Double> clutchFallDistance = rangedDouble("Clutch Fall Distance", "Fall distance before clutch", 5.0D, 2.0D, 10.0D, 0.5D);
    private final Setting<Double> clutchMinY = rangedDouble("Clutch Min Y", "Minimum vertical speed before clutch", -0.5D, -1.0D, 0.0D, 0.1D);
    private final Setting<Boolean> safeWalk = new Setting<>("SafeWalk", "Sneak at block edge in Legit mode", true);
    private final Setting<Double> legitEdgeDistance = rangedDouble("Legit Edge Distance", "SafeWalk edge distance", 0.3D, 0.05D, 0.8D, 0.01D);
    private final Setting<Boolean> legitKeepFov = new Setting<>("Keep Fov", "Keep normal FOV while Legit scaffolding", true);
    private final Setting<Boolean> devTest = new Setting<>("Dev Test", "Show scaffold debug speed", false);
    private final Setting<Double> devTestX = rangedDouble("Dev Test Position X", "Debug display X", 8.0D, 0.0D, 1000.0D, 1.0D);
    private final Setting<Double> devTestY = rangedDouble("Dev Test Position Y", "Debug display Y", 120.0D, 0.0D, 1000.0D, 1.0D);
    private final Setting<Boolean> esp = new Setting<>("ESP", "Render current placement target", true);

    private final Consumer<PlayerTickEvent> tickListener = this::onTick;
    private final Consumer<PlayerMotionEvent> motionListener = this::onMotion;
    private final Consumer<GlobalPacketEvent> packetListener = this::onPacket;
    private final Consumer<UpdateFovEvent> fovListener = this::onUpdateFov;
    private final Consumer<Render3DEvent> render3DListener = this::onRender3D;
    private final Consumer<RenderHudEvent> renderHudListener = this::onRenderHud;

    private int airTick;
    private int southSideAirTicks;
    private int southSideGroundTicks;
    private int southSideVelocityTicks;
    private boolean southSideSafeTargetActive;
    private int yLevel;
    private int targetYLevel = -1;
    private int velocityDelay;
    private int groundTicks;
    private int rotationDelay;
    private boolean canBuildNow = true;
    private double lastYawDiff = Double.NaN;
    private double lastPitchDiff = Double.NaN;
    private int jitterCounter;
    private BlockPos blockPos;
    private Direction facing;
    private int oldSlot = -1;
    private final ArrayDeque<long[]> onGroundDeltaHistory = new ArrayDeque<>();
    private float onGroundLastSentYaw = Float.NaN;
    private float onGroundLastSentPitch = Float.NaN;
    private int onGroundPhase;
    private int onGroundRotationStableTicks;
    private double currentBps;
    private boolean pendingSneak;

    private ClutchState clutchState = ClutchState.IDLE;
    private ClutchTarget clutchTarget;
    private boolean clutchActivated;
    private double clutchStartY = Double.NaN;
    private int clutchTicks;
    private int clutchPlaceAttempts;
    private int clutchReleaseTicks;

    public ScaffoldModule() {
        super("Scaffold", "Automatically places blocks under you", Category.MOVEMENT);
        addSetting(mode);
        addSetting(heypixelMode);
        addSetting(snap);
        addSetting(rotateSpeed);
        addSetting(rotateBackSpeed);
        addSetting(tellyTick);
        addSetting(upTellyRotateSpeed);
        addSetting(upTellyPlaceTicks);
        addSetting(smartUpTellyRotations);
        addSetting(tellyKeepFov);
        addSetting(safeMode);
        addSetting(testOnGround);
        addSetting(onGround);
        addSetting(clutch);
        addSetting(clutchAutoStuck);
        addSetting(clutchFallDistance);
        addSetting(clutchMinY);
        addSetting(safeWalk);
        addSetting(legitEdgeDistance);
        addSetting(legitKeepFov);
        addSetting(devTest);
        addSetting(devTestX);
        addSetting(devTestY);
        addSetting(esp);
    }

    @Override
    public void onEnable() {
        if (mc.player != null) {
            oldSlot = mc.player.getInventory().selectedSlot;
        }
        resetRuntime();
        subscribe(PlayerTickEvent.class, tickListener);
        subscribe(PlayerMotionEvent.class, motionListener);
        subscribe(GlobalPacketEvent.class, packetListener);
        subscribe(UpdateFovEvent.class, fovListener);
        subscribe(Render3DEvent.class, render3DListener);
        subscribe(RenderHudEvent.class, renderHudListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(PlayerTickEvent.class, tickListener);
        unsubscribe(PlayerMotionEvent.class, motionListener);
        unsubscribe(GlobalPacketEvent.class, packetListener);
        unsubscribe(UpdateFovEvent.class, fovListener);
        unsubscribe(Render3DEvent.class, render3DListener);
        unsubscribe(RenderHudEvent.class, renderHudListener);
        if (mc.player != null && oldSlot >= 0 && oldSlot < 9) {
            restoreSlot(oldSlot);
        }
        oldSlot = -1;
        resetClutch(true);
        resetRuntime();
    }

    public String getSuffix() {
        return isHeypixelMode() ? "Heypixel " + heypixelMode.getString() : "Legit";
    }

    public static PlayerInput handleMoveInput(PlayerInput input) {
        ScaffoldModule scaffold = ModuleManager.INSTANCE.getByClass(ScaffoldModule.class);
        if (scaffold == null || !scaffold.isEnabled() || mc.player == null) {
            return input;
        }
        return scaffold.onMoveInput(input);
    }

    private PlayerInput onMoveInput(PlayerInput input) {
        boolean jump = input.jump();
        boolean sneak = input.sneak();
        if (isHeypixelMode() && mc.player.isOnGround() && !input.jump() && isMoving(input)) {
            jump = true;
        }
        if (isLegitMode() && safeWalk.getBoolean() && isMoving(input)) {
            sneak = mc.player.isOnGround() && MovementEdgeUtil.isOnBlockEdge((float) legitEdgeDistance.getDouble());
        }
        if (pendingSneak) {
            sneak = true;
        }
        return new PlayerInput(input.forward(), input.backward(), input.left(), input.right(), jump, sneak, input.sprint());
    }

    private void onPacket(GlobalPacketEvent event) {
        if (mc.player == null || mc.world == null || !event.isReceive()) {
            return;
        }
        if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket motion && motion.getEntityId() == mc.player.getId()) {
            double length = Math.hypot(motion.getVelocityX(), motion.getVelocityZ());
            if (length >= 1.5D) {
                velocityDelay = 60;
            }
        }
    }

    private void onMotion(PlayerMotionEvent event) {
        if (!event.isPre() || mc.player == null || mc.world == null) {
            return;
        }
        if (isLegitMode() && safeWalk.getBoolean()) {
            pendingSneak = mc.player.isOnGround() && MovementEdgeUtil.isOnBlockEdge((float) legitEdgeDistance.getDouble());
        }
    }

    private void onUpdateFov(UpdateFovEvent event) {
        if (!shouldKeepFov() || mc.options == null) {
            return;
        }
        event.setFov(mc.options.getFov().getValue().floatValue() / 100.0F);
    }

    private void onRender3D(Render3DEvent event) {
        if (mc.player == null || mc.world == null || blockPos == null || !esp.getBoolean()) {
            return;
        }
        if (mc.gameRenderer == null || mc.gameRenderer.getCamera() == null) {
            return;
        }
        renderPlacementBox();
    }

    private void onRenderHud(RenderHudEvent event) {
        if (!devTest.getBoolean() || mc.player == null) {
            return;
        }

        boolean startedFrame = false;
        if (!NanoVGManager.INSTANCE.isFrameActive()) {
            if (!NanoVGManager.INSTANCE.isInitialized()) {
                NanoVGManager.INSTANCE.init();
            }
            NanoVGManager.INSTANCE.beginFrame(
                    mc.getWindow().getScaledWidth(),
                    mc.getWindow().getScaledHeight(),
                    (float) mc.getWindow().getScaleFactor()
            );
            startedFrame = true;
        }

        float x = (float) devTestX.getDouble();
        float y = (float) devTestY.getDouble();
        float radius = 5.0F;
        float paddingX = 10.0F;
        float paddingY = 8.0F;
        float fontSize = 10.0F;
        String font = NanoVGManager.INSTANCE.hasFont("chinese") ? "chinese" : "regular";
        String text = String.format("BPS:%.2f方块/S", currentBps);
        float textWidth = NanoVGRender.textWidth(text, fontSize, font);
        float textHeight = 10.0F;
        float width = textWidth + paddingX * 2.0F;
        float height = textHeight + paddingY * 2.0F;

        NanoVGRender.drawShadow(x, y, width, height, radius, 8.0F, 0.35F);
        NanoVGRender.drawRoundedRect(x, y, width, height, radius, 0x3EEBF2FA);
        NanoVGRender.drawRoundedRectStroke(x, y, width, height, radius, 1.0F, 0x60FFFFFF);
        NanoVGRender.drawText(text, x + paddingX, y + paddingY + textHeight * 0.78F, 0xEBFFFFFF, fontSize, font);

        if (startedFrame) {
            NanoVGManager.INSTANCE.endFrame();
        }
    }

    private void onTick(PlayerTickEvent event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || !event.isPre()) {
            return;
        }
        int slot = findBlockSlot();
        if (slot != -1) {
            selectSlot(slot);
        }
        currentBps = calculateCurrentBps();
        if (velocityDelay > 0) {
            velocityDelay--;
        }
        updateSouthSideMovementTicks();
        if (mc.player.isOnGround()) {
            airTick = 0;
            groundTicks++;
            if (velocityDelay <= 30) {
                velocityDelay = 0;
            }
            onGroundLastSentYaw = Float.NaN;
            onGroundLastSentPitch = Float.NaN;
            onGroundPhase = 0;
            onGroundRotationStableTicks = 0;
            resetClutch(true);
        } else {
            groundTicks = 0;
        }

        if (runClutch()) {
            return;
        }

        boolean jumpHeld = mc.options.jumpKey.isPressed();
        if (targetYLevel == -1
                || targetYLevel > MathHelper.floor(mc.player.getY()) - 1
                || mc.player.isOnGround()
                || !isMoving()
                || jumpHeld
                || !isHeypixelMode()) {
            targetYLevel = MathHelper.floor(mc.player.getY()) - 1;
        }
        yLevel = targetYLevel;
        getBlockInfo();

        if (isHeypixelMode()) {
            runHeypixelBridge(jumpHeld);
        } else {
            runLegit();
        }
    }

    private void runHeypixelBridge(boolean jumpHeld) {
        boolean southSideJumpDown = jumpHeld;
        boolean southSideGroundTest = safeMode.getBoolean()
                && testOnGround.getBoolean()
                && southSideJumpDown
                && southSideGroundTicks > 0;
        if (mc.player.isOnGround() && !southSideGroundTest) {
            blockPos = null;
            facing = null;
            if (ToolManager.INSTANCE.ROTATION.isServerRotationActive()) {
                ToolManager.INSTANCE.ROTATION.setServerRotation(new Vec2f(mc.player.getYaw(), mc.player.getPitch()), rotateBackSpeed.getInt());
            }
            return;
        }
        if (blockPos == null || facing == null) {
            airTick++;
            return;
        }

        canBuildNow = canBuildNow();
        boolean southSideTestGround = safeMode.getBoolean()
                && testOnGround.getBoolean()
                && southSideJumpDown
                && southSideGroundTicks == 1;
        boolean upTelly = isTellyBridgeMode() && isMoving() && jumpHeld && mc.player.getVelocity().y > 0.0D;
        boolean mustPlace = mc.player.getVelocity().y <= 0.0D || mc.player.fallDistance > 0.0F || airTick >= upTellyPlaceTicks.getInt() + 1;
        boolean useSouthSideTestOnGround = safeMode.getBoolean()
                && testOnGround.getBoolean()
                && southSideJumpDown
                && southSideGroundTicks > 0;
        boolean useOnGround = !useSouthSideTestOnGround && upTelly && onGround.getBoolean();
        boolean willPlace = canBuildNow && southSideAirTicks >= tellyTick.getInt() && (!upTelly || airTick >= upTellyPlaceTicks.getInt() || mustPlace);
        if (safeMode.getBoolean() && testOnGround.getBoolean() && !willPlace && southSideJumpDown) {
            willPlace = southSideGroundTicks == 1;
        }
        boolean southSideForcedTarget = southSideJumpDown && applySouthSideSafeModeTarget();
        if (southSideForcedTarget) {
            willPlace = true;
        }
        boolean lockToTarget = useOnGround && (mustPlace || willPlace);

        Vec2f rotation;
        double speed;
        if (useSouthSideTestOnGround) {
            rotation = getSouthSideTestOnGroundRotation(blockPos, facing, southSideTestGround);
            speed = rotateSpeed.getInt();
        } else if (useOnGround) {
            rotation = getOnGroundUpTellyRotation(blockPos, facing, lockToTarget);
            speed = getOnGroundUpTellyRotationSpeed(lockToTarget);
        } else {
            rotation = upTelly && smartUpTellyRotations.getBoolean()
                    ? getSmartUpTellyRotation(blockPos, facing, mustPlace)
                    : getHeypixelRotation(blockPos, facing);
            speed = upTelly && smartUpTellyRotations.getBoolean()
                    ? getSmartUpTellyRotationSpeed(mustPlace)
                    : (upTelly && !mustPlace ? upTellyRotateSpeed.getInt() : rotateSpeed.getInt());
        }
        ToolManager.INSTANCE.ROTATION.setServerRotation(rotation, speed);

        if (willPlace) {
            if (useOnGround) {
                boolean rotationReady = rayHitsBlock(rotation, blockPos);
                if (rotationReady) {
                    onGroundRotationStableTicks++;
                    if (onGroundRotationStableTicks >= 2) {
                        place();
                        rotationDelay = 0;
                        onGroundRotationStableTicks = 0;
                    }
                } else {
                    onGroundRotationStableTicks = 0;
                    rotationDelay++;
                }
            } else {
                place();
                rotationDelay = 0;
            }
        } else if (canBuildNow) {
            onGroundRotationStableTicks = 0;
            rotationDelay = 0;
        } else {
            onGroundRotationStableTicks = 0;
            rotationDelay++;
        }
        airTick++;
    }

    private void runLegit() {
        if (blockPos == null) {
            ToolManager.INSTANCE.ROTATION.setServerRotation(new Vec2f(MathHelper.wrapDegrees(mc.player.getYaw() - 180.0F), 89.64F), rotateSpeed.getInt());
        }
        if (onAir() || !snap.getBoolean()) {
            ToolManager.INSTANCE.ROTATION.setServerRotation(getRotation(blockPos, facing), rotateSpeed.getInt());
        }
        place();
    }

    private void place() {
        if (blockPos == null || facing == null || (!onAir() && !isSouthSidePlaceTarget())) {
            return;
        }
        Vec2f rotation = ToolManager.INSTANCE.ROTATION.getServerRotation();
        if (!rayHitsBlock(rotation, blockPos)) {
            return;
        }
        BlockHitResult ray = new BlockHitResult(hitVec(blockPos, facing), facing, blockPos, false);
        ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, ray);
        if (result.isAccepted()) {
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    private void getBlockInfo() {
        blockPos = null;
        facing = null;
        Vec3d baseVec = mc.player.getEyePos();
        BlockPos base = BlockPos.ofFloored(baseVec.x, getYLevel(), baseVec.z);
        if (isSolidAndNonInteractive(base)) {
            return;
        }
        if (checkBlock(baseVec, base)) {
            return;
        }
        int baseX = base.getX();
        int baseZ = base.getZ();
        for (int d = 1; d <= 6; d++) {
            if (checkBlock(baseVec, new BlockPos(baseX, getYLevel() - d, baseZ))) {
                return;
            }
            for (int x = 0; x <= d; x++) {
                for (int z = 0; z <= d - x; z++) {
                    int y = d - x - z;
                    for (int rev1 = 0; rev1 <= 1; rev1++) {
                        for (int rev2 = 0; rev2 <= 1; rev2++) {
                            if (checkBlock(baseVec, new BlockPos(baseX + (rev1 == 0 ? x : -x), getYLevel() - y, baseZ + (rev2 == 0 ? z : -z)))) {
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean checkBlock(Vec3d baseVec, BlockPos pos) {
        Block block = mc.world.getBlockState(pos).getBlock();
        if (!(block instanceof AirBlock) && !(block instanceof LilyPadBlock)) {
            return false;
        }
        if (pos.getY() > getYLevel()) {
            return false;
        }

        Vec3d center = new Vec3d(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        for (Direction direction : Direction.values()) {
            Vec3i offset = direction.getVector();
            Vec3d normal = new Vec3d(offset.getX(), offset.getY(), offset.getZ());
            Vec3d hit = center.add(normal.multiply(0.5D));
            BlockPos support = pos.offset(direction);
            if (!isSolidAndNonInteractive(support)) {
                continue;
            }
            Vec3d relevant = hit.subtract(baseVec);
            if (relevant.lengthSquared() <= CLUTCH_REACH * CLUTCH_REACH && relevant.dotProduct(normal) >= 0.0D) {
                if (direction.getOpposite() == Direction.UP && isMoving() && !mc.options.jumpKey.isPressed()) {
                    continue;
                }
                blockPos = support.toImmutable();
                facing = direction.getOpposite();
                return true;
            }
        }
        return false;
    }

    private boolean isSolidAndNonInteractive(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        return !state.getCollisionShape(mc.world, pos).isEmpty() && state.createScreenHandlerFactory(mc.world, pos) == null;
    }

    private void updateSouthSideMovementTicks() {
        boolean jumpDown = mc.options != null && mc.options.jumpKey.isPressed();
        if (mc.player.isOnGround()) {
            southSideAirTicks = 0;
            southSideGroundTicks = jumpDown ? southSideGroundTicks + 1 : 0;
        } else {
            southSideAirTicks++;
            if (!jumpDown) {
                southSideGroundTicks = 0;
            } else if (southSideGroundTicks > 0 && southSideGroundTicks < 2) {
                southSideGroundTicks++;
            }
        }
        if (southSideVelocityTicks > 0) {
            southSideVelocityTicks--;
        }
    }

    private PlaceTarget findSouthSideTarget(BlockPos pos) {
        if (mc.world == null || !canPlaceIn(pos)) {
            return null;
        }

        BlockPos playerBlock = BlockPos.ofFloored(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        PlaceTarget best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Direction direction : Direction.values()) {
            BlockPos support = pos.offset(direction);
            if (support.equals(playerBlock) || !isSolidAndNonInteractive(support)) {
                continue;
            }

            Direction face = direction.getOpposite();
            Vec3d hit = hitVec(support, face);
            if (mc.player.getEyePos().distanceTo(hit) > CLUTCH_REACH) {
                continue;
            }

            double distance = blockDistance(playerBlock, support);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = new PlaceTarget(support.toImmutable(), face);
            }
        }
        return best;
    }

    private boolean applySouthSideSafeModeTarget() {
        if (!safeMode.getBoolean()) {
            southSideSafeTargetActive = false;
            return false;
        }

        BlockPos underPlayer = BlockPos.ofFloored(
                Math.floor(mc.player.getX()),
                MathHelper.floor(mc.player.getY()) - 1,
                Math.floor(mc.player.getZ())
        );
        PlaceTarget target = findSouthSideTarget(underPlayer);
        if (target == null) {
            southSideSafeTargetActive = false;
            return false;
        }

        Vec3d predicted = simulateSouthSidePlayer(1);
        double distance = predicted.distanceTo(Vec3d.ofCenter(target.support()));
        double predictedY = simulateSouthSidePlayer(2).y;
        if (distance >= SOUTH_SIDE_CLUTCH_SAFE_DISTANCE && target.support().getY() <= predictedY) {
            southSideSafeTargetActive = false;
            return false;
        }

        blockPos = target.support();
        facing = target.face();
        southSideVelocityTicks = 8;
        southSideSafeTargetActive = true;
        return true;
    }

    private Vec3d simulateSouthSidePlayer(int ticks) {
        Vec3d position = mc.player.getPos();
        Vec3d velocity = mc.player.getVelocity();
        for (int i = 0; i < ticks; i++) {
            position = position.add(velocity);
            velocity = new Vec3d(velocity.x * 0.91D, (velocity.y - 0.08D) * 0.98D, velocity.z * 0.91D);
        }
        return position;
    }

    private static double blockDistance(BlockPos from, BlockPos to) {
        int dx = from.getX() - to.getX();
        int dy = from.getY() - to.getY();
        int dz = from.getZ() - to.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private boolean isSouthSidePlaceTarget() {
        return safeMode.getBoolean()
                && southSideSafeTargetActive
                && blockPos != null
                && facing != null
                && canPlaceIn(blockPos.offset(facing));
    }

    private boolean runClutch() {
        if (!isHeypixelMode() || !clutch.getBoolean() || !clutchAutoStuck.getBoolean()) {
            resetClutch(true);
            return false;
        }
        if (mc.player.isOnGround()) {
            resetClutch(true);
            return false;
        }
        if (clutchState == ClutchState.IDLE && !shouldStartClutch()) {
            return false;
        }
        int blockSlot = findBlockSlot();
        if (blockSlot < 0) {
            resetClutch(true);
            return false;
        }
        if (clutchState == ClutchState.IDLE) {
            clutchState = ClutchState.ARMED;
            clutchTicks = 0;
            clutchPlaceAttempts = 0;
            clutchReleaseTicks = 0;
            clutchTarget = null;
        }
        if (++clutchTicks > CLUTCH_TIMEOUT_TICKS) {
            resetClutch(true);
            return false;
        }

        selectSlot(blockSlot);
        ensureClutchStuck();
        if (clutchState == ClutchState.RELEASE) {
            if (clutchReleaseTicks-- <= 0) {
                resetClutch(true);
            }
            return true;
        }

        ClutchTarget target = findClutchTarget();
        if (target == null) {
            blockPos = null;
            facing = null;
            ToolManager.INSTANCE.ROTATION.setServerRotation(new Vec2f(mc.player.getYaw(), 88.5F), rotateSpeed.getInt());
            return true;
        }

        clutchTarget = target;
        blockPos = target.support();
        facing = target.face();
        Vec2f rotation = smoothClutchRotation(target.rotation());
        ToolManager.INSTANCE.ROTATION.setServerRotation(rotation, rotateSpeed.getInt());

        if (rayHitsSupport(rotation, target.support(), target.face()) || clutchPlaceAttempts >= 1) {
            BlockHitResult hit = raycastClutch(rotation, target);
            if (hit != null && placeClutchTarget(target, hit)) {
                clutchState = ClutchState.RELEASE;
                clutchReleaseTicks = CLUTCH_RELEASE_DELAY_TICKS;
                clutchPlaceAttempts = 0;
                return true;
            }
            clutchPlaceAttempts++;
        } else {
            clutchPlaceAttempts = 0;
        }
        return true;
    }

    private boolean shouldStartClutch() {
        double motionY = mc.player.getVelocity().y;
        if (motionY >= clutchMinY.getDouble() && !isOverVoid()) {
            clutchStartY = Double.NaN;
            return false;
        }
        if (Double.isNaN(clutchStartY)) {
            clutchStartY = mc.player.getY();
        }
        double trackedFall = Math.max(0.0D, clutchStartY - mc.player.getY());
        return mc.player.fallDistance >= clutchFallDistance.getDouble()
                || trackedFall >= clutchFallDistance.getDouble()
                || isOverVoid();
    }

    private void ensureClutchStuck() {
        StuckModule stuck = ModuleManager.INSTANCE.getByClass(StuckModule.class);
        if (stuck != null && !stuck.isEnabled()) {
            stuck.setEnabled(true);
            clutchActivated = true;
        }
    }

    private ClutchTarget findClutchTarget() {
        Vec3d eye = mc.player.getEyePos();
        Vec3d velocity = mc.player.getVelocity();
        BlockPos origin = BlockPos.ofFloored(mc.player.getX() + velocity.x * 1.5D, mc.player.getY() - 1.0D, mc.player.getZ() + velocity.z * 1.5D);
        ClutchTarget best = null;
        double bestScore = Double.MAX_VALUE;

        for (int down = 0; down <= CLUTCH_VERTICAL_SCAN; down++) {
            int radius = Math.min(CLUTCH_HORIZONTAL_SCAN, 1 + down);
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.abs(x) + Math.abs(z) > radius + 1) {
                        continue;
                    }
                    BlockPos placePos = origin.add(x, -down, z);
                    if (!canPlaceIn(placePos)) {
                        continue;
                    }
                    ClutchTarget target = resolveClutchTarget(placePos, eye, velocity);
                    if (target == null) {
                        continue;
                    }
                    double score = scoreClutchTarget(target, velocity);
                    if (score < bestScore) {
                        best = target;
                        bestScore = score;
                    }
                }
            }
        }
        return best;
    }

    private ClutchTarget resolveClutchTarget(BlockPos placePos, Vec3d eye, Vec3d velocity) {
        ClutchTarget best = null;
        double bestScore = Double.MAX_VALUE;
        for (Direction supportDirection : CLUTCH_SUPPORT_DIRECTIONS) {
            BlockPos support = placePos.offset(supportDirection);
            Direction face = supportDirection.getOpposite();
            if (!isSolidAndNonInteractive(support)) {
                continue;
            }
            Vec3d hit = getClutchHitVec(support, face, velocity);
            if (eye.distanceTo(hit) > CLUTCH_REACH || !rayHitsSupport(hit, support, face)) {
                continue;
            }
            Vec2f rotation = ToolManager.INSTANCE.ROTATION.calculate(eye, hit);
            double score = eye.squaredDistanceTo(hit) + Math.abs(MathHelper.wrapDegrees(rotation.x - mc.player.getYaw())) * 0.01D;
            if (score < bestScore) {
                best = new ClutchTarget(placePos.toImmutable(), support.toImmutable(), face, hit, rotation);
                bestScore = score;
            }
        }
        return best;
    }

    private boolean placeClutchTarget(ClutchTarget target, BlockHitResult hit) {
        if (!isClutchTargetValid(target)) {
            return false;
        }
        ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        if (result.isAccepted()) {
            mc.player.swingHand(Hand.MAIN_HAND);
            return true;
        }
        return false;
    }

    private BlockHitResult raycastClutch(Vec2f rotation, ClutchTarget target) {
        BlockHitResult result = raycast(rotation);
        if (result != null && result.getBlockPos().equals(target.support()) && result.getSide() == target.face()) {
            return result;
        }
        return new BlockHitResult(target.hit(), target.face(), target.support(), false);
    }

    private Vec2f smoothClutchRotation(Vec2f target) {
        Vec2f reference = ToolManager.INSTANCE.ROTATION.getServerRotation();
        float yawDelta = MathHelper.wrapDegrees(target.x - reference.x);
        float pitchDelta = target.y - reference.y;
        float yawStep = MathHelper.clamp(yawDelta, -85.0F, 85.0F);
        float pitchStep = MathHelper.clamp(pitchDelta, -55.0F, 55.0F);
        return new Vec2f(reference.x + yawStep, MathHelper.clamp(reference.y + pitchStep, -90.0F, 89.5F));
    }

    private boolean isClutchTargetValid(ClutchTarget target) {
        return target != null
                && canPlaceIn(target.placePos())
                && isSolidAndNonInteractive(target.support())
                && withinReach(target.hit())
                && rayHitsSupport(target.hit(), target.support(), target.face());
    }

    private boolean canPlaceIn(BlockPos pos) {
        return mc.world.getBlockState(pos).isReplaceable();
    }

    private Vec3d getClutchHitVec(BlockPos pos, Direction face, Vec3d velocity) {
        double inset = 0.35D;
        double x = pos.getX() + 0.5D + face.getOffsetX() * 0.5D;
        double y = pos.getY() + 0.5D + face.getOffsetY() * 0.5D;
        double z = pos.getZ() + 0.5D + face.getOffsetZ() * 0.5D;
        if (face.getAxis() != Direction.Axis.X) {
            x += MathHelper.clamp(velocity.x * 0.25D, -inset, inset);
        }
        if (face.getAxis() != Direction.Axis.Y) {
            y += MathHelper.clamp(mc.player.getVelocity().y * 0.12D, -inset, inset);
        }
        if (face.getAxis() != Direction.Axis.Z) {
            z += MathHelper.clamp(velocity.z * 0.25D, -inset, inset);
        }
        return new Vec3d(x, y, z);
    }

    private boolean withinReach(Vec3d hit) {
        return mc.player.getEyePos().distanceTo(hit) <= CLUTCH_REACH;
    }

    private boolean rayHitsSupport(Vec3d hit, BlockPos support, Direction face) {
        BlockHitResult result = mc.world.raycast(new RaycastContext(mc.player.getEyePos(), hit, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
        return result.getType() != HitResult.Type.MISS && result.getBlockPos().equals(support) && result.getSide() == face;
    }

    private boolean rayHitsSupport(Vec2f rotation, BlockPos support, Direction face) {
        BlockHitResult result = raycast(rotation);
        return result != null && result.getBlockPos().equals(support) && result.getSide() == face;
    }

    private boolean rayHitsBlock(Vec2f rotation, BlockPos support) {
        BlockHitResult result = raycast(rotation);
        return result != null && result.getBlockPos().equals(support);
    }

    private boolean isOverVoid() {
        Vec3d start = mc.player.getPos();
        Vec3d end = new Vec3d(start.x, mc.world.getBottomY() - 2.0D, start.z);
        BlockHitResult hit = mc.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        return hit.getType() == HitResult.Type.MISS;
    }

    private double scoreClutchTarget(ClutchTarget target, Vec3d velocity) {
        BlockPos place = target.placePos();
        double predictedX = mc.player.getX() + velocity.x * 3.0D;
        double predictedZ = mc.player.getZ() + velocity.z * 3.0D;
        double dx = place.getX() + 0.5D - predictedX;
        double dz = place.getZ() + 0.5D - predictedZ;
        double dy = Math.abs(place.getY() + 1.0D - mc.player.getY());
        double yaw = Math.abs(MathHelper.wrapDegrees(target.rotation().x - mc.player.getYaw())) * 0.015D;
        return dx * dx + dz * dz + dy * 0.55D + target.hit().distanceTo(mc.player.getEyePos()) * 0.25D + yaw;
    }

    private Vec2f getRotation(BlockPos pos, Direction direction) {
        if (pos == null || direction == null) {
            return new Vec2f(MathHelper.wrapDegrees(mc.player.getYaw() - 180.0F), 89.64F);
        }
        // 对齐 Naven: onAir 时瞄面表面正中(calculateBlock = 方块中心+faceNormal*0.5),不是 hitVec 内的随机点。
        // 之前用 calculate(hitVec(...)) 会瞄进方块内部、且每 tick 随机,与 place() 发出的 BlockHitResult 不一致 →
        // DuplicateRotPlace + AirLiquidPlace。
        Vec2f rotation = onAir()
                ? ToolManager.INSTANCE.ROTATION.calculateBlock(pos, direction)
                : ToolManager.INSTANCE.ROTATION.calculate(Vec3d.ofCenter(pos));
        Vec2f reverseYaw = new Vec2f(MathHelper.wrapDegrees(mc.player.getYaw() - 180.0F), rotation.y);
        return rayHitsBlock(reverseYaw, pos) ? reverseYaw : rotation;
    }

    private Vec2f getHeypixelRotation(BlockPos pos, Direction direction) {
        Vec2f rotation = getRotation(pos, direction);
        Vec2f reference = ToolManager.INSTANCE.ROTATION.getServerRotation();
        double maxStep = Math.max(50.0D, 180.0D / Math.max(1.0D, tellyTick.getInt() + 1.0D));
        if (groundTicks > 0 && isTellyBridgeMode()) {
            rotation = new Vec2f(mc.player.getYaw(), mc.options.jumpKey.isPressed() ? 75.5F : rotation.y);
        } else {
            float yawDelta = MathHelper.wrapDegrees(rotation.x - reference.x);
            rotation = new Vec2f(reference.x + MathHelper.clamp(yawDelta, (float) -maxStep, (float) maxStep), rotation.y);
        }
        return applyRotationJitter(rotation, reference);
    }

    private Vec2f getSouthSideTestOnGroundRotation(BlockPos pos, Direction direction, boolean forcedTarget) {
        Vec2f target = ToolManager.INSTANCE.ROTATION.calculateBlock(pos, direction);
        Vec2f reference = ToolManager.INSTANCE.ROTATION.getServerRotation();
        float yawDistance = MathHelper.wrapDegrees(target.x - reference.x);

        if (southSideVelocityTicks > 0) {
            return target;
        }
        if (southSideGroundTicks > 0) {
            if (!safeMode.getBoolean() || !testOnGround.getBoolean() || mc.options.jumpKey.isPressed()) {
                switch (southSideGroundTicks) {
                    case 1 -> {
                        if (!forcedTarget) {
                            return new Vec2f(reference.x + MathHelper.wrapDegrees(yawDistance / 2.0F), 75.5F);
                        }
                        return ToolManager.INSTANCE.ROTATION.calculate(mc.player.getEyePos(), hitVec(pos, direction));
                    }
                    case 2 -> {
                        return new Vec2f(mc.player.getYaw(), 75.5F);
                    }
                    default -> {
                    }
                }
            } else {
                return new Vec2f(mc.player.getYaw(), 75.5F);
            }
        }
        if (!forcedTarget) {
            float yawStep = southSideAirTicks == 1 ? 80.0F : 50.0F;
            yawStep -= randomFloat(0.001F, 0.005F);
            return new Vec2f(reference.x + MathHelper.clamp(yawDistance, -yawStep, yawStep), target.y);
        }
        if (southSideGroundTicks == 1) {
            return ToolManager.INSTANCE.ROTATION.calculate(mc.player.getEyePos(), hitVec(pos, direction));
        }
        if (southSideGroundTicks == 2) {
            return new Vec2f(mc.player.getYaw(), 75.5F);
        }
        return target;
    }

    private Vec2f applyRotationJitter(Vec2f rotation, Vec2f reference) {
        double yawDiff = Math.abs(MathHelper.wrapDegrees(rotation.x - reference.x));
        double pitchDiff = Math.abs(rotation.y - reference.y);
        boolean stuckYaw = yawDiff > 2.0D && !Double.isNaN(lastYawDiff) && Math.abs(yawDiff - lastYawDiff) < 1.0E-4D;
        boolean stuckPitch = pitchDiff > 2.0D && !Double.isNaN(lastPitchDiff) && Math.abs(pitchDiff - lastPitchDiff) < 1.0E-4D;
        if (stuckYaw || stuckPitch) {
            float yawJitter = randomFloat(0.095F, 0.19F);
            float pitchJitter = randomFloat(0.016F, 0.055F);
            if ((jitterCounter++ & 1) == 0) {
                yawJitter = -yawJitter;
            }
            rotation = new Vec2f(rotation.x + yawJitter, MathHelper.clamp(rotation.y + pitchJitter, -89.5F, 89.5F));
            yawDiff = Math.abs(MathHelper.wrapDegrees(rotation.x - reference.x));
            pitchDiff = Math.abs(rotation.y - reference.y);
        }
        lastYawDiff = yawDiff;
        lastPitchDiff = pitchDiff;
        return rotation;
    }

    private boolean canBuildNow() {
        if (!clutch.getBoolean() || mc.player.isOnGround()) {
            return velocityDelay <= 0 || rotationDelay > 8;
        }
        if (velocityDelay > 0 && rotationDelay <= 8) {
            return false;
        }
        if (mc.player.getVelocity().y < -0.1D && blockPos != null) {
            double predictedY = mc.player.getY() + mc.player.getVelocity().y * 2.0D;
            return blockPos.getY() <= predictedY;
        }
        return true;
    }

    private Vec2f getSmartUpTellyRotation(BlockPos pos, Direction direction, boolean mustPlace) {
        Vec2f direct = ToolManager.INSTANCE.ROTATION.calculate(hitVec(pos, direction));
        Vec2f preferred = getRotation(pos, direction);
        float placeWindow = Math.max(1.0F, upTellyPlaceTicks.getInt());
        float progress = MathHelper.clamp((airTick + 1.0F) / placeWindow, 0.0F, 1.0F);
        double verticalMotion = mc.player.getVelocity().y;
        boolean risingFast = verticalMotion > 0.12D;
        boolean nearingApex = verticalMotion <= 0.12D;
        float targetYaw = preferred.x;
        float targetPitch = preferred.y;
        if (!mustPlace) {
            float earlyPitch = MathHelper.clamp(direct.y + (risingFast ? 10.0F : 6.0F), 78.0F, 89.2F);
            float stagedYaw = mc.player.getYaw() + MathHelper.wrapDegrees(preferred.x - mc.player.getYaw());
            float yawBlend = nearingApex ? 0.8F : 0.55F + progress * 0.2F;
            float pitchBlend = nearingApex ? 0.85F : 0.45F + progress * 0.25F;
            targetYaw = MathHelper.lerp(yawBlend, mc.player.getYaw(), stagedYaw);
            targetPitch = MathHelper.lerp(pitchBlend, mc.player.getPitch(), earlyPitch);
        }
        Vec2f staged = new Vec2f(targetYaw, MathHelper.clamp(targetPitch, -90.0F, 89.64F));
        if (mustPlace || progress >= 0.72F || !rayHitsBlock(staged, pos)) {
            return preferred;
        }
        return staged;
    }

    private double getSmartUpTellyRotationSpeed(boolean mustPlace) {
        if (mustPlace) {
            return rotateSpeed.getInt();
        }
        float placeWindow = Math.max(1.0F, upTellyPlaceTicks.getInt());
        float progress = MathHelper.clamp((airTick + 1.0F) / placeWindow, 0.0F, 1.0F);
        float minSpeed = Math.min(upTellyRotateSpeed.getInt(), rotateSpeed.getInt());
        float maxSpeed = Math.max(upTellyRotateSpeed.getInt(), rotateSpeed.getInt());
        float risingSpeed = MathHelper.lerp(progress * progress, minSpeed, maxSpeed);
        return mc.player.getVelocity().y > 0.12D ? risingSpeed : Math.max(risingSpeed, rotateSpeed.getInt() * 0.9D);
    }

    private void pruneOnGroundDeltaHistory(long now) {
        while (!onGroundDeltaHistory.isEmpty() && now - onGroundDeltaHistory.peekFirst()[0] > 1000L) {
            onGroundDeltaHistory.pollFirst();
        }
    }

    private double sumRecentMagnitudes(long now) {
        pruneOnGroundDeltaHistory(now);
        double sum = 0.0D;
        for (long[] entry : onGroundDeltaHistory) {
            sum += Double.longBitsToDouble(entry[1]);
        }
        return sum;
    }

    private int countLargeRecentDeltas(long now, double threshold) {
        pruneOnGroundDeltaHistory(now);
        int count = 0;
        for (long[] entry : onGroundDeltaHistory) {
            if (Double.longBitsToDouble(entry[1]) > threshold) {
                count++;
            }
        }
        return count;
    }

    private void recordOnGroundDelta(long now, double magnitude) {
        onGroundDeltaHistory.addLast(new long[]{now, Double.doubleToRawLongBits(magnitude)});
    }

    private Vec2f getOnGroundUpTellyRotation(BlockPos pos, Direction direction, boolean lockToTarget) {
        Vec2f preferred = getRotation(pos, direction);
        Vec2f reference = ToolManager.INSTANCE.ROTATION.getServerRotation();

        float currentYaw = !Float.isNaN(onGroundLastSentYaw) ? onGroundLastSentYaw : reference.x;
        float currentPitch = !Float.isNaN(onGroundLastSentPitch) ? onGroundLastSentPitch : reference.y;

        long now = System.currentTimeMillis();
        double currentSum = sumRecentMagnitudes(now);
        int largeCount = countLargeRecentDeltas(now, 25.0D);

        float maxYawStepHard = 28.0F;
        float maxPitchStep = 28.0F;
        float sumBudget = 480.0F;
        float remainingBudget = (float) Math.max(0.0D, sumBudget - currentSum);

        boolean nearSwitchLimit = largeCount >= 4;
        if (nearSwitchLimit) {
            maxYawStepHard = 22.0F;
        }

        if (lockToTarget) {
            float deltaYaw = MathHelper.wrapDegrees(preferred.x - currentYaw);
            float deltaPitch = MathHelper.clamp(preferred.y, -90.0F, 89.5F) - currentPitch;
            float maxStepHere = Math.min(35.0F, maxYawStepHard);
            if (Math.abs(deltaYaw) > maxStepHere) {
                deltaYaw = Math.signum(deltaYaw) * maxStepHere;
            }
            if (Math.abs(deltaPitch) > maxStepHere) {
                deltaPitch = Math.signum(deltaPitch) * maxStepHere;
            }

            float newYaw = currentYaw + deltaYaw;
            float newPitch = MathHelper.clamp(currentPitch + deltaPitch, -90.0F, 89.5F);
            Vec2f lockCandidate = new Vec2f(MathHelper.wrapDegrees(newYaw), newPitch);
            if (Math.abs(deltaYaw) <= 0.05F && Math.abs(deltaPitch) <= 0.05F) {
                lockCandidate = new Vec2f(MathHelper.wrapDegrees(preferred.x), MathHelper.clamp(preferred.y, -90.0F, 89.5F));
                deltaYaw = MathHelper.wrapDegrees(lockCandidate.x - currentYaw);
                deltaPitch = lockCandidate.y - currentPitch;
            }
            double magnitude = Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);
            recordOnGroundDelta(now, magnitude);
            onGroundLastSentYaw = lockCandidate.x;
            onGroundLastSentPitch = lockCandidate.y;
            onGroundPhase++;
            return lockCandidate;
        }

        float placeWindow = Math.max(1.0F, upTellyPlaceTicks.getInt());
        float progress = MathHelper.clamp((airTick + 1.0F) / placeWindow, 0.0F, 1.0F);
        double verticalMotion = mc.player.getVelocity().y;

        float earlyPitchTarget = MathHelper.clamp(77.0F + progress * 9.0F, 75.0F, 86.0F);
        if (verticalMotion <= 0.10D) {
            earlyPitchTarget = MathHelper.clamp(earlyPitchTarget + 2.5F, 75.0F, 88.0F);
        }

        float ease = (float) (0.5D - 0.5D * Math.cos(Math.PI * progress));
        float yawBlend = 0.50F + ease * 0.35F;
        float pitchBlend = 0.55F + ease * 0.35F;

        float stagedYaw = currentYaw + MathHelper.wrapDegrees(preferred.x - currentYaw);
        float targetYaw = MathHelper.lerp(yawBlend, currentYaw, stagedYaw);
        float targetPitch = MathHelper.lerp(pitchBlend, currentPitch, earlyPitchTarget);

        float yawDelta = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float pitchDelta = targetPitch - currentPitch;

        float clampedYawStep = Math.min(maxYawStepHard, Math.max(6.0F, remainingBudget * 0.48F));
        if (Math.abs(yawDelta) > clampedYawStep) {
            yawDelta = Math.signum(yawDelta) * clampedYawStep;
        }
        if (Math.abs(pitchDelta) > maxPitchStep) {
            pitchDelta = Math.signum(pitchDelta) * maxPitchStep;
        }

        float newYaw = currentYaw + yawDelta;
        float newPitch = MathHelper.clamp(currentPitch + pitchDelta, -90.0F, 89.5F);

        double magnitude = Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);
        if (currentSum + magnitude > sumBudget) {
            double scale = Math.max(0.2D, (sumBudget - currentSum) / Math.max(magnitude, 0.0001D));
            yawDelta = (float) (yawDelta * scale);
            pitchDelta = (float) (pitchDelta * scale);
            newYaw = currentYaw + yawDelta;
            newPitch = MathHelper.clamp(currentPitch + pitchDelta, -90.0F, 89.5F);
            magnitude = Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);
        }

        Vec2f candidate = new Vec2f(MathHelper.wrapDegrees(newYaw), newPitch);
        recordOnGroundDelta(now, magnitude);
        onGroundLastSentYaw = candidate.x;
        onGroundLastSentPitch = candidate.y;
        onGroundPhase++;
        return candidate;
    }

    private double getOnGroundUpTellyRotationSpeed(boolean lockToTarget) {
        if (lockToTarget) {
            return Math.max(100.0D, rotateSpeed.getInt() * 0.85D);
        }
        double baseSpeed = Math.min(upTellyRotateSpeed.getInt(), rotateSpeed.getInt());
        return MathHelper.clamp(baseSpeed * 0.75D, 60.0D, 85.0D);
    }

    private boolean onAir() {
        BlockPos base = BlockPos.ofFloored(mc.player.getEyePos().x, getYLevel(), mc.player.getEyePos().z);
        Block block = mc.world.getBlockState(base).getBlock();
        return block instanceof AirBlock || block instanceof LilyPadBlock;
    }

    private int getYLevel() {
        if (isHeypixelMode() && isTellyBridgeMode() && !mc.options.jumpKey.isPressed() && isMoving()) {
            return targetYLevel;
        }
        return MathHelper.floor(mc.player.getY()) - 1;
    }

    private int findBlockSlot() {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (isValidStack(stack)) {
                return slot;
            }
        }
        return -1;
    }

    private boolean isValidStack(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem) || stack.getCount() <= 1) {
            return false;
        }
        String name = stack.getName().getString();
        if (name.contains("Click") || name.contains("点击")) {
            return false;
        }
        Block block = blockItem.getBlock();
        return !(block instanceof FlowerBlock)
                && !(block instanceof PlantBlock)
                && !(block instanceof FungusBlock)
                && !(block instanceof MushroomPlantBlock)
                && !(block instanceof CropBlock)
                && !(block instanceof SlabBlock)
                && !BLOCK_BLACKLIST.contains(block);
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

    private void restoreSlot(int slot) {
        if (slot < 0 || slot > 8 || mc.player == null) {
            return;
        }
        mc.player.getInventory().selectedSlot = slot;
        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }
    }

    private BlockHitResult raycast(Vec2f rotation) {
        Vec3d eye = mc.player.getEyePos();
        Vec3d look = Vec3d.fromPolar(rotation.y, rotation.x);
        BlockHitResult hit = mc.world.raycast(new RaycastContext(
                eye,
                eye.add(look.multiply(CLUTCH_REACH)),
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));
        return hit.getType() == HitResult.Type.MISS ? null : hit;
    }

    private Vec3d hitVec(BlockPos pos, Direction face) {
        // 对齐 Naven getVec3: 其 getRandomDoubleInRange(0.3, -0.3) 因 min>=max 恒返回常量 0.3,
        // 即偏移是确定的 +0.3 而非随机。之前移植成真随机 nextDouble(-0.3,0.3),使 cursor 每 tick 抖动、
        // 与 getRotation 瞄准点不一致 → DuplicateRotPlace/AirLiquidPlace。这里还原成确定常量。
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        if (face != Direction.UP && face != Direction.DOWN) {
            y += 0.08D;
        } else {
            x += 0.3D;
            z += 0.3D;
        }
        if (face == Direction.WEST || face == Direction.EAST) {
            z += 0.3D;
        }
        if (face == Direction.SOUTH || face == Direction.NORTH) {
            x += 0.3D;
        }
        return new Vec3d(x, y, z);
    }

    private boolean isMoving() {
        return mc.player != null && (mc.player.input.movementForward != 0.0F || mc.player.input.movementSideways != 0.0F);
    }

    private boolean isMoving(PlayerInput input) {
        return input.forward() || input.backward() || input.left() || input.right();
    }

    private boolean isHeypixelMode() {
        return "Heypixel".equals(mode.getString());
    }

    private boolean isTellyBridgeMode() {
        return "Telly Bridge".equals(heypixelMode.getString());
    }

    private boolean isLegitMode() {
        return "Legit".equals(mode.getString());
    }

    private double calculateCurrentBps() {
        double dx = mc.player.getX() - mc.player.prevX;
        double dz = mc.player.getZ() - mc.player.prevZ;
        return Math.sqrt(dx * dx + dz * dz) * 20.0D;
    }

    private boolean shouldKeepFov() {
        if (!isEnabled()) {
            return false;
        }
        if (isLegitMode()) {
            return legitKeepFov.getBoolean();
        }
        return isHeypixelMode() && isTellyBridgeMode() && tellyKeepFov.getBoolean();
    }

    private void renderPlacementBox() {
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();

        Quaternionf cameraRotation = camera.getRotation();
        Quaternionf inverseRotation = new Quaternionf(cameraRotation).conjugate();
        modelView.rotate(cameraRotation);

        VertexConsumerProvider.Immediate providers = mc.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer lines = providers.getBuffer(RenderLayer.LINES);
        MatrixStack matrices = new MatrixStack();

        Vec3d blockCenter = Vec3d.ofCenter(blockPos);
        Vector3f relative = new Vector3f(
                (float) (blockCenter.x - camPos.x),
                (float) (blockCenter.y - camPos.y),
                (float) (blockCenter.z - camPos.z)
        );
        relative.rotate(inverseRotation);

        matrices.push();
        matrices.translate(relative.x(), relative.y(), relative.z());
        Box box = new Box(blockPos).offset(-blockCenter.x, -blockCenter.y, -blockCenter.z);
        VertexRendering.drawBox(matrices, lines, box, 1.0F, 1.0F, 1.0F, 0.4F);
        matrices.pop();

        providers.draw(RenderLayer.LINES);
        modelView.popMatrix();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }

    private void resetRuntime() {
        airTick = 0;
        southSideAirTicks = 0;
        southSideGroundTicks = 0;
        southSideVelocityTicks = 0;
        southSideSafeTargetActive = false;
        targetYLevel = -1;
        velocityDelay = 0;
        groundTicks = 0;
        rotationDelay = 0;
        canBuildNow = true;
        lastYawDiff = Double.NaN;
        lastPitchDiff = Double.NaN;
        jitterCounter = 0;
        onGroundDeltaHistory.clear();
        onGroundLastSentYaw = Float.NaN;
        onGroundLastSentPitch = Float.NaN;
        onGroundPhase = 0;
        onGroundRotationStableTicks = 0;
        blockPos = null;
        facing = null;
        currentBps = 0.0D;
        pendingSneak = false;
        resetClutch(false);
    }

    private void resetClutch(boolean disableStuck) {
        boolean shouldDisableStuck = disableStuck && clutchActivated;
        clutchState = ClutchState.IDLE;
        clutchTarget = null;
        clutchActivated = false;
        clutchStartY = Double.NaN;
        clutchTicks = 0;
        clutchPlaceAttempts = 0;
        clutchReleaseTicks = 0;
        if (shouldDisableStuck) {
            StuckModule stuck = ModuleManager.INSTANCE.getByClass(StuckModule.class);
            if (stuck != null && stuck.isEnabled()) {
                stuck.setEnabled(false);
            }
        }
    }

    private float randomFloat(float min, float max) {
        return (float) ThreadLocalRandom.current().nextDouble(min, max);
    }

    private static Setting<Integer> rangedInt(String name, String description, int defaultValue, int min, int max, int step) {
        Setting<Integer> setting = new Setting<>(name, description, defaultValue);
        setting.setRange(min, max, step);
        return setting;
    }

    private static Setting<Double> rangedDouble(String name, String description, double defaultValue, double min, double max, double step) {
        Setting<Double> setting = new Setting<>(name, description, defaultValue);
        setting.setRange(min, max, step);
        return setting;
    }

    private enum ClutchState {
        IDLE,
        ARMED,
        RELEASE
    }

    private record ClutchTarget(BlockPos placePos, BlockPos support, Direction face, Vec3d hit, Vec2f rotation) {
    }

    private record PlaceTarget(BlockPos support, Direction face) {
    }
}
