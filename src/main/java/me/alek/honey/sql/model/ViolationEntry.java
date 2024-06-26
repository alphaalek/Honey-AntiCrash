package me.alek.honey.sql.model;

public class ViolationEntry {

    private final long unixTime;
    private final String playerName;
    private final String violationName;
    private final String context;
    private final String devData;

    public ViolationEntry(long unixTime, String playerName, String violationName, String context,String devData) {
        this.unixTime = unixTime;
        this.playerName = playerName;
        this.violationName = violationName;
        this.context = context;
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

    public String getDevData() {
        return devData;
    }

    public String getContext() {
        return context;
    }
}
