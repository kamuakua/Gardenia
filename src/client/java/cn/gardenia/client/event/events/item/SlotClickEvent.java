package cn.gardenia.client.event.events.item;

import cn.gardenia.client.event.Event;

public class SlotClickEvent extends Event {
    private final int slotIndex;
    private final int button;
    private final SlotActionType action;

    public enum SlotActionType {
        PICKUP, QUICK_MOVE, SWAP, CLONE, THROW, QUICK_CRAFT, PICKUP_ALL
    }

    public SlotClickEvent(int slotIndex, int button, SlotActionType action) {
        this.slotIndex = slotIndex;
        this.button = button;
        this.action = action;
    }

    public int getSlotIndex() { return slotIndex; }
    public int getButton() { return button; }
    public SlotActionType getAction() { return action; }
}
