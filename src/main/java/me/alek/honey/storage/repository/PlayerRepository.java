package me.alek.honey.storage.repository;

import me.alek.honey.storage.QueryFactory;
import me.alek.honey.storage.RepositoryComponent;
import me.alek.honey.storage.exceptions.SQLFactoryException;
import me.alek.honey.storage.exceptions.SQLPlayerNotFoundException;
import me.alek.honey.storage.model.PlayerEntry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.util.concurrent.CompletableFuture;

public class PlayerRepository implements RepositoryComponent {

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
            if (ex instanceof SQLFactoryException) throw new SQLFactoryException();
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
            if (ex instanceof SQLFactoryException) throw new SQLFactoryException();
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
            if (ex instanceof SQLFactoryException) throw new SQLFactoryException();
        }
        throw new SQLFactoryException();
    }

    public void setupPlayer(QueryFactory queryFactory, Player player) throws SQLFactoryException {
        CompletableFuture<Boolean> resultSetFuture = queryFactory.runInsert(
                "INSERT INTO honey_players (playerUUID, playerName) " +
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

        CompletableFuture<Boolean> resultSetFuture = queryFactory.runInsert(
                "UPDATE honey_players AS p " +
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

    public void updatePlayerName(QueryFactory queryFactory, Player player) throws SQLFactoryException {
        CompletableFuture<Boolean> resultSetFuture = queryFactory.runInsert(
                "UPDATE honey_players AS p " +
                "SET p.playerName = ? " +
                "WHERE p.playerUUID = ?",
                player.getName(), player.getUniqueId().toString()
        );
        try {
            resultSetFuture.get();

        } catch (Exception ex) {
            ex.printStackTrace();
            if (ex instanceof SQLFactoryException) throw new SQLFactoryException();
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
            if (ex instanceof SQLFactoryException) throw new SQLFactoryException();
        }
    }

    public void preparePlayer(QueryFactory queryFactory, Player player) throws SQLFactoryException, SQLPlayerNotFoundException {
        final String uuid = player.getUniqueId().toString();
        boolean existsPlayer = existsPlayer(queryFactory, player.getName());

        // if the uuid is matching the player name, everything is correct and we can just return
        if (existsPlayer && getPlayerData(queryFactory, player.getName()).getPlayerUUID().equals(uuid)) {
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
                removePlayerName(queryFactory, player.getName());
            }
            // finally we will setup this player, because at this point the uuid and name will not be present anywhere
            setupPlayer(queryFactory, player);
        }
    }
}
