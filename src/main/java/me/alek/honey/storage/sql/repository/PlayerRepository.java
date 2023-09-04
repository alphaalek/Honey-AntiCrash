package me.alek.honey.storage.sql.repository;

import me.alek.honey.storage.sql.RepositoryComponent;
import me.alek.honey.storage.sql.QueryFactory;
import org.bukkit.entity.Player;

public class PlayerRepository implements RepositoryComponent {

    @Override
    public void prepare(QueryFactory queryFactory) {
        queryFactory.runInsert(
                "CREATE TABLE IF NOT EXISTS players (" +
                "playerId PRIMARY KEY, " +
                "playerUUID VARCHAR(36), " +
                "playerName VARCHAR(16)" +
                ");"
        );
    }

    public void preparePlayer(QueryFactory queryFactory, Player player) {


    }
}
