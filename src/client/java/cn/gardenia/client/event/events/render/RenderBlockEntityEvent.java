package cn.gardenia.client.event.events.render;

import cn.gardenia.client.event.Event;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;

public class RenderBlockEntityEvent extends Event {
    private final BlockEntity blockEntity;
    private final float tickDelta;
    private final MatrixStack matrices;
    private final VertexConsumerProvider vertexConsumers;

    public RenderBlockEntityEvent(BlockEntity blockEntity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        this.blockEntity = blockEntity;
        this.tickDelta = tickDelta;
        this.matrices = matrices;
        this.vertexConsumers = vertexConsumers;
    }

    public BlockEntity getBlockEntity() { return blockEntity; }
    public float getTickDelta() { return tickDelta; }
    public MatrixStack getMatrices() { return matrices; }
    public VertexConsumerProvider getVertexConsumers() { return vertexConsumers; }
}
