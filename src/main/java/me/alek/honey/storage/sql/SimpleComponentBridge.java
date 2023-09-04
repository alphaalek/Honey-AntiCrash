package me.alek.honey.storage.sql;

import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.storage.sql.exceptions.SQLFactoryException;
import me.alek.honey.storage.sql.exceptions.SQLPlayerNotFoundException;
import me.alek.honey.storage.sql.model.ViolationEntry;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class SimpleComponentBridge implements ComponentBridge {

    private final QueryFactory queryFactory;
    private final BeanRepositoryCollection repositoryCollection;

    private boolean enabled;

    public SimpleComponentBridge(BeanRepositoryCollection repositoryCollection, QueryFactory queryFactory) {
        this.queryFactory = queryFactory;
        this.repositoryCollection = repositoryCollection;
    }

    public void reload() {
        this.enabled = ConfigurationOptions.General.SQL.ENABLED;
    }

    public void preparePlayer(Player player) throws SQLPlayerNotFoundException, SQLFactoryException {
        if (checkInvalid()) return;

        repositoryCollection.playerRepository.preparePlayer(queryFactory, player);
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

    public void postViolation(Player player, String violationName, ConfigurationOptions.VlSetting setting, CheckContextContainer.DevelopmentData developmentData) throws SQLPlayerNotFoundException, SQLFactoryException {
        if (checkInvalid()) return;

        repositoryCollection.violationRepository.postViolation(this, queryFactory, player, violationName, setting, developmentData);
    }

    public int getPlayerId(String playerName) throws SQLPlayerNotFoundException, SQLFactoryException {
        if (checkInvalid()) throw new UnsupportedOperationException();

        return repositoryCollection.playerRepository.getPlayerId(queryFactory, playerName);
    }

    public int getPlayerId(Player player) throws SQLPlayerNotFoundException, SQLFactoryException {
        return getPlayerId(player.getName());
    }

    @Override
    public UUID getPlayerUUID(String playerName) throws SQLPlayerNotFoundException, SQLFactoryException {
        if (checkInvalid()) throw new UnsupportedOperationException();

        return repositoryCollection.playerRepository.getPlayerUUID(queryFactory, playerName);
    }
}
