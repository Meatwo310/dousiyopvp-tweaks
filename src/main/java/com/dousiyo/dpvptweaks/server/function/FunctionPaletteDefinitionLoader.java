package com.dousiyo.dpvptweaks.server.function;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.functionpalette.FunctionPaletteAction;
import com.dousiyo.dpvptweaks.functionpalette.FunctionPaletteCategory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class FunctionPaletteDefinitionLoader {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private FunctionPaletteDefinitionLoader() {
    }

    public static Definition load(List<String> allFunctionIds) {
        Path jsonPath = resolveJsonPath();
        if (!Files.exists(jsonPath)) {
            return new Definition(List.of());
        }

        Root root;
        try (Reader reader = Files.newBufferedReader(jsonPath, StandardCharsets.UTF_8)) {
            root = GSON.fromJson(reader, Root.class);
        } catch (IOException | JsonParseException e) {
            DpvpTweaks.LOGGER.error("[{}] Failed to read function palette definition from {}", DpvpTweaks.MOD_NAME, jsonPath, e);
            return new Definition(List.of());
        }

        if (root == null) {
            return new Definition(List.of());
        }

        Map<String, String> loadedByLowercase = allFunctionIds.stream()
                .collect(Collectors.toMap(
                        id -> id.toLowerCase(Locale.ROOT),
                        id -> id,
                        (left, right) -> left
                ));

        List<FunctionPaletteCategory> categories = new ArrayList<>();
        if (root.categories != null) {
            for (CategoryEntry entry : root.categories) {
                FunctionPaletteCategory category = toCategory(entry, loadedByLowercase);
                if (category != null && !category.actions().isEmpty()) {
                    categories.add(category);
                }
            }
        }

        categories.sort(Comparator.comparing(FunctionPaletteCategory::displayName, String.CASE_INSENSITIVE_ORDER));
        return new Definition(categories);
    }

    private static Path resolveJsonPath() {
        String configured = FunctionPaletteServerConfig.DEFINITION_JSON.get().trim();
        Path candidate = Path.of(configured);
        if (candidate.isAbsolute()) {
            return candidate;
        }
        return FMLPaths.GAMEDIR.get().resolve("dousiyo").resolve(candidate);
    }

    private static FunctionPaletteCategory toCategory(CategoryEntry entry, Map<String, String> loadedByLowercase) {
        if (entry == null || entry.id == null || entry.title == null) {
            return null;
        }

        String id = entry.id.trim();
        String title = entry.title.trim();
        if (id.isEmpty() || title.isEmpty()) {
            return null;
        }

        List<FunctionPaletteAction> actions = new ArrayList<>();
        if (entry.buttons != null) {
            for (ButtonEntry button : entry.buttons) {
                if (button == null || button.label == null || button.function == null) {
                    continue;
                }

                String loadedId = loadedByLowercase.get(button.function.trim().toLowerCase(Locale.ROOT));
                if (loadedId != null) {
                    actions.add(new FunctionPaletteAction(button.label.trim(), loadedId));
                }
            }
        }

        return new FunctionPaletteCategory(id, title, List.copyOf(actions));
    }

    public record Definition(List<FunctionPaletteCategory> categories) {
    }

    private static final class Root {
        List<CategoryEntry> categories = List.of();
    }

    private static final class CategoryEntry {
        String id;
        String title;
        List<ButtonEntry> buttons = List.of();
    }

    private static final class ButtonEntry {
        String label;
        String function;
    }
}
