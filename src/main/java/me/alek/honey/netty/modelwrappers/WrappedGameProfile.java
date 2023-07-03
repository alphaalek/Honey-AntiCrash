package me.alek.honey.netty.modelwrappers;

import java.util.UUID;

public class WrappedGameProfile extends WrappedObject<WrappedGameProfile> {

    public WrappedGameProfile(Object handle) {
        super(handle);
    }

    public UUID getUUID() {
        return getModifier(UUID.class).readField(0);
    }

    public boolean isLegacy() {
        return getModifier(boolean.class).readField(0);
    }

    public String getName() {
        return getModifier(String.class).readField(0);
    }
}
