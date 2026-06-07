package cn.gardenia.client.tool.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.*;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.util.math.BlockPos;

public class GameTool {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public void init() {}

    public ClientWorld world() {
        return mc.world;
    }

    public boolean hasWorld() {
        return mc.world != null;
    }

    public boolean isSingleplayer() {
        return mc.isIntegratedServerRunning();
    }

    public String serverIp() {
        if (mc.getCurrentServerEntry() == null) return "localhost";
        return mc.getCurrentServerEntry().address;
    }

    public int serverPort() {
        if (mc.getCurrentServerEntry() == null) return 25565;
        String addr = mc.getCurrentServerEntry().address;
        if (addr != null && addr.contains(":")) {
            try { return Integer.parseInt(addr.split(":")[1]); } catch (Exception e) {}
        }
        return 25565;
    }

    public String serverBrand() {
        if (mc.getNetworkHandler() == null) return "";
        return mc.getNetworkHandler().getBrand();
    }

    public int getPing() {
        if (mc.player == null || mc.getNetworkHandler() == null) return 0;
        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        if (entry == null) return 0;
        return entry.getLatency();
    }

    public GameMode gameMode() {
        if (mc.player == null) return GameMode.DEFAULT;
        if (mc.interactionManager == null) return GameMode.DEFAULT;
        return mc.interactionManager.getCurrentGameMode();
    }

    public boolean isCreative() {
        return gameMode() == GameMode.CREATIVE;
    }

    public boolean isSurvival() {
        return gameMode() == GameMode.SURVIVAL;
    }

    public boolean isAdventure() {
        return gameMode() == GameMode.ADVENTURE;
    }

    public boolean isSpectator() {
        return gameMode() == GameMode.SPECTATOR;
    }

    public int tickCount() {
        if (mc.player == null) return 0;
        return mc.player.age;
    }

    public long worldTime() {
        if (mc.world == null) return 0;
        return mc.world.getTime();
    }

    public long timeOfDay() {
        if (mc.world == null) return 0;
        return mc.world.getTimeOfDay();
    }

    public boolean isDay() {
        long t = timeOfDay() % 24000;
        return t < 13000;
    }

    public boolean isNight() {
        return !isDay();
    }

    public boolean isRaining() {
        return mc.world != null && mc.world.isRaining();
    }

    public boolean isThundering() {
        return mc.world != null && mc.world.isThundering();
    }

    public float rainGradient() {
        if (mc.world == null) return 0;
        return mc.world.getRainGradient(1.0f);
    }

    public float thunderGradient() {
        if (mc.world == null) return 0;
        return mc.world.getThunderGradient(1.0f);
    }

    public Difficulty difficulty() {
        if (mc.world == null) return Difficulty.PEACEFUL;
        return mc.world.getDifficulty();
    }

    public DimensionType dimension() {
        if (mc.world == null) return null;
        return mc.world.getDimension();
    }

    public String dimensionName() {
        if (mc.world == null) return "";
        return mc.world.getRegistryKey().getValue().toString();
    }

    public boolean isOverworld() {
        return mc.world != null && mc.world.getRegistryKey() == World.OVERWORLD;
    }

    public boolean isNether() {
        return mc.world != null && mc.world.getRegistryKey() == World.NETHER;
    }

    public boolean isEnd() {
        return mc.world != null && mc.world.getRegistryKey() == World.END;
    }

    public int[] serverVersion() {
        if (mc.getNetworkHandler() == null) return new int[]{0, 0, 0};
        try {
            String brand = mc.getNetworkHandler().getBrand();
            if (brand == null) return new int[]{0, 0, 0};
            return new int[]{1, 0, 0};
        } catch (Exception e) {
            return new int[]{0, 0, 0};
        }
    }

    public long serverFps() {
        return 0;
    }

    public boolean hasOptifine() {
        try {
            Class.forName("net.optifine.Config");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
