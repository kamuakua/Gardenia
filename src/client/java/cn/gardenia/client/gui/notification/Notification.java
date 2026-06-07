package cn.gardenia.client.gui.notification;

import cn.gardenia.client.gui.animation.Animation;
import cn.gardenia.client.gui.animation.Easing;
import cn.gardenia.client.module.Module;

public class Notification {
    public enum State {
        ENTERING, DISPLAYING, EXITING, FINISHED
    }

    private final String title;
    private final String description;
    private final String iconName;
    private final int displayDuration;

    private State state = State.ENTERING;
    private int elapsed;
    private final Animation scaleAnim;
    private final Animation alphaAnim;
    private final Animation entrySlideAnim;

    // Stack Y: smoothly lerps to targetStackY for stack shift animations
    private float currentY;
    private float targetY;

    private float renderScale;
    private float renderAlpha;
    private float renderEntrySlide;

    // ── constants ──
    private static final int ENTER_MS = 350;
    private static final int EXIT_MS = 280;
    private static final int FAST_EXIT_MS = 140;

    public Notification(String title, String description, String iconName, int displayDurationMs) {
        this.title = title;
        this.description = description;
        this.iconName = iconName;
        this.displayDuration = displayDurationMs;

        this.scaleAnim = new Animation(0.85f, 1f / ENTER_MS, Easing.EASE_OUT_CUBIC);
        this.scaleAnim.setTarget(1f);

        this.alphaAnim = new Animation(0f, 1f / ENTER_MS, Easing.EASE_OUT_CUBIC);
        this.alphaAnim.setTarget(1f);

        this.entrySlideAnim = new Animation(12f, 1f / ENTER_MS, Easing.EASE_OUT_CUBIC);
        this.entrySlideAnim.setTarget(0f);
    }

    public static Notification info(String title, String description) {
        return new Notification(title, description, "notification_on", 1800);
    }

    public static Notification forModuleToggle(Module module, boolean enabled) {
        String icon = enabled ? "notification_on" : "notification_off";
        String desc = enabled ? "Enabled" : "Disabled";
        return new Notification(module.getName(), desc, icon, 2000);
    }

    /** Tick animation. Returns true while alive. */
    public boolean update() {
        elapsed += 16;
        // Smoothly follow stack Y
        currentY += (targetY - currentY) * 0.18f;

        switch (state) {
            case ENTERING:
                scaleAnim.update();
                alphaAnim.update();
                entrySlideAnim.update();
                renderScale = scaleAnim.getCurrent();
                renderAlpha = alphaAnim.getCurrent();
                renderEntrySlide = entrySlideAnim.getCurrent();
                if (elapsed >= ENTER_MS || scaleAnim.isFinished()) {
                    state = State.DISPLAYING;
                    elapsed = 0;
                    renderScale = 1f;
                    renderAlpha = 1f;
                    renderEntrySlide = 0f;
                }
                break;
            case DISPLAYING:
                renderScale = 1f;
                renderAlpha = 1f;
                renderEntrySlide = 0f;
                if (elapsed >= displayDuration) startExit();
                break;
            case EXITING:
                alphaAnim.update();
                renderAlpha = alphaAnim.getCurrent();
                if (elapsed >= EXIT_MS || alphaAnim.isFinished()) {
                    state = State.FINISHED;
                }
                break;
            case FINISHED:
                return false;
        }
        return true;
    }

    private void startExit() {
        state = State.EXITING;
        elapsed = 0;
        alphaAnim.reset(1f);
        alphaAnim.setTarget(0f);
        alphaAnim.setSpeed(1f / EXIT_MS);
    }

    /** Fast fade-out when max stack is exceeded. */
    public void fastExit() {
        if (state == State.FINISHED) return;
        state = State.EXITING;
        elapsed = 0;
        alphaAnim.reset(Math.max(renderAlpha, 0.5f));
        alphaAnim.setTarget(0f);
        alphaAnim.setSpeed(1f / FAST_EXIT_MS);
    }

    // ── stack Y ──
    public void setTargetY(float y) { this.targetY = y; }
    public float getCurrentY() { return currentY; }

    // ── getters ──
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getIconName() { return iconName; }
    public State getState() { return state; }
    public float getRenderScale() { return renderScale; }
    public float getRenderAlpha() { return renderAlpha; }
    public float getRenderEntrySlide() { return renderEntrySlide; }
    public boolean isFinished() { return state == State.FINISHED; }
    public boolean isExiting() { return state == State.EXITING; }
}
