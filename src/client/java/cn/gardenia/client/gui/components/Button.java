package cn.gardenia.client.gui.components;

import cn.gardenia.client.gui.animation.Easing;
import cn.gardenia.client.gui.nanovg.NanoVGRender;
import cn.gardenia.client.gui.theme.Theme;

import java.util.function.Consumer;

public class Button extends Component {
    private String text;
    private Consumer<Button> onClick;
    private float textSize = 14;
    private String font = "medium";
    private int textColor = Theme.TEXT_PRIMARY;
    private int bgColor = Theme.SURFACE;
    private int bgHoverColor = Theme.SURFACE_HOVER;
    private int bgActiveColor = Theme.SURFACE_ACTIVE;
    private float cornerRadius = Theme.CORNER_RADIUS_MEDIUM;

    public Button(float x, float y, float width, float height, String text) {
        super(x, y, width, height);
        this.text = text;
        this.hoverAnimation.setSpeed(Theme.ANIMATION_SPEED_FAST);
        this.hoverAnimation.setEasing(Easing.EASE_OUT_CUBIC);
    }

    @Override
    public void render(float mouseX, float mouseY, float delta) {
        if (!visible) return;
        update();

        float hover = getHoverProgress();
        float alpha = getAlpha();

        int bg = Theme.blend(Theme.blend(bgColor, bgHoverColor, hover), bgActiveColor, 0);
        int bgWithAlpha = Theme.withAlpha(bg, alpha);

        NanoVGRender.save();
        NanoVGRender.globalAlpha(alpha);

        NanoVGRender.drawRoundedRect(x, y, width, height, cornerRadius, bgWithAlpha);
        NanoVGRender.drawRoundedRectStroke(x, y, width, height, cornerRadius, 1,
                Theme.withAlpha(Theme.blend(Theme.BORDER, Theme.BORDER_HOVER, hover), alpha * 0.5f));

        NanoVGRender.drawTextCenter(text, x + width / 2, y + height / 2,
                Theme.withAlpha(textColor, alpha), textSize, font);

        NanoVGRender.restore();
    }

    @Override
    protected void onClick(double mouseX, double mouseY, int button) {
        if (onClick != null) {
            onClick.accept(this);
        }
    }

    public void setOnClick(Consumer<Button> onClick) {
        this.onClick = onClick;
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public void setTextSize(float size) { this.textSize = size; }
    public void setFont(String font) { this.font = font; }
    public void setTextColor(int color) { this.textColor = color; }
    public void setBgColor(int color) { this.bgColor = color; }
    public void setBgHoverColor(int color) { this.bgHoverColor = color; }
    public void setCornerRadius(float radius) { this.cornerRadius = radius; }
}
