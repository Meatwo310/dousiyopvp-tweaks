package com.dousiyo.dpvptweaks.secretoperations;

public enum SecretConvoyPhase {
    IDLE, PREPARING, ACTIVE, OVERTIME, INTERMISSION, ENDING;
    public boolean running() { return this != IDLE && this != ENDING; }
}
