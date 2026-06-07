package cn.gardenia.client.event.events.player;

import cn.gardenia.client.event.Event;

public class PlayerExperienceEvent extends Event {
    private final int experienceLevel;
    private final float progress;
    private final int totalExperience;

    public PlayerExperienceEvent(int experienceLevel, float progress, int totalExperience) {
        this.experienceLevel = experienceLevel;
        this.progress = progress;
        this.totalExperience = totalExperience;
    }

    public int getExperienceLevel() { return experienceLevel; }
    public float getProgress() { return progress; }
    public int getTotalExperience() { return totalExperience; }
}
