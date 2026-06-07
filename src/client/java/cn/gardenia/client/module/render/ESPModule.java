package cn.gardenia.client.module.render;

import cn.gardenia.client.event.events.render.Render3DEvent;
import cn.gardenia.client.event.events.render.RenderHudEvent;
import cn.gardenia.client.gui.nanovg.NanoVGManager;
import cn.gardenia.client.gui.nanovg.NanoVGRender;
import cn.gardenia.client.gui.theme.Theme;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.Setting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.function.Consumer;

public class ESPModule extends Module {

    private final Setting<String> mode = new Setting<>(
            "Mode", "ESP render mode", "2D",
            new String[]{"2D", "3D"});

    private final Setting<Boolean> players = new Setting<>("Players", "Target players", true);
    private final Setting<Boolean> mobs = new Setting<>("Mobs", "Target hostile mobs", true);
    private final Setting<Boolean> animals = new Setting<>("Animals", "Target passive mobs", false);
    private final Setting<Boolean> invisible = new Setting<>("Invisible", "Show invisible entities", true);

    private final Setting<Integer> playerColor = Setting.color("Player Color", "Color for players", 0xFFFF3333);
    private final Setting<Integer> mobColor = Setting.color("Mob Color", "Color for hostile mobs", 0xFFAA55FF);
    private final Setting<Integer> animalColor = Setting.color("Animal Color", "Color for passive mobs", 0xFF33FF33);

    private final Setting<Boolean> showBox = new Setting<>("Box", "Show 2D box outline", true);
    private final Setting<Boolean> showName = new Setting<>("Name", "Show name tag", true);
    private final Setting<Boolean> showHealth = new Setting<>("Health", "Show health bar", true);
    private final Setting<Boolean> showDistance = new Setting<>("Distance", "Show distance", true);
    private final Setting<Boolean> showTracer = new Setting<>("Tracer", "Show tracer lines", true);

    private final Setting<Integer> lineWidth = new Setting<>("Line Width", "3D box line width", 2, 1, 5);

    private final Consumer<Render3DEvent> render3DListener = this::onRender3D;
    private final Consumer<RenderHudEvent> renderHudListener = this::onRenderHud;

    public ESPModule() {
        super("ESP", "Highlight entities through walls", Category.RENDER);
        addSetting(mode);
        addSetting(players);
        addSetting(mobs);
        addSetting(animals);
        addSetting(invisible);
        addSetting(playerColor);
        addSetting(mobColor);
        addSetting(animalColor);
        addSetting(showBox);
        addSetting(showName);
        addSetting(showHealth);
        addSetting(showDistance);
        addSetting(showTracer);
        addSetting(lineWidth);
    }

    @Override
    public void onEnable() {
        subscribe(Render3DEvent.class, render3DListener);
        subscribe(RenderHudEvent.class, renderHudListener);
    }

    @Override
    public void onDisable() {
        unsubscribe(Render3DEvent.class, render3DListener);
        unsubscribe(RenderHudEvent.class, renderHudListener);
    }

    private void onRender3D(Render3DEvent event) {
        if (mc.world == null || mc.player == null) return;
        if (mc.gameRenderer == null || mc.gameRenderer.getCamera() == null) return;

        if ("3D".equals(mode.getString())) {
            render3DBoxes();
        }
    }

    private void onRenderHud(RenderHudEvent event) {
        if (mc.world == null || mc.player == null) return;
        if (!"2D".equals(mode.getString())) return;

        render2DOverlay();
    }

    // ── 3D ────────────────────────────────────────────────────────

    private void render3DBoxes() {
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.lineWidth(lineWidth.getInt());

        Matrix4fStack mvStack = RenderSystem.getModelViewStack();
        mvStack.pushMatrix();
        mvStack.identity();

        Quaternionf camRot = camera.getRotation();
        Quaternionf invRot = new Quaternionf(camRot).conjugate();
        mvStack.rotate(camRot);

        VertexConsumerProvider.Immediate providers =
                mc.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer lines = providers.getBuffer(RenderLayer.LINES);

        MatrixStack matrices = new MatrixStack();

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player || !isValidTarget(entity)) continue;

            Vec3d entityPos = entity.getPos();
            double relX = entityPos.x - camPos.x;
            double relY = entityPos.y - camPos.y;
            double relZ = entityPos.z - camPos.z;

            Vector3f relVec = new Vector3f((float) relX, (float) relY, (float) relZ);
            relVec.rotate(invRot);

            matrices.push();
            matrices.translate(relVec.x(), relVec.y(), relVec.z());

            Box box = entity.getBoundingBox().offset(-entityPos.x, -entityPos.y, -entityPos.z);
            int c = getEntityColorValue(entity);
            float r = ((c >> 16) & 0xFF) / 255f;
            float g = ((c >> 8) & 0xFF) / 255f;
            float b = (c & 0xFF) / 255f;

            VertexRendering.drawBox(matrices, lines, box, r, g, b, 1.0f);

