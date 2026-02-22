package com.dousiyo.dpvptweaks.client.sync;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.network.ClientNetwork;
import com.dousiyo.dpvptweaks.network.SessionSyncSeedPacket;
import com.dousiyo.dpvptweaks.network.SessionSyncStatePacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.forgespi.language.IModInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientSessionSyncManager {
    private static final long HEARTBEAT_INTERVAL_MS = 60_000L;

    private static String activeSessionId = "";
    private static String activeNonce = "";
    private static long seedExpiresAtEpochMs = 0L;
    private static long nextHeartbeatAtEpochMs = 0L;

    private ClientSessionSyncManager() {
    }

    public static void onSessionSeed(SessionSyncSeedPacket seed) {
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }
        activeSessionId = seed.sessionId();
        activeNonce = seed.nonce();
        seedExpiresAtEpochMs = seed.expiresAtEpochMs();
        nextHeartbeatAtEpochMs = Instant.now().toEpochMilli() + HEARTBEAT_INTERVAL_MS;

        sendSyncState("SEED_ACK", seed.sessionId(), seed.nonce());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (activeSessionId.isEmpty() || activeNonce.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        long now = Instant.now().toEpochMilli();
        if (now >= seedExpiresAtEpochMs) {
            clearState();
            return;
        }
        if (now < nextHeartbeatAtEpochMs) {
            return;
        }

        sendSyncState("HEARTBEAT", activeSessionId, activeNonce);
        nextHeartbeatAtEpochMs = now + HEARTBEAT_INTERVAL_MS;
    }

    private static void sendSyncState(String stateType, String sessionId, String nonce) {
        SyncSnapshot snapshot = buildSnapshot();
        String digestSeed = String.join("|",
                stateType,
                sessionId,
                nonce,
                Long.toString(snapshot.clientTimestampEpochMs()),
                Boolean.toString(snapshot.checksPassed()),
                snapshot.modListHash(),
                snapshot.scriptStateHash(),
                snapshot.clientMetaHash()
        );
        String payloadDigest = sha256(digestSeed);

        ClientNetwork.CHANNEL.sendToServer(new SessionSyncStatePacket(
                stateType,
                sessionId,
                nonce,
                snapshot.clientTimestampEpochMs(),
                snapshot.checksPassed(),
                snapshot.modListHash(),
                snapshot.scriptStateHash(),
                snapshot.clientMetaHash(),
                payloadDigest
        ));
    }

    private static SyncSnapshot buildSnapshot() {
        long now = Instant.now().toEpochMilli();

        Set<String> currentMods = ModList.get().getMods().stream()
                .map(IModInfo::getModId)
                .collect(Collectors.toSet());

        Set<String> unauthorizedMods = currentMods.stream()
                .filter(modId -> !DpvpTweaks.ALLOWED_MODS.contains(modId))
                .collect(Collectors.toSet());

        ScriptScanResult scriptScanResult = scanClientScripts();
        boolean checksPassed = unauthorizedMods.isEmpty() && scriptScanResult.allowed();

        String modListHash = sha256(currentMods.stream().sorted().collect(Collectors.joining(",")));
        String scriptStateHash = sha256(scriptScanResult.hashInput());
        String clientMetaHash = sha256(buildClientMeta());

        return new SyncSnapshot(
                now,
                checksPassed,
                modListHash,
                scriptStateHash,
                clientMetaHash
        );
    }

    private static ScriptScanResult scanClientScripts() {
        Path scriptsDir = Path.of("kubejs", "client_scripts");
        if (!Files.isDirectory(scriptsDir)) {
            return new ScriptScanResult(true, "none");
        }

        try (Stream<Path> stream = Files.list(scriptsDir)) {
            List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .sorted()
                    .toList();

            boolean onlyExample = files.size() == 1 && files.get(0).getFileName().toString().equals("example.js");
            boolean allowed = files.isEmpty() || onlyExample;

            String input = files.stream()
                    .map(path -> path.getFileName() + ":" + safeFileSize(path))
                    .collect(Collectors.joining(","));

            return new ScriptScanResult(allowed, input.isEmpty() ? "empty" : input);
        } catch (IOException e) {
            return new ScriptScanResult(false, "io_error");
        }
    }

    private static long safeFileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return -1L;
        }
    }

    private static String buildClientMeta() {
        Minecraft mc = Minecraft.getInstance();
        String username = mc.getUser() != null ? mc.getUser().getName() : "unknown";
        return String.join("|",
                "mod=" + DpvpTweaks.MODID,
                "channel=" + ClientNetwork.PROTOCOL_VERSION,
                "user=" + username,
                "java=" + System.getProperty("java.version", "unknown"),
                "os=" + System.getProperty("os.name", "unknown"),
                "arch=" + System.getProperty("os.arch", "unknown")
        );
    }

    private static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            DpvpTweaks.LOGGER.error("[{}] SHA-256 is not available", DpvpTweaks.MOD_NAME, e);
            return "sha256_unavailable";
        }
    }

    private static void clearState() {
        activeSessionId = "";
        activeNonce = "";
        seedExpiresAtEpochMs = 0L;
        nextHeartbeatAtEpochMs = 0L;
    }

    private record SyncSnapshot(
            long clientTimestampEpochMs,
            boolean checksPassed,
            String modListHash,
            String scriptStateHash,
            String clientMetaHash
    ) {
    }

    private record ScriptScanResult(boolean allowed, String hashInput) {
    }
}
