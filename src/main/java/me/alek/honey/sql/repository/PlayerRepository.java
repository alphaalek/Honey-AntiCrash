package me.alek.honey.sql.repository;

import me.alek.honey.sql.QueryFactory;
import me.alek.honey.sql.RepositoryComponent;

public class PlayerRepository implements RepositoryComponent {

    @Override
    public boolean prepare(QueryFactory queryFactory) {


        return true;
    }
}
