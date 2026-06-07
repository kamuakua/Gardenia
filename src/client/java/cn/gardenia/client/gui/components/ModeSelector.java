package cn.gardenia.client.gui.components;

import cn.gardenia.client.gui.animation.Animation;
import cn.gardenia.client.gui.animation.Easing;
import cn.gardenia.client.gui.nanovg.NanoVGRender;
import cn.gardenia.client.gui.theme.Theme;

import java.util.function.Consumer;

public class ModeSelector extends Component {
    private String label;
    private String[] options;
    private int selectedIndex;
    private Consumer<String> onChange;
    private float textSize = 12;
    private String font = "regular";
    private boolean expanded = false;
    private Animation expandAnimation;
    private float itemHeight = 26;
    private float cornerRadius = Theme.CORNER_RADIUS_MEDIUM;
    private float collapsedHeight = 56;
    private float expandedExtraHeight = 0;

    public ModeSelector(float x, float y, float width, float height, String label, String[] options, int initialIndex) {
        super(x, y, width, height);
        this.label = label;
        this.options = options != null ? options : new String[0];
        this.selectedIndex = Math.max(0, Math.min(initialIndex, this.options.length - 1));
        this.expandAnimation = new Animation(0, Theme.ANIMATION_SPEED_FAST, Easing.EASE_OUT_CUBIC);
        this.hoverAnimation.setSpeed(Theme.ANIMATION_SPEED_FAST);
        this.hoverAnimation.setEasing(Easing.EASE_OUT_QUAD);
    }

    @Override
    public void render(float mouseX, float mouseY, float delta) {
        if (!visible) return;
        update();
        expandAnimation.update();

        float hover = getHoverProgress();
        float alpha = getAlpha();
        float expand = expandAnimation.getCurrent();
        
        // Update height based on expansion
        float targetExtraHeight = expanded ? options.length * itemHeight + 12 : 0;
        expandedExtraHeight += (targetExtraHeight - expandedExtraHeight) * 0.3f;
        this.height = collapsedHeight + expandedExtraHeight;

        NanoVGRender.save();
        NanoVGRender.globalAlpha(alpha);

        NanoVGRender.drawText(label, x + 8, y + 10,
                Theme.withAlpha(Theme.TEXT_PRIMARY, alpha), textSize, font);

        float selectorY = y + 22;
        float selectorHeight = 28;

        NanoVGRender.drawRoundedRect(x + 8, selectorY, width - 16, selectorHeight, cornerRadius,
                Theme.withAlpha(Theme.SURFACE, alpha));
        NanoVGRender.drawRoundedRectStroke(x + 8, selectorY, width - 16, selectorHeight, cornerRadius, 1,
                Theme.withAlpha(Theme.blend(Theme.BORDER, Theme.BORDER_HOVER, hover), alpha));

        String currentText = selectedIndex >= 0 && selectedIndex < options.length ? options[selectedIndex] : "";
        NanoVGRender.drawText(currentText, x + 18, selectorY + selectorHeight / 2,
                Theme.withAlpha(Theme.TEXT_PRIMARY, alpha), textSize + 1, font);

        float arrowX = x + width - 22;
        float arrowY = selectorY + selectorHeight / 2;
        drawArrow(arrowX, arrowY, expand, alpha);

        if (expand > 0.01f) {
            float listY = selectorY + selectorHeight + 4;
            float listHeight = (options.length * itemHeight + 8) * expand;

            NanoVGRender.scissor(x + 8, listY, width - 16, listHeight);
            NanoVGRender.drawRoundedRect(x + 8, listY, width - 16, options.length * itemHeight + 8, cornerRadius,
                    Theme.withAlpha(Theme.SURFACE, alpha));
            NanoVGRender.drawRoundedRectStroke(x + 8, listY, width - 16, options.length * itemHeight + 8, cornerRadius, 1,
                    Theme.withAlpha(Theme.BORDER, alpha));

            for (int i = 0; i < options.length; i++) {
                float itemY = listY + 4 + i * itemHeight;
                boolean itemHovered = isMouseOver(mouseX, mouseY, x + 8, itemY, width - 16, itemHeight);
                int itemBg = itemHovered ? Theme.SURFACE_HOVER : 0;
                if (i == selectedIndex) {
                    itemBg = Theme.SURFACE_ACTIVE;
                }
                if (itemBg != 0) {
                    NanoVGRender.drawRoundedRect(x + 10, itemY + 1, width - 20, itemHeight - 2, cornerRadius - 2,
                            Theme.withAlpha(itemBg, alpha));
                }
                NanoVGRender.drawText(options[i], x + 18, itemY + itemHeight / 2,
                        Theme.withAlpha(i == selectedIndex ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY, alpha), textSize, font);
            }
            NanoVGRender.resetScissor();
        }

        NanoVGRender.restore();
    }

    private void drawArrow(float x, float y, float expandProgress, float alpha) {
        float size = 3;
        float offset = expandProgress * size;
        int color = Theme.withAlpha(Theme.TEXT_SECONDARY, alpha);
        NanoVGRender.drawLine(x - size, y - offset, x, y + size - offset, 1.2f, color);
        NanoVGRender.drawLine(x, y + size - offset, x + size, y - offset, 1.2f, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;

        float selectorY = y + 22;
        float selectorHeight = 28;

        if (isMouseOver(mouseX, mouseY, x + 8, selectorY, width - 16, selectorHeight)) {
            expanded = !expanded;
            expandAnimation.setTarget(expanded ? 1 : 0);
            return true;
        }

        if (expanded) {
            float listY = selectorY + selectorHeight + 4;
            for (int i = 0; i < options.length; i++) {
                float itemY = listY + 4 + i * itemHeight;
                if (isMouseOver(mouseX, mouseY, x + 8, itemY, width - 16, itemHeight)) {
                    selectedIndex = i;
                    expanded = false;
                    expandAnimation.setTarget(0);
                    if (onChange != null) {
                        onChange.accept(options[i]);
                    }
                    return true;
                }
            }
        }

        if (expanded) {
            expanded = false;
            expandAnimation.setTarget(0);
            return true;
        }

        return false;
    }

    @Override
    protected void onClick(double mouseX, double mouseY, int button) {
    }

    @Override
    public void onMouseMove(double mouseX, double mouseY) {
        super.onMouseMove(mouseX, mouseY);
    }

    public void setOnChange(Consumer<String> onChange) {
        this.onChange = onChange;
    }

    public String getSelected() {
        return selectedIndex >= 0 && selectedIndex < options.length ? options[selectedIndex] : "";
    }

    public int getSelectedIndex() { return selectedIndex; }
    public void setSelectedIndex(int index) {
        this.selectedIndex = Math.max(0, Math.min(index, options.length - 1));
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}
