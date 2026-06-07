package cn.gardenia.client.module;

public abstract class Skill extends Module {
    private final int cooldownTicks;
    private int currentCooldown;

    protected Skill(String name, String description, int cooldownTicks) {
        super(name, description, Category.PLAYER);
        this.cooldownTicks = cooldownTicks;
        this.currentCooldown = 0;
    }

    public abstract void onActivate();

    @Override
    public void onEnable() {
        if (currentCooldown > 0) {
            setEnabled(false);
            return;
        }
        onActivate();
        currentCooldown = cooldownTicks;
        setEnabled(false);
    }

    @Override
    public void onTick() {
        if (currentCooldown > 0) currentCooldown--;
    }

    public int getCooldownTicks() { return cooldownTicks; }
    public int getCurrentCooldown() { return currentCooldown; }
    public boolean isOnCooldown() { return currentCooldown > 0; }
}