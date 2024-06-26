package me.alek.honey.sql.model;

public class PlayerEntry {

    private final String playerName;
    private final String playerUUID;

    public PlayerEntry(String playerName, String playerUUID) {
        this.playerName = playerName;
        this.playerUUID = playerUUID;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getPlayerUUID() {
        return playerUUID;
    }
}
