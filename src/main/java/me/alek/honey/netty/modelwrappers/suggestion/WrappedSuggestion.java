package me.alek.honey.netty.modelwrappers.suggestion;

import me.alek.honey.netty.modelwrappers.WrappedObject;
import me.alek.honey.netty.utility.ConstructorInvoker;
import me.alek.honey.netty.utility.NMS;
import me.alek.honey.netty.utility.Reflection;

public class WrappedSuggestion extends WrappedObject<WrappedSuggestion> {

    private static final Class<?> SUGGESTION_CLASS = NMS.getSuggestionClass();
    private static final Class<?> STRING_RANGE_CLASS = NMS.getStringRangeClass();
    private static final ConstructorInvoker<Object> stringRangeInvoker;
    private static final ConstructorInvoker<Object> suggestionInvoker;

    static {
        stringRangeInvoker = Reflection.getConstructor(STRING_RANGE_CLASS, int.class, int.class);
        suggestionInvoker = Reflection.getConstructor(SUGGESTION_CLASS, STRING_RANGE_CLASS, String.class);
    }

    private static Object createNewSuggestion(String text) {
        try {
            Object stringRange = stringRangeInvoker.invoke(0, text.length());
            return suggestionInvoker.invoke(stringRange, text);
        } catch (Exception ex) {
            return null;
        }
    }

    private String text;

    public WrappedSuggestion(Object handle) {
        super(handle);
    }

    public WrappedSuggestion(String text) {
        super(createNewSuggestion(text));
        this.text = text;
    }

    public String getText() {
        if (text == null) return getHandle() != null ? (text = getModifier(String.class).readField(0)) : null;

        return text;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof WrappedSuggestion) {
            return getText().equals(((WrappedSuggestion)o).getText());
        }
        if (SUGGESTION_CLASS.isInstance(o)) {
            return getText().equals(new WrappedSuggestion(o).getText());
        }
        return false;
    }
}
