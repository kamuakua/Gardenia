package cn.gardenia.client;

import cn.gardenia.client.command.*;
import cn.gardenia.client.config.ConfigManager;
import cn.gardenia.client.gui.clickgui.ClickGUIModule;
import cn.gardenia.client.gui.hud.HudManager;
import cn.gardenia.client.gui.notification.NotificationManager;
import cn.gardenia.client.module.FullBrightModule;
import cn.gardenia.client.module.ModuleManager;
import cn.gardenia.client.module.SprintModule;
import cn.gardenia.client.module.combat.AutoClickerModule;
import cn.gardenia.client.module.combat.AimAssistModule;
import cn.gardenia.client.module.combat.AntiBots;
import cn.gardenia.client.module.combat.AutoHealModule;
import cn.gardenia.client.module.combat.AutoThrowModule;
import cn.gardenia.client.module.combat.BowAimModule;
import cn.gardenia.client.module.combat.CriticalsModule;
import cn.gardenia.client.module.combat.CrystalAuraModule;
import cn.gardenia.client.module.combat.GrimveloModule;
import cn.gardenia.client.module.combat.KeepSprintModule;
import cn.gardenia.client.module.combat.KillAura;
import cn.gardenia.client.module.combat.NewBackTrackModule;
import cn.gardenia.client.module.combat.NewVelocityModule;
import cn.gardenia.client.module.combat.TestCriticalModule;
import cn.gardenia.client.module.combat.TpAuraModule;
import cn.gardenia.client.module.combat.Velocity;
import cn.gardenia.client.module.misc.AutoGardeniaModule;
import cn.gardenia.client.module.misc.AutoPearlModule;
import cn.gardenia.client.module.misc.AutoSoupModule;
import cn.gardenia.client.module.misc.AutoToolModule;
import cn.gardenia.client.module.misc.AntiFireballModule;
import cn.gardenia.client.module.misc.ChestStealerModule;
import cn.gardenia.client.module.misc.ClientFriendModule;
import cn.gardenia.client.module.misc.DisablerModule;
import cn.gardenia.client.module.misc.FastPlaceModule;
import cn.gardenia.client.module.misc.FlagCheckModule;
import cn.gardenia.client.module.misc.GhostHandModule;
import cn.gardenia.client.module.misc.InterceptModule;
import cn.gardenia.client.module.misc.KillSayModule;
import cn.gardenia.client.module.misc.LanguageModule;
import cn.gardenia.client.module.misc.MiddleClickModule;
import cn.gardenia.client.module.misc.NewInvManagerModule;
import cn.gardenia.client.module.misc.Target;
import cn.gardenia.client.module.misc.Teams;
import cn.gardenia.client.module.move.AutoStuckModule;
import cn.gardenia.client.module.move.BlinkModule;
import cn.gardenia.client.module.move.EagleModule;
import cn.gardenia.client.module.move.FastWebModule;
import cn.gardenia.client.module.move.AutoBucketModule;
import cn.gardenia.client.module.move.GrimFlyModule;
import cn.gardenia.client.module.move.GrimLowHopModule;
import cn.gardenia.client.module.move.GrimSpeedModule;
import cn.gardenia.client.module.move.LongJumpModule;
import cn.gardenia.client.module.move.NoFallModule;
import cn.gardenia.client.module.move.NoJumpDelayModule;
import cn.gardenia.client.module.move.NoSlowModule;
import cn.gardenia.client.module.move.SafeWalkModule;
import cn.gardenia.client.module.move.ScaffoldModule;
import cn.gardenia.client.module.move.SpeedModule;
import cn.gardenia.client.module.move.StuckModule;
import cn.gardenia.client.module.move.TargetStrafeModule;
import cn.gardenia.client.module.render.ESPModule;
import cn.gardenia.client.module.render.HUDModule;
import cn.gardenia.client.module.render.NotificationModule;
import cn.gardenia.client.module.render.SwordBlockModule;
import cn.gardenia.client.module.world.TimerModule;
import cn.gardenia.client.module.world.XRayModule;
import cn.gardenia.client.tool.ToolManager;
import net.fabricmc.api.ClientModInitializer;

public class GardeniaClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ToolManager.INSTANCE.init();

        ModuleManager.INSTANCE.registerAll(
                new SprintModule(),
                new FullBrightModule(),
                new ClickGUIModule(),
                new HUDModule(),
                new AutoClickerModule(),
                new AimAssistModule(),
                new AutoHealModule(),
                new AutoThrowModule(),
                new BowAimModule(),
                new CrystalAuraModule(),
                new CriticalsModule(),
                new GrimveloModule(),
                new KeepSprintModule(),
                new KillAura(),
                new NewBackTrackModule(),
                new NewVelocityModule(),
                new TestCriticalModule(),
                new TpAuraModule(),
                new AntiBots(),
                new Velocity(),
                new Teams(),
                new Target(),
                new XRayModule(),
                new ESPModule(),
                new TimerModule(),
                new AntiFireballModule(),
                new AutoGardeniaModule(),
                new AutoPearlModule(),
                new AutoSoupModule(),
                new AutoToolModule(),
                new ChestStealerModule(),
                new ClientFriendModule(),
                new DisablerModule(),
                new FastPlaceModule(),
                new FlagCheckModule(),
                new GhostHandModule(),
                new InterceptModule(),
                new KillSayModule(),
                new LanguageModule(),
                new MiddleClickModule(),
                new NewInvManagerModule(),
                new NotificationModule(),
                new SwordBlockModule(),
                new NoJumpDelayModule(),
                new SafeWalkModule(),
                new ScaffoldModule(),
                new FastWebModule(),
                new EagleModule(),
                new SpeedModule(),
                new NoSlowModule(),
                new NoFallModule(),
                new AutoBucketModule(),
                new GrimFlyModule(),
                new GrimLowHopModule(),
                new GrimSpeedModule(),
                new LongJumpModule(),
                new StuckModule(),
                new TargetStrafeModule(),
                new AutoStuckModule(),
                new BlinkModule()
        );

        // 初始化 HUD 系统（注册事件监听，创建 HUD 元素）
        HudManager.INSTANCE.init();

        // 初始化通知系统
        NotificationManager.INSTANCE.init();

        CommandManager.INSTANCE.registerAll(
                new ToggleCommand(),
                new BindCommand(),
                new UnbindCommand(),
                new HelpCommand(),
                new ListCommand(),
                new SetCommand(),
                new MacroCommand(),
                new PrefixCommand(),
                new ConfigCommand(),
                new ClearCommand(),
                new PanicCommand(),
                new KeysCommand()
        );

        ConfigManager.INSTANCE.load();
    }
}
