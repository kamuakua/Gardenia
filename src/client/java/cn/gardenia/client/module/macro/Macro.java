package cn.gardenia.client.module.macro;

import net.minecraft.client.MinecraftClient;

public class Macro {
    private final String name;
    private final int key;
    private final String[] commands;

    public Macro(String name, int key, String... commands) {
        this.name = name;
        this.key = key;
        this.commands = commands;
    }

    public void execute() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        for (String cmd : commands) {
            mc.player.networkHandler.sendCommand(cmd);
        }
    }

    public String getName() { return name; }
    public int getKey() { return key; }
    public String[] getCommands() { return commands; }
}
