package cn.gardenia.client.command;

import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.ModuleManager;
import java.util.ArrayList;
import java.util.List;

public class HelpCommand extends Command {
    public HelpCommand() {
        super("help", "Show available commands and modules", ".help [command|module]", "h", "?");
    }

    @Override
    public void execute(String[] args) {
        StringBuilder sb = new StringBuilder();

        if (args.length > 0) {
            Command cmd = CommandManager.INSTANCE.getByName(args[0]);
            if (cmd != null) {
                sb.append("§6--- §e").append(cmd.getName()).append(" §6---\n");
                sb.append("§7Description: §f").append(cmd.getDescription()).append("\n");
                sb.append("§7Usage: §f").append(cmd.getUsage()).append("\n");
                if (!cmd.getAliases().isEmpty()) {
                    sb.append("§7Aliases: §f");
                    for (String a : cmd.getAliases()) {
                        sb.append(CommandManager.INSTANCE.getPrefix()).append(a).append(" ");
                    }
                }
                CommandManager.msg(sb.toString());
                return;
            }

            Module module = ModuleManager.INSTANCE.getByName(args[0]);
            if (module != null) {
                sb.append("§6--- §e").append(module.getName()).append(" §6---\n");
                sb.append("§7Category: §f").append(module.getCategory().name()).append("\n");
                sb.append("§7Status: ").append(module.isEnabled() ? "§aEnabled" : "§cDisabled").append("\n");
                int key = module.getKeyBind();
                if (key != -1) {
                    sb.append("§7Key: §f").append(BindCommand.getKeyName(key)).append("\n");
                }
                sb.append("§7Description: §f").append(module.getDescription()).append("\n");
                if (!module.getSettings().isEmpty()) {
                    sb.append("§7Settings:\n");
                    module.getSettings().forEach(s ->
                        sb.append(" §7- §f").append(s.getName()).append(": §a").append(s.getValue()).append("\n"));
                }
                CommandManager.msg(sb.toString());
                return;
            }

            CommandManager.msg("§cUnknown: " + args[0]);
            return;
        }

        sb.append("§6--- §eCommands §6---\n");
        String p = CommandManager.INSTANCE.getPrefix();
        for (Command cmd : CommandManager.INSTANCE.getCommands()) {
            sb.append("§6").append(p).append(cmd.getName())
                    .append("§7 - §f").append(cmd.getDescription()).append("\n");
        }

        sb.append("§6--- §eModules §6---\n");
        for (Category cat : Category.values()) {
            List<Module> catModules = ModuleManager.INSTANCE.getModulesByCategory(cat);
            if (catModules.isEmpty()) continue;
            sb.append("§6[").append(cat.name()).append("]§7 ");
            for (Module m : catModules) {
                sb.append(m.isEnabled() ? "§a" : "§7")
                        .append(m.getName()).append(" ");
            }
            sb.append("\n");
        }

        CommandManager.msg(sb.toString());
    }

    @Override
    public List<String> suggest(String[] args) {
        if (args.length == 1) {
            List<String> result = new ArrayList<>();
            String partial = args[0].toLowerCase();
            for (Command cmd : CommandManager.INSTANCE.getCommands()) {
                if (cmd.getName().toLowerCase().startsWith(partial)) {
                    result.add(cmd.getName());
                }
            }
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
