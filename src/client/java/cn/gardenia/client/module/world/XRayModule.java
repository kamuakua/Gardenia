package cn.gardenia.client.module.world;

import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import cn.gardenia.client.module.Setting;
import cn.gardenia.client.event.events.render.RenderBlockEntityEvent;
import java.util.function.Consumer;
import java.util.Arrays;
import java.util.List;

public class XRayModule extends Module {
    private final Setting<Boolean> diamonds = new Setting<>("Diamonds", "Show diamonds", true);
    private final Setting<Boolean> iron = new Setting<>("Iron", "Show iron", true);
    private final Setting<Boolean> gold = new Setting<>("Gold", "Show gold", true);
    private final Setting<Boolean> redstone = new Setting<>("Redstone", "Show redstone", true);
    private final Setting<Boolean> lapis = new Setting<>("Lapis", "Show lapis", true);
    private final Setting<Boolean> emerald = new Setting<>("Emerald", "Show emerald", true);
    private final Consumer<RenderBlockEntityEvent> renderListener;

    private final List<net.minecraft.block.Block> ores = Arrays.asList(
        net.minecraft.block.Blocks.DIAMOND_ORE,
        net.minecraft.block.Blocks.DEEPSLATE_DIAMOND_ORE,
        net.minecraft.block.Blocks.IRON_ORE,
        net.minecraft.block.Blocks.DEEPSLATE_IRON_ORE,
        net.minecraft.block.Blocks.GOLD_ORE,
        net.minecraft.block.Blocks.DEEPSLATE_GOLD_ORE,
        net.minecraft.block.Blocks.REDSTONE_ORE,
        net.minecraft.block.Blocks.DEEPSLATE_REDSTONE_ORE,
        net.minecraft.block.Blocks.LAPIS_ORE,
        net.minecraft.block.Blocks.DEEPSLATE_LAPIS_ORE,
        net.minecraft.block.Blocks.EMERALD_ORE,
        net.minecraft.block.Blocks.DEEPSLATE_EMERALD_ORE
    );

    public XRayModule() {
        super("XRay", "See ores through blocks", Category.WORLD);
        addSetting(diamonds);
        addSetting(iron);
        addSetting(gold);
        addSetting(redstone);
        addSetting(lapis);
        addSetting(emerald);
        this.renderListener = this::onRenderBlock;
    }

    @Override
    public void onEnable() {
        subscribe(RenderBlockEntityEvent.class, renderListener);
        if (mc.worldRenderer != null) {
            mc.worldRenderer.reload();
        }
    }

    @Override
    public void onDisable() {
        unsubscribe(RenderBlockEntityEvent.class, renderListener);
        if (mc.worldRenderer != null) {
            mc.worldRenderer.reload();
        }
    }

    private void onRenderBlock(RenderBlockEntityEvent event) {
    }

    public boolean shouldRenderBlock(net.minecraft.block.Block block) {
        if (!ores.contains(block)) return true;

        if (block == net.minecraft.block.Blocks.DIAMOND_ORE || block == net.minecraft.block.Blocks.DEEPSLATE_DIAMOND_ORE) {
            return diamonds.getBoolean();
        }
        if (block == net.minecraft.block.Blocks.IRON_ORE || block == net.minecraft.block.Blocks.DEEPSLATE_IRON_ORE) {
            return iron.getBoolean();
        }
        if (block == net.minecraft.block.Blocks.GOLD_ORE || block == net.minecraft.block.Blocks.DEEPSLATE_GOLD_ORE) {
            return gold.getBoolean();
        }
        if (block == net.minecraft.block.Blocks.REDSTONE_ORE || block == net.minecraft.block.Blocks.DEEPSLATE_REDSTONE_ORE) {
            return redstone.getBoolean();
        }
        if (block == net.minecraft.block.Blocks.LAPIS_ORE || block == net.minecraft.block.Blocks.DEEPSLATE_LAPIS_ORE) {
            return lapis.getBoolean();
        }
        if (block == net.minecraft.block.Blocks.EMERALD_ORE || block == net.minecraft.block.Blocks.DEEPSLATE_EMERALD_ORE) {
            return emerald.getBoolean();
        }
        return true;
    }
}