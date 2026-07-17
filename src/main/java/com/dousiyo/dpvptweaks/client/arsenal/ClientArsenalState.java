package com.dousiyo.dpvptweaks.client.arsenal;

import com.dousiyo.dpvptweaks.network.arsenal.ArsenalStatePacket;

import java.util.List;

public final class ClientArsenalState {
    private static volatile boolean active;
    private static volatile boolean protectedState;
    private static volatile boolean participant;
    private static volatile boolean finished;
    private static volatile int stage;
    private static volatile int kills;
    private static volatile int deaths;
    private static volatile int countdownTicks;
    private static volatile String leaderName = "";
    private static volatile int leaderStage;
    private static volatile List<Integer> occupiedStages = List.of();

    private ClientArsenalState() {}

    public static void update(ArsenalStatePacket packet) {
        active = packet.active(); participant = packet.participant();
        protectedState = packet.protectedState(); finished = packet.finished();
        stage = packet.stage(); kills = packet.kills(); deaths = packet.deaths();
        countdownTicks = packet.countdownTicks(); leaderName = packet.leaderName(); leaderStage = packet.leaderStage();
        occupiedStages = packet.occupiedStages();
    }

    public static boolean blocksCombat() { return active && protectedState; }
    public static boolean finished() { return active && finished; }
    public static boolean protectedState() { return active && protectedState; }
    public static int stage() { return stage; }
    public static int kills() { return kills; }
    public static int deaths() { return deaths; }
    public static boolean active() { return active; }
    public static boolean participant() { return participant; }
    public static int countdownTicks() { return countdownTicks; }
    public static String leaderName() { return leaderName; }
    public static int leaderStage() { return leaderStage; }
    public static List<Integer> occupiedStages() { return occupiedStages; }
}
