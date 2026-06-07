package cn.gardenia.client.module;

import cn.gardenia.client.gui.notification.Notification;
import cn.gardenia.client.gui.notification.NotificationManager;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModuleManager {
    public static final ModuleManager INSTANCE = new ModuleManager();

    private final List<Module> modules = new ArrayList<>();

    private ModuleManager() {}

    public void register(Module module) {
        modules.add(module);
    }

    public void registerAll(Module... modules) {
        for (Module m : modules) register(m);
    }

    public List<Module> getModules() { return modules; }

    public List<Module> getModulesByCategory(Category category) {
        return modules.stream()
                .filter(m -> m.getCategory() == category)
                .sorted((m1, m2) -> Integer.compare(m2.getName().length(), m1.getName().length()))
                .collect(Collectors.toList());
    }

    public Module getByName(String name) {
        for (Module m : modules) {
            if (m.getName().equalsIgnoreCase(name)) return m;
        }
        return null;
    }

    public <T extends Module> T getByClass(Class<T> moduleClass) {
        for (Module m : modules) {
            if (moduleClass.isInstance(m)) return moduleClass.cast(m);
        }
        return null;
    }

    public void onTick() {
        for (Module m : modules) {
            if (m.isEnabled()) m.onTick();
        }
    }

    public void onKeyPress(int keyCode) {
        for (Module m : modules) {
            if (m.getKeyBind() == keyCode) {
                m.toggle();
            }
        }
    }

    public void onModuleEnabled(Module module) {
        NotificationManager.INSTANCE.show(
                Notification.forModuleToggle(module, true));
    }

    public void onModuleDisabled(Module module) {
        NotificationManager.INSTANCE.show(
                Notification.forModuleToggle(module, false));
    }

    public int getModuleCount() { return modules.size(); }

    public int getEnabledCount() {
        int count = 0;
        for (Module m : modules) {
            if (m.isEnabled()) count++;
        }
        return count;
    }
}
