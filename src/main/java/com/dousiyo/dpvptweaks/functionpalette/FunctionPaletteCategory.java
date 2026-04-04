package com.dousiyo.dpvptweaks.functionpalette;

import java.util.List;

public record FunctionPaletteCategory(String id, String displayName, List<FunctionPaletteAction> actions) {
    public FunctionPaletteCategory {
        actions = List.copyOf(actions);
    }
}
