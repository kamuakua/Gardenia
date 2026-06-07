package cn.gardenia.client.command;

import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.ModuleManager;

public class KeysCommand extends Command {
    public KeysCommand() {
        super("keys", "List all bound keys", ".keys", "k", "binds");
    }

    @Override
    public void execute(String[] args) {
        StringBuilder sb = new StringBuilder("§6--- §eBound Keys §6---\n");
        boolean hasBinds = false;

        for (Module m : ModuleManager.INSTANCE.getModules()) {
            int key = m.getKeyBind();
            if (key != -1) {
                hasBinds = true;
                sb.append(" §6").append(BindCommand.getKeyName(key)).append(" §7→ ")
                        .append(m.isEnabled() ? "§a" : "§7")
                        .append(m.getName()).append("\n");
            }
        }

        if (!hasBinds) {
            CommandManager.msg("§7No bound keys");
            return;
        }

        CommandManager.msg(sb.toString());
    }
}
