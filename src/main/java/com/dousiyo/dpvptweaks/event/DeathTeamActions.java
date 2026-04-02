package com.dousiyo.dpvptweaks.event;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.config.ServerConfig;
import com.dousiyo.dpvptweaks.network.LoadoutGuiNetwork;
import com.dousiyo.dpvptweaks.network.OpenMiniLoadoutGuiPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.Team;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Set;

@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DeathTeamActions {
    private static final Set<String> MINI_LOADOUT_TEAMS = Set.of(
            "tbg.mini.blue",
            "tbg.mini.red"
    );

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().isClientSide) {
            return;
        }

        String teamName = getTeamName(player);
        if (teamName == null) {
            return;
        }

        if (matchesConfiguredTeam(teamName, ServerConfig.CLEAR_INVENTORY_ON_DEATH_TEAMS.get())) {
            player.getInventory().clearContent();
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().isClientSide) {
            return;
        }

        String teamName = getTeamName(player);
        if (teamName == null) {
            return;
        }

        if (matchesConfiguredTeam(teamName, ServerConfig.SET_SPECTATOR_ON_DEATH_TEAMS.get())) {
            player.setGameMode(GameType.SPECTATOR);
        }
        if (MINI_LOADOUT_TEAMS.contains(teamName)) {
            LoadoutGuiNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenMiniLoadoutGuiPacket());
        }
    }

    private static String getTeamName(ServerPlayer player) {
        Team team = player.getTeam();
        return team == null ? null : team.getName();
    }

    private static boolean matchesConfiguredTeam(String playerTeamName, java.util.List<? extends String> configuredTeams) {
        return configuredTeams.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .anyMatch(playerTeamName::equals);
    }
}
