package cn.gardenia.client.event.events.game;

import cn.gardenia.client.event.Event;
import net.minecraft.client.gui.screen.Screen;

public class ScreenOpenEvent extends Event {
    private final Screen screen;

    public ScreenOpenEvent(Screen screen) {
        this.screen = screen;
    }

    public Screen getScreen() { return screen; }
}
