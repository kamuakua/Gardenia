package cn.gardenia.client.command;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class Command {
    private final String name;
    private final String description;
    private final String usage;
    private final List<String> aliases;

    protected Command(String name, String description, String usage, String... aliases) {
        this.name = name;
        this.description = description;
        this.usage = usage;
        this.aliases = Arrays.asList(aliases);
    }

    public abstract void execute(String[] args);

    public List<String> suggest(String[] args) {
        return Collections.emptyList();
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getUsage() { return usage; }
    public List<String> getAliases() { return aliases; }

    public boolean matches(String input) {
        if (name.equalsIgnoreCase(input)) return true;
        for (String alias : aliases) {
            if (alias.equalsIgnoreCase(input)) return true;
        }
        return false;
    }
}
