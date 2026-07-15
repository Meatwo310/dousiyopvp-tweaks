package com.dousiyo.dpvptweaks.loadout;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.inteldraft.IntelDraftDefinitionLoader;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(DpvpTweaks.MODID)
@PrefixGameTestTemplate(false)
public final class RandomLoadoutGameTests {
    private static final ResourceLocation TEST_GUN = ResourceLocation.fromNamespaceAndPath("tacz", "glock_17");

    private RandomLoadoutGameTests() {}

    @GameTest(template = "inteldraftgametests.empty")
    public static void gunNbtSurvivesSnbtRoundTrip(GameTestHelper helper) {
        ItemStack original = gun("main");
        CompoundTag dummyAmmo = new CompoundTag();
        dummyAmmo.putString("AmmoId", "test:dummy");
        dummyAmmo.putInt("Amount", 37);
        original.getOrCreateTag().put("RandomLoadoutDummyAmmoTest", dummyAmmo);
        try {
            ItemStack restored = RandomLoadoutProfileManager.deserialize(RandomLoadoutProfileManager.serialize(original));
            if (!ItemStack.matches(original, restored)) helper.fail("TaCZ gun ItemStack changed during SNBT round-trip");
            helper.succeed();
        } catch (Exception exception) {
            helper.fail("TaCZ gun ItemStack could not be restored: " + exception.getMessage());
        }
    }

    @GameTest(template = "inteldraftgametests.empty")
    public static void drawsMainTwiceAndSlot2ForNormal(GameTestHelper helper) {
        RandomLoadoutProfileManager.Profile profile = new RandomLoadoutProfileManager.Profile(
                "test", List.of(gun("main")), List.of(gun("slot2")));
        RandomLoadoutProfileManager.DrawResult result = RandomLoadoutProfileManager.draw(profile, 3, RandomSource.create(123L));
        if (!result.valid() || result.weapons().size() != 3) helper.fail("Normal random draw did not return three guns");
        if (!"main".equals(marker(result.weapons().get(0))) || !"main".equals(marker(result.weapons().get(1))))
            helper.fail("Slots 0 and 1 were not drawn from main");
        if (!"slot2".equals(marker(result.weapons().get(2)))) helper.fail("Slot 2 was not drawn from slot2");
        helper.succeed();
    }

    @GameTest(template = "inteldraftgametests.empty")
    public static void miniDrawsOnlyTwoMainGuns(GameTestHelper helper) {
        RandomLoadoutProfileManager.Profile profile = new RandomLoadoutProfileManager.Profile(
                "test", List.of(gun("main")), List.of());
        RandomLoadoutProfileManager.DrawResult result = RandomLoadoutProfileManager.draw(profile, 2, RandomSource.create(456L));
        if (!result.valid() || result.weapons().size() != 2) helper.fail("Mini random draw did not return two guns");
        if (!"main".equals(marker(result.weapons().get(0))) || !"main".equals(marker(result.weapons().get(1))))
            helper.fail("Mini slots were not both drawn from main");
        helper.succeed();
    }

    @GameTest(template = "inteldraftgametests.empty")
    public static void rejectsEmptyPoolsAndNonGunNbt(GameTestHelper helper) {
        RandomLoadoutProfileManager.Profile empty = new RandomLoadoutProfileManager.Profile("test", List.of(), List.of());
        if (RandomLoadoutProfileManager.draw(empty, 2, RandomSource.create()).valid())
            helper.fail("An empty main pool was accepted");
        try {
            RandomLoadoutProfileManager.deserialize(RandomLoadoutProfileManager.serialize(new ItemStack(Items.STONE)));
            helper.fail("A non-TaCZ ItemStack was accepted");
        } catch (Exception expected) {
            helper.succeed();
        }
    }

    private static ItemStack gun(String marker) {
        ItemStack stack = IntelDraftDefinitionLoader.loadedGunStack(TEST_GUN, 1);
        if (stack.isEmpty()) throw new IllegalStateException("Test TaCZ gun is unavailable: " + TEST_GUN);
        stack.getOrCreateTag().putString("RandomLoadoutPoolTest", marker);
        return stack;
    }

    private static String marker(ItemStack stack) {
        return stack.getOrCreateTag().getString("RandomLoadoutPoolTest");
    }
}
