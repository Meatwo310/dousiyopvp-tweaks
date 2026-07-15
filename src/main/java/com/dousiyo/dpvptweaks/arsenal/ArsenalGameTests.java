package com.dousiyo.dpvptweaks.arsenal;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.tacz.guns.api.item.gun.FireMode;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@GameTestHolder(DpvpTweaks.MODID)
@PrefixGameTestTemplate(false)
public final class ArsenalGameTests {
    private ArsenalGameTests() {}

    @GameTest(template = "inteldraftgametests.empty")
    public static void createsFullTaczGun(GameTestHelper helper) {
        ArsenalWeaponStage stage = new ArsenalWeaponStage(
                ResourceLocation.fromNamespaceAndPath("tacz", "glock_17"), FireMode.SEMI, Map.of(), 4);
        ArsenalWeaponFactory.Result result = ArsenalWeaponFactory.create(stage);
        if (!result.valid()) helper.fail("Arsenal weapon generation failed: " + result.error());
        if (result.capacity() <= 0) helper.fail("Arsenal weapon capacity was not positive");
        if (result.ammo().stream().mapToInt(stack -> stack.getCount()).sum() != result.capacity() * 4)
            helper.fail("Reserve ammo did not equal four final magazines");
        helper.succeed();
    }

    @GameTest(template = "inteldraftgametests.empty")
    public static void savedDataKeepsSnapshotAndProgress(GameTestHelper helper) {
        ArsenalWeaponStage stage = new ArsenalWeaponStage(
                ResourceLocation.fromNamespaceAndPath("tacz", "glock_17"), FireMode.SEMI, Map.of(), 4);
        ArsenalSavedData original = new ArsenalSavedData();
        original.state = ArsenalMatchState.RUNNING;
        original.matchId = UUID.randomUUID();
        original.weaponSetId = "test";
        original.snapshot = new ArsenalWeaponSet(1, "test", "Test",
                Collections.nCopies(ArsenalWeaponSet.STAGE_COUNT, stage));
        ArsenalPlayerData player = new ArsenalPlayerData(UUID.randomUUID(), "Tester");
        player.stage = 17; player.kills = 19; player.deaths = 3; player.protectionEndGameTime = 1234;
        original.players.put(player.playerId, player);

        ArsenalSavedData loaded = ArsenalSavedData.load(original.save(new CompoundTag()));
        ArsenalPlayerData loadedPlayer = loaded.players.get(player.playerId);
        if (loaded.state != ArsenalMatchState.RUNNING || loaded.snapshot == null || loaded.snapshot.stages().size() != 30)
            helper.fail("Match snapshot did not survive SavedData round-trip");
        if (loadedPlayer == null || loadedPlayer.stage != 17 || loadedPlayer.kills != 19
                || loadedPlayer.deaths != 3 || loadedPlayer.protectionEndGameTime != 1234)
            helper.fail("Player progress did not survive SavedData round-trip");
        helper.succeed();
    }
}
