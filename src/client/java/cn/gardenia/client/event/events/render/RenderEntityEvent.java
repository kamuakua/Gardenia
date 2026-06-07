package cn.gardenia.client.event.events.render;

import cn.gardenia.client.event.Event;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.client.render.VertexConsumerProvider;

public class RenderEntityEvent extends Event {
    private final Entity entity;
    private final float tickDelta;
    private final MatrixStack matrices;
    private final VertexConsumerProvider vertexConsumers;

    public RenderEntityEvent(Entity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        this.entity = entity;
        this.tickDelta = tickDelta;
        this.matrices = matrices;
        this.vertexConsumers = vertexConsumers;
    }

    public Entity getEntity() { return entity; }
    public float getTickDelta() { return tickDelta; }
    public MatrixStack getMatrices() { return matrices; }
    public VertexConsumerProvider getVertexConsumers() { return vertexConsumers; }
}
