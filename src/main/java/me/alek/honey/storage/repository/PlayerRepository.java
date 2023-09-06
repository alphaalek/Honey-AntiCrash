package me.alek.honey.storage.repository;

import me.alek.honey.storage.QueryFactory;
import me.alek.honey.storage.RepositoryComponent;
import me.alek.honey.storage.exceptions.SQLFactoryException;
import me.alek.honey.storage.exceptions.SQLPlayerNotFoundException;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerRepository implements RepositoryComponent {

    @Override
    public void prepare(QueryFactory queryFactory) {
        queryFactory.runInsert(
                "CREATE TABLE IF NOT EXISTS players (" +
                "playerId INT AUTO_INCREMENT PRIMARY KEY, " +
                "playerUUID VARCHAR(36), " +
                "playerName VARCHAR(16)" +
                ");"
        );
    }

    public boolean existsPlayer(QueryFactory queryFactory, String playerName) {
        try {
            getPlayerId(queryFactory, playerName);
            return true;

        } catch (SQLPlayerNotFoundException ex) {
            return false;
        }
    }

    public boolean containsUUID(QueryFactory queryFactory, String UUID) throws SQLFactoryException {
        CompletableFuture<ResultSet> resulSetFuture = queryFactory.runQuery(
                "SELECT COUNT(*) FROM players AS p " +
                "WHERE p.playerUUID = ? " +
                "LIMIT 1",
                UUID
        );
        try {
            ResultSet resultSet = resulSetFuture.get();

            if (resultSet != null && resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            if (ex instanceof SQLFactoryException) throw new SQLFactoryException();
        }
        return false;
    }

    public int getPlayerId(QueryFactory queryFactory, String playerName) throws SQLPlayerNotFoundException, SQLFactoryException {
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
            if (ex instanceof SQLFactoryException) throw new SQLFactoryException();
        }
        throw new SQLPlayerNotFoundException();
    }

    public UUID getPlayerUUID(QueryFactory queryFactory, String playerName) throws SQLFactoryException, SQLPlayerNotFoundException {
        if (!existsPlayer(queryFactory, playerName)) throw new SQLPlayerNotFoundException();

        CompletableFuture<ResultSet> resultSetFuture = queryFactory.runQuery(
                "SELECT p.playerUUID FROM players AS p " +
                "WHERE p.playerName = ?",
                playerName
        );
        try {
            ResultSet resultSet = resultSetFuture.get();

            if (resultSet != null && resultSet.next()) {
                return UUID.fromString(resultSet.getString(1));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            if (ex instanceof SQLFactoryException) throw new SQLFactoryException();
        }
        throw new SQLFactoryException();
    }

    public void setupPlayer(QueryFactory queryFactory, Player player) throws SQLFactoryException {
        CompletableFuture<Boolean> resultSetFuture = queryFactory.runInsert(
                "INSERT INTO players (playerUUID, playerName) " +
                "VALUES (?, ?)",
                player.getUniqueId().toString(), player.getName()
        );
        try {
            boolean insertSuccessful = resultSetFuture.get();
            if (!insertSuccessful) throw new SQLFactoryException();

        } catch (Exception ex) {
            ex.printStackTrace();
            if (ex instanceof SQLFactoryException) throw new SQLFactoryException();
        }
    }

    public void updatePlayer(QueryFactory queryFactory, Player player) throws SQLFactoryException, SQLPlayerNotFoundException {
        final int id = getPlayerId(queryFactory, player.getName());

        CompletableFuture<ResultSet> resultSetFuture = queryFactory.runQuery(
                "UPDATE players AS p " +
                "SET p.playerName = ?, p.playerUUID = ? " +
                "WHERE p.playerId = ?",
                player.getName(), player.getUniqueId().toString(), id
        );
        try {
            resultSetFuture.get();

        } catch (Exception ex) {
            ex.printStackTrace();
            if (ex instanceof SQLFactoryException) throw new SQLFactoryException();
        }
    }

    public void preparePlayer(QueryFactory queryFactory, Player player) throws SQLFactoryException, SQLPlayerNotFoundException {
        final String uuid = player.getUniqueId().toString();
        boolean existsPlayer = existsPlayer(queryFactory, player.getName());

        if (existsPlayer && getPlayerUUID(queryFactory, player.getName()).toString().equals(uuid)) {
            // the player name exists in the database and the uuid registered to this player name is also the same as the player
            // everything is matching, so just return
            return;
        }
        if (!existsPlayer && !containsUUID(queryFactory, uuid)) {
            // the player name does not exist in the database and the uuid is not registered as well, so lets setup this player
            setupPlayer(queryFactory, player);
            return;
        }
        // the player name does not exist although the uuid is registered in the database (or the other way around),
        // so lets update the player record to have this player name and uuid instead of the old ones
        updatePlayer(queryFactory, player);
    }
}
