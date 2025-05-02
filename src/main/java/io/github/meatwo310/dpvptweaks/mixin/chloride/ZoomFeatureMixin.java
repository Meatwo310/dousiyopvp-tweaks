package io.github.meatwo310.dpvptweaks.mixin.chloride;

import me.srrapero720.chloride.features.ZoomFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = ZoomFeature.class, remap = false)
public class ZoomFeatureMixin {
    /**
     * @author Meatwo310
     * @reason Blocks zoom feature
     */
    @Overwrite
    public static boolean canUseZoom() {
        return false;
    }
}
