package me.alek.honey.storage.sql;

import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.configuration.ConfigurationOptions;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.util.concurrent.CompletableFuture;

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

    public void preparePlayer(Player player) {
        if (checkInvalid()) return;

        repositoryCollection.playerRepository.preparePlayer(queryFactory, player);
    }

    private boolean checkInvalid() {
        return !enabled || queryFactory.isConnectionClosed();
    }

    public void postViolation(Player player, String violationName, ConfigurationOptions.VlSetting setting, CheckContextContainer.DevelopmentData developmentData) {
        if (checkInvalid()) return;

        repositoryCollection.flagRepository.postViolation(queryFactory, player, violationName, setting, developmentData);
    }

    public int getPlayerId(String playerName) {
        if (checkInvalid()) throw new UnsupportedOperationException();

        CompletableFuture<ResultSet> resultSetFuture = queryFactory.runQuery(
                "SELECT p.playerId FROM players AS p " +
                "WHERE p.playerName = ?",
                playerName
        );
        try {
            ResultSet resultSet = resultSetFuture.get();

            if (resultSet != null && resultSet.next()) {
                return resultSet.getInt(1);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return -1;
    }

    public int getPlayerId(Player player) {
        return getPlayerId(player.getName());
    }
}
