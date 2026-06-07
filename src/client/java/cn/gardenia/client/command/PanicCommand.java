package cn.gardenia.client.command;

import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.ModuleManager;

public class PanicCommand extends Command {
    public PanicCommand() {
        super("panic", "Disable all enabled modules", ".panic");
    }

    @Override
    public void execute(String[] args) {
        int count = 0;
        for (Module m : ModuleManager.INSTANCE.getModules()) {
            if (m.isEnabled()) {
                m.setEnabled(false);
                count++;
            }
        }
        CommandManager.msg("§cPanic! §7Disabled §f" + count + " §7modules");
    }
}
