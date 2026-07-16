package com.dousiyo.dpvptweaks.content;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ContentService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final Path ROOT = FMLPaths.GAMEDIR.get().resolve("dousiyo").resolve("content");
    private static final Path ANNOUNCEMENTS = ROOT.resolve("announcements");
    private static final Path RULES = ROOT.resolve("rules");
    private static final Path INDEX = ANNOUNCEMENTS.resolve("index.json");
    private static volatile List<Announcement> announcements = List.of();
    private static volatile List<Rule> rules = List.of();
    private static volatile int nextId = 1;
    private static volatile boolean loaded;
    private static volatile AnnouncementAudienceProvider rankAudienceProvider = (player, announcement) -> false;

    private ContentService() {}

    public static synchronized void reload() {
        try {
            Files.createDirectories(ANNOUNCEMENTS);
            Files.createDirectories(RULES);
            announcements = loadAnnouncements();
            rules = loadRules();
            int highest = announcements.stream().mapToInt(Announcement::id).max().orElse(0);
            nextId = Math.max(highest + 1, readNextId());
            writeIndex();
            loaded = true;
            DpvpTweaks.LOGGER.info("Loaded {} announcements and {} rules", announcements.size(), rules.size());
        } catch (Exception e) {
            DpvpTweaks.LOGGER.error("Failed to load announcements/rules", e);
        }
    }

    public static void setRankAudienceProvider(AnnouncementAudienceProvider provider) {
        rankAudienceProvider = provider == null ? (player, announcement) -> false : provider;
    }

    public static List<Announcement> visibleAnnouncements(ServerPlayer player) {
        ensureLoaded();
        long now = Instant.now().toEpochMilli();
        return announcements.stream().filter(a -> a.published && a.publishedAt <= now && (a.expiresAt <= 0 || now < a.expiresAt))
                .filter(a -> switch (a.audience) {
                    case ALL -> true;
                    case OP -> player.hasPermissions(a.minimumOpLevel);
                    case RANK -> rankAudienceProvider.matches(player, a);
                }).sorted(Comparator.comparingLong(Announcement::publishedAt).reversed()).toList();
    }

    public static List<Rule> visibleRules() {
        ensureLoaded();
        return rules.stream().filter(Rule::published).sorted(Comparator.comparing(Rule::modeId)).toList();
    }

    public static Announcement announcement(ServerPlayer player, int id) {
        return visibleAnnouncements(player).stream().filter(a -> a.id == id).findFirst().orElse(null);
    }

    public static Rule rule(String modeId) {
        return visibleRules().stream().filter(r -> r.modeId.equals(sanitizeKey(modeId))).findFirst().orElse(null);
    }

    public static synchronized int createAnnouncement(String title, Audience audience, int minimumOpLevel,
                                                      Importance importance, String author, String markdown) throws Exception {
        ensureLoaded();
        int id = nextId++;
        long now = Instant.now().toEpochMilli();
        Announcement value = new Announcement(id, cleanTitle(title), now, 0L, audience, minimumOpLevel,
                importance, author, author, now, now, true, markdownFile(ANNOUNCEMENTS, Integer.toString(id)));
        writeJson(ANNOUNCEMENTS.resolve(id + ".json"), value);
        writeMarkdown(ANNOUNCEMENTS.resolve(value.markdownFile), markdown);
        writeIndex();
        reload();
        return id;
    }

    public static synchronized boolean editAnnouncement(int id, String title, String editor, String markdown) throws Exception {
        ensureLoaded();
        Announcement old = announcements.stream().filter(a -> a.id == id).findFirst().orElse(null);
        if (old == null) return false;
        Announcement value = new Announcement(old.id, cleanTitle(title), old.publishedAt, old.expiresAt, old.audience,
                old.minimumOpLevel, old.importance, old.createdBy, editor, old.createdAt, Instant.now().toEpochMilli(),
                old.published, old.markdownFile);
        writeJson(ANNOUNCEMENTS.resolve(id + ".json"), value);
        writeMarkdown(ANNOUNCEMENTS.resolve(value.markdownFile), markdown);
        reload();
        return true;
    }

    public static synchronized boolean deleteAnnouncement(int id) throws Exception {
        ensureLoaded();
        Announcement old = announcements.stream().filter(a -> a.id == id).findFirst().orElse(null);
        if (old == null) return false;
        Files.deleteIfExists(ANNOUNCEMENTS.resolve(id + ".json"));
        Files.deleteIfExists(ANNOUNCEMENTS.resolve(old.markdownFile));
        reload();
        return true;
    }

    public static synchronized void setRule(String modeId, String title, String editor, String markdown) throws Exception {
        ensureLoaded();
        String key = sanitizeKey(modeId);
        Rule old = rules.stream().filter(r -> r.modeId.equals(key)).findFirst().orElse(null);
        long now = Instant.now().toEpochMilli();
        Rule value = new Rule(key, cleanTitle(title), true, old == null ? editor : old.createdBy,
                editor, old == null ? now : old.createdAt, now, markdownFile(RULES, key));
        writeJson(RULES.resolve(key + ".json"), value);
        writeMarkdown(RULES.resolve(value.markdownFile), markdown);
        reload();
    }

    public static synchronized boolean deleteRule(String modeId) throws Exception {
        ensureLoaded();
        Rule old = rules.stream().filter(r -> r.modeId.equals(sanitizeKey(modeId))).findFirst().orElse(null);
        if (old == null) return false;
        Files.deleteIfExists(RULES.resolve(old.modeId + ".json"));
        Files.deleteIfExists(RULES.resolve(old.markdownFile));
        reload();
        return true;
    }

    public static String body(Announcement announcement) { return readMarkdown(ANNOUNCEMENTS.resolve(announcement.markdownFile)); }
    public static String body(Rule rule) { return readMarkdown(RULES.resolve(rule.markdownFile)); }

    private static void ensureLoaded() { if (!loaded) reload(); }

    private static List<Announcement> loadAnnouncements() throws Exception {
        List<Announcement> result = new ArrayList<>();
        try (var files = Files.list(ANNOUNCEMENTS)) {
            for (Path path : files.filter(p -> p.getFileName().toString().matches("\\d+\\.json")).toList()) {
                try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    Announcement value = GSON.fromJson(reader, Announcement.class);
                    if (value != null && value.id > 0 && value.audience != null && value.importance != null
                            && value.title != null && safeMarkdownName(value.markdownFile)
                            && Files.isRegularFile(ANNOUNCEMENTS.resolve(value.markdownFile))) result.add(value);
                } catch (Exception e) { DpvpTweaks.LOGGER.warn("Ignoring invalid announcement metadata: {}", path, e); }
            }
        }
        return List.copyOf(result);
    }

    private static List<Rule> loadRules() throws Exception {
        List<Rule> result = new ArrayList<>();
        try (var files = Files.list(RULES)) {
            for (Path path : files.filter(p -> p.getFileName().toString().endsWith(".json")).toList()) {
                try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    Rule value = GSON.fromJson(reader, Rule.class);
                    if (value != null && value.modeId != null && value.title != null && value.modeId.equals(sanitizeKey(value.modeId))
                            && safeMarkdownName(value.markdownFile) && Files.isRegularFile(RULES.resolve(value.markdownFile))) result.add(value);
                } catch (Exception e) { DpvpTweaks.LOGGER.warn("Ignoring invalid rule metadata: {}", path, e); }
            }
        }
        return List.copyOf(result);
    }

    private static int readNextId() {
        if (!Files.isRegularFile(INDEX)) return 1;
        try (Reader reader = Files.newBufferedReader(INDEX, StandardCharsets.UTF_8)) {
            Index index = GSON.fromJson(reader, Index.class);
            return index == null ? 1 : Math.max(1, index.nextId);
        } catch (Exception ignored) { return 1; }
    }

    private static void writeIndex() throws Exception { writeJson(INDEX, new Index(nextId)); }

    private static void writeJson(Path path, Object value) throws Exception {
        Files.createDirectories(path.getParent());
        Path temporary = Files.createTempFile(path.getParent(), path.getFileName().toString(), ".tmp");
        Files.writeString(temporary, GSON.toJson(value), StandardCharsets.UTF_8);
        Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void writeMarkdown(Path path, String markdown) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, markdown == null ? "" : markdown, StandardCharsets.UTF_8);
    }

    private static String readMarkdown(Path path) {
        try {
            String text = Files.readString(path, StandardCharsets.UTF_8);
            return text.length() <= 32767 ? text : text.substring(0, 32767);
        } catch (Exception e) {
            DpvpTweaks.LOGGER.warn("Could not read markdown: {}", path, e);
            return "本文を読み込めませんでした。";
        }
    }

    private static String markdownFile(Path root, String key) { return key + ".md"; }
    private static String cleanTitle(String title) {
        String cleaned = title == null || title.isBlank() ? "無題" : title.strip();
        return cleaned.length() <= 200 ? cleaned : cleaned.substring(0, 200);
    }
    private static boolean safeMarkdownName(String value) { return value != null && value.matches("[a-zA-Z0-9_.-]+\\.md"); }
    private static String sanitizeKey(String value) {
        String key = value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
        if (key.isBlank()) throw new IllegalArgumentException("mode_id が不正です");
        return key;
    }

    public enum Audience { ALL, OP, RANK }
    public enum Importance { NORMAL, IMPORTANT, CRITICAL }

    public record Announcement(int id, String title, long publishedAt, long expiresAt, Audience audience,
                               int minimumOpLevel, Importance importance, String createdBy, String updatedBy,
                               long createdAt, long updatedAt, boolean published, String markdownFile) {}
    public record Rule(String modeId, String title, boolean published, String createdBy, String updatedBy,
                       long createdAt, long updatedAt, String markdownFile) {}
    private record Index(int nextId) {}
}
