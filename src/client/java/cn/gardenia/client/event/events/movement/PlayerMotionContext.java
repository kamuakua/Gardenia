package cn.gardenia.client.event.events.movement;

public final class PlayerMotionContext {
    private static PlayerMotionEvent activeEvent;

    private PlayerMotionContext() {
    }

    public static PlayerMotionEvent getActiveEvent() {
        return activeEvent;
    }

    public static void setActiveEvent(PlayerMotionEvent event) {
        activeEvent = event;
    }

    public static void clear() {
        activeEvent = null;
    }
}
