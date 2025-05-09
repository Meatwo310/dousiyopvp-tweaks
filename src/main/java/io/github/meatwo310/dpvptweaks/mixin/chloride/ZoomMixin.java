package io.github.meatwo310.dpvptweaks.mixin.chloride;

import me.srrapero720.chloride.impl.Zoom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = Zoom.class, remap = false)
public class ZoomMixin {
    /**
     * @author Meatwo310
     * @reason Blocks zoom feature
     */
    @Overwrite
    public static boolean canUseZoom() {
        return false;
    }
}
