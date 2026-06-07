package cn.gardenia.client.event.events.input;

import cn.gardenia.client.event.Event;

public class KeyEvent extends Event {
    private final int keyCode;
    private final int scanCode;
    private final int action;
    private final int modifiers;

    public static final int PRESS = 1;
    public static final int RELEASE = 0;
    public static final int REPEAT = 2;

    public KeyEvent(int keyCode, int scanCode, int action, int modifiers) {
        this.keyCode = keyCode;
        this.scanCode = scanCode;
        this.action = action;
        this.modifiers = modifiers;
    }

    public int getKeyCode() { return keyCode; }
    public int getScanCode() { return scanCode; }
    public int getAction() { return action; }
    public int getModifiers() { return modifiers; }

    public boolean isPress() { return action == PRESS; }
    public boolean isRelease() { return action == RELEASE; }
    public boolean isRepeat() { return action == REPEAT; }
}
