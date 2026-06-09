package cn.gardenia.client.module.move;

import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.ModuleManager;
import cn.gardenia.client.module.Setting;
import cn.gardenia.client.module.combat.KillAura;
import net.minecraft.entity.Entity;
import net.minecraft.util.PlayerInput;

public class TargetStrafeModule extends Module {
    public static TargetStrafeModule INSTANCE;

    private final Setting<Double> range = rangedDouble("Range", "Target follow range", 5.0D, 1.0D, 6.0D, 0.1D);
    private final Setting<Double> switchDelay = rangedDouble("SwitchDelay", "Delay before following a new aura target", 1000.0D, 0.0D, 10000.0D, 100.0D);
    private final Setting<Boolean> collisionSmart = new Setting<>("Collision Smart", "Ignore range during supported collision speed mode", true);

    private Entity currentTarget;
    private long lastTargetSwitchTime;
    private boolean ignoringRange;

    public TargetStrafeModule() {
        super("TargetStrafe", "Automatically moves forward to follow the KillAura target", Category.MOVEMENT);
        INSTANCE = this;
        addSetting(range);
        addSetting(switchDelay);
        addSetting(collisionSmart);
    }

    @Override
    public void onEnable() {
        INSTANCE = this;
        currentTarget = null;
        ignoringRange = false;
    }

    @Override
    public void onDisable() {
        currentTarget = null;
        ignoringRange = false;
    }

    public String getSuffix() {
        return ignoringRange ? "Smart" : null;
    }

    public static PlayerInput handleMoveInput(PlayerInput input) {
        if (INSTANCE == null || !INSTANCE.isEnabled()) {
            return input;
        }
        return INSTANCE.onMoveInput(input);
    }

    private PlayerInput onMoveInput(PlayerInput input) {
        if (mc.player == null) {
            return input;
        }

        KillAura killAura = ModuleManager.INSTANCE.getByClass(KillAura.class);
        if (killAura == null || !killAura.isEnabled()) {
            clearTarget();
            return input;
        }

        Entity auraTarget = killAura.getTarget();
        if (auraTarget == null) {
            clearTarget();
            return input;
        }

        boolean shouldIgnoreRange = collisionSmart.getBoolean() && shouldIgnoreRangeForSpeed();
        if (!shouldIgnoreRange && mc.player.distanceTo(auraTarget) > range.getDouble()) {
            clearTarget();
            return input;
        }

        long now = System.currentTimeMillis();
        if (currentTarget == null || currentTarget != auraTarget) {
            if (now - lastTargetSwitchTime < switchDelay.getDouble()) {
                return input;
            }
            currentTarget = auraTarget;
            lastTargetSwitchTime = now;
            ignoringRange = shouldIgnoreRange;
        } else if (shouldIgnoreRange) {
            ignoringRange = true;
        }

        return new PlayerInput(
                true,
                false,
                input.left(),
                input.right(),
                input.jump(),
                input.sneak(),
                input.sprint()
        );
    }

    private void clearTarget() {
        currentTarget = null;
        ignoringRange = false;
    }

    private boolean shouldIgnoreRangeForSpeed() {
        try {
            SpeedModule speed = ModuleManager.INSTANCE.getByClass(SpeedModule.class);
            if (speed == null || !speed.isEnabled()) {
                return false;
            }

            java.lang.reflect.Field modeField = SpeedModule.class.getDeclaredField("mode");
            modeField.setAccessible(true);
            Object mode = modeField.get(speed);
            if (mode instanceof Setting<?> setting && !"Collision".equals(setting.getString())) {
                return false;
            }

            java.lang.reflect.Method bpsMethod = SpeedModule.class.getDeclaredMethod("getCurrentBPS");
            bpsMethod.setAccessible(true);
            Object bps = bpsMethod.invoke(speed);
            return bps instanceof Number number && number.doubleValue() > 8.0D;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static Setting<Double> rangedDouble(String name, String description, double defaultValue, double min, double max, double step) {
        Setting<Double> setting = new Setting<>(name, description, defaultValue);
        setting.setRange(min, max, step);
        return setting;
    }
}
