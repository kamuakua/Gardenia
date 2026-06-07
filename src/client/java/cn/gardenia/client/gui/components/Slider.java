package cn.gardenia.client.gui.components;

import cn.gardenia.client.gui.animation.Animation;
import cn.gardenia.client.gui.animation.Easing;
import cn.gardenia.client.gui.nanovg.NanoVGRender;
import cn.gardenia.client.gui.theme.Theme;

import java.util.function.Consumer;

public class Slider extends Component {
    private String label;
    private double min, max, value;
    private double step;
    private Consumer<Double> onChange;
    private float textSize = 12;
    private String font = "regular";
    private boolean dragging = false;
    private float trackHeight = 4;
    private float handleSize = 12;
    private Animation valueAnimation;
    private String suffix = "";

    public Slider(float x, float y, float width, float height, String label, double min, double max, double initial, double step) {
        super(x, y, width, height);
        this.label = label;
        this.min = min;
        this.max = max;
        this.value = initial;
        this.step = step;
        double progress = (initial - min) / (max - min);
        this.valueAnimation = new Animation((float) progress, Theme.ANIMATION_SPEED_FAST, Easing.EASE_OUT_CUBIC);
        this.hoverAnimation.setSpeed(Theme.ANIMATION_SPEED_FAST);
        this.hoverAnimation.setEasing(Easing.EASE_OUT_QUAD);
    }

    @Override
    public void render(float mouseX, float mouseY, float delta) {
        if (!visible) return;
        update();
        valueAnimation.update();

        float hover = getHoverProgress();
        float alpha = getAlpha();
        float progress = valueAnimation.getCurrent();

        float trackY = y + height - 18;
        float trackWidth = width - 16;
        float trackX = x + 8;

        NanoVGRender.save();
        NanoVGRender.globalAlpha(alpha);

        NanoVGRender.drawRoundedRect(trackX, trackY + (trackHeight - 4) / 2, trackWidth, trackHeight, trackHeight / 2,
                Theme.withAlpha(Theme.SLIDER_TRACK, alpha));

        float fillWidth = trackWidth * progress;
        NanoVGRender.drawRoundedRect(trackX, trackY + (trackHeight - 4) / 2, fillWidth, trackHeight, trackHeight / 2,
                Theme.withAlpha(Theme.SLIDER_FILL, alpha));

        float handleX = trackX + fillWidth;
        float handleY = trackY + trackHeight / 2;
        float handleRadius = handleSize / 2 + hover * 2;

        NanoVGRender.drawCircle(handleX, handleY, handleRadius, Theme.withAlpha(Theme.TEXT_PRIMARY, alpha));
        NanoVGRender.drawCircle(handleX, handleY, handleRadius - 2, Theme.withAlpha(Theme.BACKGROUND, alpha));

        String valueText = formatValue(value) + suffix;
        NanoVGRender.drawTextRight(valueText, x + width - 8, y + 10,
                Theme.withAlpha(Theme.TEXT_SECONDARY, alpha), textSize, font);

        NanoVGRender.drawText(label, x + 8, y + 10,
                Theme.withAlpha(Theme.TEXT_PRIMARY, alpha), textSize, font);

        NanoVGRender.restore();
    }

    @Override
    protected void onClick(double mouseX, double mouseY, int button) {
        dragging = true;
        updateValue(mouseX);
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging) {
            updateValue(mouseX);
        }
    }

    @Override
    protected void onRelease(double mouseX, double mouseY, int button) {
        dragging = false;
    }

    private void updateValue(double mouseX) {
        float trackX = x + 8;
        float trackWidth = width - 16;
        double rel = (mouseX - trackX) / trackWidth;
        rel = Math.max(0, Math.min(1, rel));
        double newValue = min + rel * (max - min);
        if (step > 0) {
            newValue = Math.round(newValue / step) * step;
        }
        newValue = Math.max(min, Math.min(max, newValue));
        if (newValue != value) {
            value = newValue;
            valueAnimation.setTarget((float) rel);
            if (onChange != null) {
                onChange.accept(value);
            }
        }
    }

    private String formatValue(double val) {
        if (step >= 1) {
            return String.valueOf((int) val);
        } else if (step >= 0.1) {
            return String.format("%.1f", val);
        } else {
            return String.format("%.2f", val);
        }
    }

    public void setOnChange(Consumer<Double> onChange) {
        this.onChange = onChange;
    }

    public double getValue() { return value; }
    public void setValue(double value) {
        this.value = Math.max(min, Math.min(max, value));
        float progress = (float) ((this.value - min) / (max - min));
        valueAnimation.setTarget(progress);
    }

    public void setSuffix(String suffix) { this.suffix = suffix; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}
