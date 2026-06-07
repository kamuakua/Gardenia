package cn.gardenia.client.module.macro;

import java.util.ArrayList;
import java.util.List;

public class MacroManager {
    public static final MacroManager INSTANCE = new MacroManager();

    private final List<Macro> macros = new ArrayList<>();

    private MacroManager() {}

    public void register(Macro macro) { macros.add(macro); }

    public void remove(Macro macro) { macros.remove(macro); }

    public List<Macro> getMacros() { return macros; }

    public Macro getByName(String name) {
        for (Macro m : macros) {
            if (m.getName().equalsIgnoreCase(name)) return m;
        }
        return null;
    }

    public void onKeyPress(int keyCode) {
        for (Macro m : macros) {
            if (m.getKey() == keyCode) {
                m.execute();
            }
        }
    }

    public void clear() { macros.clear(); }

    public void setMacros(List<Macro> newMacros) {
        macros.clear();
        macros.addAll(newMacros);
    }
}
