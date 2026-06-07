package cn.gardenia.client.event;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class EventBus {
    public static final EventBus INSTANCE = new EventBus();

    private final Map<Class<?>, List<Consumer<?>>> listeners = new LinkedHashMap<>();

    private EventBus() {}

    public <T extends Event> void subscribe(Class<T> eventClass, Consumer<T> listener) {
        listeners.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public <T extends Event> void unsubscribe(Class<T> eventClass, Consumer<T> listener) {
        List<Consumer<?>> list = listeners.get(eventClass);
        if (list != null) list.remove(listener);
    }

    @SuppressWarnings("unchecked")
    public <T extends Event> void post(T event) {
        List<Consumer<?>> list = listeners.get(event.getClass());
        if (list == null) return;
        for (Consumer<?> consumer : list) {
            if (event.isStopped()) break;
            ((Consumer<T>) consumer).accept(event);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Event> void post(Class<? extends T> eventClass, T event) {
        List<Consumer<?>> list = listeners.get(eventClass);
        if (list == null) return;
        for (Consumer<?> consumer : list) {
            if (event.isStopped()) break;
            ((Consumer<T>) consumer).accept(event);
        }
    }

    public void clear() {
        listeners.clear();
    }
}
