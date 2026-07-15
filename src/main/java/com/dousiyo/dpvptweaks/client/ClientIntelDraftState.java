package com.dousiyo.dpvptweaks.client;

import java.util.Set;

public final class ClientIntelDraftState {
    private static volatile boolean active;
    private static volatile Set<String> effects = Set.of();
    private ClientIntelDraftState() {}
    public static boolean active() { return active; }
    public static boolean has(String effect) { return active && effects.contains(effect); }
    public static void update(boolean isActive, Set<String> effectTypes) {
        active = isActive; effects = isActive ? Set.copyOf(effectTypes) : Set.of();
    }
}
