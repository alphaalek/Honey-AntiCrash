package me.alek.honey.netty.modelwrappers.suggestion;

import me.alek.honey.netty.modelwrappers.WrappedObject;

public class WrappedSuggestion extends WrappedObject<WrappedSuggestion> {

    private String text;

    public WrappedSuggestion(Object handle) {
        super(handle);
    }

    public WrappedSuggestion(String text) {
        super(null);
        this.text = text;
    }

    public String getText() {
        if (text == null) return (text = getModifier(String.class).readField(0));

        return text;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof WrappedSuggestion)) return false;

        return getText().equals(((WrappedSuggestion)o).getText());
    }
}
