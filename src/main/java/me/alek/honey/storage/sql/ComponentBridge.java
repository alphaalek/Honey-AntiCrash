package me.alek.honey.storage.sql;

import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.configuration.ConfigurationOptions;
import org.bukkit.entity.Player;

public interface ComponentBridge {

    int getPlayerId(String playerName);

    int getPlayerId(Player player);

    void postViolation(Player player, String violationName, ConfigurationOptions.VlSetting setting, CheckContextContainer.DevelopmentData developmentData);

    void preparePlayer(Player player);

    void reload();
}
