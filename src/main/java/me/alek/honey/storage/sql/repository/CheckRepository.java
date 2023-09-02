package me.alek.honey.storage.sql.repository;

import me.alek.honey.storage.sql.RepositoryComponent;
import me.alek.honey.storage.sql.QueryFactory;

public class CheckRepository implements RepositoryComponent {

    @Override
    public boolean prepare(QueryFactory queryFactory) {
        return true;
    }
}
