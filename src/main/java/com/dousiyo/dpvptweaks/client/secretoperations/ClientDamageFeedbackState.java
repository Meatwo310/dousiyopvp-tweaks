package com.dousiyo.dpvptweaks.client.secretoperations;

public final class ClientDamageFeedbackState {
    private static volatile boolean enabled;

    private ClientDamageFeedbackState() {}

    public static boolean enabled() {
        return enabled;
    }

    public static void update(boolean isEnabled) {
        enabled = isEnabled;
        if (!isEnabled) ClientDamageFeedback.clear();
    }
}
