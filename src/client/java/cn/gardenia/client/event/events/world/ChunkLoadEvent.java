package cn.gardenia.client.event.events.world;

import cn.gardenia.client.event.Event;
import net.minecraft.world.chunk.WorldChunk;

public class ChunkLoadEvent extends Event {
    private final WorldChunk chunk;

    public ChunkLoadEvent(WorldChunk chunk) {
        this.chunk = chunk;
    }

    public WorldChunk getChunk() { return chunk; }
}
