package me.alek.honey.storage;

import me.alek.honey.storage.repository.ViolationRepositoryProvider;
import me.alek.honey.storage.repository.PlayerRepositoryProvider;

import java.util.Arrays;
import java.util.Iterator;

public class BeanRepositoryCollection implements Iterable<RepositoryComponentProvider> {

    public PlayerRepositoryProvider playerRepository = new PlayerRepositoryProvider();
    public ViolationRepositoryProvider violationRepository = new ViolationRepositoryProvider();

    @Override
    public Iterator<RepositoryComponentProvider> iterator() {
        return Arrays.asList(playerRepository, violationRepository).iterator();
    }

    public int size() {
        return 2;
    }
}
