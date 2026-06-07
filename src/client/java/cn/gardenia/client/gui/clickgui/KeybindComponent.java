package cn.gardenia.client.gui.clickgui;

import cn.gardenia.client.gui.components.Component;
import cn.gardenia.client.gui.nanovg.NanoVGRender;
import cn.gardenia.client.gui.theme.Theme;
import cn.gardenia.client.module.Module;
import org.lwjgl.glfw.GLFW;

public class KeybindComponent extends Component {
    private final Module module;
    private boolean listening = false;
    private long pulseTime = 0;

    public KeybindComponent(float x, float y, float width, Module module) {
        super(x, y, width, 32);
        this.module = module;
    }

    // ── key name mapping ──────────────────────────────────────────

    public static String getKeyName(int key) {
        if (key <= 0) return "None";
        if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z)
            return String.valueOf((char) ('A' + (key - GLFW.GLFW_KEY_A)));
        if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9)
            return String.valueOf((char) ('0' + (key - GLFW.GLFW_KEY_0)));
        if (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F24)
            return "F" + (key - GLFW.GLFW_KEY_F1 + 1);

        return switch (key) {
            case GLFW.GLFW_KEY_LEFT_SHIFT      -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT     -> "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL    -> "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL   -> "RCTRL";
            case GLFW.GLFW_KEY_LEFT_ALT        -> "LALT";
            case GLFW.GLFW_KEY_RIGHT_ALT       -> "RALT";
            case GLFW.GLFW_KEY_CAPS_LOCK       -> "CAPS";
            case GLFW.GLFW_KEY_SPACE           -> "SPACE";
            case GLFW.GLFW_KEY_TAB             -> "TAB";
            case GLFW.GLFW_KEY_ENTER           -> "ENTER";
            case GLFW.GLFW_KEY_ESCAPE          -> "ESC";
            case GLFW.GLFW_KEY_INSERT          -> "INSERT";
            case GLFW.GLFW_KEY_DELETE          -> "DELETE";
            case GLFW.GLFW_KEY_HOME            -> "HOME";
            case GLFW.GLFW_KEY_END             -> "END";
            case GLFW.GLFW_KEY_PAGE_UP         -> "PAGE_UP";
            case GLFW.GLFW_KEY_PAGE_DOWN       -> "PAGE_DOWN";
            case GLFW.GLFW_KEY_UP              -> "UP";
            case GLFW.GLFW_KEY_DOWN            -> "DOWN";
            case GLFW.GLFW_KEY_LEFT            -> "LEFT";
            case GLFW.GLFW_KEY_RIGHT           -> "RIGHT";
            case GLFW.GLFW_KEY_GRAVE_ACCENT    -> "GRAVE";
            case GLFW.GLFW_KEY_MINUS           -> "MINUS";
            case GLFW.GLFW_KEY_EQUAL           -> "EQUAL";
            case GLFW.GLFW_KEY_LEFT_BRACKET    -> "LBRACKET";
            case GLFW.GLFW_KEY_RIGHT_BRACKET   -> "RBRACKET";
            case GLFW.GLFW_KEY_SEMICOLON       -> "SEMICOLON";
            case GLFW.GLFW_KEY_APOSTROPHE      -> "APOSTROPHE";
            case GLFW.GLFW_KEY_COMMA           -> "COMMA";
            case GLFW.GLFW_KEY_PERIOD          -> "PERIOD";
            case GLFW.GLFW_KEY_SLASH           -> "SLASH";
            case GLFW.GLFW_KEY_BACKSLASH       -> "BACKSLASH";
            default -> "KEY_" + key;
        };
    }

    // ── rendering ────────────────────────────────────────────────

    @Override
    public void render(float mouseX, float mouseY, float delta) {
        update();
        float hover = getHoverProgress();
        float a = getAlpha();

        // Background highlight on hover
        if (hover > 0.01f) {
            NanoVGRender.drawRoundedRect(x, y, width, height, 6,
                    Theme.withAlpha(Theme.SURFACE_HOVER, hover * a));
        }

        // Label
        NanoVGRender.drawText("Keybind",
                x + 10, y + height / 2,
                Theme.withAlpha(Theme.TEXT_SECONDARY, a), 13, "bold");

        // Value / hint
        if (listening) {
            // Pulsing "Press a key..." indicator
            pulseTime += 16;
            float pulse = (float) (Math.sin(pulseTime * 0.0045) * 0.3 + 0.7);
            NanoVGRender.drawTextRight("Press a key...",
                    x + width - 10, y + height / 2,
                    Theme.withAlpha(Theme.ACCENT, a * pulse), 12, "regular");
        } else {
            int key = module.getKeyBind();
            String keyName = getKeyName(key);
            boolean hasBind = key > 0;
            int color = hasBind ? Theme.ACCENT : Theme.TEXT_DISABLED;
            NanoVGRender.drawTextRight(keyName,
                    x + width - 10, y + height / 2,
                    Theme.withAlpha(color, a), 12, "regular");
        }
    }

    // ── input ────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !isMouseOver(mouseX, mouseY)) return false;
        if (button == 1) {
            // Right-click: clear bind
            if (listening) {
                listening = false;
                focused = false;
            } else {
                module.setKeyBind(-1);
            }
            return true;
        }
        if (button == 0) {
            if (listening) {
                // Click again while listening → abort
                listening = false;
                focused = false;
            } else {
                listening = true;
                focused = true;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible || !focused || !listening) return false;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            // ESC → abort listening
            listening = false;
            focused = false;
            return true;
        }
        // Capture any other key
        module.setKeyBind(keyCode);
        listening = false;
        focused = false;
        return true;
    }

    public boolean isListening() {
        return listening;
    }
}
