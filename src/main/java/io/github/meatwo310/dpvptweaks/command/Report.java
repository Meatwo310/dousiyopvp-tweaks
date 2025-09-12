package io.github.meatwo310.dpvptweaks.command;

import java.util.UUID;

public class Report {
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
}
