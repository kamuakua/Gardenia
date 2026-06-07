package cn.gardenia.client.command;

import java.util.ArrayList;
import java.util.List;

public class PrefixCommand extends Command {
    public PrefixCommand() {
        super("prefix", "Change the command prefix", ".prefix <char>", "p");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 1 || args[0].isEmpty()) {
            CommandManager.msg("§cUsage: " + getUsage());
            return;
        }

        String newPrefix = args[0];
        String oldPrefix = CommandManager.INSTANCE.getPrefix();
        CommandManager.INSTANCE.setPrefix(newPrefix);
        CommandManager.msg("§aPrefix changed from §f'" + oldPrefix + "' §ato §f'" + newPrefix + "'");
    }

    @Override
    public List<String> suggest(String[] args) {
        if (args.length == 1) {
            List<String> result = new ArrayList<>();
            for (String s : new String[]{".", ",", ";", "/", "-", "!", "#"}) {
                if (s.startsWith(args[0])) result.add(s);
            }
            return result;
        }
        return super.suggest(args);
    }
}
