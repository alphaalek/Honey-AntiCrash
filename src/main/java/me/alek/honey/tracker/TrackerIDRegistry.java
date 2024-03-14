package me.alek.honey.tracker;

public class TrackerIDRegistry {

    public static final TrackerIDRegistry DEFAULT = new TrackerIDRegistry();

    private int[] ids = new int[0];

    public synchronized int getID() {
        try {
            int i = 0;
            for (int id : ids) {
                if (id == -1) return ids[i] = i;
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

    public synchronized void remove(int id) {
        ids[id] = -1;
        int maxId = 0;
        for (int id0 : ids) {
            if (id0 != -1) maxId = id0;
        }
        if (maxId != ids.length - 1) {
            int[] newIds = new int[maxId + 1];
            System.arraycopy(ids, 0, newIds, 0, newIds.length);
            ids = newIds;
        }
    }

    public int size() {
        return ids.length;
    }
}
