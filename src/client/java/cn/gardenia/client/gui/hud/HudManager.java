package cn.gardenia.client.gui.hud;

import cn.gardenia.client.event.EventBus;
import cn.gardenia.client.event.events.game.ScreenCloseEvent;
import cn.gardenia.client.event.events.game.ScreenOpenEvent;
import cn.gardenia.client.event.events.input.MouseDragEvent;
import cn.gardenia.client.event.events.input.MouseEvent;
import cn.gardenia.client.event.events.input.MouseScrollEvent;
import cn.gardenia.client.event.events.render.RenderHudEvent;
import cn.gardenia.client.gui.hud.elements.ArrayListElement;
import cn.gardenia.client.gui.hud.elements.CoordsElement;
import cn.gardenia.client.gui.hud.elements.FpsElement;
import cn.gardenia.client.gui.hud.elements.PingElement;
import cn.gardenia.client.gui.hud.elements.TargetHudElement;
import cn.gardenia.client.gui.nanovg.NanoVGManager;
import cn.gardenia.client.gui.nanovg.NanoVGRender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;
import java.util.List;

public class HudManager {
    public static final HudManager INSTANCE = new HudManager();
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private final List<HudElement> elements = new ArrayList<>();
    private boolean active = false;
    private boolean editing = false;
    private boolean chatOpen = false;
    private boolean initializedPositions = false;
    private boolean hasUserDragged = false;

    private HudManager() {}

    public void init() {
        FpsElement fps = new FpsElement(4, 4);
        CoordsElement coords = new CoordsElement(4, 30);
        PingElement ping = new PingElement(4, 56);
        ArrayListElement arrayList = new ArrayListElement(mc.getWindow() != null ? mc.getWindow().getScaledWidth() - 130 : 100, 4);
        TargetHudElement targetHud = new TargetHudElement(100, 100);

        elements.add(fps);
        elements.add(coords);
        elements.add(ping);
        elements.add(arrayList);
        elements.add(targetHud);

        EventBus.INSTANCE.subscribe(RenderHudEvent.class, this::onRenderHud);
        EventBus.INSTANCE.subscribe(ScreenOpenEvent.class, this::onScreenOpen);
        EventBus.INSTANCE.subscribe(ScreenCloseEvent.class, this::onScreenClose);
        EventBus.INSTANCE.subscribe(MouseEvent.class, this::onMouseEvent);
        EventBus.INSTANCE.subscribe(MouseDragEvent.class, this::onMouseDrag);
        EventBus.INSTANCE.subscribe(MouseScrollEvent.class, this::onMouseScroll);
    }

    private void onRenderHud(RenderHudEvent event) {
        if (mc.player == null) return;
        // 编辑模式下依然渲染（让玩家能拖动定位），否则只在模块开启时渲染
        if (!active && !editing) return;

        if (!initializedPositions && mc.getWindow() != null && !hasUserDragged) {
            initializedPositions = true;
            for (HudElement el : elements) {
                if (el instanceof ArrayListElement) {
                    el.setPosition(mc.getWindow().getScaledWidth() - 130, 4);
                } else if (el instanceof TargetHudElement) {
                    el.setPosition(mc.getWindow().getScaledWidth() / 2f - 90, mc.getWindow().getScaledHeight() - 100);
                }
            }
        }

        if (!NanoVGManager.INSTANCE.isInitialized()) {
            NanoVGManager.INSTANCE.init();
        }

        NanoVGManager.INSTANCE.beginFrame(
                mc.getWindow().getScaledWidth(),
                mc.getWindow().getScaledHeight(),
                (float) mc.getWindow().getScaleFactor()
        );

        float mouseX = (float) (mc.mouse.getX() / mc.getWindow().getScaleFactor());
        float mouseY = (float) (mc.mouse.getY() / mc.getWindow().getScaleFactor());

        for (HudElement element : elements) {
            if (!element.isVisible()) continue;
            element.onMouseMove(mouseX, mouseY);
            element.render(mouseX, mouseY, event.getTickDelta(), editing);
        }

        if (editing) {
            for (HudElement element : elements) {
                if (!element.isVisible()) continue;
                element.renderEditOverlay(mouseX, mouseY);
            }
        }

        NanoVGManager.INSTANCE.endFrame();
    }

    private void onScreenOpen(ScreenOpenEvent event) {
        Screen screen = event.getScreen();
        if (screen instanceof ChatScreen) {
            chatOpen = true;
            setEditing(true);
        }
    }

    private void onScreenClose(ScreenCloseEvent event) {
        if (chatOpen) {
            chatOpen = false;
            setEditing(false);
        }
    }

    private void onMouseEvent(MouseEvent event) {
        if (!editing) return;

        double mouseX = event.getX();
        double mouseY = event.getY();

        if (event.isClick()) {
            for (HudElement element : elements) {
                if (element.mouseClicked(mouseX, mouseY, event.getButton())) {
                    break;
                }
            }
        } else if (event.isRelease()) {
            boolean wasDragging = false;
            for (HudElement element : elements) {
                if (element.isDragging()) wasDragging = true;
                element.mouseReleased(mouseX, mouseY, event.getButton());
            }
            if (wasDragging) hasUserDragged = true;
        }
    }

    private void onMouseDrag(MouseDragEvent event) {
        if (!editing) return;
        for (HudElement element : elements) {
            element.mouseDragged(event.getX(), event.getY(), event.getButton(), event.getDeltaX(), event.getDeltaY());
        }
    }

    private void onMouseScroll(MouseScrollEvent event) {
        if (!editing) return;
        for (HudElement element : elements) {
            element.onMouseMove(event.getX(), event.getY());
        }
    }

    public void setEditing(boolean editing) {
        this.editing = editing;
        for (HudElement element : elements) {
            element.setEditing(editing);
        }
    }

    /** 由 HUDModule 调用：开启/关闭 HUD 渲染 */
    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() { return active; }
    public boolean isEditing() { return editing; }
    public boolean isChatOpen() { return chatOpen; }
    public List<HudElement> getElements() { return elements; }
}
