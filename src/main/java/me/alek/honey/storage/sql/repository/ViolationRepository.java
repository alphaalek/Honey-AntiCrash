package me.alek.honey.storage.sql.repository;

import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.storage.sql.RepositoryComponent;
import me.alek.honey.storage.sql.QueryFactory;
import org.bukkit.entity.Player;

public class ViolationRepository implements RepositoryComponent {

    @Override
    public void prepare(QueryFactory queryFactory) {
        queryFactory.runInsert(
                "CREATE TABLE IF NOT EXISTS violations (" +
                "playerId INT, " +
                "unixTime LONG, " +
                "checkName VARCHAR(16), " +
                "settings VARCHAR(255), " +
                "quotients VARCHAR(255), " +
                "devData VARCHAR(255), " +
                ");"
        );
    }

    public void postViolation(QueryFactory queryFactory, Player player, String violationName, ConfigurationOptions.VlSetting setting, CheckContextContainer.DevelopmentData developmentData) {

    }
}
