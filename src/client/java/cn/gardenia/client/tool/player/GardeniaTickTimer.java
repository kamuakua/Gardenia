package cn.gardenia.client.tool.player;

public final class GardeniaTickTimer {
    private static volatile float persistentMultiplier = 1.0F;
    private static volatile float transientMultiplier = 1.0F;

    private GardeniaTickTimer() {
    }

    public static void setPersistentMultiplier(float multiplier) {
        persistentMultiplier = sanitize(multiplier);
    }

    public static void resetPersistentMultiplier() {
        persistentMultiplier = 1.0F;
    }

    public static void setTransientMultiplier(float multiplier) {
        transientMultiplier = sanitize(multiplier);
    }

    public static void resetTransientMultiplier() {
        transientMultiplier = 1.0F;
    }

    public static float multiplier() {
        return sanitize(persistentMultiplier * transientMultiplier);
    }

    private static float sanitize(float multiplier) {
        if (Float.isNaN(multiplier) || Float.isInfinite(multiplier)) {
            return 1.0F;
        }
        return Math.clamp(multiplier, 0.05F, 20.0F);
    }
}
