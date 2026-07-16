package com.dousiyo.dpvptweaks.arsenal;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.gun.FireMode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public final class ArsenalWeaponSetManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().create();
    private static final Path DIRECTORY = FMLPaths.GAMEDIR.get().resolve("dousiyo").resolve("arsenal").resolve("weapons");
    private static final Pattern SAFE_ID = Pattern.compile("[a-z0-9_.-]{1,64}");
    private static volatile Map<String, ArsenalWeaponSet> validSets = Map.of();
    private static volatile Map<String, String> diagnostics = Map.of();

    private ArsenalWeaponSetManager() {}

    public static synchronized void reload() {
        Map<String, ArsenalWeaponSet> loaded = new LinkedHashMap<>();
        Map<String, String> errors = new LinkedHashMap<>();
        try {
            Files.createDirectories(DIRECTORY);
            try (var paths = Files.list(DIRECTORY)) {
                for (Path path : paths.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList()) {
                    String fileId = path.getFileName().toString().replaceFirst("\\.json$", "");
                    try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                        RawSet raw = GSON.fromJson(reader, RawSet.class);
                        ParseResult result = parse(raw, fileId);
                        if (result.valid()) loaded.put(result.set().id(), result.set());
                        else errors.put(fileId, result.error());
                    } catch (Exception exception) {
                        errors.put(fileId, "読込エラー: " + exception.getMessage());
                    }
                }
            }
        } catch (Exception exception) {
            DpvpTweaks.LOGGER.error("Failed to load Arsenal weapon sets from {}", DIRECTORY, exception);
            errors.put("*", "ディレクトリ読込エラー: " + exception.getMessage());
        }
        validSets = Map.copyOf(loaded);
        diagnostics = Map.copyOf(errors);
        DpvpTweaks.LOGGER.info("[{}] Loaded {} Arsenal weapon sets; {} invalid", DpvpTweaks.MOD_NAME, loaded.size(), errors.size());
        errors.forEach((id, error) -> DpvpTweaks.LOGGER.warn("Invalid Arsenal weapon set '{}': {}", id, error));
    }

    public static Optional<ArsenalWeaponSet> get(String id) {
        return Optional.ofNullable(validSets.get(normalizeId(id)));
    }

    public static List<String> list() {
        List<String> result = new ArrayList<>();
        validSets.keySet().stream().sorted().forEach(id -> result.add(id + " (valid)"));
        diagnostics.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> result.add(entry.getKey() + " (invalid: " + entry.getValue() + ")"));
        return result;
    }

    public static String validate(String id) {
        String normalized = normalizeId(id);
        if (validSets.containsKey(normalized)) return null;
        return diagnostics.getOrDefault(normalized, "武器セットが存在しません");
    }

    public static synchronized void setHeldWeapon(String requestedId, int stageNumber, int reserveMagazines,
                                                  ItemStack held) throws Exception {
        String id = normalizeId(requestedId);
        if (!SAFE_ID.matcher(id).matches()) throw new IllegalArgumentException("weapon_set IDが不正です");
        if (stageNumber < 1 || stageNumber > ArsenalWeaponSet.STAGE_COUNT) throw new IllegalArgumentException("段階は1～30です");
        RawSet raw = readOrEmpty(id);
        ensureThirtySlots(raw);
        raw.stages.set(stageNumber - 1, rawStage(held, reserveMagazines));
        writeAtomic(id, raw);
        reload();
    }

    public static synchronized void setHeldWeaponAll(String requestedId, int reserveMagazines, ItemStack held) throws Exception {
        String id = normalizeId(requestedId);
        if (!SAFE_ID.matcher(id).matches()) throw new IllegalArgumentException("weapon_set IDが不正です");
        RawSet raw = readOrEmpty(id);
        ensureThirtySlots(raw);
        RawStage stage = rawStage(held, reserveMagazines);
        for (int i = 0; i < ArsenalWeaponSet.STAGE_COUNT; i++) raw.stages.set(i, stage);
        writeAtomic(id, raw);
        reload();
    }

    public static synchronized void setInventoryWeapons(String requestedId, int reserveMagazines,
                                                        ServerPlayer player) throws Exception {
        String id = normalizeId(requestedId);
        if (!SAFE_ID.matcher(id).matches()) throw new IllegalArgumentException("weapon_set IDが不正です");
        List<ItemStack> weapons = new ArrayList<>(ArsenalWeaponSet.STAGE_COUNT);
        // Inventory GUI order: main inventory top-left to bottom-right, then hotbar left to right.
        for (int slot = 9; slot <= 35; slot++) addItemIfPresent(weapons, player.getInventory().getItem(slot));
        for (int slot = 0; slot <= 8; slot++) addItemIfPresent(weapons, player.getInventory().getItem(slot));
        if (weapons.size() != ArsenalWeaponSet.STAGE_COUNT)
            throw new IllegalArgumentException("空でないアイテムスタックが30個必要です（検出: " + weapons.size() + "個）");

        RawSet raw = readOrEmpty(id);
        ensureThirtySlots(raw);
        for (int i = 0; i < ArsenalWeaponSet.STAGE_COUNT; i++)
            raw.stages.set(i, rawStage(weapons.get(i), reserveMagazines));
        writeAtomic(id, raw);
        reload();
    }

    private static void addItemIfPresent(List<ItemStack> weapons, ItemStack stack) {
        if (!stack.isEmpty()) weapons.add(stack.copy());
    }

    private static RawStage rawStage(ItemStack held, int reserveMagazines) {
        if (reserveMagazines < 0 || reserveMagazines > 256) throw new IllegalArgumentException("予備マガジン数は0～256です");
        if (held == null || held.isEmpty()) throw new IllegalArgumentException("登録するアイテムが空です");
        IGun gun = IGun.getIGunOrNull(held);
        ResourceLocation gunId = gun == null ? null : gun.getGunId(held);
        RawStage stage = new RawStage();
        if (gunId == null) {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(held.getItem());
            if (itemId == null || held.getItem() == Items.AIR) throw new IllegalArgumentException("アイテムIDを取得できません");
            stage.type = "item";
            stage.item_id = itemId.toString();
            stage.count = held.getCount();
            if (held.hasTag()) {
                CompoundTag tag = held.getTag().copy();
                tag.remove("dpvptweaksArsenal");
                if (!tag.isEmpty()) stage.tag_snbt = tag.toString();
            }
            return stage;
        }
        if (TimelessAPI.getCommonGunIndex(gunId).isEmpty())
            throw new IllegalArgumentException("TaCZ銃IDが登録されていません: " + gunId);
        stage.type = "tacz_gun";
        stage.gun_id = gunId.toString();
        stage.loaded_ammo = "full";
        stage.reserve_magazines = reserveMagazines;
        stage.fire_mode = gun.getFireMode(held).name().toLowerCase(Locale.ROOT);
        stage.attachments = new LinkedHashMap<>();
        for (AttachmentType type : AttachmentType.values()) {
            if (type == AttachmentType.NONE) continue;
            ResourceLocation attachmentId = gun.getAttachmentId(held, type);
            if (attachmentId != null && TimelessAPI.getCommonAttachmentIndex(attachmentId).isPresent())
                stage.attachments.put(type.name().toLowerCase(Locale.ROOT), attachmentId.toString());
        }
        return stage;
    }

    private static RawSet readOrEmpty(String id) throws Exception {
        Path path = DIRECTORY.resolve(id + ".json");
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                RawSet raw = GSON.fromJson(reader, RawSet.class);
                if (raw != null) return raw;
            }
        }
        RawSet raw = new RawSet();
        raw.schema_version = 1;
        raw.id = id;
        raw.display_name = id;
        raw.stages = new ArrayList<>();
        return raw;
    }

    private static void ensureThirtySlots(RawSet raw) {
        if (raw.stages == null) raw.stages = new ArrayList<>();
        while (raw.stages.size() < ArsenalWeaponSet.STAGE_COUNT) raw.stages.add(null);
        while (raw.stages.size() > ArsenalWeaponSet.STAGE_COUNT) raw.stages.remove(raw.stages.size() - 1);
    }

    private static void writeAtomic(String id, RawSet raw) throws Exception {
        Files.createDirectories(DIRECTORY);
        raw.schema_version = 1;
        raw.id = id;
        if (raw.display_name == null || raw.display_name.isBlank()) raw.display_name = id;
        Path target = DIRECTORY.resolve(id + ".json");
        Path temporary = DIRECTORY.resolve(id + ".json.tmp");
        try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) { GSON.toJson(raw, writer); }
        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static ParseResult parse(RawSet raw, String fileId) {
        if (raw == null) return ParseResult.error("JSONルートが空です");
        if (raw.schema_version != 1) return ParseResult.error("schema_versionは1である必要があります");
        String id = normalizeId(raw.id == null || raw.id.isBlank() ? fileId : raw.id);
        if (!SAFE_ID.matcher(id).matches()) return ParseResult.error("idが不正です");
        if (raw.stages == null || raw.stages.size() != ArsenalWeaponSet.STAGE_COUNT)
            return ParseResult.error("stagesは30個必要です");
        List<ArsenalWeaponStage> stages = new ArrayList<>();
        for (int i = 0; i < raw.stages.size(); i++) {
            RawStage entry = raw.stages.get(i);
            if (entry == null) return ParseResult.error("第" + (i + 1) + "段階が未設定です");
            if ("item".equals(entry.type)) {
                ResourceLocation itemId = ResourceLocation.tryParse(entry.item_id == null ? "" : entry.item_id);
                if (itemId == null || !ForgeRegistries.ITEMS.containsKey(itemId))
                    return ParseResult.error("第" + (i + 1) + "段階のitem_idが不正です");
                Item item = ForgeRegistries.ITEMS.getValue(itemId);
                if (item == null || item == Items.AIR) return ParseResult.error("第" + (i + 1) + "段階のアイテムが存在しません");
                if (entry.count < 1 || entry.count > item.getMaxStackSize())
                    return ParseResult.error("第" + (i + 1) + "段階のcountが不正です");
                ItemStack itemStack = new ItemStack(item, entry.count);
                if (entry.tag_snbt != null && !entry.tag_snbt.isBlank()) {
                    try { itemStack.setTag(TagParser.parseTag(entry.tag_snbt)); }
                    catch (Exception exception) { return ParseResult.error("第" + (i + 1) + "段階のtag_snbtが不正です"); }
                }
                ArsenalWeaponStage stage = ArsenalWeaponStage.item(itemStack);
                ArsenalWeaponFactory.Result generated = ArsenalWeaponFactory.create(stage);
                if (!generated.valid()) return ParseResult.error("第" + (i + 1) + "段階: " + generated.error());
                stages.add(stage);
                continue;
            }
            if (!"tacz_gun".equals(entry.type)) return ParseResult.error("第" + (i + 1) + "段階のtypeが不正です");
            if (!"full".equals(entry.loaded_ammo)) return ParseResult.error("第" + (i + 1) + "段階のloaded_ammoはfullのみです");
            ResourceLocation gunId = ResourceLocation.tryParse(entry.gun_id == null ? "" : entry.gun_id);
            if (gunId == null) return ParseResult.error("第" + (i + 1) + "段階のgun_idが不正です");
            FireMode fireMode;
            try { fireMode = FireMode.valueOf(entry.fire_mode == null ? "" : entry.fire_mode.toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException exception) { return ParseResult.error("第" + (i + 1) + "段階のfire_modeが不正です"); }
            if (fireMode == FireMode.UNKNOWN) return ParseResult.error("第" + (i + 1) + "段階のfire_modeが不正です");
            if (entry.reserve_magazines < 0 || entry.reserve_magazines > 256)
                return ParseResult.error("第" + (i + 1) + "段階のreserve_magazinesが不正です");
            EnumMap<AttachmentType, ResourceLocation> attachments = new EnumMap<>(AttachmentType.class);
            if (entry.attachments != null) for (var attachment : entry.attachments.entrySet()) {
                AttachmentType type;
                try { type = AttachmentType.valueOf(attachment.getKey().toUpperCase(Locale.ROOT)); }
                catch (IllegalArgumentException exception) { return ParseResult.error("第" + (i + 1) + "段階のアタッチメント種別が不正です"); }
                if (type == AttachmentType.NONE) return ParseResult.error("NONEは指定できません");
                ResourceLocation attachmentId = ResourceLocation.tryParse(attachment.getValue());
                if (attachmentId == null) return ParseResult.error("第" + (i + 1) + "段階のアタッチメントIDが不正です");
                attachments.put(type, attachmentId);
            }
            ArsenalWeaponStage stage = new ArsenalWeaponStage(gunId, fireMode, attachments, entry.reserve_magazines);
            ArsenalWeaponFactory.Result generated = ArsenalWeaponFactory.create(stage);
            if (!generated.valid()) return ParseResult.error("第" + (i + 1) + "段階: " + generated.error());
            stages.add(stage);
        }
        return ParseResult.ok(new ArsenalWeaponSet(1, id, raw.display_name, stages));
    }

    private static String normalizeId(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }

    private record ParseResult(ArsenalWeaponSet set, String error) {
        static ParseResult ok(ArsenalWeaponSet set) { return new ParseResult(set, null); }
        static ParseResult error(String error) { return new ParseResult(null, error); }
        boolean valid() { return set != null && error == null; }
    }

    private static final class RawSet {
        int schema_version = 1; String id; String display_name; List<RawStage> stages;
    }
    private static final class RawStage {
        String type; String gun_id; String loaded_ammo; int reserve_magazines = 4; String fire_mode;
        String item_id; int count = 1; String tag_snbt;
        Map<String, String> attachments;
    }
}
