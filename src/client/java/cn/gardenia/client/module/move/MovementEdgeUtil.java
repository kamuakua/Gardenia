package cn.gardenia.client.module.move;

import net.minecraft.client.MinecraftClient;

final class MovementEdgeUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private MovementEdgeUtil() {
    }

    static boolean isOnBlockEdge(float sensitivity) {
        if (mc.player == null || mc.world == null) {
            return false;
        }
        return !mc.world
                .getBlockCollisions(
                        mc.player,
                        mc.player.getBoundingBox()
                                .offset(0.0D, -0.5D, 0.0D)
                                .expand(-sensitivity, 0.0D, -sensitivity)
                )
                .iterator()
                .hasNext();
    }
}
