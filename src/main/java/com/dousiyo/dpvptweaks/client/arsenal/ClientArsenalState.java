package com.dousiyo.dpvptweaks.client.arsenal;

import com.dousiyo.dpvptweaks.network.ArsenalStatePacket;

public final class ClientArsenalState {
    private static volatile boolean active;
    private static volatile boolean protectedState;
    private static volatile boolean finished;
    private static volatile int stage;
    private static volatile int kills;
    private static volatile int deaths;

    private ClientArsenalState() {}

    public static void update(ArsenalStatePacket packet) {
        active = packet.active(); protectedState = packet.protectedState(); finished = packet.finished();
        stage = packet.stage(); kills = packet.kills(); deaths = packet.deaths();
    }

    public static boolean blocksCombat() { return active && protectedState; }
    public static boolean finished() { return active && finished; }
    public static boolean protectedState() { return active && protectedState; }
    public static int stage() { return stage; }
    public static int kills() { return kills; }
    public static int deaths() { return deaths; }
}
