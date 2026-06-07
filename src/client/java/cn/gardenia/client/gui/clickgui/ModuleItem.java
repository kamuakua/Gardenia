package cn.gardenia.client.gui.clickgui;

import cn.gardenia.client.gui.animation.Animation;
import cn.gardenia.client.gui.animation.Easing;
import cn.gardenia.client.gui.components.Component;
import cn.gardenia.client.gui.nanovg.NanoVGRender;
import cn.gardenia.client.gui.theme.Theme;
import cn.gardenia.client.module.Module;

import java.util.function.Consumer;

public class ModuleItem extends Component {
    private final Module module;
    private Consumer<Module> onRightClick;
    private float textSize = 12;
    private String font = "regular";
    private Animation enableAnimation;
    private Animation slideAnimation;

    public ModuleItem(float x, float y, float width, float height, Module module) {
        super(x, y, width, height);
        this.module = module;
        this.enableAnimation = new Animation(module.isEnabled() ? 1 : 0, Theme.ANIMATION_SPEED_FAST, Easing.EASE_OUT_CUBIC);
        this.slideAnimation = new Animation(0, Theme.ANIMATION_SPEED_NORMAL, Easing.EASE_OUT_BACK);
        this.hoverAnimation.setSpeed(Theme.ANIMATION_SPEED_FAST);
        this.hoverAnimation.setEasing(Easing.EASE_OUT_QUAD);
    }

    @Override
    public void render(float mouseX, float mouseY, float delta) {
        if (!visible) return;
        update();
        enableAnimation.update();
        slideAnimation.update();

        float hover = getHoverProgress();
        float alpha = getAlpha();
        float enabled = enableAnimation.getCurrent();
        float slide = slideAnimation.getCurrent();

        NanoVGRender.save();
        NanoVGRender.globalAlpha(alpha);

        float itemX = x + slide * 4;

        int bg = Theme.blend(0, Theme.SURFACE_HOVER, hover);
        bg = Theme.blend(bg, Theme.SURFACE_ACTIVE, enabled * 0.5f);
        if (bg != 0) {
            NanoVGRender.drawRoundedRect(itemX, y, width, height, Theme.CORNER_RADIUS_SMALL, Theme.withAlpha(bg, alpha));
        }

        float indicatorWidth = 3;
        if (enabled > 0.01f) {
            NanoVGRender.drawRoundedRect(itemX, y + (height - 14) / 2, indicatorWidth * enabled, 14, indicatorWidth / 2,
                    Theme.withAlpha(Theme.ACCENT, alpha));
        }

        NanoVGRender.drawText(module.getName(), itemX + 10 + indicatorWidth, y + height / 2,
                Theme.withAlpha(Theme.blend(Theme.TEXT_SECONDARY, Theme.TEXT_PRIMARY, enabled), alpha), textSize, font);

        float toggleX = itemX + width - 26;
        float toggleY = y + (height - 12) / 2;
        float toggleW = 20;
        float toggleH = 12;
        float knobR = 4;

        NanoVGRender.drawRoundedRect(toggleX, toggleY, toggleW, toggleH, toggleH / 2,
                Theme.withAlpha(Theme.blend(Theme.TOGGLE_OFF, Theme.TOGGLE_ON, enabled), alpha));

        float knobX = toggleX + 2 + (toggleW - knobR * 2 - 4) * enabled;
        float knobY = toggleY + toggleH / 2;
        NanoVGRender.drawCircle(knobX + knobR, knobY, knobR, Theme.withAlpha(0xFFFFFFFF, alpha));

        NanoVGRender.restore();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !isMouseOver(mouseX, mouseY)) return false;
        if (button == 0) {
            module.toggle();
            enableAnimation.setTarget(module.isEnabled() ? 1 : 0);
            slideAnimation.reset(0);
            slideAnimation.setTarget(1);
        } else if (button == 1) {
            if (onRightClick != null) {
                onRightClick.accept(module);
            }
        }
        return true;
    }

    public void setOnRightClick(Consumer<Module> onRightClick) {
        this.onRightClick = onRightClick;
    }

    public Module getModule() { return module; }
}
