package com.dousiyo.dpvptweaks.arsenal;

import java.util.UUID;

public final class ArsenalPlayerData {
    public final UUID playerId;
    public String lastKnownName;
    public int stage;
    public int kills;
    public int deaths;
    public long protectionEndGameTime;

    public ArsenalPlayerData(UUID playerId, String lastKnownName) {
        this.playerId = playerId;
        this.lastKnownName = lastKnownName == null ? "" : lastKnownName;
    }

    public boolean protectedAt(long gameTime) { return protectionEndGameTime > gameTime; }
}
