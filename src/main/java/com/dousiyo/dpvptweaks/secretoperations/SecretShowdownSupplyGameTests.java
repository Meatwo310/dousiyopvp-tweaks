package com.dousiyo.dpvptweaks.secretoperations;

import com.dousiyo.airstrike.entity.AirdropCrateEntity;
import com.dousiyo.dpvptweaks.DpvpTweaks;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(DpvpTweaks.MODID)
@PrefixGameTestTemplate(false)
public final class SecretShowdownSupplyGameTests {
    private SecretShowdownSupplyGameTests() {}

    @GameTest(template = "inteldraftgametests.empty")
    public static void openingThresholdAndWeightedBoundariesAreExact(GameTestHelper helper) {
        if (SecretShowdownSupplyManager.openingComplete(199, 200)) helper.fail("199 ticks must not unlock the crate");
        if (!SecretShowdownSupplyManager.openingComplete(200, 200)) helper.fail("200 ticks must unlock the crate");
        List<Integer> weights = List.of(2, 3, 1);
        int[] expected = {0, 0, 1, 1, 1, 2};
        for (int draw = 0; draw < expected.length; draw++) {
            int actual = SecretShowdownSupplyManager.weightedIndex(weights, draw);
            if (actual != expected[draw]) helper.fail("weighted draw " + draw + " selected " + actual);
        }
        helper.succeed();
    }

    @GameTest(template = "inteldraftgametests.empty")
    public static void markedAirstrikeCrateIsProtected(GameTestHelper helper) {
        AirdropCrateEntity crate = com.dousiyo.airstrike.registry.ModEntities.AIRDROP_CRATE.get().create(helper.getLevel());
        if (crate == null) { helper.fail("Air Strike crate entity could not be created"); return; }
        SecretShowdownSupplyManager.markSupply(crate);
        crate.setPos(helper.absolutePos(net.minecraft.core.BlockPos.ZERO).getCenter());
        helper.getLevel().addFreshEntity(crate);
        if (!SecretShowdownSupplyManager.isMarked(crate)) helper.fail("crate marker was not stored");
        if (crate.hurt(helper.getLevel().damageSources().generic(), 1.0F)) helper.fail("marked crate accepted damage");
        SecretShowdownSupplyManager.markUnlocked(crate);
        if (!crate.hurt(helper.getLevel().damageSources().generic(), 1.0F)) helper.fail("unlocked crate should use Air Strike damage behavior");
        crate.discard();
        helper.succeed();
    }
}
