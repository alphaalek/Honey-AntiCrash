package me.alek.honey.storage.sql.repository;

import me.alek.honey.storage.sql.RepositoryComponent;
import me.alek.honey.storage.sql.QueryFactory;
import org.bukkit.entity.Player;

public class FlagRepository implements RepositoryComponent {

    @Override
    public void prepare(QueryFactory queryFactory) {
        queryFactory.runInsert(
                "CREATE TABLE IF NOT EXISTS flags (" +
                "playerId INT, " +
                "unixTime LONG, " +
                "checkName VARCHAR(16), " +
                "settings VARCHAR(255), " +
                "quotients VARCHAR(255), " +
                "devData VARCHAR(255), " +
                ");"
        );
    }

    @Override
    public void preparePlayer(QueryFactory queryFactory, Player player) {


    }
}
