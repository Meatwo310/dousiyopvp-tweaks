package com.dousiyo.dpvptweaks.pvpstats.model;

public final class AggregateStats {
    private long wins;
    private long losses;
    private long draws;
    private long kills;
    private long deaths;
    private long matches;

    public AggregateStats() {
    }

    public AggregateStats(long wins, long losses, long kills, long deaths, long matches) {
        this(wins, losses, 0L, kills, deaths, matches);
    }

    public AggregateStats(long wins, long losses, long draws, long kills, long deaths, long matches) {
        this.wins = Math.max(0L, wins);
        this.losses = Math.max(0L, losses);
        this.draws = Math.max(0L, draws);
        this.kills = Math.max(0L, kills);
        this.deaths = Math.max(0L, deaths);
        this.matches = Math.max(0L, matches);
        normalizeMatches();
    }

    public long wins() {
        return wins;
    }

    public long losses() {
        return losses;
    }

    public long draws() {
        return draws;
    }

    public long kills() {
        return kills;
    }

    public long deaths() {
        return deaths;
    }

    public long matches() {
        return matches;
    }

    public void addBundle(long wins, long losses, long kills, long deaths) {
        this.wins += Math.max(0L, wins);
        this.losses += Math.max(0L, losses);
        this.kills += Math.max(0L, kills);
        this.deaths += Math.max(0L, deaths);
        normalizeMatches();
    }

    public void addWins(long amount) {
        wins += Math.max(0L, amount);
        normalizeMatches();
    }

    public void addLosses(long amount) {
        losses += Math.max(0L, amount);
        normalizeMatches();
    }

    public void addDraws(long amount) {
        draws += Math.max(0L, amount);
        normalizeMatches();
    }

    public void addKills(long amount) {
        kills += Math.max(0L, amount);
    }

    public void addDeaths(long amount) {
        deaths += Math.max(0L, amount);
    }

    public void addMatches(long amount) {
        matches += Math.max(0L, amount);
        normalizeMatches();
    }

    public void clear() {
        wins = 0L;
        losses = 0L;
        draws = 0L;
        kills = 0L;
        deaths = 0L;
        matches = 0L;
    }

    public boolean hasAnyValue() {
        return wins > 0L || losses > 0L || draws > 0L || kills > 0L || deaths > 0L || matches > 0L;
    }

    public AggregateStats copy() {
        return new AggregateStats(wins, losses, draws, kills, deaths, matches);
    }

    private void normalizeMatches() {
        matches = Math.max(matches, wins + losses + draws);
    }
}
