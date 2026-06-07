package cn.gardenia.client.command;

import cn.gardenia.client.module.ModuleManager;
import cn.gardenia.client.module.macro.Macro;
import cn.gardenia.client.module.macro.MacroManager;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MacroCommand extends Command {
    public MacroCommand() {
        super("macro", "Manage macros: add, del, list", ".macro <add/del/list> [args]", "m");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 1) {
            CommandManager.msg("§cUsage:");
            CommandManager.msg("§c .macro add <name> <key> <command...>");
            CommandManager.msg("§c .macro del <name>");
            CommandManager.msg("§c .macro list");
            return;
        }

        switch (args[0].toLowerCase()) {
            case "add" -> addMacro(args);
            case "del" -> delMacro(args);
            case "list" -> listMacros();
            case "remove" -> delMacro(args);
            default -> CommandManager.msg("§cUnknown subcommand: " + args[0]);
        }
    }

    private void addMacro(String[] args) {
        if (args.length < 4) {
            CommandManager.msg("§cUsage: .macro add <name> <key> <command...>");
            return;
        }

        String name = args[1];
        String keyName = args[2].toUpperCase();
        int keyCode = getKeyCode(keyName);
        if (keyCode == -1) {
            CommandManager.msg("§cInvalid key: " + args[2]);
            return;
        }

        StringBuilder cmdBuilder = new StringBuilder();
        for (int i = 3; i < args.length; i++) {
            if (cmdBuilder.length() > 0) cmdBuilder.append(" ");
            cmdBuilder.append(args[i]);
        }

        String[] commands = cmdBuilder.toString().split(";");
        for (int i = 0; i < commands.length; i++) {
            commands[i] = commands[i].trim();
        }

        if (MacroManager.INSTANCE.getByName(name) != null) {
            CommandManager.msg("§cMacro '" + name + "' already exists");
            return;
        }

        Macro macro = new Macro(name, keyCode, commands);
        MacroManager.INSTANCE.register(macro);
        CommandManager.msg("§aMacro §7'" + name + "' §abound to §f" + keyName);
    }

    private void delMacro(String[] args) {
        if (args.length < 2) {
            CommandManager.msg("§cUsage: .macro del <name>");
            return;
        }

        Macro macro = MacroManager.INSTANCE.getByName(args[1]);
        if (macro == null) {
            CommandManager.msg("§cMacro not found: " + args[1]);
            return;
        }

        MacroManager.INSTANCE.remove(macro);
        CommandManager.msg("§aRemoved macro §7'" + args[1] + "'");
    }

    private void listMacros() {
        List<Macro> macros = MacroManager.INSTANCE.getMacros();
        if (macros.isEmpty()) {
            CommandManager.msg("§7No macros defined");
            return;
        }

        StringBuilder sb = new StringBuilder("§6--- §eMacros §6---\n");
        for (Macro m : macros) {
            sb.append(" §a").append(m.getName()).append(" §7→ §f")
                    .append(BindCommand.getKeyName(m.getKey())).append("\n");
            for (String cmd : m.getCommands()) {
                sb.append("   §7> §f").append(cmd).append("\n");
            }
        }
        CommandManager.msg(sb.toString());
    }

    @Override
    public List<String> suggest(String[] args) {
        if (args.length == 1) {
            List<String> result = new ArrayList<>();
            String partial = args[0].toLowerCase();
            for (String s : new String[]{"add", "del", "list"}) {
                if (s.startsWith(partial)) result.add(s);
            }
            return result;
        }

        if (args[0].equalsIgnoreCase("del") && args.length == 2) {
            List<String> result = new ArrayList<>();
            String partial = args[1].toLowerCase();
            for (Macro m : MacroManager.INSTANCE.getMacros()) {
                if (m.getName().toLowerCase().startsWith(partial)) {
                    result.add(m.getName());
                }
            }
            return result;
        }

        return super.suggest(args);
    }

    private int getKeyCode(String name) {
        switch (name) {
            case "RSHIFT": return GLFW.GLFW_KEY_RIGHT_SHIFT;
            case "LSHIFT": return GLFW.GLFW_KEY_LEFT_SHIFT;
            case "LCONTROL": case "LCTRL": return GLFW.GLFW_KEY_LEFT_CONTROL;
            case "RCONTROL": case "RCTRL": return GLFW.GLFW_KEY_RIGHT_CONTROL;
            case "CAPS": return GLFW.GLFW_KEY_CAPS_LOCK;
            case "LALT": return GLFW.GLFW_KEY_LEFT_ALT;
            case "RALT": return GLFW.GLFW_KEY_RIGHT_ALT;
            default:
                if (name.length() == 1) {
                    char c = name.charAt(0);
                    if (c >= 'A' && c <= 'Z') return GLFW.GLFW_KEY_A + (c - 'A');
                    if (c >= '0' && c <= '9') return GLFW.GLFW_KEY_0 + (c - '0');
                }
                if (name.startsWith("F") && name.length() <= 3) {
                    try {
                        int n = Integer.parseInt(name.substring(1));
                        if (n >= 1 && n <= 24) return GLFW.GLFW_KEY_F1 + (n - 1);
                    } catch (NumberFormatException ignored) {}
                }
                try {
                    int field = GLFW.class.getField("GLFW_KEY_" + name).getInt(null);
                    return field;
                } catch (Exception e) {
                    return -1;
                }
        }
    }
}
