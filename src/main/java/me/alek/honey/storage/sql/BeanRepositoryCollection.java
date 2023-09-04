package me.alek.honey.storage.sql;

import me.alek.honey.storage.sql.repository.ViolationRepository;
import me.alek.honey.storage.sql.repository.PlayerRepository;

import java.util.Arrays;
import java.util.Iterator;

public class BeanRepositoryCollection implements Iterable<RepositoryComponent> {

    public PlayerRepository playerRepository = new PlayerRepository();
    public ViolationRepository violationRepository = new ViolationRepository();

    @Override
    public Iterator<RepositoryComponent> iterator() {
        return Arrays.asList(playerRepository, violationRepository).iterator();
    }

    public int size() {
        return 2;
    }
}
