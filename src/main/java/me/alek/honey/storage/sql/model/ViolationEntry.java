package me.alek.honey.storage.sql.model;

public class ViolationEntry {

    private final long unixTime;
    private final String playerName;
    private final String violationName;
    private final String settings;
    private final String quotients;
    private final String devData;

    public ViolationEntry(long unixTime, String playerName, String violationName, String settings, String quotients, String devData) {
        this.unixTime = unixTime;
        this.playerName = playerName;
        this.violationName = violationName;
        this.settings = settings;
        this.quotients = quotients;
        this.devData = devData;
    }

    public long getUnixTime() {
        return unixTime;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getViolationName() {
        return violationName;
    }

    public String getSettings() {
        return settings;
    }

    public String getQuotients() {
        return quotients;
    }

    public String getDevData() {
        return devData;
    }
}
