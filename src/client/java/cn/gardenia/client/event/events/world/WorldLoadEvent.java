package cn.gardenia.client.event.events.world;

import cn.gardenia.client.event.Event;
import net.minecraft.world.World;

public class WorldLoadEvent extends Event {
    private final World world;

    public WorldLoadEvent(World world) {
        this.world = world;
    }

    public World getWorld() { return world; }
}
