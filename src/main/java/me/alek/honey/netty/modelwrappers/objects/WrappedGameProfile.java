package me.alek.honey.netty.modelwrappers.objects;

import me.alek.honey.netty.modelwrappers.WrappedObject;

import java.util.UUID;

public class WrappedGameProfile extends WrappedObject<WrappedGameProfile> {

    public WrappedGameProfile(Object handle) {
        // TODO: make own
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
