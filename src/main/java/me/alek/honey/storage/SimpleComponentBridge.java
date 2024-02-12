package me.alek.honey.storage;

import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.storage.exceptions.SQLFactoryException;
import me.alek.honey.storage.exceptions.SQLPlayerNotFoundException;
import me.alek.honey.storage.model.PlayerEntry;
import me.alek.honey.storage.model.ViolationEntry;
import org.bukkit.entity.Player;

import java.util.List;

public class SimpleComponentBridge implements ComponentBridge {

    private final QueryFactory queryFactory;
    private final BeanRepositoryCollection repositoryCollection;

    private boolean enabled;

    public SimpleComponentBridge(BeanRepositoryCollection repositoryCollection, QueryFactory queryFactory) {
        this.queryFactory = queryFactory;
        this.repositoryCollection = repositoryCollection;
    }

    public void reload() {
        this.enabled = ConfigurationOptions.SQL.ENABLED;
    }

    public void preparePlayer(Player player) throws SQLPlayerNotFoundException, SQLFactoryException {
        if (checkInvalid()) return;

        repositoryCollection.playerRepository.preparePlayer(queryFactory, player);
    }

    @Override
    public void updatePlayer(Player player) throws SQLPlayerNotFoundException, SQLFactoryException {
        repositoryCollection.playerRepository.updatePlayer(queryFactory, player);
    }

    @Override
    public void setupPlayer(Player player) throws SQLFactoryException {
        repositoryCollection.playerRepository.setupPlayer(queryFactory, player);
    }

    @Override
    public void removePlayerName(String playerName) throws SQLFactoryException {
        repositoryCollection.playerRepository.removePlayerName(queryFactory, playerName);
    }

    @Override
    public boolean existsPlayer(String playerName) {
        return repositoryCollection.playerRepository.existsPlayer(queryFactory, playerName);
    }

    @Override
    public boolean containsUUID(String UUID) throws SQLFactoryException {
        return repositoryCollection.playerRepository.containsUUID(queryFactory, UUID);
    }

    @Override
    public List<ViolationEntry> getViolations(String playerName) throws SQLPlayerNotFoundException, SQLFactoryException {
        return repositoryCollection.violationRepository.getViolations(this, queryFactory, playerName);
    }

    @Override
    public List<ViolationEntry> getViolationsOf(String playerName, String violationName) throws SQLPlayerNotFoundException, SQLFactoryException {
        return repositoryCollection.violationRepository.getViolationsOf(this, queryFactory, playerName, violationName);
    }

    @Override
    public List<ViolationEntry> getViolationsBefore(String playerName, int unixTime) throws SQLPlayerNotFoundException, SQLFactoryException {
        return repositoryCollection.violationRepository.getViolationsBefore(this, queryFactory, playerName, unixTime);
    }

    private boolean checkInvalid() {
        return !enabled || queryFactory.isConnectionClosed();
    }

    public void postViolation(Player player, String violationName, String context, CheckContextContainer.DevelopmentData developmentData) throws SQLPlayerNotFoundException, SQLFactoryException {
        if (checkInvalid()) return;

        repositoryCollection.violationRepository.postViolation(this, queryFactory, player, violationName, context, developmentData);
    }

    public int getPlayerId(String playerName) throws SQLPlayerNotFoundException, SQLFactoryException {
        if (checkInvalid()) throw new UnsupportedOperationException();

        return repositoryCollection.playerRepository.getPlayerId(queryFactory, playerName);
    }

    public int getPlayerId(Player player) throws SQLPlayerNotFoundException, SQLFactoryException {
        return getPlayerId(player.getName());
    }

    @Override
    public PlayerEntry getPlayerData(String playerName) throws SQLPlayerNotFoundException, SQLFactoryException {
        if (checkInvalid()) throw new UnsupportedOperationException();

        return repositoryCollection.playerRepository.getPlayerData(queryFactory, playerName);
    }
}
