package me.alek.honey.storage;

import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.storage.exceptions.SQLFactoryException;
import me.alek.honey.storage.exceptions.SQLPlayerNotFoundException;
import me.alek.honey.storage.model.ViolationEntry;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public interface ComponentBridge {

    int getPlayerId(String playerName) throws SQLPlayerNotFoundException, SQLFactoryException;

    int getPlayerId(Player player) throws SQLPlayerNotFoundException, SQLFactoryException;

    UUID getPlayerUUID(String playerName) throws SQLPlayerNotFoundException, SQLFactoryException;;

    void preparePlayer(Player player) throws SQLPlayerNotFoundException, SQLFactoryException;

    void updatePlayer(Player player) throws SQLPlayerNotFoundException, SQLFactoryException;

    void setupPlayer(Player player) throws SQLFactoryException;

    boolean existsPlayer(String playerName);

    boolean containsUUID(String UUID) throws SQLFactoryException;

    void postViolation(Player player, String violationName, String context, CheckContextContainer.DevelopmentData developmentData) throws SQLPlayerNotFoundException, SQLFactoryException;

    List<ViolationEntry> getViolations(String playerName) throws SQLPlayerNotFoundException, SQLFactoryException;

    List<ViolationEntry> getViolationsOf(String playerName, String violationName) throws SQLPlayerNotFoundException, SQLFactoryException;

    List<ViolationEntry> getViolationsBefore(String playerName, int unixTime) throws SQLPlayerNotFoundException, SQLFactoryException;

    void reload();
}
