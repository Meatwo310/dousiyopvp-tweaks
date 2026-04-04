package com.dousiyo.dpvptweaks.client.effect;

import com.dousiyo.dpvptweaks.effect.ModEffects;
import com.dousiyo.dpvptweaks.effect.OverlayImageEffect;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.List;

public final class DvdOverlay {
    private static final double BASE_VELOCITY_X_MIN = 90.0D;
    private static final double BASE_VELOCITY_X_MAX = 170.0D;
    private static final double BASE_VELOCITY_Y_MIN = 70.0D;
    private static final double BASE_VELOCITY_Y_MAX = 150.0D;
    private static final double MAX_DELTA_SECONDS = 0.25D;
    private static final RandomSource RANDOM = RandomSource.create();
    private static final List<MovingImageState> IMAGES = new ArrayList<>();

    private static long lastTimeNanos = -1L;

    private DvdOverlay() {
    }

    public static void render(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        long now = System.nanoTime();

        if (mc.options.hideGui) {
            if (!IMAGES.isEmpty()) {
                lastTimeNanos = now;
            }
            return;
        }

        if (mc.player == null || mc.level == null) {
            reset();
            return;
        }

        MobEffectInstance effectInstance = mc.player.getEffect(ModEffects.DVD_EFFECT.get());
        if (effectInstance == null) {
            reset();
            return;
        }

        syncImageCount(effectInstance.getAmplifier() + 1, screenWidth, screenHeight);
        updatePositions(now, screenWidth, screenHeight, effectInstance.getAmplifier());

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (MovingImageState image : IMAGES) {
            guiGraphics.blit(OverlayImageEffect.TEXTURE, (int) Math.round(image.x), (int) Math.round(image.y),
                    OverlayImageEffect.HUD_DRAW_WIDTH, OverlayImageEffect.HUD_DRAW_HEIGHT,
                    0, 0,
                    OverlayImageEffect.TEXTURE_WIDTH, OverlayImageEffect.TEXTURE_HEIGHT,
                    OverlayImageEffect.TEXTURE_WIDTH, OverlayImageEffect.TEXTURE_HEIGHT);
        }
        RenderSystem.disableBlend();
    }

    private static void syncImageCount(int targetCount, int screenWidth, int screenHeight) {
        int desired = Math.max(1, targetCount);
        while (IMAGES.size() < desired) {
            IMAGES.add(createRandomImage(screenWidth, screenHeight));
        }
        while (IMAGES.size() > desired) {
            IMAGES.remove(IMAGES.size() - 1);
        }
    }

    private static void updatePositions(long now, int screenWidth, int screenHeight, int amplifier) {
        if (lastTimeNanos < 0L) {
            lastTimeNanos = now;
            clampAll(screenWidth, screenHeight);
            return;
        }

        double dt = Math.min((now - lastTimeNanos) / 1_000_000_000.0D, MAX_DELTA_SECONDS);
        lastTimeNanos = now;
        double speedMultiplier = 1.0D + (amplifier * 0.25D);
        double maxX = maxX(screenWidth);
        double maxY = maxY(screenHeight);

        for (MovingImageState image : IMAGES) {
            image.x += image.baseVx * speedMultiplier * dt;
            image.y += image.baseVy * speedMultiplier * dt;

            if (image.x < 0.0D) {
                image.x = 0.0D;
                image.baseVx = Math.abs(image.baseVx);
            } else if (image.x > maxX) {
                image.x = maxX;
                image.baseVx = -Math.abs(image.baseVx);
            }

            if (image.y < 0.0D) {
                image.y = 0.0D;
                image.baseVy = Math.abs(image.baseVy);
            } else if (image.y > maxY) {
                image.y = maxY;
                image.baseVy = -Math.abs(image.baseVy);
            }
        }
    }

    private static void clampAll(int screenWidth, int screenHeight) {
        double maxX = maxX(screenWidth);
        double maxY = maxY(screenHeight);
        for (MovingImageState image : IMAGES) {
            image.x = clamp(image.x, 0.0D, maxX);
            image.y = clamp(image.y, 0.0D, maxY);
        }
    }

    private static MovingImageState createRandomImage(int screenWidth, int screenHeight) {
        double maxX = maxX(screenWidth);
        double maxY = maxY(screenHeight);
        double x = maxX <= 0.0D ? 0.0D : RANDOM.nextDouble() * maxX;
        double y = maxY <= 0.0D ? 0.0D : RANDOM.nextDouble() * maxY;
        double baseVx = randomSignedVelocity(BASE_VELOCITY_X_MIN, BASE_VELOCITY_X_MAX);
        double baseVy = randomSignedVelocity(BASE_VELOCITY_Y_MIN, BASE_VELOCITY_Y_MAX);
        return new MovingImageState(x, y, baseVx, baseVy);
    }

    private static double randomSignedVelocity(double min, double max) {
        double value = min + (RANDOM.nextDouble() * (max - min));
        return RANDOM.nextBoolean() ? value : -value;
    }

    private static double maxX(int screenWidth) {
        return Math.max(0.0D, screenWidth - OverlayImageEffect.HUD_DRAW_WIDTH);
    }

    private static double maxY(int screenHeight) {
        return Math.max(0.0D, screenHeight - OverlayImageEffect.HUD_DRAW_HEIGHT);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void reset() {
        IMAGES.clear();
        lastTimeNanos = -1L;
    }

    private static final class MovingImageState {
        private double x;
        private double y;
        private double baseVx;
        private double baseVy;

        private MovingImageState(double x, double y, double baseVx, double baseVy) {
            this.x = x;
            this.y = y;
            this.baseVx = baseVx;
            this.baseVy = baseVy;
        }
    }
}
