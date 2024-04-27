package me.alek.honey.util;

import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IDRegistry {

    /* pre-made ID registries */
    public static final IDRegistry TRACKER_CONTEXT = new IDRegistry();
    public static final IDRegistry PLAYERS = IDRegistry.create(1);

    private int[] ids = new int[0];

    public static IDRegistry create(int ids) {
        IDRegistry registry = new IDRegistry();
        for (int i = 0; i < ids; i++) {
            registry.acquireID();
        }
        return registry;
    }

    public static IDRegistry from(int... ids) {
        Bukkit.broadcastMessage("creating registry from " + Arrays.toString(ids));
        int max = 0;
        List<Integer> idsList = new ArrayList<>();
        for (int i : ids) {
            idsList.add(i);
            if (i > max) max = i;
        }

        Bukkit.broadcastMessage("registry " + idsList + " " + max);

        IDRegistry registry = new IDRegistry();
        for (int i = 0; i <= max; i++) {
            registry.acquireID();
        }

        Bukkit.broadcastMessage("registry " + Arrays.toString(registry.ids));

        for (int i = 0; i < max; i++) {
            if (!idsList.contains(i)) {
                registry.remove(i);
            }
        }

        Bukkit.broadcastMessage("registry " + Arrays.toString(registry.ids));
        return registry;
    }

    public IDRegistry() {

    }

    public synchronized int acquireID() {
        try {
            int i = 0;
            for (int id : ids) {
                if (id == -1) return ids[i] = i;
                ++i;
            }
            int[] newIds = new int[ids.length + 1];
            System.arraycopy(ids, 0, newIds, 0, ids.length);
            ids = newIds;
            return ids[ids.length - 1] = ids.length - 1;
        } catch (Exception ex) {
            ex.printStackTrace();
            return 0;
        }
    }

    public synchronized void addID(int id) {
        if (id >= ids.length) {
            int[] newIds = new int[id + 1];
            System.arraycopy(ids, 0, newIds, 0, ids.length);
            ids = newIds;
        }
        ids[id] = id;
    }

    public synchronized void remove(int id) {
        ids[id] = -1;
        int maxId = 0;
        for (int id0 : ids) {
            if (id0 != -1) maxId = id0 - 1;
        }
        if (maxId < ids.length - 1) {
            int[] newIds = new int[maxId + 2];
            System.arraycopy(ids, 0, newIds, 0, newIds.length);
            ids = newIds;
        }
    }

    public boolean hasID(int id) {
        return ids.length > id && ids[id] != -1;
    }

    public int[] getIDs() {
        return ids;
    }

    public int size() {
        return ids.length;
    }
}
