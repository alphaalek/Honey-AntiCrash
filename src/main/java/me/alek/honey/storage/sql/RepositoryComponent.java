package me.alek.honey.storage.sql;

import org.bukkit.entity.Player;

public interface RepositoryComponent {

    void prepare(QueryFactory queryFactory);

    void preparePlayer(QueryFactory queryFactory, Player player);
}
