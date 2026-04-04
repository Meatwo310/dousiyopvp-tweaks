package com.dousiyo.dpvptweaks.pvpstats.util;

import com.dousiyo.dpvptweaks.pvpstats.data.PvpStatsSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.DimensionDataStorage;

public final class SavedDataAccessor {
    private SavedDataAccessor() {
    }

    public static PvpStatsSavedData get(ServerLevel level) {
        DimensionDataStorage dataStorage = level.getServer().overworld().getDataStorage();
        return dataStorage.computeIfAbsent(PvpStatsSavedData::load, PvpStatsSavedData::new, PvpStatsSavedData.DATA_NAME);
    }
}
