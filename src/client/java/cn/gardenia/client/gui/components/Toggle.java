package cn.gardenia.client.gui.components;

import cn.gardenia.client.gui.animation.Animation;
import cn.gardenia.client.gui.animation.Easing;
import cn.gardenia.client.gui.nanovg.NanoVGRender;
import cn.gardenia.client.gui.theme.Theme;

import java.util.function.Consumer;

public class Toggle extends Component {
    private String label;
    private boolean state;
    private Consumer<Boolean> onChange;
    private float textSize = 13;
    private String font = "regular";
    private float toggleWidth = 36;
    private float toggleHeight = 20;
    private float knobSize = 14;
    private Animation stateAnimation;

    public Toggle(float x, float y, float width, float height, String label, boolean initialState) {
        super(x, y, width, height);
        this.label = label;
        this.state = initialState;
        this.stateAnimation = new Animation(initialState ? 1 : 0, Theme.ANIMATION_SPEED_FAST, Easing.EASE_OUT_CUBIC);
        this.hoverAnimation.setSpeed(Theme.ANIMATION_SPEED_FAST);
        this.hoverAnimation.setEasing(Easing.EASE_OUT_QUAD);
    }

    @Override
    public void render(float mouseX, float mouseY, float delta) {
        if (!visible) return;
        update();
        stateAnimation.update();

        float hover = getHoverProgress();
        float alpha = getAlpha();
        float stateProgress = stateAnimation.getCurrent();

        float toggleX = x + width - toggleWidth - 8;
        float toggleY = y + (height - toggleHeight) / 2;

        NanoVGRender.save();
        NanoVGRender.globalAlpha(alpha);

        int trackColor = Theme.blend(Theme.TOGGLE_OFF, Theme.TOGGLE_ON, stateProgress);
        NanoVGRender.drawRoundedRect(toggleX, toggleY, toggleWidth, toggleHeight, toggleHeight / 2, Theme.withAlpha(trackColor, alpha));

        float knobX = toggleX + 3 + (toggleWidth - knobSize - 6) * stateProgress;
        float knobY = toggleY + (toggleHeight - knobSize) / 2;

        int knobColor = Theme.blend(0xFF888888, 0xFFFFFFFF, stateProgress);
        NanoVGRender.drawCircle(knobX + knobSize / 2, knobY + knobSize / 2, knobSize / 2, Theme.withAlpha(knobColor, alpha));

        NanoVGRender.drawText(label, x + 8, y + height / 2,
                Theme.withAlpha(Theme.TEXT_PRIMARY, alpha), textSize, font);

        NanoVGRender.restore();
    }

    @Override
    protected void onClick(double mouseX, double mouseY, int button) {
        state = !state;
        stateAnimation.setTarget(state ? 1 : 0);
        if (onChange != null) {
            onChange.accept(state);
        }
    }

    public void setOnChange(Consumer<Boolean> onChange) {
        this.onChange = onChange;
    }

    public boolean getState() { return state; }
    public void setState(boolean state) {
        this.state = state;
        stateAnimation.setTarget(state ? 1 : 0);
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}
