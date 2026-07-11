package com.dousiyo.dpvptweaks.pvpstats.service;

import java.util.Locale;
import java.util.regex.Pattern;

public final class MatchIdService {
    private static final Pattern VALID_ID = Pattern.compile("[a-z0-9_./:-]+");

    private MatchIdService() {
    }

    public static String normalize(String rawId) {
        return rawId == null ? "" : rawId.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isValid(String rawId) {
        String normalized = normalize(rawId);
        return !normalized.isBlank() && normalized.length() <= 64 && VALID_ID.matcher(normalized).matches();
    }
}
