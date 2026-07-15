package com.dousiyo.dpvptweaks.event;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.config.ServerConfig;
import com.dousiyo.dpvptweaks.loadout.LoadoutDataManager;
import com.dousiyo.dpvptweaks.loadout.LoadoutSessionManager;
import com.dousiyo.dpvptweaks.arsenal.ArsenalMatchManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.Team;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DeathTeamActions {
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().isClientSide) {
            return;
        }
        if (ArsenalMatchManager.isParticipant(player)) return;

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
        if (ArsenalMatchManager.isParticipant(player)) return;

        String teamName = getTeamName(player);
        if (teamName == null) {
            return;
        }

        if (matchesConfiguredTeam(teamName, ServerConfig.SET_SPECTATOR_ON_DEATH_TEAMS.get())) {
            player.setGameMode(GameType.SPECTATOR);
        }
        int separator = teamName.indexOf('.');
        if (separator > 0) {
            var setId = LoadoutDataManager.findSetIdByPath(teamName.substring(0, separator));
            if (setId != null) {
                LoadoutSessionManager.open(player, setId);
                return;
            }
        }
        if (matchesConfiguredTeam(teamName, ServerConfig.OPEN_MINI_LOADOUT_ON_RESPAWN_TEAMS.get())) {
            LoadoutSessionManager.open(player, LoadoutDataManager.DEFAULT_MINI_LOADOUT_SET, true);
            return;
        }
        if (matchesConfiguredTeam(teamName, ServerConfig.OPEN_LOADOUT_ON_RESPAWN_TEAMS.get())) {
            LoadoutSessionManager.open(player, LoadoutDataManager.DEFAULT_LOADOUT_SET);
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
