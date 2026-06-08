package cn.gardenia.client.config;

import cn.gardenia.client.command.CommandManager;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.ModuleManager;
import cn.gardenia.client.module.Setting;
import cn.gardenia.client.module.macro.Macro;
import cn.gardenia.client.module.macro.MacroManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class ConfigManager {
    public static final ConfigManager INSTANCE = new ConfigManager();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("gardenia");
    private static final Path MODULES_FILE = CONFIG_DIR.resolve("module.json");
    private static final Path LEGACY_MODULES_FILE = CONFIG_DIR.resolve("modules.json");
    private static final Path MISC_FILE = CONFIG_DIR.resolve("misc.json");

    private ConfigManager() {
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_DIR);
            saveModules();
            saveMisc();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void load() {
        try {
            Files.createDirectories(CONFIG_DIR);
            loadModules();
            loadMisc();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveModules() throws IOException {
        JsonObject root = new JsonObject();
        for (Module module : ModuleManager.INSTANCE.getModules()) {
            JsonObject moduleObject = new JsonObject();
            moduleObject.addProperty("enabled", module.isEnabled());
            moduleObject.addProperty("bind", module.getKeyBind());
            moduleObject.addProperty("key", module.getKeyBind());
            moduleObject.addProperty("keyBind", module.getKeyBind());

            JsonObject settings = new JsonObject();
            for (Setting<?> setting : module.getSettings()) {
                settings.add(setting.getName(), settingToJson(setting));
            }
            if (!settings.isEmpty()) {
                moduleObject.add("settings", settings);
            }

            root.add(module.getName(), moduleObject);
        }

        try (Writer writer = Files.newBufferedWriter(MODULES_FILE, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }
    }

    private void loadModules() throws IOException {
        Path modulesFile = Files.exists(MODULES_FILE) ? MODULES_FILE : LEGACY_MODULES_FILE;
        if (!Files.exists(modulesFile)) {
            return;
        }

        JsonObject root;
        try (Reader reader = Files.newBufferedReader(modulesFile, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (parsed == null || !parsed.isJsonObject()) {
                return;
            }
            root = parsed.getAsJsonObject();
        }

        for (Module module : ModuleManager.INSTANCE.getModules()) {
            JsonObject moduleObject = findObject(root, module.getName());
            if (moduleObject == null) {
                continue;
            }

            JsonObject settings = objectOrNull(moduleObject.get("settings"));
            if (settings != null) {
                loadSettings(module, settings);
            }

            Integer bind = readInt(moduleObject, "bind", "keyBind", "key");
            if (bind != null) {
                module.setKeyBind(bind);
            }

            Boolean enabled = readBoolean(moduleObject, "enabled");
            if (enabled != null) {
                module.setEnabled(enabled);
            }
        }
    }

    private void loadSettings(Module module, JsonObject settings) {
        for (Setting<?> setting : module.getSettings()) {
            JsonElement value = findElement(settings, setting.getName());
            if (value == null || value.isJsonNull()) {
                continue;
            }

            try {
                applySetting(setting, value);
            } catch (Exception ignored) {
                // Keep loading the rest of the config if one stale setting is invalid.
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void applySetting(Setting<?> setting, JsonElement value) {
        Object current = setting.getValue();
        if (current instanceof Boolean) {
            ((Setting<Boolean>) setting).setValue(asBoolean(value, (Boolean) current));
        } else if (current instanceof Integer) {
            ((Setting<Integer>) setting).setValue(asInt(value, (Integer) current));
        } else if (current instanceof Double) {
            ((Setting<Double>) setting).setValue(asDouble(value, (Double) current));
        } else if (current instanceof Float) {
            ((Setting<Float>) setting).setValue((float) asDouble(value, (Float) current));
        } else if (current instanceof Long) {
            ((Setting<Long>) setting).setValue(asLong(value, (Long) current));
        } else if (current instanceof String) {
            ((Setting<String>) setting).setValue(asString(value, (String) current));
        } else if (current instanceof Enum<?> enumValue) {
            Class enumClass = enumValue.getDeclaringClass();
            ((Setting<Enum>) setting).setValue(Enum.valueOf(enumClass, asString(value, enumValue.name()).toUpperCase(Locale.ROOT)));
        }
    }

    private JsonElement settingToJson(Setting<?> setting) {
        Object value = setting.getValue();
        if (value == null) {
            return com.google.gson.JsonNull.INSTANCE;
        }
        if (value instanceof Boolean bool) {
            return GSON.toJsonTree(bool);
        }
        if (value instanceof Number number) {
            return GSON.toJsonTree(number);
        }
        if (value instanceof Enum<?> enumValue) {
            return GSON.toJsonTree(enumValue.name());
        }
        return GSON.toJsonTree(String.valueOf(value));
    }

    private void saveMisc() throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("prefix", CommandManager.INSTANCE.getPrefix());

        JsonArray macros = new JsonArray();
        for (Macro macro : MacroManager.INSTANCE.getMacros()) {
            JsonObject macroObject = new JsonObject();
            macroObject.addProperty("name", macro.getName());
            macroObject.addProperty("key", macro.getKey());

            JsonArray commands = new JsonArray();
            if (macro.getCommands() != null) {
                for (String command : macro.getCommands()) {
                    commands.add(command);
                }
            }
            macroObject.add("commands", commands);
            macros.add(macroObject);
        }
        root.add("macros", macros);

        try (Writer writer = Files.newBufferedWriter(MISC_FILE, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }
    }

    private void loadMisc() throws IOException {
        if (!Files.exists(MISC_FILE)) {
            return;
        }

        JsonObject root;
        try (Reader reader = Files.newBufferedReader(MISC_FILE, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (parsed == null || !parsed.isJsonObject()) {
                return;
            }
            root = parsed.getAsJsonObject();
        }

        String prefix = readString(root, "prefix");
        if (prefix != null && !prefix.isEmpty()) {
            CommandManager.INSTANCE.setPrefix(prefix);
        }

        JsonArray macros = arrayOrNull(root.get("macros"));
        if (macros != null) {
            MacroManager.INSTANCE.clear();
            for (JsonElement element : macros) {
                JsonObject macroObject = objectOrNull(element);
                if (macroObject == null) {
                    continue;
                }

                String name = readString(macroObject, "name");
                Integer key = readInt(macroObject, "key");
                JsonArray commandsJson = arrayOrNull(macroObject.get("commands"));
                if (name == null || key == null || commandsJson == null) {
                    continue;
                }

                String[] commands = new String[commandsJson.size()];
                for (int i = 0; i < commandsJson.size(); i++) {
                    commands[i] = asString(commandsJson.get(i), "");
                }
                MacroManager.INSTANCE.register(new Macro(name, key, commands));
            }
        }
    }

    private JsonObject findObject(JsonObject object, String key) {
        JsonElement direct = object.get(key);
        if (direct != null && direct.isJsonObject()) {
            return direct.getAsJsonObject();
        }

        for (String candidate : object.keySet()) {
            if (candidate.equalsIgnoreCase(key)) {
                return objectOrNull(object.get(candidate));
            }
        }
        return null;
    }

    private JsonElement findElement(JsonObject object, String key) {
        JsonElement direct = object.get(key);
        if (direct != null) {
            return direct;
        }

        String normalizedKey = normalize(key);
        for (String candidate : object.keySet()) {
            if (candidate.equalsIgnoreCase(key) || normalize(candidate).equals(normalizedKey)) {
                return object.get(candidate);
            }
        }
        return null;
    }

    private String normalize(String value) {
        return value == null ? "" : value.replace(" ", "").replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
    }

    private JsonObject objectOrNull(JsonElement element) {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private JsonArray arrayOrNull(JsonElement element) {
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    private Boolean readBoolean(JsonObject object, String key) {
        JsonElement element = findElement(object, key);
        return element == null || element.isJsonNull() ? null : asBoolean(element, false);
    }

    private Integer readInt(JsonObject object, String... keys) {
        for (String key : keys) {
            JsonElement element = findElement(object, key);
            if (element != null && !element.isJsonNull()) {
                return asInt(element, 0);
            }
        }
        return null;
    }

    private String readString(JsonObject object, String key) {
        JsonElement element = findElement(object, key);
        return element == null || element.isJsonNull() ? null : asString(element, null);
    }

    private boolean asBoolean(JsonElement element, boolean fallback) {
        try {
            if (element.isJsonPrimitive()) {
                return element.getAsBoolean();
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private int asInt(JsonElement element, int fallback) {
        try {
            if (element.isJsonPrimitive()) {
                return element.getAsInt();
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private long asLong(JsonElement element, long fallback) {
        try {
            if (element.isJsonPrimitive()) {
                return element.getAsLong();
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private double asDouble(JsonElement element, double fallback) {
        try {
            if (element.isJsonPrimitive()) {
                return element.getAsDouble();
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private String asString(JsonElement element, String fallback) {
        try {
            if (element.isJsonPrimitive()) {
                return element.getAsString();
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }
}
