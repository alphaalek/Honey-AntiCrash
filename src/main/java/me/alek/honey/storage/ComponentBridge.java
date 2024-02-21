package me.alek.honey.storage;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.storage.exceptions.SQLFactoryException;
import me.alek.honey.storage.exceptions.SQLPlayerNotFoundException;
import me.alek.honey.storage.model.PlayerEntry;
import me.alek.honey.storage.model.ViolationEntry;
import org.bukkit.entity.Player;

import java.util.List;

public interface ComponentBridge {

    int getPlayerId(String playerName) throws SQLPlayerNotFoundException, SQLFactoryException;

    int getPlayerId(HoneyPlayer player) throws SQLPlayerNotFoundException, SQLFactoryException;

    PlayerEntry getPlayerData(String playerName) throws SQLPlayerNotFoundException, SQLFactoryException;;

    void preparePlayer(HoneyPlayer player) throws SQLPlayerNotFoundException, SQLFactoryException;

    void updatePlayer(HoneyPlayer player) throws SQLPlayerNotFoundException, SQLFactoryException;

    void setupPlayer(HoneyPlayer player) throws SQLFactoryException;

    void removePlayerName(String playerName) throws SQLFactoryException;

    boolean existsPlayer(String playerName);

    boolean containsUUID(String UUID) throws SQLFactoryException;

    void postViolation(HoneyPlayer player, String violationName, String context, CheckContextContainer.DevelopmentData developmentData) throws SQLPlayerNotFoundException, SQLFactoryException;

    List<ViolationEntry> getViolations(String playerName) throws SQLPlayerNotFoundException, SQLFactoryException;

    List<ViolationEntry> getViolationsOf(String playerName, String violationName) throws SQLPlayerNotFoundException, SQLFactoryException;

    List<ViolationEntry> getViolationsBefore(String playerName, int unixTime) throws SQLPlayerNotFoundException, SQLFactoryException;

    void reload();
}
