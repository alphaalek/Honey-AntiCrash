package me.alek.honey.storage.sql;

import me.alek.honey.storage.sql.RepositoryComponent;
import me.alek.honey.storage.sql.repository.CheckRepository;
import me.alek.honey.storage.sql.repository.PlayerRepository;

import java.util.Arrays;
import java.util.Iterator;

public class BeanRepositoryCollection implements Iterable<RepositoryComponent> {

    public PlayerRepository playerRepository = new PlayerRepository();
    public CheckRepository checkRepository = new CheckRepository();

    @Override
    public Iterator<RepositoryComponent> iterator() {
        return Arrays.asList(playerRepository, checkRepository).iterator();
    }

    public int size() {
        return 2;
    }
}
