package com.dousiyo.dpvptweaks.command;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class Report {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    public Long timestamp;
    public String reporterName;
    public UUID reporterUUID;
    public String reportedName;
    public UUID reportedUUID;
    public String reason;

    public Report() {
    }

    public Report(Long timestamp, String reporterName, UUID reporterUUID, String reportedName, UUID reportedUUID, String reason) {
        this.timestamp = timestamp;
        this.reporterName = reporterName;
        this.reporterUUID = reporterUUID;
        this.reportedName = reportedName;
        this.reportedUUID = reportedUUID;
        this.reason = reason;
    }

    public @NotNull String toStringWithoutTimestamp() {
        return String.format("""
                        §7通報者:§r §e%s§r §8(%s)§r
                        §7対象者:§r §c%s§r §8(%s)§r
                        §7理由:§r %s""",
                this.reporterName,
                this.reporterUUID,
                this.reportedName,
                this.reportedUUID,
                this.reason
        );
    }

    public @NotNull String toString() {
        var time = Instant.ofEpochSecond(this.timestamp)
                .atZone(ZoneId.systemDefault())
                .format(FORMATTER);
        return "[%s]\n%s".formatted(time, toStringWithoutTimestamp());
    }
}
