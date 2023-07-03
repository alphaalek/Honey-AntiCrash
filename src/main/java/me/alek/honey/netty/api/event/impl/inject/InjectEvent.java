package me.alek.honey.netty.api.event.impl.inject;

import me.alek.honey.netty.api.event.Event;

public class InjectEvent extends Event {

    public enum InjectType {
        LATE,
        EARLY
    }

    public enum InjectBound {
        PLAYER,
        SERVER
    }

    public enum InjectCallback {
        SUCCESS,
        ERROR;
    }

    private final InjectType injectType;
    private final InjectCallback injectCallback;
    private final InjectBound injectBound;

    public InjectEvent(InjectType injectType, InjectCallback callback, InjectBound injectBound) {
        this.injectType = injectType;
        this.injectCallback = callback;
        this.injectBound = injectBound;

        /*if (callback == InjectCallback.ERROR) {
            Bukkit.getLogger().severe("Fejl ved inject event: " + PacketLibrary.get());

            if (injectType == InjectType.EARLY && injectBound == InjectBound.SERVER) {
                Bukkit.getLogger().info("Sætter proxy til late injection");
               PacketLibrary.get().setLateProxy();
            }
        }*/
    }

    public InjectType getInjectType() {
        return injectType;
    }

    public InjectCallback getCallback() {
        return injectCallback;
    }

    public InjectBound getBound() {
        return injectBound;
    }
}
