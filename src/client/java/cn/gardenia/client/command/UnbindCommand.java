package cn.gardenia.client.command;

import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.ModuleManager;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;

public class UnbindCommand extends Command {
    public UnbindCommand() {
        super("unbind", "Unbind a module's key", ".unbind <module>", "ub");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 1) {
            CommandManager.msg("§cUsage: " + getUsage());
            return;
        }

        Module module = ModuleManager.INSTANCE.getByName(args[0]);
        if (module == null) {
            CommandManager.msg("§cModule not found: " + args[0]);
            return;
        }

        module.setKeyBind(-1);
        CommandManager.msg("§aUnbound §7" + module.getName());
    }

    @Override
    public List<String> suggest(String[] args) {
        if (args.length == 1) {
            List<String> result = new ArrayList<>();
            String partial = args[0].toLowerCase();
            for (Module m : ModuleManager.INSTANCE.getModules()) {
                if (m.getKeyBind() != -1 && m.getName().toLowerCase().startsWith(partial)) {
                    result.add(m.getName());
                }
            }
            return result;
        }
        return super.suggest(args);
    }
}
