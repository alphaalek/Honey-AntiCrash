package me.alek.honey.sql;

import me.alek.honey.sql.repository.PlayerRepositoryProvider;
import me.alek.honey.sql.repository.ViolationRepositoryProvider;

import java.util.Arrays;
import java.util.Iterator;

public class RepositoryCollection implements Iterable<Repository> {

    public PlayerRepositoryProvider playerRepository = new PlayerRepositoryProvider();
    public ViolationRepositoryProvider violationRepository = new ViolationRepositoryProvider();

    @Override
    public Iterator<Repository> iterator() {
        return Arrays.asList(playerRepository, violationRepository).iterator();
    }

    public int size() {
        return 2;
    }
}
