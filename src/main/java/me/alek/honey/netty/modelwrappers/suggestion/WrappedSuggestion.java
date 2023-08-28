package me.alek.honey.netty.modelwrappers.suggestion;

import me.alek.honey.netty.modelwrappers.WrappedObject;

public class WrappedSuggestion extends WrappedObject<WrappedSuggestion> {

    public WrappedSuggestion(Object handle) {
        super(handle);
    }

    public String getText() {
        return getModifier(String.class).readField(0);
    }
}
