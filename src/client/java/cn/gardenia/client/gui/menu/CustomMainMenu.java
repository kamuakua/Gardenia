package cn.gardenia.client.gui.menu;

import cn.gardenia.client.gui.animation.Easing;
import cn.gardenia.client.gui.nanovg.NanoVGManager;
import cn.gardenia.client.gui.nanovg.NanoVGRender;
import cn.gardenia.client.gui.theme.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import org.lwjgl.nanovg.NanoVG;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CustomMainMenu extends Screen {
    private final List<MenuButton> buttons = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final List<Wave> waves = new ArrayList<>();
    private final List<Star> stars = new ArrayList<>();
    private final Random random = new Random();
    private double initAnimation = 0;
    private float time = 0;
    private float mouseXSmooth = 0;
    private float mouseYSmooth = 0;
    private double themeButtonHover = 0.0;
    private boolean isDarkTheme = true;

    public CustomMainMenu() {
        super(Text.of("Main Menu"));
    }

    @Override
    protected void init() {
        buttons.clear();
        particles.clear();
        waves.clear();
        stars.clear();

        int btnWidth = 140;
        int btnHeight = 26;
        int spacing = 10;
        int startX = 50;
        int startY = height / 2 - 20;

        buttons.add(new MenuButton("Singleplayer", startX, startY, btnWidth, btnHeight, () -> {
            client.setScreen(new SelectWorldScreen(this));
        }));
        buttons.add(new MenuButton("Multiplayer", startX, startY + (btnHeight + spacing), btnWidth, btnHeight, () -> {
            client.setScreen(new MultiplayerScreen(this));
        }));
        buttons.add(new MenuButton("Options", startX, startY + (btnHeight + spacing) * 2, btnWidth, btnHeight, () -> {
            client.setScreen(new OptionsScreen(this, client.options));
        }));
        buttons.add(new MenuButton("Quit", startX, startY + (btnHeight + spacing) * 3, btnWidth, btnHeight, () -> {
            client.scheduleStop();
        }));

        initParticles();
        initWaves();
        initStars();
    }

    private void initParticles() {
        for (int i = 0; i < 80; i++) {
            particles.add(new Particle(
                    random.nextFloat() * width,
                    random.nextFloat() * height,
                    random.nextFloat() * 2 + 0.5f,
                    random.nextFloat() * 0.5f + 0.2f,
                    new Color(
                            100 + random.nextInt(155),
                            150 + random.nextInt(105),
                            255,
                            100 + random.nextInt(155)
                    )
            ));
        }
    }

    private void initWaves() {
        for (int i = 0; i < 5; i++) {
            waves.add(new Wave(
                    i * 0.4f,
                    20 + i * 15,
                    0.3f + i * 0.1f,
                    new Color(60 + i * 20, 100 + i * 30, 200 + i * 11, 30 + i * 10)
            ));
        }
    }

    private void initStars() {
        for (int i = 0; i < 150; i++) {
            stars.add(new Star(
                    random.nextFloat() * width,
                    random.nextFloat() * height,
                    random.nextFloat() * 1.5f + 0.5f,
                    random.nextFloat() * 0.05f + 0.01f,
                    random.nextFloat() * (float)Math.PI * 2
            ));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        time += delta * 0.016f;
        mouseXSmooth = (float)animate(mouseXSmooth, mouseX, 0.08);
        mouseYSmooth = (float)animate(mouseYSmooth, mouseY, 0.08);

        renderDynamicBackground(mouseXSmooth, mouseYSmooth);

        NanoVGManager.INSTANCE.beginFrame(width, height, (float)client.getWindow().getScaleFactor());

        initAnimation = animate(initAnimation, 1.0, 0.05);

        if (initAnimation > 0.01) {
            float uiAlpha = (float) easeOutQuart(initAnimation);

            long ctx = NanoVGManager.INSTANCE.getContext();
            NanoVG.nvgSave(ctx);
            NanoVG.nvgGlobalAlpha(ctx, uiAlpha);

            float animOffset = (float) (1.0 - easeOutQuart(initAnimation));

            String title = "GARDENIA";
            float titleSize = 60;
            float titleX = 50 - animOffset * 50;
            float titleY = height / 2f - 80;

            float glitchSpeed = 35;
            float glitchOffset1X = (float)(Math.sin(time * glitchSpeed) * 2.5);
            float glitchOffset1Y = (float)(Math.cos(time * glitchSpeed * 0.8) * 1.5);
            float glitchOffset2X = (float)(Math.cos(time * glitchSpeed * 1.2) * 2.0);
            float glitchOffset2Y = (float)(Math.sin(time * glitchSpeed * 0.9) * 1.8);
            float rgbSplit = (float)(Math.sin(time * glitchSpeed * 0.5) * 3);

            Color glitchColor1 = new Color(255, 30, 80, 220);
            Color glitchColor2 = new Color(30, 255, 180, 220);
            Color mainColor = isDarkTheme ? Color.WHITE : Color.BLACK;

            NanoVGRender.drawText(title, titleX + glitchOffset1X + rgbSplit, titleY + glitchOffset1Y, glitchColor1.getRGB(), titleSize, "bold");
            NanoVGRender.drawText(title, titleX + glitchOffset2X - rgbSplit, titleY + glitchOffset2Y, glitchColor2.getRGB(), titleSize, "bold");
            NanoVGRender.drawText(title, titleX, titleY, mainColor.getRGB(), titleSize, "bold");

            String subtitle = "Gardenia Client | Fabric 1.21.4";
            float subSize = 14;
            NanoVGRender.drawText(subtitle, titleX + 2, titleY + 35, isDarkTheme ? 0xFFAAAAAA : 0xFF666666, subSize, "medium");

            for (int i = 0; i < buttons.size(); i++) {
                MenuButton button = buttons.get(i);
                float buttonAnimOffset = (float) (1.0 - easeOutQuart(Math.max(0, (initAnimation * 1.5) - (i * 0.1))));
                button.render(mouseX, mouseY, buttonAnimOffset, width, height, time);
            }

            renderThemeButton(mouseX, mouseY);

            String version = "Fabric 1.21.4 | v1.0.0";
            float infoSize = 12;
            NanoVGRender.drawText(version, 50, height - 30, isDarkTheme ? 0xFF888888 : 0xFF666666, infoSize, "medium");

            NanoVG.nvgRestore(ctx);
        }

        NanoVGManager.INSTANCE.endFrame();

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderThemeButton(int mouseX, int mouseY) {
        float btnX = width - 50;
        float btnY = 30;
        float btnSize = 24;

        boolean hovered = mouseX >= btnX - btnSize/2 && mouseX <= btnX + btnSize/2 && mouseY >= btnY - btnSize/2 && mouseY <= btnY + btnSize/2;
        themeButtonHover = animate(themeButtonHover, hovered ? 1.0 : 0.0, 0.15);

        float alpha = (float) (0.6 + themeButtonHover * 0.4);
        int btnColor = isDarkTheme ?
                ((int)(alpha * 255) << 24) | 0xFFFFFF :
                ((int)(alpha * 255) << 24);

        String icon = isDarkTheme ? "☀" : "☾";
        NanoVGRender.drawTextCenter(icon, btnX, btnY, btnColor, 16, "medium");
    }

    private void renderDynamicBackground(float mouseX, float mouseY) {
        if (!NanoVGManager.INSTANCE.isInitialized()) {
            NanoVGManager.INSTANCE.init();
        }

        NanoVGManager.INSTANCE.beginFrame(width, height, (float)client.getWindow().getScaleFactor());

        int bgColor = isDarkTheme ? 0xFF0a0a0f : 0xFFF5F5F5;
        NanoVGRender.drawRect(0, 0, width, height, bgColor);

        float parallaxX = (mouseX - width / 2f) * 0.02f;
        float parallaxY = (mouseY - height / 2f) * 0.02f;

        for (Star star : stars) {
            star.render(time, parallaxX, parallaxY, width, height, isDarkTheme);
        }

        for (int i = 0; i < waves.size(); i++) {
            Wave wave = waves.get(i);
            wave.render(time, width, height, parallaxX * (i + 1) * 0.3f);
        }

        for (Particle particle : particles) {
            particle.update(width, height, time);
            particle.render(isDarkTheme);
        }

        renderAuroraEffect(time, mouseX, mouseY);

        renderGradientOrbs(time, mouseX, mouseY);

        if (isDarkTheme) {
            NanoVGRender.drawRect(0, 0, width, height, 0x64000000);
        } else {
            NanoVGRender.drawRect(0, 0, width, height, 0x20FFFFFF);
        }

        NanoVGManager.INSTANCE.endFrame();
    }

    private void renderAuroraEffect(float time, float mouseX, float mouseY) {
        int segments = 100;
        float baseY = height * 0.7f;

        for (int layer = 0; layer < 3; layer++) {
            float layerOffset = layer * 0.5f;
            Color auroraColor = new Color(
                    50 + layer * 30,
                    100 + layer * 50,
                    200 + layer * 20,
                    25 - layer * 5
            );

            for (int i = 0; i < segments; i++) {
                float x1 = (float)i / segments * width;
                float x2 = (float)(i + 1) / segments * width;

                float wave1 = (float)Math.sin(x1 * 0.01f + time * 0.5f + layerOffset) * 30;
                float wave2 = (float)Math.sin(x1 * 0.02f + time * 0.3f + layerOffset * 2) * 20;
                float wave3 = (float)Math.sin(x1 * 0.005f + time * 0.2f) * 50;

                float y1 = baseY + wave1 + wave2 + wave3;
                float y2 = baseY +
                        (float)Math.sin(x2 * 0.01f + time * 0.5f + layerOffset) * 30 +
                        (float)Math.sin(x2 * 0.02f + time * 0.3f + layerOffset * 2) * 20 +
                        (float)Math.sin(x2 * 0.005f + time * 0.2f) * 50;

                NanoVGRender.drawLine(x1, y1, x2, y2, 40 - layer * 10, auroraColor.getRGB());
            }
        }
    }

    private void renderGradientOrbs(float time, float mouseX, float mouseY) {
        float centerX = width * 0.5f + (float)Math.sin(time * 0.2f) * 100;
        float centerY = height * 0.4f + (float)Math.cos(time * 0.15f) * 50;

        Color orb1Color = new Color(80, 120, 255, 20);
        NanoVGRender.drawCircle(centerX, centerY, 200, orb1Color.getRGB());

        float orb2X = width * 0.3f + (float)Math.cos(time * 0.25f) * 80;
        float orb2Y = height * 0.6f + (float)Math.sin(time * 0.2f) * 60;
        Color orb2Color = new Color(150, 80, 255, 15);
        NanoVGRender.drawCircle(orb2X, orb2Y, 150, orb2Color.getRGB());

        float orb3X = width * 0.7f + (float)Math.sin(time * 0.3f) * 60;
        float orb3Y = height * 0.5f + (float)Math.cos(time * 0.25f) * 40;
        Color orb3Color = new Color(80, 200, 200, 15);
        NanoVGRender.drawCircle(orb3X, orb3Y, 180, orb3Color.getRGB());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            float btnX = width - 50;
            float btnY = 30;
            float btnSize = 24;
            if (mouseX >= btnX - btnSize/2 && mouseX <= btnX + btnSize/2 && mouseY >= btnY - btnSize/2 && mouseY <= btnY + btnSize/2) {
                isDarkTheme = !isDarkTheme;
                return true;
            }

            for (MenuButton btn : buttons) {
                if (btn.isHovered(mouseX, mouseY)) {
                    btn.onClick();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static double animate(double current, double target, double speed) {
        if (Math.abs(target - current) < 0.001) return target;
        return current + (target - current) * speed;
    }

    private static double easeOutQuart(double x) {
        return 1 - Math.pow(1 - x, 4);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    private static class MenuButton {
        private final String text;
        private final float x, y, width, height;
        private final Runnable action;
        private double hoverProgress = 0;

        public MenuButton(String text, float x, float y, float width, float height, Runnable action) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.action = action;
        }

        public void render(int mouseX, int mouseY, float animOffset, int screenWidth, int screenHeight, float time) {
            boolean hovered = isHovered(mouseX, mouseY);
            hoverProgress = animate(hoverProgress, hovered ? 1.0 : 0.0, 0.15);

            float currentX = x - animOffset * 40 + (float)(hoverProgress * 8);

            int bgColor = (int)(40 + hoverProgress * 20);
            int alpha = (int)(100 + hoverProgress * 100);
            int bg = (alpha << 24) | (bgColor << 16) | (bgColor << 8) | bgColor;

            NanoVGRender.drawRoundedRect(currentX, y, width, height, 4, bg);

            int outlineAlpha = (int)(50 + hoverProgress * 100);
            int outline = (outlineAlpha << 24) | 0xFFFFFF;
            NanoVGRender.drawRoundedRectStroke(currentX, y, width, height, 4, 1.0f, outline);

            if (hoverProgress > 0) {
                int glowColor = (int)(hoverProgress * 100) << 24 | 0x6496FF;
                NanoVGRender.drawShadow(currentX, y, width, height, 4, 8, (float)hoverProgress * 0.5f);
            }

            float fontSize = 16;
            int textColor = hovered ? 0xFFFFFFFF : 0xFFCCCCCC;

            NanoVGRender.drawText(text, currentX + 12, y + height / 2f + 1, textColor, fontSize, "medium");

            if (hoverProgress > 0) {
                float lineH = height * 0.6f;
                int themeColor = 0xFF6496FF;
                int lineColor = (int)(hoverProgress * 255) << 24 | (themeColor & 0xFFFFFF);
                NanoVGRender.drawRect(currentX + 2, y + (height - lineH) / 2f, 2, lineH, lineColor);
            }
        }

        public boolean isHovered(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }

        public void onClick() {
            action.run();
        }
    }

    private class Particle {
        float x, y, size, speed;
        Color color;
        float offset;

        Particle(float x, float y, float size, float speed, Color color) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.speed = speed;
            this.color = color;
            this.offset = random.nextFloat() * (float)Math.PI * 2;
        }

        void update(int width, int height, float time) {
            y -= speed;
            x += (float)Math.sin(time * 0.5f + offset) * 0.3f;

            if (y < -10) {
                y = height + 10;
                x = random.nextFloat() * width;
            }
        }

        void render(boolean isDarkTheme) {
            float alpha = color.getAlpha() / 255f;
            float pulse = (float)(Math.sin(time * 2 + offset) * 0.3 + 0.7);
            int a = (int)(color.getAlpha() * pulse);
            int renderColor = (a << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
            NanoVGRender.drawCircle(x, y, size * pulse, renderColor);
        }
    }

    private class Wave {
        float phase, amplitude, frequency;
        Color color;

        Wave(float phase, float amplitude, float frequency, Color color) {
            this.phase = phase;
            this.amplitude = amplitude;
            this.frequency = frequency;
            this.color = color;
        }

        void render(float time, int width, int height, float parallaxX) {
            int segments = 80;
            float baseY = height * 0.85f;

            for (int i = 0; i < segments; i++) {
                float x1 = (float)i / segments * width + parallaxX;
                float x2 = (float)(i + 1) / segments * width + parallaxX;

                float y1 = baseY + (float)Math.sin(x1 * frequency + time + phase) * amplitude +
                        (float)Math.sin(x1 * frequency * 0.5f + time * 0.7f) * (amplitude * 0.5f);
                float y2 = baseY + (float)Math.sin(x2 * frequency + time + phase) * amplitude +
                        (float)Math.sin(x2 * frequency * 0.5f + time * 0.7f) * (amplitude * 0.5f);

                NanoVGRender.drawLine(x1, y1, x2, y2, 3, color.getRGB());
            }
        }
    }

    private class Star {
        float x, y, size, twinkleSpeed, phase;

        Star(float x, float y, float size, float twinkleSpeed, float phase) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.twinkleSpeed = twinkleSpeed;
            this.phase = phase;
        }

        void render(float time, float parallaxX, float parallaxY, int width, int height, boolean isDarkTheme) {
            float twinkle = (float)(Math.sin(time * twinkleSpeed + phase) * 0.5 + 0.5);
            int alpha = (int)(100 + twinkle * 155);
            int starColor = isDarkTheme ?
                    (alpha << 24) | 0xFFFFFF :
                    ((alpha / 2) << 24) | 0x666666;

            float renderX = x + parallaxX * (size * 0.1f);
            float renderY = y + parallaxY * (size * 0.1f);

            NanoVGRender.drawCircle(renderX, renderY, size * (0.5f + twinkle * 0.5f), starColor);
        }
    }
}
