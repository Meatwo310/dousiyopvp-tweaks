package com.dousiyo.dpvptweaks.pvpstats.model;

import java.util.Locale;

public enum PvpStatKey {
    WINS("wins"),
    LOSSES("losses"),
    DRAWS("draws"),
    KILLS("kills"),
    DEATHS("deaths");

    private final String id;

    PvpStatKey(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static PvpStatKey fromString(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        for (PvpStatKey key : values()) {
            if (key.id.equals(normalized)) {
                return key;
            }
        }
        return null;
    }
}
