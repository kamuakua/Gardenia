package cn.gardenia.client.command;

import cn.gardenia.client.config.ConfigManager;

public class ConfigCommand extends Command {
    public ConfigCommand() {
        super("config", "Save or load the config", ".config <save/load>", "cfig");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 1) {
            CommandManager.msg("§cUsage: " + getUsage());
            return;
        }

        switch (args[0].toLowerCase()) {
            case "save" -> {
                ConfigManager.INSTANCE.save();
                CommandManager.msg("§aConfig saved");
            }
            case "load" -> {
                ConfigManager.INSTANCE.load();
                CommandManager.msg("§aConfig loaded");
            }
            default -> CommandManager.msg("§cUsage: " + getUsage());
        }
    }

    @Override
    public java.util.List<String> suggest(String[] args) {
        if (args.length == 1) {
            java.util.List<String> result = new java.util.ArrayList<>();
            String partial = args[0].toLowerCase();
            for (String s : new String[]{"save", "load"}) {
                if (s.startsWith(partial)) result.add(s);
            }
            return result;
        }
        return super.suggest(args);
    }
}
