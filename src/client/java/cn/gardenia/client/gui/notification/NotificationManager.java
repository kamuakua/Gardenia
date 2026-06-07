package cn.gardenia.client.gui.notification;

import cn.gardenia.client.event.EventBus;
import cn.gardenia.client.event.events.render.RenderOverlayEvent;
import cn.gardenia.client.gui.nanovg.NanoVGManager;
import cn.gardenia.client.gui.nanovg.NanoVGRender;
import cn.gardenia.client.gui.theme.Theme;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NotificationManager {
    public static final NotificationManager INSTANCE = new NotificationManager();
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private final List<Notification> notifications = new ArrayList<>();
    private static final int MAX_STACK = 4;
    private boolean initialized = false;
    private boolean moduleEnabled = true; // controlled by NotificationModule

    // ── layout ──
    private static final float W = 240f;
    private static final float H = 42f;
    private static final float GAP = 5f;
    private static final float CR = 8f;
    private static final float ICON_SZ = 14f;
    private static final float ICON_L = 10f;
    private static final float TX_L = ICON_L + ICON_SZ + 7f;
    private static final float TITLE_Y = 15f;
    private static final float DESC_Y = 28f;
    private static final float TITLE_SZ = 12f;
    private static final float DESC_SZ = 10f;

    private NotificationManager() {}

    public void init() {
        if (initialized) return;
        // 预注册名称，图像延迟到 NanoVG 就绪后再真正加载
        EventBus.INSTANCE.subscribe(RenderOverlayEvent.class, this::onRenderOverlay);
        initialized = true;
    }

    /** 确保图标已加载（NanoVG context 就绪后调用） */
    private void ensureIcons() {
        if (NanoVGManager.INSTANCE.getImage("notification_on") != -1) return;
        NanoVGManager.INSTANCE.loadImage("notification_on", "gardenia/icons/on.png");
        NanoVGManager.INSTANCE.loadImage("notification_off", "gardenia/icons/off.png");
    }

    public void show(Notification n) {
        if (!moduleEnabled) return;
        // If max stack would be exceeded, fast-exit the oldest entering/displaying notification
        if (notifications.size() >= MAX_STACK) {
            for (Notification old : notifications) {
                if (!old.isExiting() && !old.isFinished()) {
                    old.fastExit();
                    break;
                }
            }
        }
        notifications.add(n);
    }

    public void show(String title, String description, boolean enabled) {
        String icon = enabled ? "notification_on" : "notification_off";
        show(new Notification(title, description, icon, 2000));
    }

    private void onRenderOverlay(RenderOverlayEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (!moduleEnabled || notifications.isEmpty()) return;

        if (!NanoVGManager.INSTANCE.isInitialized()) {
            NanoVGManager.INSTANCE.init();
        }
        // 确保图标已加载（即使 NanoVG 早已被 HudManager 初始化）
        ensureIcons();

        float sw = mc.getWindow().getScaledWidth();
        float sh = mc.getWindow().getScaledHeight();

        NanoVGManager.INSTANCE.beginFrame((int) sw, (int) sh, (float) mc.getWindow().getScaleFactor());

        // Remove finished ones
        notifications.removeIf(Notification::isFinished);

        // Recalculate stack Y positions
        recalcStack(sh);

        // Tick & render each
        for (Notification n : notifications) {
            n.update();
            render(n, sw);
        }

        NanoVGManager.INSTANCE.endFrame();
    }

    private void recalcStack(float sh) {
        int n = 0;
        for (Notification nt : notifications) {
            if (!nt.isExiting()) n++;
        }
        // Include exiting ones in height calc so they fade out in place
        int total = notifications.size();
        float stackH = total * (H + GAP) - GAP;
        float baseY = sh / 2f - stackH / 2f;
        for (int i = 0; i < total; i++) {
            notifications.get(i).setTargetY(baseY + i * (H + GAP));
        }
    }

    private void render(Notification n, float sw) {
        float a = n.getRenderAlpha();
        if (a < 0.005f) return;

        float cx = sw / 2f;
        float y = n.getCurrentY();
        float entrySlide = n.getRenderEntrySlide();
        float scale = n.getRenderScale();

        long ctx = NanoVGManager.INSTANCE.getContext();

        // Entry slide: subtle horizontal drift (fades in from right)
        float drawX = cx - W / 2f + entrySlide;

        NanoVGRender.save();
        // Scale around center
        org.lwjgl.nanovg.NanoVG.nvgTranslate(ctx, cx, y + H / 2f);
        org.lwjgl.nanovg.NanoVG.nvgScale(ctx, scale, scale);
        org.lwjgl.nanovg.NanoVG.nvgTranslate(ctx, -cx, -(y + H / 2f));

        // Shadow
        NanoVGRender.drawShadow(drawX, y, W, H, CR, 12, 0.12f * a);

        // ── Glass background ──
        int bg = ((int) (a * 0.50f * 255) << 24) | 0xFFFFFF;
        NanoVGRender.drawRoundedRect(drawX, y, W, H, CR, bg);

        // Subtle border
        int border = ((int) (a * 0.12f * 255) << 24);
        NanoVGRender.drawRoundedRectStroke(drawX, y, W, H, CR, 1f, border);

        // Icon
        int iconId = NanoVGManager.INSTANCE.getImage(n.getIconName());
        if (iconId != -1) {
            NanoVGRender.drawImage(drawX + ICON_L, y + (H - ICON_SZ) / 2f, ICON_SZ, ICON_SZ, iconId, a);
        }

        // Title
        NanoVGRender.drawText(n.getTitle(),
                drawX + TX_L, y + TITLE_Y,
                Theme.withAlpha(Theme.TEXT_PRIMARY, a), TITLE_SZ, "bold");

        // Description
        NanoVGRender.drawText(n.getDescription(),
                drawX + TX_L, y + DESC_Y,
                Theme.withAlpha(Theme.TEXT_SECONDARY, a), DESC_SZ, "regular");

        NanoVGRender.restore();
    }

    public boolean isActive() {
        return moduleEnabled && !notifications.isEmpty();
    }

    public void setModuleEnabled(boolean enabled) {
        this.moduleEnabled = enabled;
        if (!enabled) {
            clearAll();
        }
    }

    public boolean isModuleEnabled() {
        return moduleEnabled;
    }

    public void clearAll() {
        for (Notification n : notifications) {
            if (!n.isFinished()) n.fastExit();
        }
    }
}
