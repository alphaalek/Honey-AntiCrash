package me.alek.honey.util;

import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IDRegistry {

    /* pre-made ID registries */
    public static final IDRegistry TRACKER_CONTEXT = new IDRegistry();
    public static final IDRegistry PLAYERS = new IDRegistry(1);

    private int[] ids = new int[0];

    public IDRegistry(int preMadeIDs) {
        for (int i = 0; i < preMadeIDs; i++) {
            acquireID();
        }
    }

    public IDRegistry(int... preMadeIDs) {
        Bukkit.broadcastMessage("creating registry from " + Arrays.toString(preMadeIDs));
        int max = 0;
        List<Integer> ids = new ArrayList<>();
        for (int i : preMadeIDs) {
            ids.add(i);
            if (i > max) max = i;
        }

        Bukkit.broadcastMessage("registry " + ids + " " + max);

        for (int i = 0; i <= max; i++) {
            acquireID();
        }

        Bukkit.broadcastMessage("registry " + Arrays.toString(this.ids));

        for (int i = 0; i < max; i++) {
            if (!ids.contains(i)) {
                remove(i);
            }
        }

        Bukkit.broadcastMessage("registry " + Arrays.toString(this.ids));
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
