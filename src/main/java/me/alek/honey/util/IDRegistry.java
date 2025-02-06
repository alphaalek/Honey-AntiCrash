package me.alek.honey.util;

import java.util.ArrayList;
import java.util.List;

public class IDRegistry {

    /* pre-made ID registries */
    public static final IDRegistry VALIDATION = new IDRegistry();
    public static final IDRegistry TRACKER_CONTEXT = new IDRegistry();
    public static final IDRegistry PLAYERS = new IDRegistry();

    private static final int EMPTY = -1;

    private int[] ids = new int[0];

    public static IDRegistry create(int ids) {
        IDRegistry registry = new IDRegistry();
        for (int i = 0; i < ids; i++) {
            registry.acquire();
        }
        return registry;
    }

    public static IDRegistry from(int... ids) {
        int max = 0;
        List<Integer> idsList = new ArrayList<>();
        for (int i : ids) {
            idsList.add(i);
            if (i > max) max = i;
        }

        IDRegistry registry = new IDRegistry();
        for (int i = 0; i <= max; i++) {
            registry.add(i);
            if (!idsList.contains(i)) {
                registry.remove(i);
            }
        }

        return registry;
    }

    public IDRegistry() {

    }

    public synchronized int acquire() {
        for (int i = 0; i < this.ids.length; i++) {
            if (this.ids[i] == EMPTY) return this.ids[i] = i;
        }
        int[] newIds = new int[this.ids.length + 1];
        System.arraycopy(this.ids, 0, newIds, 0, this.ids.length);
        this.ids = newIds;
        return this.ids[this.ids.length - 1] = this.ids.length - 1;
    }

    public synchronized void add(int id) {
        if (id >= this.ids.length) {
            int[] newIds = new int[id + 1];
            System.arraycopy(this.ids, 0, newIds, 0, this.ids.length);
            this.ids = newIds;
        }
        this.ids[id] = id;
    }

    public synchronized void remove(int id) {
        this.ids[id] = EMPTY;
        int maxId = 0;
        for (int i = 0; i < this.ids.length; i++) {
            if (this.ids[i] != EMPTY) maxId = i;
        }
        if (maxId < this.ids.length - 1) {
            int[] newIds = new int[maxId + 1];
            System.arraycopy(this.ids, 0, newIds, 0, newIds.length);
            this.ids = newIds;
        }
    }

    public boolean hasID(int id) {
        return this.ids.length > id && this.ids[id] != EMPTY;
    }

    public int[] getIDs() {
        return this.ids;
    }

    public int size() {
        return this.ids.length;
    }
}
