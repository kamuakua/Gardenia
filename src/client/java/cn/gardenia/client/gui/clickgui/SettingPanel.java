package cn.gardenia.client.gui.clickgui;

import cn.gardenia.client.gui.animation.Animation;
import cn.gardenia.client.gui.animation.Easing;
import cn.gardenia.client.gui.components.ColorPicker;
import cn.gardenia.client.gui.components.Component;
import cn.gardenia.client.gui.components.ModeSelector;
import cn.gardenia.client.gui.components.Slider;
import cn.gardenia.client.gui.components.Toggle;
import cn.gardenia.client.gui.nanovg.NanoVGRender;
import cn.gardenia.client.gui.theme.Theme;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.Setting;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public class SettingPanel extends Component {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private Module module;
    private final List<Component> components = new ArrayList<>();
    private final Animation showAnimation;
    private float headerHeight = 38;
    private float contentPadding = 8;
    private float scrollOffset = 0;
    private float targetScroll = 0;
    private float maxScroll = 0;
    private float contentHeight = 0;
    private float maxHeight = 420;
    private float componentSpacing = 5;

    // Drag state
    private float targetX, targetY;
    private boolean draggingHeader = false;
    private double dragOffX, dragOffY;

    public SettingPanel(float x, float y, float width, Module module) {
        super(x, y, width, 0);
        this.targetX = x;
        this.targetY = y;
        this.module = module;
        this.showAnimation = new Animation(0, Theme.ANIMATION_SPEED_NORMAL, Easing.EASE_OUT_CUBIC);
        buildComponents();
        updateHeight();
    }

    public void setModule(Module module) {
        this.module = module;
        components.clear();
        buildComponents();
        updateHeight();
        scrollOffset = 0;
        targetScroll = 0;
        targetX = x;
        targetY = y;
    }

    private void buildComponents() {
        if (module == null) return;
        components.clear();
        float cy = 0;

        // Keybind row (always first)
        KeybindComponent kb = new KeybindComponent(contentPadding, cy, width - contentPadding * 2, module);
        components.add(kb);
        cy += kb.getHeight() + componentSpacing;

        for (Setting<?> setting : module.getSettings()) {
            Component comp = createComponentForSetting(setting, cy);
            if (comp != null) {
                components.add(comp);
                cy += comp.getHeight() + componentSpacing;
            }
        }
        contentHeight = cy;
    }

    /** Returns true if the keybind component is in listening mode. */
    public boolean isKeybindListening() {
        if (components.isEmpty()) return false;
        Component first = components.get(0);
        if (first instanceof KeybindComponent) {
            return ((KeybindComponent) first).isListening();
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private Component createComponentForSetting(Setting<?> setting, float yOffset) {
        float cw = width - contentPadding * 2;
        float cx = contentPadding;
        float cy = yOffset;

        return switch (setting.getType()) {
            case BOOLEAN -> {
                Toggle toggle = new Toggle(cx, cy, cw, 32, setting.getName(), setting.getBoolean());
                toggle.setOnChange(v -> ((Setting<Boolean>) setting).setValue(v));
                yield toggle;
            }
            case INTEGER -> {
                if (setting.getName().toLowerCase().contains("color") || setting.getType() == Setting.SettingType.COLOR) {
                    ColorPicker picker = new ColorPicker(cx, cy, cw, 32, setting.getName(), setting.getColor());
                    picker.setOnChange(v -> ((Setting<Integer>) setting).setValue(v));
                    yield picker;
                }
                Slider slider = new Slider(cx, cy, cw, 48, setting.getName(),
                        setting.getMin(), setting.getMax(), setting.getInt(), (int) setting.getStep());
                slider.setOnChange(v -> ((Setting<Integer>) setting).setValue(v.intValue()));
                yield slider;
            }
            case DOUBLE -> {
                Slider slider = new Slider(cx, cy, cw, 48, setting.getName(),
                        setting.getMin(), setting.getMax(), setting.getDouble(), setting.getStep());
                slider.setOnChange(v -> ((Setting<Double>) setting).setValue(v.doubleValue()));
                yield slider;
            }
            case MODE -> {
                ModeSelector selector = new ModeSelector(cx, cy, cw, 56, setting.getName(),
                        setting.getOptions(), 0);
                String current = setting.getString();
                String[] opts = setting.getOptions();
                for (int i = 0; i < opts.length; i++) {
                    if (opts[i].equals(current)) {
                        selector.setSelectedIndex(i);
                        break;
                    }
                }
                selector.setOnChange(v -> ((Setting<String>) setting).setValue(v));
                yield selector;
            }
            case COLOR -> {
                ColorPicker picker = new ColorPicker(cx, cy, cw, 32, setting.getName(), setting.getColor());
                picker.setOnChange(v -> ((Setting<Integer>) setting).setValue(v));
                yield picker;
            }
            default -> null;
        };
    }

    private void updateHeight() {
        float visibleContent = Math.min(contentHeight, maxHeight - headerHeight);
        this.height = headerHeight + visibleContent;
        maxScroll = Math.max(0, contentHeight - visibleContent);
    }

    @Override
    public void render(float mouseX, float mouseY, float delta) {
        if (!visible) return;

        // Smooth position follow
        x += (targetX - x) * 0.15f;
        y += (targetY - y) * 0.15f;

        update();
        showAnimation.update();

        float show = showAnimation.getCurrent();
        if (show < 0.01f) return;

        float alpha = show;
        float scale = 0.97f + 0.03f * show;

        NanoVGRender.save();
        NanoVGRender.globalAlpha(alpha);

        float cx = x + width / 2;
        float cy = y + height / 2;
        NanoVGRender.translate(cx, cy);
        NanoVGRender.scale(scale, scale);
        NanoVGRender.translate(-cx, -cy);

        NanoVGRender.drawShadow(x, y, width, height, Theme.CORNER_RADIUS_LARGE, 18, 0.18f);
        NanoVGRender.drawGlassmorphism(x, y, width, height, Theme.CORNER_RADIUS_LARGE, 1);

        NanoVGRender.drawRoundedRectTop(x, y, width, headerHeight, Theme.CORNER_RADIUS_LARGE,
                Theme.withAlpha(Theme.SURFACE, alpha));

        NanoVGRender.drawText(module.getName(), x + 14, y + headerHeight / 2,
                Theme.withAlpha(Theme.TEXT_PRIMARY, alpha), 14, "bold");

        NanoVGRender.drawTextRight("Settings", x + width - 14, y + headerHeight / 2,
                Theme.withAlpha(Theme.TEXT_SECONDARY, alpha), 11, "regular");

        NanoVGRender.drawLine(x + 10, y + headerHeight - 1, x + width - 10, y + headerHeight - 1, 1,
                Theme.withAlpha(Theme.BORDER, alpha * 0.5f));

        float contentY = y + headerHeight;
        float visibleContentHeight = height - headerHeight;

        if (visibleContentHeight > 0) {
            NanoVGRender.scissor(x, contentY, width, visibleContentHeight);

            scrollOffset += (targetScroll - scrollOffset) * 0.2f;

            float drawY = contentY + contentPadding - scrollOffset;
            for (Component comp : components) {
                comp.setX(x + contentPadding);
                comp.setY(drawY);
                comp.render(mouseX, mouseY, delta);
                drawY += comp.getHeight() + componentSpacing;
            }

            // 动态更新 contentHeight：支持 ColorPicker 等组件展开后实际高度的变化
            float actualContentHeight = 0;
            for (Component comp : components) {
                actualContentHeight += comp.getHeight() + componentSpacing;
            }
            if (Math.abs(contentHeight - actualContentHeight) > 1f) {
                contentHeight = actualContentHeight;
                updateHeight();
            }

            if (maxScroll > 0) {
                float scrollbarHeight = visibleContentHeight * (visibleContentHeight / contentHeight);
                float scrollbarY = contentY + (scrollOffset / maxScroll) * (visibleContentHeight - scrollbarHeight);
                NanoVGRender.drawRoundedRect(x + width - 5, scrollbarY, 3, scrollbarHeight, 1.5f,
                        Theme.withAlpha(0x60FFFFFF, alpha));
            }

            NanoVGRender.resetScissor();
        }

        NanoVGRender.restore();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        boolean overPanel = isMouseOver(mouseX, mouseY);

        // Header drag
        if (overPanel && mouseY >= y && mouseY < y + headerHeight) {
            if (button == 0) {
                draggingHeader = true;
                dragOffX = mouseX - targetX;
                dragOffY = mouseY - targetY;
            }
            return true;
        }

        for (Component comp : components) {
            if (comp.mouseClicked(mouseX, mouseY, button)) return true;
        }
        return overPanel;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingHeader = false;
        for (Component comp : components) comp.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingHeader) {
            targetX = (float) Math.max(0, Math.min(mc.getWindow().getScaledWidth() - width, mouseX - dragOffX));
            targetY = (float) Math.max(0, Math.min(mc.getWindow().getScaledHeight() - height, mouseY - dragOffY));
            return true;
        }
        for (Component comp : components) {
            if (comp.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        targetScroll = (float) Math.max(0, Math.min(maxScroll, targetScroll - amount * 25));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (Component comp : components) {
            if (comp.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return false;
    }

    @Override
    public void onMouseMove(double mouseX, double mouseY) {
        super.onMouseMove(mouseX, mouseY);
        for (Component comp : components) comp.onMouseMove(mouseX, mouseY);
    }

    public void show() {
        visible = true;
        showAnimation.setTarget(1);
    }

    public void hide() {
        showAnimation.setTarget(0);
    }
}
