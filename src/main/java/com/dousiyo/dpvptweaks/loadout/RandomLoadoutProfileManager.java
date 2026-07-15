package com.dousiyo.dpvptweaks.loadout;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tacz.guns.api.item.IGun;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class RandomLoadoutProfileManager {
    public static final Pattern SAFE_ID = Pattern.compile("[a-z0-9][a-z0-9._-]{0,63}");
    private static final int SCHEMA = 1;
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private static final Path DIRECTORY = FMLPaths.GAMEDIR.get().resolve("dousiyo").resolve("loadout").resolve("random");

    private RandomLoadoutProfileManager() {}

    public enum Pool {
        MAIN("main"), SLOT2("slot2");

        private final String jsonName;

        Pool(String jsonName) {
            this.jsonName = jsonName;
        }

        public String jsonName() {
            return jsonName;
        }
    }

    public static SaveResult saveFromInventory(ServerPlayer player, String rawProfile, Pool pool) {
        if (player == null || pool == null) return SaveResult.error("プレイヤーまたはプールが不正です");
        String profile = normalizeId(rawProfile);
        if (profile == null) return SaveResult.error("プロファイルIDが不正です");

        List<ItemStack> guns = new ArrayList<>();
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && IGun.getIGunOrNull(stack) != null) guns.add(stack.copy());
        }
        if (guns.isEmpty()) return SaveResult.error("メインインベントリとホットバーにTaCZ銃がありません");

        LoadResult loaded = load(profile);
        if (Files.isRegularFile(path(profile)) && !loaded.valid())
            return SaveResult.error("既存プロファイルが不正なため上書きしません: " + loaded.error());
        Profile current = loaded.profile();
        List<ItemStack> main = pool == Pool.MAIN ? guns : current == null ? List.of() : current.main();
        List<ItemStack> slot2 = pool == Pool.SLOT2 ? guns : current == null ? List.of() : current.slot2();
        try {
            write(new Profile(profile, main, slot2));
            return SaveResult.ok(guns.size(), path(profile));
        } catch (Exception exception) {
            DpvpTweaks.LOGGER.error("[{}] Failed to save random loadout profile '{}'", DpvpTweaks.MOD_NAME, profile, exception);
            return SaveResult.error("ランダム武器プロファイルを保存できませんでした: " + exception.getMessage());
        }
    }

    public static LoadResult load(String rawProfile) {
        String profile = normalizeId(rawProfile);
        if (profile == null) return LoadResult.error("プロファイルIDが不正です");
        Path file = path(profile);
        if (!Files.isRegularFile(file)) return LoadResult.error("プロファイルが存在しません: " + profile);
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null || integer(root, "schema", -1) != SCHEMA)
                return LoadResult.error("未対応のschemaです: " + profile);
            if (!profile.equals(string(root, "id"))) return LoadResult.error("プロファイルIDがファイル名と一致しません: " + profile);
            List<ItemStack> main = readPool(root, "main", profile);
            List<ItemStack> slot2 = readPool(root, "slot2", profile);
            return LoadResult.ok(new Profile(profile, main, slot2));
        } catch (Exception exception) {
            DpvpTweaks.LOGGER.error("[{}] Failed to load random loadout profile from {}", DpvpTweaks.MOD_NAME, file, exception);
            return LoadResult.error("プロファイルを読み込めません: " + profile);
        }
    }

    public static String availabilityError(LoadoutSetDefinition.RandomDefinition definition) {
        if (definition == null) return "random定義がありません";
        LoadResult result = load(definition.profile());
        return availabilityError(definition, result);
    }

    private static String availabilityError(LoadoutSetDefinition.RandomDefinition definition, LoadResult result) {
        if (!result.valid()) return result.error();
        if (result.profile().main().isEmpty()) return "mainプールが空です: " + definition.profile();
        if (definition.weaponCount() == 3 && result.profile().slot2().isEmpty())
            return "slot2プールが空です: " + definition.profile();
        LoadoutSetDefinition.Entry template = new LoadoutSetDefinition.Entry(
                definition.template(), definition.template(), "", null, null);
        if (SavedLoadoutPreviewLoader.load(template) == null)
            return "テンプレート保存ロードアウトが存在しないか不正です: " + definition.template();
        return null;
    }

    public static DrawResult draw(LoadoutSetDefinition.RandomDefinition definition, RandomSource random) {
        if (definition == null) return DrawResult.error("random定義がありません");
        LoadResult loaded = load(definition.profile());
        String unavailable = availabilityError(definition, loaded);
        if (unavailable != null) return DrawResult.error(unavailable);
        return draw(loaded.profile(), definition.weaponCount(), random);
    }

    static DrawResult draw(Profile profile, int weaponCount, RandomSource random) {
        if (profile == null || random == null) return DrawResult.error("抽選条件が不正です");
        if (weaponCount != 2 && weaponCount != 3) return DrawResult.error("武器数が不正です");
        if (profile.main().isEmpty()) return DrawResult.error("mainプールが空です");
        if (weaponCount == 3 && profile.slot2().isEmpty()) return DrawResult.error("slot2プールが空です");
        List<ItemStack> result = new ArrayList<>(weaponCount);
        result.add(pick(profile.main(), random));
        result.add(pick(profile.main(), random));
        if (weaponCount == 3) result.add(pick(profile.slot2(), random));
        return DrawResult.ok(result);
    }

    static String serialize(ItemStack stack) {
        return stack.save(new CompoundTag()).toString();
    }

    static ItemStack deserialize(String snbt) throws Exception {
        ItemStack stack = ItemStack.of(TagParser.parseTag(snbt));
        if (stack.isEmpty() || IGun.getIGunOrNull(stack) == null) throw new IllegalArgumentException("保存候補がTaCZ銃ではありません");
        return stack;
    }

    private static ItemStack pick(List<ItemStack> pool, RandomSource random) {
        return pool.get(random.nextInt(pool.size())).copy();
    }

    private static List<ItemStack> readPool(JsonObject root, String name, String profile) throws Exception {
        JsonArray array = root.has(name) && root.get(name).isJsonArray() ? root.getAsJsonArray(name) : new JsonArray();
        List<ItemStack> result = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
                throw new IllegalArgumentException(name + "に文字列以外の候補があります: " + profile);
            result.add(deserialize(element.getAsString()));
        }
        return List.copyOf(result);
    }

    private static void write(Profile profile) throws Exception {
        Files.createDirectories(DIRECTORY);
        JsonObject root = new JsonObject();
        root.addProperty("schema", SCHEMA);
        root.addProperty("id", profile.id());
        root.add("main", writePool(profile.main()));
        root.add("slot2", writePool(profile.slot2()));

        Path target = path(profile.id());
        Path temporary = Files.createTempFile(DIRECTORY, profile.id() + "-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static JsonArray writePool(List<ItemStack> stacks) {
        JsonArray array = new JsonArray();
        for (ItemStack stack : stacks) array.add(serialize(stack));
        return array;
    }

    private static Path path(String profile) {
        return DIRECTORY.resolve(profile + ".json");
    }

    private static String normalizeId(String raw) {
        if (raw == null) return null;
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return SAFE_ID.matcher(value).matches() ? value : null;
    }

    private static String string(JsonObject object, String key) {
        try { return object.has(key) ? object.get(key).getAsString() : ""; }
        catch (RuntimeException ignored) { return ""; }
    }

    private static int integer(JsonObject object, String key, int fallback) {
        try { return object.has(key) ? object.get(key).getAsInt() : fallback; }
        catch (RuntimeException ignored) { return fallback; }
    }

    public record Profile(String id, List<ItemStack> main, List<ItemStack> slot2) {
        public Profile {
            main = copy(main);
            slot2 = copy(slot2);
        }

        private static List<ItemStack> copy(List<ItemStack> stacks) {
            if (stacks == null) return List.of();
            return stacks.stream().map(ItemStack::copy).toList();
        }
    }

    public record LoadResult(Profile profile, String error) {
        static LoadResult ok(Profile profile) { return new LoadResult(profile, null); }
        static LoadResult error(String error) { return new LoadResult(null, error); }
        public boolean valid() { return profile != null && error == null; }
    }

    public record SaveResult(int count, Path path, String error) {
        static SaveResult ok(int count, Path path) { return new SaveResult(count, path, null); }
        static SaveResult error(String error) { return new SaveResult(0, null, error); }
        public boolean valid() { return error == null; }
    }

    public record DrawResult(List<ItemStack> weapons, String error) {
        static DrawResult ok(List<ItemStack> weapons) { return new DrawResult(List.copyOf(weapons), null); }
        static DrawResult error(String error) { return new DrawResult(List.of(), error); }
        public boolean valid() { return error == null; }
    }
}
