package cn.gardenia.client.module.misc;

import cn.gardenia.client.module.Category;
import cn.gardenia.client.module.Module;
import net.minecraft.entity.Entity;

public class ClientFriendModule extends Module {
    public ClientFriendModule() {
        super("ClientFriend", "Treat other client users as friends", Category.MISC);
    }

    public static boolean isUser(Entity entity) {
        return false;
    }
}
