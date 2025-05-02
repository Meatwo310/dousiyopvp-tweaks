package io.github.meatwo310.dpvptweaks.mixin.teampin;

import com.github.tacowasa059.teampinmod.common.network.PinAddPacket;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.function.Supplier;

@Mixin(value = PinAddPacket.class, remap = false)
public abstract class PinAddPacketMixin {
    /**
     * @author Meatwo310
     * @reason No ping
     */
    @Overwrite
    public static void handle(PinAddPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.setPacketHandled(true);
    }
}
