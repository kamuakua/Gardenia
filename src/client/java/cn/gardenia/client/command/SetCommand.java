package cn.gardenia.client.command;

import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.ModuleManager;
import cn.gardenia.client.module.Setting;
import java.util.ArrayList;
import java.util.List;

public class SetCommand extends Command {
    public SetCommand() {
        super("set", "Change a module's setting value", ".set <module> <setting> <value>", "s");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 3) {
            CommandManager.msg("§cUsage: " + getUsage());
            return;
        }

        Module module = ModuleManager.INSTANCE.getByName(args[0]);
        if (module == null) {
            CommandManager.msg("§cModule not found: " + args[0]);
            return;
        }

        String settingName = args[1];
        String valueStr = args[2];

        for (Setting<?> setting : module.getSettings()) {
            if (setting.getName().equalsIgnoreCase(settingName)) {
                try {
                    Object current = setting.getValue();
                    if (current instanceof Boolean) {
                        ((Setting<Boolean>) setting).setValue(Boolean.parseBoolean(valueStr));
                    } else if (current instanceof Number) {
                        if (current instanceof Integer) {
                            ((Setting<Integer>) setting).setValue(Integer.parseInt(valueStr));
                        } else if (current instanceof Double) {
                            ((Setting<Double>) setting).setValue(Double.parseDouble(valueStr));
                        } else if (current instanceof Float) {
                            ((Setting<Float>) setting).setValue(Float.parseFloat(valueStr));
                        } else if (current instanceof Long) {
                            ((Setting<Long>) setting).setValue(Long.parseLong(valueStr));
                        }
                    } else if (current instanceof String) {
                        ((Setting<String>) setting).setValue(valueStr);
                    } else if (current instanceof Enum) {
                        Class<Enum> enumClass = (Class<Enum>) current.getClass();
                        ((Setting<Enum>) setting).setValue(Enum.valueOf(enumClass, valueStr.toUpperCase()));
                    }
                    CommandManager.msg("§aSet §7" + module.getName() + " §f" + setting.getName()
                            + " §ato §a" + setting.getValue());
                } catch (Exception e) {
                    CommandManager.msg("§cInvalid value for " + settingName + ": " + valueStr);
                }
                return;
            }
        }

        CommandManager.msg("§cSetting not found: " + settingName);
    }

    @Override
    public List<String> suggest(String[] args) {
        if (args.length == 1) {
            List<String> result = new ArrayList<>();
            String partial = args[0].toLowerCase();
            for (Module m : ModuleManager.INSTANCE.getModules()) {
                if (!m.getSettings().isEmpty() && m.getName().toLowerCase().startsWith(partial)) {
                    result.add(m.getName());
                }
            }
            return result;
        }

        if (args.length == 2) {
            Module module = ModuleManager.INSTANCE.getByName(args[0]);
            if (module != null) {
                List<String> result = new ArrayList<>();
                String partial = args[1].toLowerCase();
                for (Setting<?> s : module.getSettings()) {
                    if (s.getName().toLowerCase().startsWith(partial)) {
                        result.add(s.getName());
                    }
                }
                return result;
            }
        }

        return super.suggest(args);
    }
}
