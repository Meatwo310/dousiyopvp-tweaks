package com.dousiyo.dpvptweaks.functionpalette;

/** Client-safe display data. The function id deliberately never leaves the server. */
public record FunctionPaletteAction(String id, String name, String description, String icon, boolean confirmation) {
}
