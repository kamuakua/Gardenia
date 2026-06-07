package cn.gardenia.client.gui.clickgui;

import cn.gardenia.client.gui.animation.Animation;
import cn.gardenia.client.gui.animation.Easing;
import cn.gardenia.client.gui.components.*;
import cn.gardenia.client.gui.nanovg.NanoVGManager;
import cn.gardenia.client.gui.nanovg.NanoVGRender;
import cn.gardenia.client.gui.theme.Theme;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.ModuleManager;
import cn.gardenia.client.module.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class ClickGUIScreen extends Screen {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static ClickGUIScreen INSTANCE;

    private final List<CategoryPanel> panels = new ArrayList<>();
    private final Animation openAnimation;
    private boolean closing = false;
    private float panelStartX = 60;
    private float panelStartY = 40;
    private float panelGap = 16;
    private float panelWidth = 220;

    private CategoryPanel selectedPanel = null;
    private SettingPanel settingPanel = null;
    private Module selectedModule = null;

    private double lastMouseX, lastMouseY;
    private boolean initialized = false;

    public ClickGUIScreen() {
        super(Text.literal("ClickGUI"));
        this.openAnimation = new Animation(0, 0.12f, Easing.EASE_OUT_CUBIC);
    }

    public static ClickGUIScreen getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ClickGUIScreen();
        }
        return INSTANCE;
    }

    public static void open() {
        ClickGUIScreen gui = getInstance();
        gui.closing = false;
        gui.openAnimation.setTarget(1);
        if (mc.currentScreen != gui) {
            mc.setScreen(gui);
        }
    }

    public static void closeGUI() {
        if (INSTANCE != null) {
            INSTANCE.closing = true;
            INSTANCE.openAnimation.setTarget(0);
        }
    }

    @Override
    protected void init() {
        super.init();
        if (!NanoVGManager.INSTANCE.isInitialized()) {
            NanoVGManager.INSTANCE.init();
        }
        if (!initialized) {
            buildPanels();
            initialized = true;
        }
        openAnimation.setTarget(1);
        closing = false;
    }

    private void buildPanels() {
        panels.clear();
        Category[] categories = Category.values();
        float x = panelStartX;

        for (int i = 0; i < categories.length; i++) {
            Category cat = categories[i];
            List<Module> modules = ModuleManager.INSTANCE.getModulesByCategory(cat);
            if (modules.isEmpty()) continue;

            CategoryPanel panel = new CategoryPanel(x, panelStartY, panelWidth, cat, modules, i);
            panel.setOnModuleSelect(this::onModuleSelect);
            panels.add(panel);
            x += panelWidth + panelGap;
        }
    }

    private void onModuleSelect(Module module) {
        // Right-click same module → close panel
        if (module == selectedModule && settingPanel != null) {
            settingPanel.hide();
            selectedModule = null;
            return;
        }

        // Different module or no panel → open / switch
        if (settingPanel == null) {
            settingPanel = new SettingPanel(width - 270, panelStartY, 240, module);
        } else {
            settingPanel.setModule(module);
        }
        selectedModule = module;
        settingPanel.show();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        if (!NanoVGManager.INSTANCE.isInitialized()) return;

        openAnimation.update();
        float anim = openAnimation.getCurrent();

        if (closing && openAnimation.isFinished()) {
            mc.setScreen(null);
            return;
        }

        float scale = 0.95f + 0.05f * anim;
        float alpha = anim;

        NanoVGManager.INSTANCE.beginFrame(width, height, (float) mc.getWindow().getScaleFactor());

        NanoVGRender.save();
        NanoVGRender.globalAlpha(alpha);

        for (int i = 0; i < panels.size(); i++) {
            CategoryPanel panel = panels.get(i);
            NanoVGRender.save();
            float staggerDelay = i * 0.06f;
            float stagger = Math.max(0, Math.min(1, (anim - staggerDelay) / (1 - staggerDelay)));
            if (stagger > 0.001f) {
                float panelScale = 0.92f + 0.08f * stagger;
                float panelAlpha = stagger;
                float cx = panel.getX() + panel.getWidth() / 2;
                float cy = panel.getY() + panel.getHeight() / 2;
                NanoVGRender.translate(cx, cy);
                NanoVGRender.scale(panelScale, panelScale);
                NanoVGRender.translate(-cx, -cy);
                NanoVGRender.globalAlpha(panelAlpha);
                panel.render(mouseX, mouseY, delta);
            }
            NanoVGRender.restore();
        }

        if (settingPanel != null && settingPanel.isVisible()) {
            NanoVGRender.save();
            float sx = settingPanel.getX() + settingPanel.getWidth() / 2;
            float sy = settingPanel.getY() + settingPanel.getHeight() / 2;
            NanoVGRender.translate(sx, sy);
            NanoVGRender.scale(scale, scale);
            NanoVGRender.translate(-sx, -sy);
            settingPanel.render(mouseX, mouseY, delta);
            NanoVGRender.restore();
        }

        NanoVGRender.restore();
        NanoVGManager.INSTANCE.endFrame();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = false;
        if (settingPanel != null && settingPanel.isVisible()) {
            handled = settingPanel.mouseClicked(mouseX, mouseY, button);
            if (handled) return true;
        }
        for (CategoryPanel panel : panels) {
            if (panel.mouseClicked(mouseX, mouseY, button)) {
                handled = true;
                break;
            }
        }
        return handled || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (settingPanel != null) settingPanel.mouseReleased(mouseX, mouseY, button);
        for (CategoryPanel panel : panels) panel.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (settingPanel != null) settingPanel.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        for (CategoryPanel panel : panels) panel.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (settingPanel != null && settingPanel.mouseScrolled(mouseX, mouseY, verticalAmount)) return true;
        for (CategoryPanel panel : panels) {
            if (panel.mouseScrolled(mouseX, mouseY, verticalAmount)) return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // If keybind is listening, route all keys to it first (including ESC)
        if (settingPanel != null && settingPanel.isKeybindListening()) {
            if (settingPanel.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            Module clickGui = ModuleManager.INSTANCE.getByName("ClickGUI");
            if (clickGui != null && clickGui.isEnabled()) {
                clickGui.setEnabled(false);
            } else {
                closeGUI();
            }
            return true;
        }
        if (settingPanel != null) settingPanel.keyPressed(keyCode, scanCode, modifiers);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);
        if (settingPanel != null) settingPanel.onMouseMove(mouseX, mouseY);
        for (CategoryPanel panel : panels) panel.onMouseMove(mouseX, mouseY);
        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    public void refresh() {
        initialized = false;
        panels.clear();
        settingPanel = null;
    }
}
