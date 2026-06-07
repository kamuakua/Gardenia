package cn.gardenia.client.event;

public abstract class Event {
    private boolean cancelled = false;
    private boolean stopping = false;

    public boolean isCancelled() { return cancelled; }
    public void cancel() { this.cancelled = true; }
    public void stop() { this.stopping = true; }
    public boolean isStopped() { return stopping; }
}
