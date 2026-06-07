package cn.gardenia.client.module.misc;

import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.ModuleManager;
import cn.gardenia.client.module.Setting;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.Objects;

public class Teams extends Module {
    public static Teams INSTANCE;

    private final Setting<String> mode = new Setting<>("Mode", "Team check mode", "Scoreboard", new String[]{"Scoreboard", "Color"});

    public Teams() {
        super("Teams", "Prevent attack teammates", Category.MISC);
        INSTANCE = this;
        addSetting(mode);
    }

    public static boolean isSameTeam(Entity entity) {
        if (mc.player == null || mc.getNetworkHandler() == null) {
            return false;
        }

        Teams module = ModuleManager.INSTANCE.getByClass(Teams.class);
        if (module == null || !module.isEnabled()) {
            return false;
        }
        if (!(entity instanceof PlayerEntity)) {
            return false;
        }

        if ("Color".equalsIgnoreCase(module.mode.getString())) {
            Formatting c1 = getTeamColor(entity);
            Formatting c2 = getTeamColor(mc.player);
            return c1 != null && c2 != null && c1.equals(c2);
        }

        String playerTeam = getTeam(entity);
        String targetTeam = getTeam(mc.player);
        return playerTeam != null && targetTeam != null && Objects.equals(playerTeam, targetTeam);
    }

    public static String getTeam(Entity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return null;
        }

        PlayerListEntry playerInfo = mc.getNetworkHandler().getPlayerListEntry(entity.getUuid());
        if (playerInfo != null && playerInfo.getScoreboardTeam() != null) {
            return playerInfo.getScoreboardTeam().getName();
        }

        Team team = entity.getScoreboardTeam();
        if (team != null) {
            return team.getName();
        }

        TextColor color = getPrimaryFormatting(entity.getDisplayName());
        return color != null ? "color:" + color.getName() : null;
    }

    private static Formatting getTeamColor(Entity entity) {
        Team team = entity.getScoreboardTeam();
        return team == null ? null : team.getColor();
    }

    private static TextColor getPrimaryFormatting(Text component) {
        if (component == null) {
            return null;
        }

        TextColor color = component.getStyle().getColor();
        if (color != null) {
            return color;
        }

        for (Text sibling : component.getSiblings()) {
            TextColor siblingColor = getPrimaryFormatting(sibling);
            if (siblingColor != null) {
                return siblingColor;
            }
        }

        return null;
    }
}
