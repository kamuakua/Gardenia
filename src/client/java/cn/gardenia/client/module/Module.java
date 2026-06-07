package cn.gardenia.client.module;

import cn.gardenia.client.event.Event;
import cn.gardenia.client.event.EventBus;
import net.minecraft.client.MinecraftClient;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class Module {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    private final String name;
    private final String description;
    private final Category category;
    private boolean enabled;
    private int keyBind;
    private final List<Setting<?>> settings;

    protected Module(String name, String description, Category category) {
        this(name, description, category, false);
    }

    protected Module(String name, String description, Category category, boolean defaultEnabled) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabled = defaultEnabled;
        this.keyBind = -1;
        this.settings = new ArrayList<>();
    }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}

    protected <T extends Event> void subscribe(Class<T> eventClass, Consumer<T> listener) {
        EventBus.INSTANCE.subscribe(eventClass, listener);
    }

    protected <T extends Event> void unsubscribe(Class<T> eventClass, Consumer<T> listener) {
        EventBus.INSTANCE.unsubscribe(eventClass, listener);
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (enabled) {
            onEnable();
            ModuleManager.INSTANCE.onModuleEnabled(this);
        } else {
            onDisable();
            ModuleManager.INSTANCE.onModuleDisabled(this);
        }
    }

    public void toggle() { setEnabled(!enabled); }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public boolean isEnabled() { return enabled; }
    public int getKeyBind() { return keyBind; }
    public List<Setting<?>> getSettings() { return settings; }

    public void setKeyBind(int key) { this.keyBind = key; }

    protected <T> void addSetting(Setting<T> setting) {
        settings.add(setting);
    }
}
