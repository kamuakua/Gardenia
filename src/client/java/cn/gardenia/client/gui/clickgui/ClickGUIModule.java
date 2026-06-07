package cn.gardenia.client.gui.clickgui;

import cn.gardenia.client.event.EventBus;
import cn.gardenia.client.event.events.input.KeyEvent;
import cn.gardenia.client.gui.nanovg.NanoVGManager;
import cn.gardenia.client.module.Module;
import org.lwjgl.glfw.GLFW;

public class ClickGUIModule extends Module {
    public ClickGUIModule() {
        super("ClickGUI", "Modern panel-based GUI", cn.gardenia.client.module.Category.RENDER);
        setKeyBind(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    @Override
    public void onEnable() {
        ClickGUIScreen.open();
    }

    @Override
    public void onDisable() {
        ClickGUIScreen.closeGUI();
    }
}
