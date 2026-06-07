package cn.gardenia.client.tool.combat;

import java.util.LinkedList;
import java.util.Queue;

public class CPSTool {
    private final Queue<Long> leftClicks = new LinkedList<>();
    private final Queue<Long> rightClicks = new LinkedList<>();
    private int totalLeftClicks;
    private int totalRightClicks;

    public void init() {}

    public void registerLeftClick() {
        long now = System.currentTimeMillis();
        leftClicks.add(now);
        totalLeftClicks++;
        cleanup();
    }

    public void registerRightClick() {
        long now = System.currentTimeMillis();
        rightClicks.add(now);
        totalRightClicks++;
        cleanup();
    }

    public int getLeftCPS() {
        cleanup(leftClicks);
        return leftClicks.size();
    }

    public int getRightCPS() {
        cleanup(rightClicks);
        return rightClicks.size();
    }

    public int getCPS() {
        return getLeftCPS() + getRightCPS();
    }

    public int getTotalLeftClicks() {
        return totalLeftClicks;
    }

    public int getTotalRightClicks() {
        return totalRightClicks;
    }

    public int getTotalClicks() {
        return totalLeftClicks + totalRightClicks;
    }

    public double getAverageCPS(long since) {
        long now = System.currentTimeMillis();
        long duration = now - since;
        if (duration <= 0) return 0;
        return (double) totalLeftClicks / (duration / 1000.0);
    }

    public void reset() {
        leftClicks.clear();
        rightClicks.clear();
        totalLeftClicks = 0;
        totalRightClicks = 0;
    }

    public void resetLeft() {
        leftClicks.clear();
        totalLeftClicks = 0;
    }

    public void resetRight() {
        rightClicks.clear();
        totalRightClicks = 0;
    }

    public long getTimeSinceLastLeftClick() {
        if (leftClicks.isEmpty()) return -1;
        return System.currentTimeMillis() - leftClicks.peek();
    }

    public long getTimeSinceLastRightClick() {
        if (rightClicks.isEmpty()) return -1;
        return System.currentTimeMillis() - rightClicks.peek();
    }

    private void cleanup() {
        cleanup(leftClicks);
        cleanup(rightClicks);
    }

    private void cleanup(Queue<Long> clicks) {
        long oneSecondAgo = System.currentTimeMillis() - 1000;
        while (!clicks.isEmpty() && clicks.peek() < oneSecondAgo) {
            clicks.poll();
        }
    }
}
