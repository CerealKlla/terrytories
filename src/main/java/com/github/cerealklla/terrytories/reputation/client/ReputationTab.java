package com.github.cerealklla.terrytories.reputation.client;

import java.util.List;

import com.github.cerealklla.nuclyus.menu.client.NuclyusMenu;
import com.github.cerealklla.terrytories.network.ReputationPayload;
import com.github.cerealklla.terrytories.network.RequestReputationPayload;
import com.github.cerealklla.terrytories.reputation.Standing;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;

/**
 * "Reputation" tab -- a scrollable list of every settlement the player currently has nonzero
 * reputation with, by name, current value, and standing tier. Requested once per client session
 * (same "request on first need, cache statically" shape as Lyfe's own {@code
 * menu.client.RuneCodexTab}'s Runewords sub-tab) rather than kept generally synced -- reputation
 * isn't needed anywhere else on the client, so there's no reason to pay a sync cost for it outside
 * this one screen.
 *
 * <p>Deliberately labeled "group," not "settlement" -- see {@code network.ReputationPayload}'s own
 * doc on the still-unbuilt territory-free Faction concept this will need to widen to cover later
 * (e.g. a hostile-mob faction with no physical settlement at all).
 */
public final class ReputationTab implements NuclyusMenu {

    private static final int MARGIN = 10;
    private static final int ROW_HEIGHT = 12;
    private static final int NAME_COLOR = 0xFFE0E0E0;
    private static final int HOSTILE_COLOR = 0xFFFF5050;
    private static final int NEUTRAL_COLOR = 0xFFC0C0C0;
    private static final int FRIENDLY_COLOR = 0xFF50FF50;

    private static volatile List<ReputationPayload.Entry> entries = List.of();
    private static boolean requested;

    private int scrollOffset;

    @Override
    public Component title() {
        return Component.literal("Reputation");
    }

    @Override
    public int order() {
        return 6;
    }

    @Override
    public void onShown() {
        scrollOffset = 0;
        if (!requested) {
            requested = true;
            var connection = Minecraft.getInstance().getConnection();
            if (connection != null) {
                connection.send(new ServerboundCustomPayloadPacket(new RequestReputationPayload()));
            }
        }
    }

    @Override
    public void onHidden() {
    }

    public static void setEntries(List<ReputationPayload.Entry> newEntries) {
        entries = newEntries;
    }

    @Override
    public boolean mouseScrolled(Rect2i area, double mouseX, double mouseY, double scrollX, double scrollY) {
        int viewport = area.getHeight() - 2 * MARGIN;
        int contentHeight = entries.size() * ROW_HEIGHT;
        int maxScroll = Math.max(0, contentHeight - viewport);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) (scrollY * ROW_HEIGHT * 2)));
        return true;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, Rect2i area, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        int left = area.getX() + MARGIN;
        int top = area.getY() + MARGIN;
        int bottom = area.getY() + area.getHeight() - MARGIN;
        int right = area.getX() + area.getWidth() - MARGIN;

        List<ReputationPayload.Entry> current = entries;
        if (current.isEmpty()) {
            graphics.text(font, Component.literal("No reputation with anyone yet."), left, top, NAME_COLOR, false);
            return;
        }

        graphics.enableScissor(left, top, right, bottom);
        int y = top - scrollOffset;
        for (ReputationPayload.Entry entry : current) {
            if (y + ROW_HEIGHT >= top && y <= bottom) {
                graphics.text(font, Component.literal(entry.groupName()), left, y, NAME_COLOR, false);
                Standing standing = Standing.forValue(entry.value());
                int color = switch (standing) {
                    case HOSTILE -> HOSTILE_COLOR;
                    case NEUTRAL -> NEUTRAL_COLOR;
                    case FRIENDLY -> FRIENDLY_COLOR;
                };
                String statusText = entry.value() + " (" + standing.name() + ")";
                int statusX = right - font.width(statusText);
                graphics.text(font, Component.literal(statusText), statusX, y, color, false);
            }
            y += ROW_HEIGHT;
        }
        graphics.disableScissor();
    }
}
