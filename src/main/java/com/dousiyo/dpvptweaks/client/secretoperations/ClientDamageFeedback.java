package com.dousiyo.dpvptweaks.client.secretoperations;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

public final class ClientDamageFeedback {
    private static final long POP_MILLIS = 140L;
    private static final long PUSH_MILLIS = 100L;
    private static final long HOLD_MILLIS = 550L;
    private static final long LIFETIME_MILLIS = 900L;
    private static final int MAX_ENTRIES = 3;
    private static final float PUSH_DISTANCE = 7.0F;
    private static final float BASE_SCALE = 1.35F;
    private static final float POP_SCALE = 1.55F;
    private static final ResourceLocation DAMAGE_FONT =
            ResourceLocation.fromNamespaceAndPath("dpvptweaks", "damage_feedback");
    private static final int HEALTH_COLOR = 0xFFFFFFFF;
    private static final int SHIELD_COLOR = 0xFF55BFFF;
    private static final int HEADSHOT_COLOR = 0xFFFFD34E;
    private static final Deque<Entry> ENTRIES = new ArrayDeque<>();

    private ClientDamageFeedback() {}

    public static void add(int targetEntityId, float healthDamage, float shieldDamage, boolean headshot) {
        if (!isEnabled() || (healthDamage <= 0.0F && shieldDamage <= 0.0F)) return;
        long now = System.currentTimeMillis();
        ENTRIES.forEach(entry -> entry.pushUp(now));
        ENTRIES.addLast(new Entry(targetEntityId, healthDamage, shieldDamage, headshot, now));
        while (ENTRIES.size() > MAX_ENTRIES) ENTRIES.removeFirst();
    }

    public static void clear() {
        ENTRIES.clear();
    }

    public static void render(GuiGraphics graphics, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isEnabled() || minecraft.options.hideGui || minecraft.font == null) {
            if (!isEnabled()) clear();
            return;
        }

        long now = System.currentTimeMillis();
        ENTRIES.removeIf(entry -> now - entry.updatedAtMillis >= LIFETIME_MILLIS);
        if (ENTRIES.isEmpty()) return;

        Font font = minecraft.font;
        // Draw oldest first so the newest hit appears on top where the numbers overlap.
        for (Iterator<Entry> iterator = ENTRIES.iterator(); iterator.hasNext();) {
            Entry entry = iterator.next();
            long age = now - entry.updatedAtMillis;
            float alpha = age <= HOLD_MILLIS ? 1.0F
                    : 1.0F - (float)(age - HOLD_MILLIS) / (LIFETIME_MILLIS - HOLD_MILLIS);
            float popProgress = Math.min(1.0F, (float)age / POP_MILLIS);
            float scale = POP_SCALE + (BASE_SCALE - POP_SCALE) * easeOutCubic(popProgress);
            int x = screenWidth / 2 + 24;
            int y = Math.round(screenHeight / 2.0F - 28.0F - entry.pushOffset(now));
            drawEntry(graphics, font, entry, x, y, alpha, scale);
        }
    }

    private static void drawEntry(GuiGraphics graphics, Font font, Entry entry, int x, int y,
                                  float alpha, float scale) {
        String shield = format(entry.shieldDamage);
        String health = format(entry.healthDamage);
        int shieldColor = entry.headshot ? HEADSHOT_COLOR : SHIELD_COLOR;
        int healthColor = entry.headshot ? HEADSHOT_COLOR : HEALTH_COLOR;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        if (!shield.isEmpty() && !health.isEmpty()) {
            String separator = " + ";
            Component shieldText = damageText(shield);
            Component separatorText = damageText(separator);
            Component healthText = damageText(health);
            graphics.drawString(font, shieldText, 0, 0, withAlpha(shieldColor, alpha), true);
            int separatorX = font.width(shieldText);
            graphics.drawString(font, separatorText, separatorX, 0, withAlpha(0xFFBBBBBB, alpha), true);
            graphics.drawString(font, healthText, separatorX + font.width(separatorText), 0,
                    withAlpha(healthColor, alpha), true);
        } else if (!shield.isEmpty()) {
            graphics.drawString(font, damageText(shield), 0, 0,
                    withAlpha(shieldColor, alpha), true);
        } else if (!health.isEmpty()) {
            graphics.drawString(font, damageText(health), 0, 0,
                    withAlpha(healthColor, alpha), true);
        }
        graphics.pose().popPose();
    }

    private static Component damageText(String text) {
        return Component.literal(text).withStyle(style -> style.withFont(DAMAGE_FONT));
    }

    private static String format(float damage) {
        if (damage <= 0.0F) return "";
        return Float.toString(damage);
    }

    private static int withAlpha(int color, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(255.0F * alpha)));
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private static float easeOutCubic(float value) {
        float inverse = 1.0F - value;
        return 1.0F - inverse * inverse * inverse;
    }

    private static boolean isEnabled() {
        return ClientDamageFeedbackState.enabled() || ClientSecretOperationsState.active();
    }

    private static final class Entry {
        private final int targetEntityId;
        private final float healthDamage;
        private final float shieldDamage;
        private final boolean headshot;
        private final long updatedAtMillis;
        private float pushFrom;
        private float pushTarget;
        private long pushStartedAtMillis;

        private Entry(int targetEntityId, float healthDamage, float shieldDamage, boolean headshot, long updatedAtMillis) {
            this.targetEntityId = targetEntityId;
            this.healthDamage = healthDamage;
            this.shieldDamage = shieldDamage;
            this.headshot = headshot;
            this.updatedAtMillis = updatedAtMillis;
        }

        private void pushUp(long now) {
            pushFrom = pushOffset(now);
            pushTarget += PUSH_DISTANCE;
            pushStartedAtMillis = now;
        }

        private float pushOffset(long now) {
            if (pushFrom >= pushTarget) return pushTarget;
            float progress = Math.min(1.0F, (float)(now - pushStartedAtMillis) / PUSH_MILLIS);
            return pushFrom + (pushTarget - pushFrom) * easeOutCubic(progress);
        }
    }
}
