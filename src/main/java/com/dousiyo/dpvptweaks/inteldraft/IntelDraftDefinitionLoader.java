package com.dousiyo.dpvptweaks.inteldraft;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.google.gson.*;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Loads once and caches. Draft opens never touch disk. */
public final class IntelDraftDefinitionLoader {
    public static final String JSON_FILE_NAME = "intel_draft_gui.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static volatile IntelDraftDefinition.Pool cached = IntelDraftDefinition.Pool.empty();
    private static final Set<String> EFFECT_TYPES = Set.of("none", "double_jump", "sneaky", "part_fish",
            "soft_landing", "emergency_speed", "battle_ready", "shield_drip", "reinvigorated",
            "from_the_brink", "strong_resolve", "air_attack", "tracer_rounds", "informant",
            "weakness_analysis", "field_medic", "hasty_harvest", "overprepared", "resupply",
            "building_supplies", "building_tool_upgrade", "incendiary_ammo");

    private IntelDraftDefinitionLoader() {}

    public static IntelDraftDefinition.Pool get() {
        return cached;
    }

    /** Compatibility entry point for the old client-only packet constructor. */
    public static IntelDraftDefinition load() {
        reload();
        return IntelDraftDefinition.empty();
    }

    public static synchronized IntelDraftDefinition.Pool reload() {
        Path path = FMLPaths.GAMEDIR.get().resolve("dousiyo").resolve(JSON_FILE_NAME);
        ensureDefault(path);
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Root root = GSON.fromJson(reader, Root.class);
            root = hydrateMissingSections(root == null ? new Root() : root);
            IntelDraftDefinition.Pool parsed = parse(root);
            cached = parsed;
            DpvpTweaks.LOGGER.info("[{}] Loaded Intel Draft: {} techs, {} guns, {} attachments, {} ammo types",
                    DpvpTweaks.MOD_NAME, parsed.techs().size(), parsed.guns().size(),
                    parsed.attachments().size(), parsed.ammo().size());
        } catch (IOException | RuntimeException e) {
            DpvpTweaks.LOGGER.error("[{}] Keeping previous Intel Draft definition; reload failed: {}",
                    DpvpTweaks.MOD_NAME, path, e);
        }
        return cached;
    }

    private static Root hydrateMissingSections(Root root) {
        boolean hasEffects = root.techs != null && !root.techs.isEmpty()
                && root.techs.stream().anyMatch(t -> t != null && t.effect != null);
        String resource = "/assets/" + DpvpTweaks.MODID + "/defaults/" + JSON_FILE_NAME;
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(
                IntelDraftDefinitionLoader.class.getResourceAsStream(resource)), StandardCharsets.UTF_8)) {
            Root defaults = GSON.fromJson(reader, Root.class);
            if (!hasEffects) {
                root.techs = defaults.techs;
                DpvpTweaks.LOGGER.info("[{}] Migrating legacy Intel Draft tech list to effect definitions", DpvpTweaks.MOD_NAME);
            } else {
                Set<String> effectTypes = new HashSet<>();
                root.techs.stream().filter(Objects::nonNull).filter(t -> t.effect != null)
                        .forEach(t -> effectTypes.add(t.effect.type));
                List<TechEntry> merged = new ArrayList<>(root.techs);
                defaults.techs.stream().filter(t -> t != null && t.effect != null)
                        .filter(t -> ("building_supplies".equals(t.effect.type)
                                || "building_tool_upgrade".equals(t.effect.type)
                                || "incendiary_ammo".equals(t.effect.type))
                                && !effectTypes.contains(t.effect.type))
                        .forEach(merged::add);
                root.techs = merged;
            }
            if (root.guns == null || root.guns.isEmpty()) {
                root.guns = defaults.guns;
            } else if (root.guns.stream().anyMatch(e -> e != null && !blank(e.item))) {
                root.guns = mergeMissingNamedEntries(root.guns, defaults.guns);
                DpvpTweaks.LOGGER.info("[{}] Migrating legacy Intel Draft gun list to current defaults",
                        DpvpTweaks.MOD_NAME);
            }
            if (root.attachments == null || root.attachments.isEmpty()) root.attachments = defaults.attachments;
            if (root.ammo == null || root.ammo.isEmpty()) root.ammo = defaults.ammo;
        } catch (IOException | RuntimeException e) {
            DpvpTweaks.LOGGER.warn("[{}] Could not fill legacy Intel Draft sections", DpvpTweaks.MOD_NAME, e);
        }
        return root;
    }

    private static List<NamedEntry> mergeMissingNamedEntries(List<NamedEntry> configured,
                                                              List<NamedEntry> defaults) {
        List<NamedEntry> merged = new ArrayList<>(configured);
        Set<ResourceLocation> ids = new HashSet<>();
        configured.stream().map(IntelDraftDefinitionLoader::effectiveNamedEntryId)
                .filter(Objects::nonNull).forEach(ids::add);
        for (NamedEntry entry : defaults) {
            ResourceLocation id = effectiveNamedEntryId(entry);
            if (id != null && ids.add(id)) merged.add(entry);
        }
        return merged;
    }

    private static ResourceLocation effectiveNamedEntryId(NamedEntry entry) {
        if (entry == null) return null;
        return id(!blank(entry.item) ? entry.item : entry.id);
    }

    private static IntelDraftDefinition.Pool parse(Root root) {
        Set<ResourceLocation> seenTech = new HashSet<>();
        Set<String> seenEffects = new HashSet<>();
        List<IntelDraftDefinition.TechDefinition> techs = new ArrayList<>();
        if (root.techs != null) for (TechEntry e : root.techs) {
            ResourceLocation id = techId(e == null ? null : e.id);
            if (e == null || id == null || blank(e.name) || !seenTech.add(id)) {
                warn("tech", e == null ? null : e.id); continue;
            }
            ResourceLocation function = blank(e.onSelectFunction) ? null : id(e.onSelectFunction);
            Map<String, Double> values = e.effect == null || e.effect.values == null ? Map.of() : e.effect.values;
            String effectType = e.effect == null || blank(e.effect.type) ? "none" : e.effect.type.trim().toLowerCase(Locale.ROOT);
            if (!EFFECT_TYPES.contains(effectType) || values.values().stream().anyMatch(v -> v == null || !Double.isFinite(v))) {
                warn("tech effect", e.id); continue;
            }
            if (!effectType.equals("none") && !seenEffects.add(effectType)) { warn("duplicate effect", effectType); continue; }
            techs.add(new IntelDraftDefinition.TechDefinition(id, e.name.trim(), safe(e.description),
                    vanillaStack(e.iconItem), new IntelDraftDefinition.EffectDefinition(
                    effectType, values), function));
        }

        Set<ResourceLocation> seenGun = new HashSet<>();
        List<IntelDraftDefinition.GunDefinition> guns = new ArrayList<>();
        if (root.guns != null) for (NamedEntry e : root.guns) {
            ResourceLocation id = id(e == null ? null : (!blank(e.item) ? e.item : e.id));
            if (e == null || id == null || blank(e.name) || !seenGun.add(id)) { warn("gun", e == null ? null : e.id); continue; }
            ItemStack stack = loadedGunStack(id, clampCount(e.count));
            if (stack.isEmpty() && TimelessAPI.getCommonGunIndex(id).isEmpty()) {
                warn("unknown gun", id.toString());
                continue;
            }
            if (stack.isEmpty()) { warn("gun", e.id); continue; }
            guns.add(new IntelDraftDefinition.GunDefinition(id, e.name.trim(), stack));
        }

        Set<ResourceLocation> seenAttachment = new HashSet<>();
        List<IntelDraftDefinition.AttachmentDefinition> attachments = new ArrayList<>();
        if (root.attachments != null) for (NamedEntry e : root.attachments) {
            ResourceLocation id = id(e == null ? null : e.id);
            if (e == null || id == null || blank(e.name) || !seenAttachment.add(id)) { warn("attachment", e == null ? null : e.id); continue; }
            if (TimelessAPI.getCommonAttachmentIndex(id).isEmpty()) { warn("unknown attachment", id.toString()); continue; }
            ItemStack stack = AttachmentItemBuilder.create().setId(id).setCount(clampCount(e.count)).build();
            if (stack.isEmpty()) { warn("attachment", e.id); continue; }
            attachments.add(new IntelDraftDefinition.AttachmentDefinition(id, e.name.trim(), stack));
        }

        Set<ResourceLocation> seenAmmo = new HashSet<>();
        List<IntelDraftDefinition.AmmoDefinition> ammo = new ArrayList<>();
        if (root.ammo != null) for (AmmoEntry e : root.ammo) {
            ResourceLocation id = id(e == null ? null : e.id);
            if (e == null || id == null || !seenAmmo.add(id)) { warn("ammo", e == null ? null : e.id); continue; }
            if (TimelessAPI.getCommonAmmoIndex(id).isEmpty()) { warn("unknown ammo", id.toString()); continue; }
            // Builder call validates that the configured representation can be constructed.
            if (AmmoItemBuilder.create().setId(id).setCount(1).build().isEmpty()) { warn("ammo", e.id); continue; }
            ammo.add(new IntelDraftDefinition.AmmoDefinition(id, e.onSelect, e.onRespawn, e.onElimination));
        }
        return new IntelDraftDefinition.Pool(Math.max(5, root.sessionSeconds), Math.max(0, root.rerollCount),
                techs, guns, attachments, ammo);
    }

    public static ItemStack ammoStack(ResourceLocation id, int count) {
        return AmmoItemBuilder.create().setId(id).setCount(Math.min(64, Math.max(1, count))).build();
    }

    public static ItemStack attachmentStack(ResourceLocation id, int count) {
        return AttachmentItemBuilder.create().setId(id).setCount(Math.min(64, Math.max(1, count))).build();
    }

    /** Creates a TACZ gun with a full magazine, including a chambered round where applicable. */
    public static ItemStack loadedGunStack(ResourceLocation id, int count) {
        var gunIndex = TimelessAPI.getCommonGunIndex(id);
        if (gunIndex.isEmpty()) return ItemStack.EMPTY;
        var gunData = gunIndex.get().getGunData();
        Bolt bolt = gunData.getBolt();
        boolean chambered = bolt == Bolt.CLOSED_BOLT || bolt == Bolt.MANUAL_ACTION;
        return GunItemBuilder.create()
                .setId(id)
                .setCount(clampCount(count))
                .setAmmoCount(Math.max(0, gunData.getAmmoAmount()))
                .setAmmoInBarrel(chambered)
                .build();
    }

    /** Appends a held TACZ gun to the user-editable UTF-8 configuration. */
    public static synchronized boolean addGun(ItemStack stack) throws IOException {
        IGun gun = IGun.getIGunOrNull(stack);
        ResourceLocation gunId = gun == null ? null : gun.getGunId(stack);
        if (gunId == null || TimelessAPI.getCommonGunIndex(gunId).isEmpty()) {
            throw new IllegalArgumentException("The held item is not a registered TACZ gun");
        }

        Path path = FMLPaths.GAMEDIR.get().resolve("dousiyo").resolve(JSON_FILE_NAME);
        ensureDefault(path);
        JsonObject root;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()) throw new JsonParseException("Intel Draft root must be an object");
            root = parsed.getAsJsonObject();
        }

        JsonArray guns;
        JsonElement configuredGuns = root.get("guns");
        if (configuredGuns == null || configuredGuns.isJsonNull()) {
            guns = new JsonArray();
            root.add("guns", guns);
        } else if (configuredGuns.isJsonArray()) {
            guns = configuredGuns.getAsJsonArray();
        } else {
            throw new JsonParseException("Intel Draft guns must be an array");
        }

        boolean migratedLegacyEntries = false;
        Set<ResourceLocation> configuredIds = new HashSet<>();
        for (JsonElement element : guns) {
            if (!element.isJsonObject()) continue;
            JsonObject entry = element.getAsJsonObject();
            String legacyItem = jsonString(entry, "item");
            if (!blank(legacyItem)) {
                entry.addProperty("id", legacyItem.trim());
                entry.remove("item");
                migratedLegacyEntries = true;
            }
            ResourceLocation configuredId = id(jsonString(entry, "id"));
            if (configuredId != null) configuredIds.add(configuredId);
        }
        if (migratedLegacyEntries) {
            mergeDefaultGunJson(guns, configuredIds);
        }
        if (configuredIds.contains(gunId)) {
            if (migratedLegacyEntries) {
                writeJsonAtomically(path, root);
                reload();
            }
            return false;
        }

        String name = stack.getHoverName().getString().trim();
        JsonObject entry = new JsonObject();
        entry.addProperty("id", gunId.toString());
        entry.addProperty("name", name.isEmpty() ? gunId.getPath() : name);
        entry.addProperty("count", 1);
        guns.add(entry);

        writeJsonAtomically(path, root);
        reload();
        return true;
    }

    private static void mergeDefaultGunJson(JsonArray guns, Set<ResourceLocation> configuredIds) throws IOException {
        String resource = "/assets/" + DpvpTweaks.MODID + "/defaults/" + JSON_FILE_NAME;
        try (InputStream in = IntelDraftDefinitionLoader.class.getResourceAsStream(resource)) {
            if (in == null) throw new FileNotFoundException(resource);
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonObject defaults = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray defaultGuns = defaults.getAsJsonArray("guns");
                if (defaultGuns == null) return;
                for (JsonElement element : defaultGuns) {
                    if (!element.isJsonObject()) continue;
                    ResourceLocation id = id(jsonString(element.getAsJsonObject(), "id"));
                    if (id != null && configuredIds.add(id)) guns.add(element.getAsJsonObject().deepCopy());
                }
            }
        }
    }

    private static String jsonString(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                ? value.getAsString() : null;
    }

    private static void writeJsonAtomically(Path path, JsonObject root) throws IOException {
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                GSON.toJson(root, writer);
                writer.write(System.lineSeparator());
            }
            try {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            Files.deleteIfExists(temporary);
            throw e;
        }
    }

    private static ItemStack vanillaStack(String raw) {
        ResourceLocation id = id(raw);
        Item item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
        return new ItemStack(item == null ? Items.BARRIER : item);
    }

    private static ResourceLocation id(String raw) { return blank(raw) ? null : ResourceLocation.tryParse(raw.trim()); }
    private static ResourceLocation techId(String raw) {
        if (blank(raw)) return null;
        String value = raw.trim();
        return value.chars().allMatch(Character::isDigit)
                ? ResourceLocation.fromNamespaceAndPath(DpvpTweaks.MODID, "legacy_" + value) : ResourceLocation.tryParse(value);
    }
    private static boolean blank(String s) { return s == null || s.trim().isEmpty(); }
    private static String safe(String s) { return s == null ? "" : s; }
    private static int clampCount(int value) { return Math.max(1, Math.min(64, value <= 0 ? 1 : value)); }
    private static void warn(String kind, String id) { DpvpTweaks.LOGGER.warn("[{}] Ignoring invalid/duplicate Intel Draft {}: {}", DpvpTweaks.MOD_NAME, kind, id); }

    private static void ensureDefault(Path path) {
        if (Files.exists(path)) return;
        try {
            Files.createDirectories(path.getParent());
            try (InputStream in = IntelDraftDefinitionLoader.class.getResourceAsStream(
                    "/assets/" + DpvpTweaks.MODID + "/defaults/" + JSON_FILE_NAME)) {
                if (in != null) Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            DpvpTweaks.LOGGER.error("[{}] Could not create default Intel Draft JSON", DpvpTweaks.MOD_NAME, e);
        }
    }

    private static final class Root {
        int sessionSeconds = 30;
        int rerollCount = 3;
        List<TechEntry> techs = List.of();
        List<NamedEntry> guns = List.of();
        List<NamedEntry> attachments = List.of();
        List<AmmoEntry> ammo = List.of();
    }
    private static final class TechEntry { String id, name, description, iconItem, onSelectFunction; EffectEntry effect; }
    private static final class EffectEntry { String type = "none"; Map<String, Double> values = Map.of(); }
    private static class NamedEntry { String id, item, name; int count = 1; }
    private static final class AmmoEntry { String id; int onSelect, onRespawn, onElimination; }
}
