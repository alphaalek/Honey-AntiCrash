package me.alek.honey.storage.repository;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.storage.ComponentBridge;
import me.alek.honey.storage.QueryFactory;
import me.alek.honey.storage.RepositoryComponent;
import me.alek.honey.storage.exceptions.SQLFactoryException;
import me.alek.honey.storage.exceptions.SQLPlayerNotFoundException;
import me.alek.honey.storage.model.ViolationEntry;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ViolationRepository implements RepositoryComponent {

    @Override
    public void prepare(QueryFactory queryFactory) {
        queryFactory.runInsert(
                "CREATE TABLE IF NOT EXISTS honey_violations (" +
                "playerId INT, " +
                "unixTime LONG, " +
                "violationName VARCHAR(32), " +
                "context VARCHAR(255), " +
                "devData VARCHAR(255)" +
                ");"
        );
    }

    public void postViolation(ComponentBridge componentBridge, QueryFactory queryFactory, HoneyPlayer player, String violationName, String context, CheckContextContainer.DevelopmentData developmentData) throws SQLPlayerNotFoundException, SQLFactoryException {
        int playerId = componentBridge.getPlayerId(player.name);

        CompletableFuture<Boolean> insertFuture = queryFactory.runInsert(
                "INSERT INTO honey_violations (playerId, unixTime, violationName, context, devData) " +
                "VALUES (?, ?, ?, ?, ?)",
                playerId, System.currentTimeMillis(), violationName, context, developmentData.toString()
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
                "SELECT v.unixTime, v.violationName, v.context, v.devData FROM honey_violations AS v " +
                "WHERE v.playerId = ?",
                playerId
        );
        try {
            ResultSet resultSet = resultSetFuture.get();

            if (resultSet != null && resultSet.next()) {
                do {
                    violations.add(new ViolationEntry(resultSet.getLong(1), playerName, resultSet.getString(2), resultSet.getString(3), resultSet.getString(4)));
                } while (resultSet.next());
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
