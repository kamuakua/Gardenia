package cn.gardenia.client.gui.screen;

import cn.gardenia.client.gui.animation.HelloAnimation;
import cn.gardenia.client.gui.menu.CustomMainMenu;
import cn.gardenia.client.gui.nanovg.NanoVGManager;
import cn.gardenia.client.gui.nanovg.NanoVGRender;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.nanovg.NanoVG;

public class GardeniaSplashScreen extends Screen {
    private HelloAnimation helloAnimation;
    private boolean keyPressed = false;
    private float fadeAlpha = 0f;
    private float pressAnyKeyTime = 0f;
    private float transitionProgress = 0f;
    private boolean transitioning = false;

    public GardeniaSplashScreen() {
        super(Text.of("Gardenia Splash"));
    }

    @Override
    protected void init() {
        if (helloAnimation == null) {
            helloAnimation = new HelloAnimation();
        }
        fadeAlpha = 0f;
        keyPressed = false;
        transitioning = false;
        transitionProgress = 0f;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!NanoVGManager.INSTANCE.isInitialized()) {
            NanoVGManager.INSTANCE.init();
        }

        NanoVGManager.INSTANCE.beginFrame(width, height, (float) client.getWindow().getScaleFactor());

        NanoVGRender.drawRect(0, 0, width, height, 0xFF000000);

        if (fadeAlpha < 1f) {
            fadeAlpha = Math.min(1f, fadeAlpha + delta * 0.03f);
        }

        long ctx = NanoVGManager.INSTANCE.getContext();

        if (transitioning) {
            NanoVG.nvgSave(ctx);
            NanoVG.nvgGlobalAlpha(ctx, 1f - transitionProgress);
            helloAnimation.update();
            helloAnimation.render(width, height, true);
            NanoVG.nvgRestore(ctx);

            transitionProgress += delta * 0.04f;
            if (transitionProgress >= 1f) {
                client.setScreen(new CustomMainMenu());
            }
        } else {
            NanoVG.nvgSave(ctx);
            NanoVG.nvgGlobalAlpha(ctx, fadeAlpha);
            helloAnimation.update();
            helloAnimation.render(width, height, true);
            NanoVG.nvgRestore(ctx);

            if (helloAnimation.isFinished()) {
                pressAnyKeyTime += delta * 0.016f;
                float textAlpha = 0.4f + 0.6f * (float) (Math.sin(pressAnyKeyTime * 3f) * 0.5 + 0.5);
                int color = ((int) (textAlpha * 255) << 24) | 0xCCCCCC;
                NanoVGRender.drawTextCenter("Press any key to continue", width / 2f, height * 0.7f, color, 16, "medium");
            }
        }

        NanoVGManager.INSTANCE.endFrame();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        triggerTransition();
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (helloAnimation != null && !helloAnimation.isFinished()) {
            return true;
        }
        triggerTransition();
        return true;
    }

    private void triggerTransition() {
        if (transitioning) return;
        if (helloAnimation != null && !helloAnimation.isFinished()) return;
        transitioning = true;
        transitionProgress = 0f;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
