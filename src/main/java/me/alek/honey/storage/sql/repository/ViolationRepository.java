package me.alek.honey.storage.sql.repository;

import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.storage.sql.ComponentBridge;
import me.alek.honey.storage.sql.QueryFactory;
import me.alek.honey.storage.sql.RepositoryComponent;
import me.alek.honey.storage.sql.exceptions.SQLFactoryException;
import me.alek.honey.storage.sql.exceptions.SQLPlayerNotFoundException;
import me.alek.honey.storage.sql.model.ViolationEntry;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ViolationRepository implements RepositoryComponent {

    @Override
    public void prepare(QueryFactory queryFactory) {
        queryFactory.runInsert(
                "CREATE TABLE IF NOT EXISTS violations (" +
                "playerId INT, " +
                "unixTime LONG, " +
                "violationName VARCHAR(16), " +
                "settings VARCHAR(255), " +
                "quotients VARCHAR(255), " +
                "devData VARCHAR(255), " +
                ");"
        );
    }

    public void postViolation(ComponentBridge componentBridge, QueryFactory queryFactory, Player player, String violationName, ConfigurationOptions.VlSetting setting, CheckContextContainer.DevelopmentData developmentData) throws SQLPlayerNotFoundException, SQLFactoryException {
        int playerId = componentBridge.getPlayerId(player.getName());

        CompletableFuture<Boolean> insertFuture = queryFactory.runInsert(
                "INSERT INTO violations (playerId, unixTime, violationName, settings, quotients, devData) " +
                "VALUES (?, ?, ?, ?, ?, ?)",
                playerId, System.currentTimeMillis(), violationName, setting.toString(), null, developmentData.toString()
        );
        try {
            boolean insertSuccessful = insertFuture.get();
            if (!insertSuccessful) throw new SQLFactoryException();

        } catch (Exception ex) {
            ex.printStackTrace();
            if (ex instanceof SQLFactoryException) throw new SQLFactoryException();
        }
    }

    public List<ViolationEntry> getViolations(ComponentBridge componentBridge, QueryFactory queryFactory, String playerName) throws SQLPlayerNotFoundException, SQLFactoryException {
        List<ViolationEntry> violations = new ArrayList<>();

        int playerId = componentBridge.getPlayerId(playerName);

        CompletableFuture<ResultSet> resultSetFuture = queryFactory.runQuery(
                "SELECT v.unixTime, v.violationName, v.settings, v.quotients, v.devData FROM violations AS v " +
                "WHERE v.playerId = ?",
                playerId
        );
        try {
            ResultSet resultSet = resultSetFuture.get();

            if (resultSet != null && resultSet.next()) {
                while (resultSet.next()) {
                    violations.add(new ViolationEntry(resultSet.getLong(1), playerName, resultSet.getString(2), resultSet.getString(3), resultSet.getString(4), resultSet.getString(5)));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            if (ex instanceof SQLFactoryException) throw new SQLFactoryException();
        }
        return violations;
    }

    public List<ViolationEntry> getViolationsOf(ComponentBridge componentBridge, QueryFactory queryFactory, String playerName, String violationName) throws SQLPlayerNotFoundException, SQLFactoryException {
        List<ViolationEntry> violations = getViolations(componentBridge, queryFactory, playerName);
        violations.removeIf(violation -> !violation.getViolationName().equals(violationName));

        return violations;
    }

    public List<ViolationEntry> getViolationsBefore(ComponentBridge componentBridge, QueryFactory queryFactory, String playerName, int unixTime) throws SQLPlayerNotFoundException, SQLFactoryException {
        List<ViolationEntry> violations = getViolations(componentBridge, queryFactory, playerName);
        violations.removeIf(violation -> violation.getUnixTime() > unixTime);

        return violations;
    }
}
