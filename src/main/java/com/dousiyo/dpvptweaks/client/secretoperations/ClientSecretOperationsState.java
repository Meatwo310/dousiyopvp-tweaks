package com.dousiyo.dpvptweaks.client.secretoperations;

public final class ClientSecretOperationsState {
    private static volatile boolean active;

    private ClientSecretOperationsState() {}

    public static boolean active() {
        return active;
    }

    public static void update(boolean isActive) {
        active = isActive;
        if (!isActive) ClientDamageFeedback.clear();
    }
}
