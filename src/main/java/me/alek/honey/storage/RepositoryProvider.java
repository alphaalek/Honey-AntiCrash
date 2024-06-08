package me.alek.honey.storage;

import me.alek.honey.storage.repository.PlayerRepositoryProvider;
import me.alek.honey.storage.repository.ViolationRepositoryProvider;

import java.util.Arrays;
import java.util.Iterator;

public class RepositoryProvider implements Iterable<Repository> {

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
