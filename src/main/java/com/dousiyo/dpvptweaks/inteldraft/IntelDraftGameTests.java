package com.dousiyo.dpvptweaks.inteldraft;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.GameTestHolder;

import java.util.HashSet;

@GameTestHolder(DpvpTweaks.MODID)
public final class IntelDraftGameTests {
    private IntelDraftGameTests() {}

    @GameTest(template = "empty")
    public static void definitionsAreUsable(GameTestHelper helper) {
        IntelDraftDefinition.Pool pool = IntelDraftDefinitionLoader.get();
        if (pool.techs().size() < 19) helper.fail("Expected at least 19 Intel Draft techs");
        if (pool.guns().size() < 3) helper.fail("Intel Draft needs at least three TACZ guns");
        if (pool.attachments().isEmpty()) helper.fail("Intel Draft needs at least one TACZ attachment");
        if (pool.ammo().isEmpty()) helper.fail("Intel Draft needs at least one TACZ ammo type");
        HashSet<String> effects = new HashSet<>();
        for (IntelDraftDefinition.TechDefinition tech : pool.techs()) {
            if (!tech.effect().type().equals("none") && !effects.add(tech.effect().type()))
                helper.fail("Duplicate Intel Draft effect type: " + tech.effect().type());
        }
        for (int attempt = 0; attempt < 100; attempt++) {
            var choices = IntelDraftSampler.sample(pool, java.util.Set.of(), null);
            if (choices.size() != 3) helper.fail("Intel Draft must always produce three cards");
            if (pool.techs().size() >= 3 && choices.stream().map(c -> c.tech().id()).distinct().count() != 3)
                helper.fail("Techs repeated in one draft");
            if (pool.guns().size() >= 3 && choices.stream().map(c -> c.gun().id()).distinct().count() != 3)
                helper.fail("Guns repeated in one draft");
            if (pool.attachments().size() >= 3 && choices.stream().map(c -> c.attachment().id()).distinct().count() != 3)
                helper.fail("Attachments repeated in one draft");
        }
        if (!effects.contains("building_supplies")) helper.fail("Building supplies tech is missing");
        if (!effects.contains("building_tool_upgrade")) helper.fail("Building tool upgrade tech is missing");
        if (!effects.contains("incendiary_ammo")) helper.fail("Incendiary ammo tech is missing");
        if (IntelDraftDefinitionLoader.attachmentStack(
                ResourceLocation.fromNamespaceAndPath("tacz", "ammo_mod_i"), 1).isEmpty())
            helper.fail("Incendiary ammo attachment could not be built");
        for (IntelDraftDefinition.GunDefinition gunDefinition : pool.guns()) {
            IGun gun = IGun.getIGunOrNull(gunDefinition.gunStack());
            if (gun == null) helper.fail("Intel Draft gun stack is not a TACZ gun: " + gunDefinition.id());
            int expected = TimelessAPI.getCommonGunIndex(gunDefinition.id()).orElseThrow().getGunData().getAmmoAmount();
            if (gun.getCurrentAmmoCount(gunDefinition.gunStack()) != expected)
                helper.fail("Intel Draft gun is not fully loaded: " + gunDefinition.id());
        }
        ResourceLocation acquired = pool.techs().get(0).id();
        if (IntelDraftSampler.sample(pool, java.util.Set.of(acquired), null).stream()
                .anyMatch(c -> acquired.equals(c.tech().id()))) helper.fail("Acquired tech appeared again");
        helper.succeed();
    }
}
