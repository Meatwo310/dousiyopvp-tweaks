package com.dousiyo.dpvptweaks.capture.core;

import com.dousiyo.dpvptweaks.config.ServerConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.OptionalInt;
import java.util.Set;

final class ScoreboardTickets {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> WARNED_KEYS = new HashSet<>();

    private ScoreboardTickets() {}

    static OptionalInt getTickets(MinecraftServer server, TeamSide side) {
        Objective objective = resolveObjective(server);
        if (objective == null) {
            return OptionalInt.empty();
        }

        String holder = holderName(side);
        if (holder.isBlank()) {
            warnOnce("blank-holder:" + side, "Ticket bleed skipped because {} ticket holder is blank", side);
            return OptionalInt.empty();
        }

        ServerScoreboard scoreboard = server.getScoreboard();
        if (!scoreboard.hasPlayerScore(holder, objective)) {
            warnOnce("missing-holder:" + objective.getName() + ":" + holder,
                    "Ticket bleed skipped because holder '{}' is not registered in objective '{}'",
                    holder, objective.getName());
            return OptionalInt.empty();
        }

        return OptionalInt.of(scoreboard.getOrCreatePlayerScore(holder, objective).getScore());
    }

    static OptionalInt subtractOne(MinecraftServer server, TeamSide side) {
        Objective objective = resolveObjective(server);
        if (objective == null) {
            return OptionalInt.empty();
        }

        String holder = holderName(side);
        if (holder.isBlank()) {
            warnOnce("blank-holder:" + side, "Ticket bleed skipped because {} ticket holder is blank", side);
            return OptionalInt.empty();
        }

        ServerScoreboard scoreboard = server.getScoreboard();
        if (!scoreboard.hasPlayerScore(holder, objective)) {
            warnOnce("missing-holder:" + objective.getName() + ":" + holder,
                    "Ticket bleed skipped because holder '{}' is not registered in objective '{}'",
                    holder, objective.getName());
            return OptionalInt.empty();
        }

        Score score = scoreboard.getOrCreatePlayerScore(holder, objective);
        int nextValue = Math.max(ServerConfig.MIN_TICKETS.get(), score.getScore() - 1);
        score.setScore(nextValue);
        return OptionalInt.of(nextValue);
    }

    private static Objective resolveObjective(MinecraftServer server) {
        String objectiveName = ServerConfig.TICKET_OBJECTIVE_NAME.get();
        if (objectiveName.isBlank()) {
            warnOnce("blank-objective", "Ticket bleed skipped because ticketObjectiveName is blank");
            return null;
        }

        Objective objective = server.getScoreboard().getObjective(objectiveName);
        if (objective == null) {
            warnOnce("missing-objective:" + objectiveName,
                    "Ticket bleed skipped because objective '{}' does not exist",
                    objectiveName);
        }
        return objective;
    }

    private static void warnOnce(String key, String message, Object... args) {
        if (WARNED_KEYS.add(key)) {
            LOGGER.warn(message, args);
        }
    }

    private static String holderName(TeamSide side) {
        return side == TeamSide.BLUE ? ServerConfig.BLUE_TICKET_HOLDER.get() : ServerConfig.RED_TICKET_HOLDER.get();
    }
}