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
import cn.gardenia.client.module.combat.AntiBots;
import cn.gardenia.client.module.combat.CriticalsModule;
import cn.gardenia.client.module.combat.Velocity;
import cn.gardenia.client.module.misc.AutoToolModule;
import cn.gardenia.client.module.misc.FastPlaceModule;
import cn.gardenia.client.module.misc.Target;
import cn.gardenia.client.module.misc.Teams;
import cn.gardenia.client.module.render.ESPModule;
import cn.gardenia.client.module.render.HUDModule;
import cn.gardenia.client.module.render.NotificationModule;
import cn.gardenia.client.module.render.SwordBlockModule;
import cn.gardenia.client.module.skill.NoFallSkill;
import cn.gardenia.client.module.skill.SpeedSkill;
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
                new NoFallSkill(),
                new SpeedSkill(),
                new ClickGUIModule(),
                new HUDModule(),
                new AutoClickerModule(),
                new CriticalsModule(),
                new AntiBots(),
                new Velocity(),
                new Teams(),
                new Target(),
                new XRayModule(),
                new ESPModule(),
                new TimerModule(),
                new AutoToolModule(),
                new FastPlaceModule(),
                new NotificationModule(),
                new SwordBlockModule()
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
