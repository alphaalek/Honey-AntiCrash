package me.alek.honey.storage.sql;

import me.alek.honey.storage.sql.repository.FlagRepository;
import me.alek.honey.storage.sql.repository.PlayerRepository;

import java.util.Arrays;
import java.util.Iterator;

public class BeanRepositoryCollection implements Iterable<RepositoryComponent> {

    public PlayerRepository playerRepository = new PlayerRepository();
    public FlagRepository flagRepository = new FlagRepository();

    @Override
    public Iterator<RepositoryComponent> iterator() {
        return Arrays.asList(playerRepository, flagRepository).iterator();
    }

    public int size() {
        return 2;
    }
}
