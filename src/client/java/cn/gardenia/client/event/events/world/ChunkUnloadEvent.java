package cn.gardenia.client.event.events.world;

import cn.gardenia.client.event.Event;
import net.minecraft.world.chunk.WorldChunk;

public class ChunkUnloadEvent extends Event {
    private final WorldChunk chunk;

    public ChunkUnloadEvent(WorldChunk chunk) {
        this.chunk = chunk;
    }

    public WorldChunk getChunk() { return chunk; }
}
