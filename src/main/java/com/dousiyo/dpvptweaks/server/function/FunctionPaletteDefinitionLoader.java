package com.dousiyo.dpvptweaks.server.function;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.google.gson.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class FunctionPaletteDefinitionLoader {
    private static final Gson GSON = new Gson();
    private static final Pattern BUTTON_ID = Pattern.compile("[a-z0-9_.:-]{1,64}");
    private static final int MAX_FILES = 128;
    private static final int MAX_BUTTONS = 256;

    private FunctionPaletteDefinitionLoader() {}

    public static List<ServerButton> load() throws LoadException {
        Path directory = FMLPaths.GAMEDIR.get().resolve("dousiyo");
        try {
            Files.createDirectories(directory);
            List<Path> files;
            try (Stream<Path> stream = Files.list(directory)) {
                files = stream.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                        .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                        .toList();
            }
            require(files.size() <= MAX_FILES, "JSON file limit exceeded: " + files.size());

            List<ServerButton> buttons = new ArrayList<>();
            Set<String> ids = new HashSet<>();
            for (Path file : files) loadFile(file, buttons, ids);
            require(buttons.size() <= MAX_BUTTONS, "Button limit exceeded: " + buttons.size());
            buttons.sort(Comparator.comparingInt(ServerButton::order).thenComparing(ServerButton::id));
            return List.copyOf(buttons);
        } catch (LoadException e) {
            throw e;
        } catch (Exception e) {
            throw new LoadException("Could not read " + directory, e);
        }
    }

    private static void loadFile(Path file, List<ServerButton> output, Set<String> ids) throws Exception {
        JsonObject root;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            require(parsed.isJsonObject(), file + ": root must be an object");
            root = parsed.getAsJsonObject();
        }
        // Other features intentionally share dousiyo/*.json. Only claim palette-shaped files.
        if (!root.has("schema_version") && !root.has("buttons")) return;
        require(root.has("schema_version") && root.get("schema_version").isJsonPrimitive()
                && root.get("schema_version").getAsInt() == 1, file + ": schema_version must be 1");
        require(root.has("buttons") && root.get("buttons").isJsonArray(), file + ": buttons must be an array");
        JsonArray array = root.getAsJsonArray("buttons");
        require(array.size() <= MAX_BUTTONS, file + ": too many buttons");
        for (int i = 0; i < array.size(); i++) {
            require(array.get(i).isJsonObject(), file + ": buttons[" + i + "] must be an object");
            JsonObject value = array.get(i).getAsJsonObject();
            String at = file + ": buttons[" + i + "]";
            String id = requiredString(value, "id", 64, at);
            require(BUTTON_ID.matcher(id).matches(), at + ": invalid id");
            require(ids.add(id), at + ": duplicate id " + id);
            String name = requiredString(value, "name", 128, at);
            String description = optionalString(value, "description", "", 512, at);
            String functionText = requiredString(value, "function", 256, at);
            ResourceLocation function = ResourceLocation.tryParse(functionText);
            require(function != null && functionText.indexOf(':') > 0, at + ": invalid function id");
            String icon = optionalString(value, "icon", "minecraft:command_block", 256, at);
            ResourceLocation iconId = ResourceLocation.tryParse(icon);
            if (iconId == null || !BuiltInRegistries.ITEM.containsKey(iconId)) {
                DpvpTweaks.LOGGER.warn("[{}] Invalid icon '{}' in {}; using command block", DpvpTweaks.MOD_NAME, icon, at);
                icon = "minecraft:command_block";
            }
            int order = optionalInt(value, "order", 0, at);
            boolean confirmation = optionalBoolean(value, "confirmation", true, at);
            output.add(new ServerButton(id, name, description, function, icon, order, confirmation));
        }
    }

    private static String requiredString(JsonObject o, String key, int max, String at) throws LoadException {
        require(o.has(key) && o.get(key).isJsonPrimitive() && o.get(key).getAsJsonPrimitive().isString(), at + ": " + key + " is required");
        String value = o.get(key).getAsString();
        require(!value.isEmpty() && value.length() <= max, at + ": invalid " + key + " length");
        return value;
    }
    private static String optionalString(JsonObject o, String key, String fallback, int max, String at) throws LoadException {
        if (!o.has(key)) return fallback;
        require(o.get(key).isJsonPrimitive() && o.get(key).getAsJsonPrimitive().isString(), at + ": " + key + " must be a string");
        String value = o.get(key).getAsString(); require(value.length() <= max, at + ": " + key + " is too long"); return value;
    }
    private static int optionalInt(JsonObject o, String key, int fallback, String at) throws LoadException {
        if (!o.has(key)) return fallback;
        try { require(o.get(key).isJsonPrimitive() && o.get(key).getAsJsonPrimitive().isNumber(), at + ": " + key + " must be an integer"); return o.get(key).getAsInt(); }
        catch (NumberFormatException e) { throw new LoadException(at + ": invalid " + key, e); }
    }
    private static boolean optionalBoolean(JsonObject o, String key, boolean fallback, String at) throws LoadException {
        if (!o.has(key)) return fallback;
        require(o.get(key).isJsonPrimitive() && o.get(key).getAsJsonPrimitive().isBoolean(), at + ": " + key + " must be boolean"); return o.get(key).getAsBoolean();
    }
    private static void require(boolean condition, String message) throws LoadException { if (!condition) throw new LoadException(message); }

    public record ServerButton(String id, String name, String description, ResourceLocation functionId,
                               String icon, int order, boolean confirmation) {}
    public static final class LoadException extends Exception {
        public LoadException(String message) { super(message); }
        public LoadException(String message, Throwable cause) { super(message, cause); }
    }
}
