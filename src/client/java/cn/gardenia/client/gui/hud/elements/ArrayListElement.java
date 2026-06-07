package cn.gardenia.client.gui.hud.elements;

import cn.gardenia.client.gui.animation.Animation;
import cn.gardenia.client.gui.animation.Easing;
import cn.gardenia.client.gui.hud.HudElement;
import cn.gardenia.client.gui.hud.HudStyle;
import cn.gardenia.client.gui.nanovg.NanoVGManager;
import cn.gardenia.client.gui.nanovg.NanoVGRender;
import cn.gardenia.client.gui.theme.Theme;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.ModuleManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ArrayListElement extends HudElement {
    private HudStyle style = HudStyle.MODERN;
    private int textColor = 0xFFFFFFFF;
    private int accentColor = 0xFF888888;
    private float fontSize = 14;
    private float bgOpacity = 0.88f;
    private boolean rainbow = false;
    private float rainbowSpeed = 0.5f;
    private boolean animation = true;
    private boolean showDescription = false;
    private float spacing = 3;
    private final List<ModuleEntry> entries = new ArrayList<>();
    private boolean rightAligned = true;
    private float padding = 12;
    private float accentWidth = 4;
    private float cardRadius = 6;

    public ArrayListElement(float x, float y) {
        super(x, y, 120, 200);
    }

    @Override
    public void render(float mouseX, float mouseY, float delta, boolean editing) {
        update();

        if (mc.getWindow() != null) {
            float screenCenter = mc.getWindow().getScaledWidth() / 2f;
            rightAligned = x + width / 2 > screenCenter;
        }

        List<Module> enabledModules = ModuleManager.INSTANCE.getModules().stream()
                .filter(Module::isEnabled)
                .sorted(Comparator.comparingInt(m -> -m.getName().length()))
                .toList();

        updateEntries(enabledModules);

        float currentY = y;
        float maxWidth = 0;

        for (ModuleEntry entry : entries) {
            if (animation) entry.anim.update();
            float alpha = editing ? 0.85f : entry.anim.getCurrent();
            if (alpha < 0.01f) continue;

            String text = entry.module.getName();
            String font = NanoVGManager.INSTANCE.hasFont("chinese") ? "chinese" : "regular";
            float textW = NanoVGRender.textWidth(text, fontSize, font);
            float entryWidth = padding + accentWidth + 6 + textW + padding;
            float entryHeight = fontSize + 12;

            int color = rainbow ? getRainbowColor(entry.index) : textColor;
            float entryX = rightAligned ? x + width - entryWidth : x;

            if (style == HudStyle.MODERN) {
                NanoVGRender.drawShadow(entryX, currentY, entryWidth, entryHeight, cardRadius, 8, 0.12f * alpha);
                NanoVGRender.drawRoundedRect(entryX, currentY, entryWidth, entryHeight, cardRadius,
                        Theme.withAlpha(0xFF1A1A1A, bgOpacity * alpha));

                float accentX = rightAligned ? entryX + entryWidth - accentWidth : entryX;
                NanoVGRender.drawRoundedRect(accentX, currentY, accentWidth, entryHeight, 2,
                        Theme.withAlpha(accentColor, alpha));

                if (rightAligned) {
                    NanoVGRender.drawTextRight(text, entryX + entryWidth - padding, currentY + entryHeight / 2,
                            Theme.withAlpha(color, alpha), fontSize, font);
                } else {
                    NanoVGRender.drawText(text, entryX + padding + accentWidth + 6, currentY + entryHeight / 2,
                            Theme.withAlpha(color, alpha), fontSize, font);
                }
            } else {
                switch (style) {
                    case GLASS -> {
                        NanoVGRender.drawGlassmorphism(entryX, currentY, entryWidth, entryHeight, cardRadius, bgOpacity * alpha);
                        NanoVGRender.drawRoundedRectStroke(entryX, currentY, entryWidth, entryHeight, cardRadius, 1,
                                Theme.withAlpha(accentColor, 0.3f * alpha));
                    }
                    case OUTLINE ->
                        NanoVGRender.drawRoundedRectStroke(entryX, currentY, entryWidth, entryHeight, 4, 1,
                                Theme.withAlpha(color, alpha));
                    case BLOCK -> {
                        NanoVGRender.drawRoundedRect(entryX, currentY, entryWidth, entryHeight, 4,
                                Theme.withAlpha(accentColor, bgOpacity * alpha));
                        NanoVGRender.drawRoundedRect(entryX, currentY, entryWidth, entryHeight, 4,
                                Theme.withAlpha(color, 0.1f * alpha));
                    }
                    case MINIMAL -> {}
                    default -> {}
                }
                float drawX = rightAligned ? entryX + entryWidth - textW - padding : entryX + padding;
                NanoVGRender.drawText(text, drawX, currentY + entryHeight / 2,
                        Theme.withAlpha(color, alpha), fontSize, font);
            }

            maxWidth = Math.max(maxWidth, entryWidth);
            currentY += entryHeight + spacing;
        }

        width = maxWidth;
        height = currentY - y;
    }

    private void updateEntries(List<Module> modules) {
        // 先标记所有旧 entry 为离开动画
        for (ModuleEntry e : entries) {
            if (!modules.contains(e.module)) {
                e.anim.setTarget(0);
            }
        }
        // 按排序后的模块顺序重建列表，保留已有 entry 的动画状态
        List<ModuleEntry> newEntries = new ArrayList<>();
        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            ModuleEntry found = null;
            for (ModuleEntry e : entries) {
                if (e.module == m) {
                    found = e;
                    break;
                }
            }
            if (found != null) {
                found.index = i;
                found.anim.setTarget(1);
                newEntries.add(found);
            } else {
                ModuleEntry e = new ModuleEntry(m, i);
                e.anim.setTarget(1);
                newEntries.add(e);
            }
        }
        // 把离开中的旧 entry 追加到末尾（它们会淡出）
        for (ModuleEntry e : entries) {
            if (!modules.contains(e.module)) {
                newEntries.add(e);
            }
        }
        entries.clear();
        entries.addAll(newEntries);
    }

    private int getRainbowColor(int index) {
        float hue = (System.currentTimeMillis() % (int)(10000 / rainbowSpeed)) / (10000 / rainbowSpeed);
        hue = (hue + index * 0.05f) % 1.0f;
        return hsvToRgb(hue, 0.7f, 1.0f);
    }

    private int hsvToRgb(float h, float s, float v) {
        int i = (int) (h * 6);
        float f = h * 6 - i, p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
        float r, g, b;
        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return 0xFF000000 | ((int)(r*255)<<16) | ((int)(g*255)<<8) | (int)(b*255);
    }

    public void setStyle(HudStyle style) { this.style = style; }
    public HudStyle getStyle() { return style; }
    public void setTextColor(int color) { this.textColor = color; }
    public int getTextColor() { return textColor; }
    public void setAccentColor(int color) { this.accentColor = color; }
    public int getAccentColor() { return accentColor; }
    public void setFontSize(float size) { this.fontSize = size; }
    public float getFontSize() { return fontSize; }
    public void setBgOpacity(float opacity) { this.bgOpacity = opacity; }
    public float getBgOpacity() { return bgOpacity; }
    public void setRainbow(boolean rb) { this.rainbow = rb; }
    public boolean isRainbow() { return rainbow; }
    public void setAnimation(boolean anim) { this.animation = anim; }
    public boolean isAnimation() { return animation; }

    private static class ModuleEntry {
        Module module;
        int index;
        Animation anim;
        ModuleEntry(Module m, int i) {
            module = m;
            index = i;
            anim = new Animation(0, 0.15f, Easing.EASE_OUT_CUBIC);
        }
    }
}
