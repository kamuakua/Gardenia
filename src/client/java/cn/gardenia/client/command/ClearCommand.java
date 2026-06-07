package cn.gardenia.client.command;

public class ClearCommand extends Command {
    public ClearCommand() {
        super("clear", "Clear the chat", ".clear", "c", "clr");
    }

    @Override
    public void execute(String[] args) {
        if (net.minecraft.client.MinecraftClient.getInstance().inGameHud != null
                && net.minecraft.client.MinecraftClient.getInstance().inGameHud.getChatHud() != null) {
            net.minecraft.client.MinecraftClient.getInstance().inGameHud.getChatHud().clear(false);
        }
    }
}
