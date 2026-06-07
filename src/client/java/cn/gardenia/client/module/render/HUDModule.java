package cn.gardenia.client.module.render;

import cn.gardenia.client.gui.hud.HudManager;
import cn.gardenia.client.gui.hud.HudStyle;
import cn.gardenia.client.gui.hud.elements.ArrayListElement;
import cn.gardenia.client.gui.hud.elements.CoordsElement;
import cn.gardenia.client.gui.hud.elements.FpsElement;
import cn.gardenia.client.gui.hud.elements.PingElement;
import cn.gardenia.client.gui.hud.elements.TargetHudElement;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.Setting;

public class HUDModule extends Module {

    // ── 颜色设置 ──────────────────────────────────────────────────
    private final Setting<Integer> textColor = Setting.color(
            "Text Color", "HUD text color", 0xFFFFFFFF,
            this::applyTextColor
    );
    private final Setting<Integer> accentColor = Setting.color(
            "Accent Color", "HUD accent color", 0xFF888888,
            this::applyAccentColor
    );

    // ── 样式设置 ──────────────────────────────────────────────────
    private final Setting<String> style = new Setting<>(
            "Style", "HUD card style", "Modern",
            this::applyStyle,
            new String[]{"Minimal", "Modern", "Glass", "Outline", "Block"}
    );

    // ── 元素可见性 ────────────────────────────────────────────────
    private final Setting<Boolean> showFps = new Setting<>(
            "Show FPS", "Show FPS counter", true,
            v -> updateVisibility()
    );
    private final Setting<Boolean> showCoords = new Setting<>(
            "Show Coords", "Show coordinates", true,
            v -> updateVisibility()
    );
    private final Setting<Boolean> showPing = new Setting<>(
            "Show Ping", "Show ping", true,
            v -> updateVisibility()
    );
    private final Setting<Boolean> showArrayList = new Setting<>(
            "Show Array List", "Show enabled modules list", true,
            v -> updateVisibility()
    );
    private final Setting<Boolean> showTargetHud = new Setting<>(
            "Show Target HUD", "Show target info overlay", false,
            v -> updateVisibility()
    );

    // ── 外观微调 ──────────────────────────────────────────────────
    private final Setting<Integer> fontSize = new Setting<>(
            "Font Size", "HUD font size", 14, 10, 24,
            this::applyFontSize
    );
    private final Setting<Integer> bgOpacity = new Setting<>(
            "BG Opacity", "Background opacity (0-100)", 88, 0, 100,
            this::applyBgOpacity
    );

    // ── 工具方法 ──────────────────────────────────────────────────
    private static HudStyle parseStyle(String name) {
        for (HudStyle hs : HudStyle.values()) {
            if (hs.getName().equalsIgnoreCase(name)) return hs;
        }
        return HudStyle.MODERN;
    }

    private FpsElement fps()    { return (FpsElement) HudManager.INSTANCE.getElements().get(0); }
    private CoordsElement coords() { return (CoordsElement) HudManager.INSTANCE.getElements().get(1); }
    private PingElement ping()  { return (PingElement) HudManager.INSTANCE.getElements().get(2); }
    private ArrayListElement arrayList() { return (ArrayListElement) HudManager.INSTANCE.getElements().get(3); }
    private TargetHudElement targetHud() { return (TargetHudElement) HudManager.INSTANCE.getElements().get(4); }

    private void applyTextColor(int color) {
        fps().setTextColor(color);
        coords().setTextColor(color);
        ping().setTextColor(color);
        arrayList().setTextColor(color);
        targetHud().setTextColor(color);
    }

    private void applyAccentColor(int color) {
        fps().setAccentColor(color);
        coords().setAccentColor(color);
        ping().setAccentColor(color);
        arrayList().setAccentColor(color);
        targetHud().setAccentColor(color);
    }

    private void applyStyle(String styleName) {
        HudStyle hs = parseStyle(styleName);
        fps().setStyle(hs);
        coords().setStyle(hs);
        ping().setStyle(hs);
        arrayList().setStyle(hs);
        targetHud().setStyle(hs);
    }

    private void applyFontSize(int size) {
        fps().setFontSize(size);
        coords().setFontSize(size);
        ping().setFontSize(size);
        arrayList().setFontSize(size);
        targetHud().setFontSize(size);
    }

    private void applyBgOpacity(int opacity) {
        float o = opacity / 100f;
        fps().setBgOpacity(o);
        coords().setBgOpacity(o);
        ping().setBgOpacity(o);
        arrayList().setBgOpacity(o);
        targetHud().setBgOpacity(o);
    }

    private void updateVisibility() {
        fps().setVisible(showFps.getBoolean());
        coords().setVisible(showCoords.getBoolean());
        ping().setVisible(showPing.getBoolean());
        arrayList().setVisible(showArrayList.getBoolean());
        targetHud().setVisible(showTargetHud.getBoolean());
    }

    // ── 构造函数 ──────────────────────────────────────────────────
    public HUDModule() {
        super("HUD", "Toggle and customize all HUD elements", Category.RENDER);
        addSetting(textColor);
        addSetting(accentColor);
        addSetting(style);
        addSetting(fontSize);
        addSetting(bgOpacity);
        addSetting(showFps);
        addSetting(showCoords);
        addSetting(showPing);
        addSetting(showArrayList);
        addSetting(showTargetHud);
    }

    @Override
    public void onEnable() {
        HudManager.INSTANCE.setActive(true);

        // 读取配置文件里的已保存值，应用到所有元素
        applyTextColor(textColor.getValue());
        applyAccentColor(accentColor.getValue());
        applyStyle(style.getString());
        applyFontSize(fontSize.getInt());
        applyBgOpacity(bgOpacity.getInt());
        updateVisibility();
    }

    @Override
    public void onDisable() {
        HudManager.INSTANCE.setActive(false);
        fps().setVisible(false);
        coords().setVisible(false);
        ping().setVisible(false);
        arrayList().setVisible(false);
        targetHud().setVisible(false);
    }
}
