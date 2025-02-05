package me.alek.honey.sql.repository;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.sql.QueryFactory;
import me.alek.honey.sql.Repository;
import me.alek.honey.sql.exceptions.SQLFactoryException;
import me.alek.honey.sql.exceptions.SQLPlayerNotFoundException;
import me.alek.honey.sql.model.PlayerEntry;

import java.sql.ResultSet;
import java.util.concurrent.CompletableFuture;

public class PlayerRepositoryProvider implements Repository {

    @Override
    public void prepare(QueryFactory queryFactory) {
        queryFactory.runInsert(
                "CREATE TABLE IF NOT EXISTS honey_players (" +
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
                "SELECT COUNT(*) FROM honey_players AS p " +
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
            if (ex instanceof SQLFactoryException) throw (SQLFactoryException) ex;
        }
        return false;
    }

    public int getPlayerId(QueryFactory queryFactory, String playerName) throws SQLPlayerNotFoundException, SQLFactoryException {
        CompletableFuture<ResultSet> resultSetFuture = queryFactory.runQuery(
                "SELECT p.playerId FROM honey_players AS p " +
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
            if (ex instanceof SQLFactoryException) throw (SQLFactoryException) ex;
        }
        throw new SQLPlayerNotFoundException();
    }

    public PlayerEntry getPlayerData(QueryFactory queryFactory, String playerName) throws SQLFactoryException, SQLPlayerNotFoundException {
        if (!existsPlayer(queryFactory, playerName)) throw new SQLPlayerNotFoundException();

        CompletableFuture<ResultSet> resultSetFuture = queryFactory.runQuery(
                "SELECT p.playerName, p.playerUUID FROM honey_players AS p " +
                "WHERE p.playerName = ?",
                playerName
        );
        try {
            ResultSet resultSet = resultSetFuture.get();

            if (resultSet != null && resultSet.next()) {
                return new PlayerEntry(resultSet.getString(1), resultSet.getString(2));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            if (ex instanceof SQLFactoryException) throw (SQLFactoryException) ex;
        }
        throw new SQLFactoryException();
    }

    public void setupPlayer(QueryFactory queryFactory, HoneyPlayer player) throws SQLFactoryException {
        CompletableFuture<Boolean> resultSetFuture = queryFactory.runInsert(
                "INSERT INTO honey_players (playerUUID, playerName) " +
                "VALUES (?, ?)",
                player.uuid.toString(), player.name
        );
        try {
            boolean insertSuccessful = resultSetFuture.get();
            if (!insertSuccessful) throw new SQLFactoryException();

        } catch (Exception ex) {
            ex.printStackTrace();
            if (ex instanceof SQLFactoryException) throw (SQLFactoryException) ex;
        }
    }

    public void updatePlayer(QueryFactory queryFactory, HoneyPlayer player) throws SQLFactoryException, SQLPlayerNotFoundException {
        final int id = getPlayerId(queryFactory, player.name);

        CompletableFuture<Boolean> resultSetFuture = queryFactory.runInsert(
                "UPDATE honey_players AS p " +
                "SET p.playerName = ?, p.playerUUID = ? " +
                "WHERE p.playerId = ?",
                player.name, player.uuid.toString(), id
        );
        try {
            resultSetFuture.get();

        } catch (Exception ex) {
            ex.printStackTrace();
            if (ex instanceof SQLFactoryException) throw (SQLFactoryException) ex;
        }
    }

    public void updatePlayerName(QueryFactory queryFactory, HoneyPlayer player) throws SQLFactoryException {
        CompletableFuture<Boolean> resultSetFuture = queryFactory.runInsert(
                "UPDATE honey_players AS p " +
                "SET p.playerName = ? " +
                "WHERE p.playerUUID = ?",
                player.name, player.uuid.toString()
        );
        try {
            resultSetFuture.get();

        } catch (Exception ex) {
            ex.printStackTrace();
            if (ex instanceof SQLFactoryException) throw (SQLFactoryException) ex;
        }
    }

    public void removePlayerName(QueryFactory queryFactory, String playerName) {
        CompletableFuture<Boolean> resultSetFuture = queryFactory.runInsert(
                "UPDATE honey_players AS p " +
                "SET p.playerName = ?" +
                "WHERE p.playerName = ?",
                "", playerName
        );
        try {
            resultSetFuture.get();

        } catch (Exception ex) {
            ex.printStackTrace();
            if (ex instanceof SQLFactoryException) throw (SQLFactoryException) ex;
        }
    }

    public void preparePlayer(QueryFactory queryFactory, HoneyPlayer player) throws SQLFactoryException, SQLPlayerNotFoundException {
        final String uuid = player.uuid.toString();
        boolean existsPlayer = existsPlayer(queryFactory, player.name);

        // if the uuid is matching the player name, everything is correct and we can just return
        if (existsPlayer && getPlayerData(queryFactory, player.name).getPlayerUUID().equals(uuid)) {
            return;
        }

        // if the uuid is registered in the database but the player name is wrong, we will update the player name
        if (!existsPlayer && containsUUID(queryFactory, uuid)) {
            updatePlayerName(queryFactory, player);
            return;
        }
        if (!containsUUID(queryFactory, uuid)) {
            // if this player name is registered but the uuid is not, we will remove this player name
            if (existsPlayer) {
                removePlayerName(queryFactory, player.name);
            }
            // finally we will setup this player, because at this point the uuid and name will not be present anywhere
            setupPlayer(queryFactory, player);
        }
    }
}
