package cn.gardenia.client.command;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;

public class CommandManager {
    public static final CommandManager INSTANCE = new CommandManager();

    private String prefix = ".";
    private final List<Command> commands = new ArrayList<>();

    private CommandManager() {}

    public void register(Command command) {
        commands.add(command);
    }

    public void registerAll(Command... commands) {
        for (Command c : commands) register(c);
    }

    public List<Command> getCommands() { return commands; }

    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }

    public boolean dispatch(String message) {
        if (!message.startsWith(prefix)) return false;

        String raw = message.substring(prefix.length()).trim();
        String[] parts = raw.split("\\s+");
        String name = parts[0];
        String[] args = new String[parts.length - 1];
        if (parts.length > 1) System.arraycopy(parts, 1, args, 0, parts.length - 1);

        for (Command cmd : commands) {
            if (cmd.matches(name)) {
                try {
                    cmd.execute(args);
                } catch (Exception e) {
                    msg("§cError: " + e.getMessage());
                }
                return true;
            }
        }

        msg("§cUnknown command. Use " + prefix + "help");
        return true;
    }

    public List<String> getSuggestions(String message) {
        if (!message.startsWith(prefix)) return null;

        String raw = message.substring(prefix.length()).trim();
        String[] parts = raw.split("\\s+");

        if (raw.isEmpty()) {
            List<String> result = new ArrayList<>();
            for (Command cmd : commands) {
                result.add(prefix + cmd.getName());
            }
            return result;
        }

        if (parts.length == 1) {
            List<String> result = new ArrayList<>();
            String partial = parts[0].toLowerCase();
            for (Command cmd : commands) {
                if (cmd.getName().toLowerCase().startsWith(partial)) {
                    result.add(prefix + cmd.getName());
                }
                for (String alias : cmd.getAliases()) {
                    if (alias.toLowerCase().startsWith(partial)) {
                        result.add(prefix + alias);
                    }
                }
            }
            return result;
        }

        String cmdName = parts[0];
        for (Command cmd : commands) {
            if (cmd.matches(cmdName)) {
                String[] cmdArgs = new String[parts.length - 1];
                System.arraycopy(parts, 1, cmdArgs, 0, parts.length - 1);
                return cmd.suggest(cmdArgs);
            }
        }

        return null;
    }

    public Command getByName(String name) {
        for (Command cmd : commands) {
            if (cmd.matches(name)) return cmd;
        }
        return null;
    }

    public static void msg(String text) {
        if (MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.sendMessage(Text.literal(text), false);
        }
    }
}
