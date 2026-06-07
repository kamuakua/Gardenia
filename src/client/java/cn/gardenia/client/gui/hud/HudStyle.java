package cn.gardenia.client.gui.hud;

public enum HudStyle {
    MINIMAL("Minimal"),
    MODERN("Modern"),
    GLASS("Glassmorphism"),
    OUTLINE("Outline"),
    BLOCK("Block");

    private final String name;

    HudStyle(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
