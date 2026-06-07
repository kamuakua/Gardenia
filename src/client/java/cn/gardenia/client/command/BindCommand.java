package cn.gardenia.client.command;

import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.ModuleManager;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;

public class BindCommand extends Command {
    public BindCommand() {
        super("bind", "Bind a key to a module", ".bind <module> <key>", "b");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 1) {
            CommandManager.msg("§cUsage: " + getUsage());
            return;
        }

        if (args.length == 1) {
            Module module = ModuleManager.INSTANCE.getByName(args[0]);
            if (module == null) {
                CommandManager.msg("§cModule not found: " + args[0]);
                return;
            }
            int key = module.getKeyBind();
            if (key == -1) {
                CommandManager.msg("§7" + module.getName() + " §cis not bound to any key");
            } else {
                CommandManager.msg("§7" + module.getName() + " §ais bound to §f" + getKeyName(key));
            }
            return;
        }

        Module module = ModuleManager.INSTANCE.getByName(args[0]);
        if (module == null) {
            CommandManager.msg("§cModule not found: " + args[0]);
            return;
        }

        String keyName = args[1].toUpperCase();
        int keyCode = getKeyCode(keyName);

        if (keyCode == -1) {
            CommandManager.msg("§cKey not found: " + args[1]);
            return;
        }

        module.setKeyBind(keyCode);
        CommandManager.msg("§aBound §7" + module.getName() + " §ato §f" + keyName);
    }

    @Override
    public List<String> suggest(String[] args) {
        if (args.length == 1) {
            List<String> result = new ArrayList<>();
            String partial = args[0].toLowerCase();
            for (Module m : ModuleManager.INSTANCE.getModules()) {
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

    public static String getKeyName(int key) {
        if (key == -1) return "None";
        if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z)
            return String.valueOf((char)('A' + (key - GLFW.GLFW_KEY_A)));
        if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9)
            return String.valueOf((char)('0' + (key - GLFW.GLFW_KEY_0)));
        if (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F24)
            return "F" + (key - GLFW.GLFW_KEY_F1 + 1);
        switch (key) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT: return "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_SHIFT: return "LSHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL: return "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL: return "RCTRL";
            case GLFW.GLFW_KEY_CAPS_LOCK: return "CAPS";
            case GLFW.GLFW_KEY_LEFT_ALT: return "LALT";
            case GLFW.GLFW_KEY_RIGHT_ALT: return "RALT";
            case GLFW.GLFW_KEY_SPACE: return "SPACE";
            case GLFW.GLFW_KEY_TAB: return "TAB";
            case GLFW.GLFW_KEY_ENTER: return "ENTER";
            case GLFW.GLFW_KEY_ESCAPE: return "ESC";
            case GLFW.GLFW_KEY_INSERT: return "INSERT";
            case GLFW.GLFW_KEY_DELETE: return "DELETE";
            case GLFW.GLFW_KEY_HOME: return "HOME";
            case GLFW.GLFW_KEY_END: return "END";
            case GLFW.GLFW_KEY_PAGE_UP: return "PAGE_UP";
            case GLFW.GLFW_KEY_PAGE_DOWN: return "PAGE_DOWN";
            case GLFW.GLFW_KEY_UP: return "UP";
            case GLFW.GLFW_KEY_DOWN: return "DOWN";
            case GLFW.GLFW_KEY_LEFT: return "LEFT";
            case GLFW.GLFW_KEY_RIGHT: return "RIGHT";
            case GLFW.GLFW_KEY_GRAVE_ACCENT: return "GRAVE";
            case GLFW.GLFW_KEY_MINUS: return "MINUS";
            case GLFW.GLFW_KEY_EQUAL: return "EQUAL";
            case GLFW.GLFW_KEY_LEFT_BRACKET: return "LBRACKET";
            case GLFW.GLFW_KEY_RIGHT_BRACKET: return "RBRACKET";
            case GLFW.GLFW_KEY_SEMICOLON: return "SEMICOLON";
            case GLFW.GLFW_KEY_APOSTROPHE: return "APOSTROPHE";
            case GLFW.GLFW_KEY_COMMA: return "COMMA";
            case GLFW.GLFW_KEY_PERIOD: return "PERIOD";
            case GLFW.GLFW_KEY_SLASH: return "SLASH";
            case GLFW.GLFW_KEY_BACKSLASH: return "BACKSLASH";
        }
        return "KEY_" + key;
    }
}
