package com.dousiyo.dpvptweaks.secretoperations;

public enum SecretShowdownPhase {
    IDLE, PREPARING, ACTIVE, OVERTIME, ENDING;

    public boolean running() { return this != IDLE && this != ENDING; }
}
