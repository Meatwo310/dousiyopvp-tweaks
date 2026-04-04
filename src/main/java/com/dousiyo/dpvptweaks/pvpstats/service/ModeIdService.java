package com.dousiyo.dpvptweaks.pvpstats.service;

import java.util.Locale;
import java.util.regex.Pattern;

public final class ModeIdService {
    private static final Pattern SIMPLE_MODE_ID = Pattern.compile("[a-z0-9_./-]+");

    private ModeIdService() {
    }

    public static String normalize(String rawModeId) {
        if (rawModeId == null) {
            return "";
        }
        return rawModeId.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isValid(String rawModeId) {
        String normalized = normalize(rawModeId);
        return !normalized.isBlank() && SIMPLE_MODE_ID.matcher(normalized).matches();
    }
}
