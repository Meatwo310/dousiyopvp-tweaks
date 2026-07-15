package com.dousiyo.dpvptweaks.inteldraft;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** Pure bounded-cost sampler shared by production and GameTests. */
final class IntelDraftSampler {
    private static final int CARD_COUNT = 3;
    private IntelDraftSampler() {}

    static List<IntelDraftDefinition.ChoiceDefinition> sample(IntelDraftDefinition.Pool pool,
            Set<ResourceLocation> acquired, List<IntelDraftDefinition.ChoiceDefinition> previous) {
        List<IntelDraftDefinition.TechDefinition> techs = pool.techs().stream().filter(t -> !acquired.contains(t.id()))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        shuffle(techs);
        List<IntelDraftDefinition.GunDefinition> guns = sampled(pool.guns());
        List<IntelDraftDefinition.AttachmentDefinition> attachments = sampled(pool.attachments());
        List<IntelDraftDefinition.ChoiceDefinition> choices = compose(techs, guns, attachments);
        if (previous != null && previous.equals(choices)
                && (techs.size() > 1 || pool.guns().size() > 1 || pool.attachments().size() > 1)) {
            if (techs.size() > 1) Collections.rotate(techs, 1);
            else if (pool.guns().size() > 1) Collections.rotate(guns, 1);
            else Collections.rotate(attachments, 1);
            choices = compose(techs, guns, attachments);
        }
        return List.copyOf(choices);
    }

    private static List<IntelDraftDefinition.ChoiceDefinition> compose(
            List<IntelDraftDefinition.TechDefinition> techs, List<IntelDraftDefinition.GunDefinition> guns,
            List<IntelDraftDefinition.AttachmentDefinition> attachments) {
        List<IntelDraftDefinition.ChoiceDefinition> choices = new ArrayList<>(CARD_COUNT);
        for (int i = 0; i < CARD_COUNT; i++) choices.add(new IntelDraftDefinition.ChoiceDefinition(
                i < techs.size() ? techs.get(i) : supplyTech(), guns.get(i), attachments.get(i)));
        return choices;
    }

    private static <T> List<T> sampled(List<T> source) {
        if (source.isEmpty()) throw new IllegalArgumentException("Intel Draft pool cannot be empty");
        ArrayList<T> list = new ArrayList<>(source); shuffle(list);
        ArrayList<T> out = new ArrayList<>(CARD_COUNT);
        for (int i = 0; i < CARD_COUNT; i++) out.add(list.get(i % list.size()));
        return out;
    }
    private static void shuffle(List<?> list) { Collections.shuffle(list, ThreadLocalRandom.current()); }
    private static IntelDraftDefinition.TechDefinition supplyTech() {
        return new IntelDraftDefinition.TechDefinition(null, "装備補給", "銃とアタッチメント、弾薬を補給する",
                new ItemStack(net.minecraft.world.item.Items.CHEST), IntelDraftDefinition.EffectDefinition.NONE, null);
    }
}
