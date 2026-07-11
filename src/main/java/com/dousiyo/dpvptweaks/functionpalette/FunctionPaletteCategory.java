package com.dousiyo.dpvptweaks.functionpalette;

import java.util.List;

/** Kept as the packet menu envelope for source compatibility with the existing feature. */
public record FunctionPaletteCategory(long revision, List<FunctionPaletteAction> actions) {
    public FunctionPaletteCategory {
        actions = List.copyOf(actions);
    }
}
