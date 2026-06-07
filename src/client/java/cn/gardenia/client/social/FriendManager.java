package cn.gardenia.client.social;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class FriendManager {
    private static final List<String> friends = new CopyOnWriteArrayList<>();

    public static boolean isFriend(Entity entity) {
        return entity instanceof PlayerEntity && isFriend(entity.getName().getString());
    }

    public static boolean isFriend(String name) {
        return friends.contains(name);
    }

    public static void addFriend(PlayerEntity player) {
        addFriend(player.getName().getString());
    }

    public static void addFriend(String name) {
        if (!friends.contains(name)) {
            friends.add(name);
        }
    }

    public static void removeFriend(PlayerEntity player) {
        removeFriend(player.getName().getString());
    }

    public static void removeFriend(String name) {
        friends.remove(name);
    }

    public static List<String> getFriends() {
        return friends;
    }
}
