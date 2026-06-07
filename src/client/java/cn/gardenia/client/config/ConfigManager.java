package cn.gardenia.client.config;

import cn.gardenia.client.command.CommandManager;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.ModuleManager;
import cn.gardenia.client.module.Setting;
import cn.gardenia.client.module.macro.Macro;
import cn.gardenia.client.module.macro.MacroManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

public class ConfigManager {
    public static final ConfigManager INSTANCE = new ConfigManager();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("gardenia");
    private static final Path MODULES_FILE = CONFIG_DIR.resolve("modules.json");
    private static final Path MISC_FILE = CONFIG_DIR.resolve("misc.json");

    private ConfigManager() {}

    public void save() {
        try {
            CONFIG_DIR.toFile().mkdirs();
            saveModules();
            saveMisc();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveModules() {
        try (Writer writer = new OutputStreamWriter(
                new FileOutputStream(MODULES_FILE.toFile()), StandardCharsets.UTF_8)) {
            Map<String, ModuleData> data = new LinkedHashMap<>();
            for (Module m : ModuleManager.INSTANCE.getModules()) {
                ModuleData md = new ModuleData();
                md.enabled = m.isEnabled();
                md.key = m.getKeyBind();
                if (!m.getSettings().isEmpty()) {
                    md.settings = new LinkedHashMap<>();
                    for (Setting<?> s : m.getSettings()) {
                        md.settings.put(s.getName(), String.valueOf(s.getValue()));
                    }
                }
                data.put(m.getName(), md);
            }
            GSON.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveMisc() {
        try (Writer writer = new OutputStreamWriter(
                new FileOutputStream(MISC_FILE.toFile()), StandardCharsets.UTF_8)) {
            MiscData data = new MiscData();
            data.prefix = CommandManager.INSTANCE.getPrefix();
            data.macros = new ArrayList<>();
            for (Macro m : MacroManager.INSTANCE.getMacros()) {
                MacroData md = new MacroData();
                md.name = m.getName();
                md.key = m.getKey();
                md.commands = m.getCommands();
                data.macros.add(md);
            }
            GSON.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void load() {
        try {
            CONFIG_DIR.toFile().mkdirs();
            loadModules();
            loadMisc();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadModules() {
        if (!MODULES_FILE.toFile().exists()) return;
        try (Reader reader = new InputStreamReader(
                new FileInputStream(MODULES_FILE.toFile()), StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, ModuleData>>() {}.getType();
            Map<String, ModuleData> data = GSON.fromJson(reader, type);
            if (data == null) return;
            for (Module m : ModuleManager.INSTANCE.getModules()) {
                ModuleData md = data.get(m.getName());
                if (md != null) {
                    m.setKeyBind(md.key);
                    m.setEnabled(md.enabled);
                    if (md.settings != null) {
                        for (Setting<?> s : m.getSettings()) {
                            String valStr = md.settings.get(s.getName());
                            if (valStr != null) {
                                try {
                                    Object current = s.getValue();
                                    if (current instanceof Boolean) {
                                        ((Setting<Boolean>) s).setValue(Boolean.parseBoolean(valStr));
                                    } else if (current instanceof Integer) {
                                        ((Setting<Integer>) s).setValue(Integer.parseInt(valStr));
                                    } else if (current instanceof Double) {
                                        ((Setting<Double>) s).setValue(Double.parseDouble(valStr));
                                    } else if (current instanceof Float) {
                                        ((Setting<Float>) s).setValue(Float.parseFloat(valStr));
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadMisc() {
        if (!MISC_FILE.toFile().exists()) return;
        try (Reader reader = new InputStreamReader(
                new FileInputStream(MISC_FILE.toFile()), StandardCharsets.UTF_8)) {
            MiscData data = GSON.fromJson(reader, MiscData.class);
            if (data == null) return;
            if (data.prefix != null) {
                CommandManager.INSTANCE.setPrefix(data.prefix);
            }
            if (data.macros != null) {
                MacroManager.INSTANCE.clear();
                for (MacroData md : data.macros) {
                    Macro macro = new Macro(md.name, md.key, md.commands);
                    MacroManager.INSTANCE.register(macro);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ModuleData {
        boolean enabled;
        int key;
        Map<String, String> settings;
    }

    private static class MiscData {
        String prefix;
        List<MacroData> macros;
    }

    private static class MacroData {
        String name;
        int key;
        String[] commands;
    }
}
