package cn.gardenia.client.gui.clickgui;

import cn.gardenia.client.gui.animation.Animation;
import cn.gardenia.client.gui.animation.Easing;
import cn.gardenia.client.gui.components.Component;
import cn.gardenia.client.gui.nanovg.NanoVGManager;
import cn.gardenia.client.gui.nanovg.NanoVGRender;
import cn.gardenia.client.gui.theme.Theme;
import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CategoryPanel extends Component {
    private final Category category;
    private final List<Module> modules;
    private final List<ModuleItem> items = new ArrayList<>();
    private final Animation heightAnimation;
    private final int index;
    private Consumer<Module> onModuleSelect;
    private float headerHeight = 34;
    private float itemHeight = 28;
    private float contentPadding = 6;
    private boolean dragging = false;
    private double dragOffsetX, dragOffsetY;
    private float scrollOffset = 0;
    private float maxScroll = 0;
    private float targetScroll = 0;
    private float contentHeight;
    private float maxHeight = 360;
    private int iconImageId = -1;
    private float iconSize = 16;

    public CategoryPanel(float x, float y, float width, Category category, List<Module> modules, int index) {
        super(x, y, width, 0);
        this.category = category;
        this.modules = modules;
        this.index = index;
        this.heightAnimation = new Animation(0, Theme.ANIMATION_SPEED_NORMAL, Easing.EASE_OUT_CUBIC);
        loadIcon();
        buildItems();
        updateHeight();
        this.heightAnimation.reset(1);
    }

    private void loadIcon() {
        String iconName = category.name().toLowerCase();
        String resourcePath = "gardenia/icons/" + iconName + ".png";
        iconImageId = NanoVGManager.INSTANCE.loadImage(iconName, resourcePath);
    }

    private void buildItems() {
        items.clear();
        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            ModuleItem item = new ModuleItem(0, 0, width - contentPadding * 2, itemHeight, module);
            item.setOnRightClick(m -> {
                if (onModuleSelect != null) onModuleSelect.accept(m);
            });
            items.add(item);
        }
    }

    private void updateHeight() {
        contentHeight = modules.size() * itemHeight + contentPadding * 2;
        float visibleHeight = Math.min(contentHeight, maxHeight - headerHeight);
        this.height = headerHeight + visibleHeight;
        maxScroll = Math.max(0, contentHeight - visibleHeight);
    }

    @Override
    public void render(float mouseX, float mouseY, float delta) {
        if (!visible) return;
        if (iconImageId == -1) {
            loadIcon();
        }
        update();
        heightAnimation.update();

        float hover = getHoverProgress();
        float alpha = getAlpha();
        float animHeight = height * heightAnimation.getCurrent();

        NanoVGRender.save();
        NanoVGRender.globalAlpha(alpha);

        NanoVGRender.drawShadow(x, y, width, animHeight, Theme.CORNER_RADIUS_LARGE, 16, 0.15f);
        NanoVGRender.drawGlassmorphism(x, y, width, animHeight, Theme.CORNER_RADIUS_LARGE, 1);

        NanoVGRender.drawRoundedRectTop(x, y, width, headerHeight, Theme.CORNER_RADIUS_LARGE,
                Theme.withAlpha(Theme.blend(Theme.SURFACE, Theme.SURFACE_HOVER, hover), alpha));

        float textX = x + 14;
        if (iconImageId != -1) {
            float iconX = x + 12;
            float iconY = y + (headerHeight - iconSize) / 2;
            NanoVGRender.drawImage(iconX, iconY, iconSize, iconSize, iconImageId, alpha);
            textX = iconX + iconSize + 8;
        }

        NanoVGRender.drawText(getCategoryName(), textX, y + headerHeight / 2,
                Theme.withAlpha(Theme.TEXT_PRIMARY, alpha), 13, "bold");

        String count = String.valueOf(modules.size());
        NanoVGRender.drawTextRight(count, x + width - 14, y + headerHeight / 2,
                Theme.withAlpha(Theme.TEXT_SECONDARY, alpha), 11, "regular");

        NanoVGRender.drawLine(x + 10, y + headerHeight - 1, x + width - 10, y + headerHeight - 1, 1,
                Theme.withAlpha(Theme.BORDER, alpha * 0.5f));

        float contentY = y + headerHeight;
        float visibleContentHeight = animHeight - headerHeight;

        if (visibleContentHeight > 0) {
            NanoVGRender.scissor(x, contentY, width, visibleContentHeight);

            scrollOffset += (targetScroll - scrollOffset) * 0.2f;

            float itemY = contentY + contentPadding - scrollOffset;
            for (ModuleItem item : items) {
                item.setPosition(x + contentPadding, itemY);
                item.render(mouseX, mouseY, delta);
                itemY += itemHeight;
            }

            if (maxScroll > 0) {
                float scrollbarHeight = visibleContentHeight * (visibleContentHeight / contentHeight);
                float scrollbarY = contentY + (scrollOffset / maxScroll) * (visibleContentHeight - scrollbarHeight);
                NanoVGRender.drawRoundedRect(x + width - 5, scrollbarY, 3, scrollbarHeight, 1.5f,
                        Theme.withAlpha(0x60000000, alpha));
            }

            NanoVGRender.resetScissor();
        }

        NanoVGRender.restore();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !isMouseOver(mouseX, mouseY)) return false;

        if (mouseY < y + headerHeight) {
            if (button == 0) {
                dragging = true;
                dragOffsetX = mouseX - x;
                dragOffsetY = mouseY - y;
            }
            return true;
        }

        for (ModuleItem item : items) {
            if (item.mouseClicked(mouseX, mouseY, button)) return true;
        }

        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        for (ModuleItem item : items) item.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging) {
            x = (float) (mouseX - dragOffsetX);
            y = (float) (mouseY - dragOffsetY);
            return true;
        }
        for (ModuleItem item : items) {
            if (item.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        targetScroll = (float) Math.max(0, Math.min(maxScroll, targetScroll - amount * 20));
        return true;
    }

    @Override
    public void onMouseMove(double mouseX, double mouseY) {
        super.onMouseMove(mouseX, mouseY);
        for (ModuleItem item : items) item.onMouseMove(mouseX, mouseY);
    }

    public void setOnModuleSelect(Consumer<Module> onModuleSelect) {
        this.onModuleSelect = onModuleSelect;
    }

    private String getCategoryName() {
        return category.name().charAt(0) + category.name().substring(1).toLowerCase();
    }

    public Category getCategory() { return category; }
}