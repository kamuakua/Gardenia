package cn.gardenia.client.tool;

import cn.gardenia.client.tool.render.*;
import cn.gardenia.client.tool.player.*;
import cn.gardenia.client.tool.movement.*;
import cn.gardenia.client.tool.rotation.*;
import cn.gardenia.client.tool.combat.*;
import cn.gardenia.client.tool.block.*;
import cn.gardenia.client.tool.item.*;
import cn.gardenia.client.tool.armor.*;

public class ToolManager {
    public static final ToolManager INSTANCE = new ToolManager();

    public final RenderTool RENDER = new RenderTool();
    public final GLTool GL = new GLTool();
    public final ShaderTool SHADER = new ShaderTool();
    public final CameraTool CAMERA = new CameraTool();
    public final ColorTool COLOR = new ColorTool();

    public final PlayerTool PLAYER = new PlayerTool();
    public final GameTool GAME = new GameTool();

    public final MovementTool MOVEMENT = new MovementTool();
    public final VelocityTool VELOCITY = new VelocityTool();

    public final RotationTool ROTATION = new RotationTool();
    public final BezierRotation BEZIER = new BezierRotation();
    public final SmoothRotation SMOOTH = new SmoothRotation();
    public final InstantRotation INSTANT_R = new InstantRotation();
    public final IncrementalRotation INCREMENTAL = new IncrementalRotation();
    public final AimAssistTool AIM_ASSIST = new AimAssistTool();

    public final AttackTool ATTACK = new AttackTool();
    public final TargetTool TARGET = new TargetTool();
    public final TargetFilter TARGET_FILTER = new TargetFilter();
    public final CPSTool CPS = new CPSTool();
    public final ReachTool REACH = new ReachTool();

    public final BlockTool BLOCK = new BlockTool();
    public final PlacementTool PLACEMENT = new PlacementTool();

    public final ItemTool ITEM = new ItemTool();
    public final InventoryTool INVENTORY = new InventoryTool();

    public final ArmorTool ARMOR = new ArmorTool();

    private ToolManager() {}

    public void init() {
        RENDER.init();
        GL.init();
        SHADER.init();
        CAMERA.init();
        COLOR.init();
        PLAYER.init();
        GAME.init();
        MOVEMENT.init();
        VELOCITY.init();
        ROTATION.init();
        BEZIER.init();
        SMOOTH.init();
        INSTANT_R.init();
        INCREMENTAL.init();
        AIM_ASSIST.init();
        ATTACK.init();
        TARGET.init();
        TARGET_FILTER.init();
        CPS.init();
        REACH.init();
        BLOCK.init();
        PLACEMENT.init();
        ITEM.init();
        INVENTORY.init();
        ARMOR.init();
    }
}
