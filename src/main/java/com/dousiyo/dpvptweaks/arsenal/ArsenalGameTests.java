package com.dousiyo.dpvptweaks.arsenal;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.tacz.guns.api.item.gun.FireMode;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Collections;
import java.util.ArrayList;
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
    public static void keepsGenericItemNbt(GameTestHelper helper) {
        ItemStack configured = new ItemStack(Items.DIAMOND_SWORD);
        configured.getOrCreateTag().putString("ArsenalTestProperty", "kept");
        ArsenalWeaponFactory.Result result = ArsenalWeaponFactory.create(ArsenalWeaponStage.item(configured));
        if (!result.valid() || result.gun().getItem() != Items.DIAMOND_SWORD)
            helper.fail("Generic Arsenal item was not generated");
        if (!"kept".equals(result.gun().getOrCreateTag().getString("ArsenalTestProperty")))
            helper.fail("Generic Arsenal item NBT was not preserved");
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
        original.previousSidebarObjective = "previous_test";
        original.countdownEndGameTime = 9876L;
        ArrayList<ArsenalWeaponStage> stages = new ArrayList<>(
                Collections.nCopies(ArsenalWeaponSet.STAGE_COUNT, stage));
        ItemStack generic = new ItemStack(Items.BOW);
        generic.getOrCreateTag().putInt("ArsenalTestValue", 42);
        stages.set(5, ArsenalWeaponStage.item(generic));
        original.snapshot = new ArsenalWeaponSet(1, "test", "Test", stages);
        ArsenalPlayerData player = new ArsenalPlayerData(UUID.randomUUID(), "Tester");
        player.stage = 17; player.kills = 19; player.deaths = 3; player.protectionEndGameTime = 1234;
        original.players.put(player.playerId, player);

        ArsenalSavedData loaded = ArsenalSavedData.load(original.save(new CompoundTag()));
        ArsenalPlayerData loadedPlayer = loaded.players.get(player.playerId);
        if (loaded.state != ArsenalMatchState.RUNNING || loaded.snapshot == null || loaded.snapshot.stages().size() != 30)
            helper.fail("Match snapshot did not survive SavedData round-trip");
        if (loaded.countdownEndGameTime != 9876L)
            helper.fail("Start countdown did not survive SavedData round-trip");
        if (!"previous_test".equals(loaded.previousSidebarObjective))
            helper.fail("Previous sidebar objective did not survive SavedData round-trip");
        ItemStack loadedGeneric = loaded.snapshot.stages().get(5).itemTemplate();
        if (loaded.snapshot.stages().get(5).type() != ArsenalWeaponStage.Type.ITEM
                || loadedGeneric.getItem() != Items.BOW
                || loadedGeneric.getOrCreateTag().getInt("ArsenalTestValue") != 42)
            helper.fail("Generic item snapshot did not survive SavedData round-trip");
        if (loadedPlayer == null || loadedPlayer.stage != 17 || loadedPlayer.kills != 19
                || loadedPlayer.deaths != 3 || loadedPlayer.protectionEndGameTime != 1234)
            helper.fail("Player progress did not survive SavedData round-trip");
        helper.succeed();
    }
}