            matrices.pop();
        }

        providers.draw(RenderLayer.LINES);

        mvStack.popMatrix();
        RenderSystem.lineWidth(1.0f);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }

    // ── 2D ────────────────────────────────────────────────────────

    private void render2DOverlay() {
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

        float sw = mc.getWindow().getScaledWidth();
        float sh = mc.getWindow().getScaledHeight();
        String font = NanoVGManager.INSTANCE.hasFont("chinese") ? "chinese" : "regular";

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player || !isValidTarget(entity)) continue;

            float[] bounds = projectBoundingBox(entity.getBoundingBox());
            if (bounds == null) continue;

            float minX = bounds[0], minY = bounds[1];
            float maxX = bounds[2], maxY = bounds[3];
            int color = getEntityColorValue(entity);

            if (showTracer.getBoolean()) {
                float cx = (minX + maxX) / 2;
                NanoVGRender.drawLine(sw / 2, sh, cx, maxY, 1.0f,
                        Theme.withAlpha(color, 0.5f));
            }

            if (showBox.getBoolean()) {
                NanoVGRender.drawRoundedRectStroke(minX, minY,
                        maxX - minX, maxY - minY, 1, 1.5f,
                        Theme.withAlpha(color, 0.9f));
            }

            if (showName.getBoolean() && entity.getDisplayName() != null) {
                String name = entity.getDisplayName().getString();
                float fontSize = 11;
                float textW = NanoVGRender.textWidth(name, fontSize, font);
                float tx = (minX + maxX) / 2;
                float ty = minY - fontSize / 2 - 3;

                NanoVGRender.drawRect(tx - textW / 2 - 2, ty - fontSize / 2 - 1,
                        textW + 4, fontSize + 2, 0x88000000);
                NanoVGRender.drawTextCenter(name, tx, ty,
                        Theme.withAlpha(color, 0.95f), fontSize, font);
            }

            if (showHealth.getBoolean() && entity instanceof LivingEntity living) {
                float ratio = Math.max(0, Math.min(1,
                        living.getHealth() / living.getMaxHealth()));
                float barW = maxX - minX;
                float barX = minX;
                float barY = maxY + 1;

                NanoVGRender.drawRect(barX, barY, barW, 2, 0x88000000);
                int hpColor = ratio > 0.5 ? 0xFF33FF33
                        : ratio > 0.25 ? 0xFFFFFF33 : 0xFFFF3333;
                NanoVGRender.drawRect(barX, barY, barW * ratio, 2, hpColor);
            }

            if (showDistance.getBoolean()) {
                float dist = mc.player.distanceTo(entity);
                String distText = String.format("%.0fm", dist);
                float fontSize = 10;
                float ty = maxY + (entity instanceof LivingEntity
                        && showHealth.getBoolean() ? 5 : 2) + fontSize / 2;

                NanoVGRender.drawTextCenter(distText,
                        (minX + maxX) / 2, ty,
                        Theme.withAlpha(0xFFAAAAAA, 0.7f), fontSize, font);
            }
        }

        if (startedFrame) {
            NanoVGManager.INSTANCE.endFrame();
        }
    }

    // ── World → Screen ────────────────────────────────────────────

    private float[] worldToScreen(Vec3d worldPos) {
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();

        double relX = worldPos.x - camPos.x;
        double relY = worldPos.y - camPos.y;
        double relZ = worldPos.z - camPos.z;

        Quaternionf invRot = new Quaternionf(camera.getRotation()).conjugate();
        Vector3f vec = new Vector3f((float) relX, (float) relY, (float) relZ);
        vec.rotate(invRot);

        float fov = mc.options.getFov().getValue();
        float aspect = (float) mc.getWindow().getScaledWidth() / (float) mc.getWindow().getScaledHeight();

        float fovRad = (float) Math.toRadians(fov);
        float tanFov = (float) Math.tan(fovRad / 2.0f);

        if (vec.z() >= -0.001f) return null;

        float ndcX = vec.x() / (-vec.z() * tanFov * aspect);
        float ndcY = vec.y() / (-vec.z() * tanFov);

        float screenX = (ndcX + 1) / 2 * mc.getWindow().getScaledWidth();
        float screenY = (1 - ndcY) / 2 * mc.getWindow().getScaledHeight();

        return new float[]{screenX, screenY};
    }

    private float[] projectBoundingBox(Box box) {
        Vec3d[] corners = new Vec3d[]{
                new Vec3d(box.minX, box.minY, box.minZ),
                new Vec3d(box.maxX, box.minY, box.minZ),
                new Vec3d(box.minX, box.maxY, box.minZ),
                new Vec3d(box.maxX, box.maxY, box.minZ),
                new Vec3d(box.minX, box.minY, box.maxZ),
                new Vec3d(box.maxX, box.minY, box.maxZ),
                new Vec3d(box.minX, box.maxY, box.maxZ),
                new Vec3d(box.maxX, box.maxY, box.maxZ)
        };

        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;
        boolean anyVisible = false;

        for (Vec3d corner : corners) {
            float[] screen = worldToScreen(corner);
            if (screen == null) continue;
            anyVisible = true;
            minX = Math.min(minX, screen[0]);
            minY = Math.min(minY, screen[1]);
            maxX = Math.max(maxX, screen[0]);
            maxY = Math.max(maxY, screen[1]);
        }

        if (!anyVisible) return null;
        return new float[]{minX, minY, maxX, maxY};
    }

    // ── Target / Color ────────────────────────────────────────────

    private boolean isValidTarget(Entity entity) {
        if (!entity.isAlive()) return false;
        if (entity.isInvisible() && !invisible.getBoolean()) return false;

        if (entity instanceof PlayerEntity) return players.getBoolean();
        if (entity instanceof HostileEntity) return mobs.getBoolean();
        if (entity instanceof PassiveEntity) return animals.getBoolean();
        return false;
    }

    private int getEntityColorValue(Entity entity) {
        if (entity instanceof PlayerEntity) return playerColor.getValue();
        if (entity instanceof HostileEntity) return mobColor.getValue();
        if (entity instanceof PassiveEntity) return animalColor.getValue();
        return 0xFFFFFFFF;
    }
}
