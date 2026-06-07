package cn.gardenia.client.command;

import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.ModuleManager;
import java.util.ArrayList;
import java.util.List;

public class ListCommand extends Command {
    public ListCommand() {
        super("list", "List all modules, optionally by category", ".list [category]", "l");
    }

    @Override
    public void execute(String[] args) {
        StringBuilder sb = new StringBuilder();

        if (args.length > 0) {
            String catName = args[0].toUpperCase();
            try {
                Category cat = Category.valueOf(catName);
                List<Module> modules = ModuleManager.INSTANCE.getModulesByCategory(cat);
                if (modules.isEmpty()) {
                    CommandManager.msg("§7No modules in §f" + cat.name());
                    return;
                }
                sb.append("§6--- §e").append(cat.name()).append(" §6---\n");
                for (Module m : modules) {
                    sb.append(" ").append(m.isEnabled() ? "§a" : "§7").append(m.getName());
                    int key = m.getKeyBind();
                    if (key != -1) {
                        sb.append(" §8[").append(BindCommand.getKeyName(key)).append("]");
                    }
                    sb.append("\n");
                }
            } catch (IllegalArgumentException e) {
                CommandManager.msg("§cInvalid category: " + args[0] + ". Use: COMBAT, RENDER, PLAYER, MISC, WORLD");
                return;
            }
        } else {
            sb.append("§6--- §eModule List §6---\n");
            for (Category cat : Category.values()) {
                List<Module> modules = ModuleManager.INSTANCE.getModulesByCategory(cat);
                if (modules.isEmpty()) continue;
                sb.append("§6[").append(cat.name()).append("]§7 ");
                for (Module m : modules) {
                    sb.append(m.isEnabled() ? "§a" : "§7").append(m.getName()).append(" ");
                }
                sb.append("\n");
            }
            sb.append("§7Total: §f").append(ModuleManager.INSTANCE.getModuleCount()).append(" modules");
            sb.append(" §7| Enabled: §a").append(ModuleManager.INSTANCE.getEnabledCount());
        }

        CommandManager.msg(sb.toString());
    }

    @Override
    public List<String> suggest(String[] args) {
        if (args.length == 1) {
            List<String> result = new ArrayList<>();
            String partial = args[0].toLowerCase();
            for (Category cat : Category.values()) {
                if (cat.name().toLowerCase().startsWith(partial)) {
                    result.add(cat.name());
                }
            }
            return result;
        }
        return super.suggest(args);
    }
}
