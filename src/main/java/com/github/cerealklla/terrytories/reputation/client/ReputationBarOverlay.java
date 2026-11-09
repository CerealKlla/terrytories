package com.github.cerealklla.terrytories.reputation.client;

import java.util.List;
import java.util.Locale;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.neoforged.neoforge.client.gui.GuiLayer;

/**
 * Transient reputation bar HUD -- one bar per Faction currently showing a reputation change,
 * direct structural/visual port of Lyfe's own {@code xpbar.client.XpBarOverlay}. Anchored
 * bottom-LEFT (Lyfe's XP bars already occupy bottom-right) -- this mod's own placement call, not
 * specified by the user, chosen purely to avoid overlapping another mod's existing HUD element.
 */
public final class ReputationBarOverlay implements GuiLayer {

    private static final int BAR_WIDTH = 180;
    private static final int BAR_HEIGHT = 5;
    private static final int GAP = 3;
    // Placeholder/tunable, not derived from any vanilla constant -- just eyeballed clearance from
    // the corner, same margins Lyfe's XP bar uses.
    private static final int BOTTOM_MARGIN = 45;
    private static final int LEFT_MARGIN = 10;

    private static final int HOSTILE_RGB = 0xFF5050;
    private static final int NEUTRAL_RGB = 0xC0C0C0;
    private static final int FRIENDLY_RGB = 0x55FF55;
    private static final int GAIN_TEXT_RGB = 0x55FF55;
    private static final int LOSS_TEXT_RGB = 0xFF5050;
    private static final int BACKGROUND_RGB = 0x101010;
    private static final int BORDER_RGB = 0x000000;
    private static final int BACKGROUND_MAX_ALPHA = 160; // slightly translucent even at full visibility

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        List<ClientReputationBarState.RenderBar> bars = ClientReputationBarState.activeBars();
        if (bars.isEmpty()) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        int left = LEFT_MARGIN;
        int right = left + BAR_WIDTH;
        int blockHeight = font.lineHeight + 2 + BAR_HEIGHT + 2;
        int bottom = guiGraphics.guiHeight() - BOTTOM_MARGIN;

        for (int i = 0; i < bars.size(); i++) {
            ClientReputationBarState.RenderBar bar = bars.get(i);
            int blockBottom = bottom - i * (blockHeight + GAP);
            int textY = blockBottom - blockHeight;
            int barTop = textY + font.lineHeight + 2;
            int barBottom = barTop + BAR_HEIGHT;

            int alpha255 = Math.round(bar.alpha() * 255.0f) & 0xFF;
            if (alpha255 <= 0) {
                continue;
            }
            int accentRgb = switch (bar.standing()) {
                case HOSTILE -> HOSTILE_RGB;
                case NEUTRAL -> NEUTRAL_RGB;
                case FRIENDLY -> FRIENDLY_RGB;
            };
            int deltaRgb = bar.delta() >= 0 ? GAIN_TEXT_RGB : LOSS_TEXT_RGB;
            int textColor = (alpha255 << 24) | deltaRgb;
            int borderColor = (alpha255 << 24) | BORDER_RGB;
            int backgroundColor = (Math.min(alpha255, BACKGROUND_MAX_ALPHA) << 24) | BACKGROUND_RGB;
            int fillColor = (alpha255 << 24) | accentRgb;

            String sign = bar.delta() >= 0 ? "+" : "";
            String label = String.format(Locale.ROOT, "%s %s%d (%s)", bar.factionName(), sign, bar.delta(), bar.standing());
            guiGraphics.text(font, label, left, textY, textColor);

            guiGraphics.fill(left - 1, barTop - 1, right + 1, barBottom + 1, borderColor);
            guiGraphics.fill(left, barTop, right, barBottom, backgroundColor);
            int filledRight = left + (int) Math.round((right - left) * bar.fraction());
            if (filledRight > left) {
                guiGraphics.fill(left, barTop, filledRight, barBottom, fillColor);
            }
        }
    }
}
