package cn.gardenia.client.command;

import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;

public class ToggleCommand extends Command {
    public ToggleCommand() {
        super("toggle", "Toggle a module on/off", ".toggle <module>", "t");
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

        module.toggle();
        CommandManager.msg("§7[" + module.getCategory().name() + "] §a" + module.getName()
                + " §7→ " + (module.isEnabled() ? "§aenabled" : "§cdisabled"));
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
}
