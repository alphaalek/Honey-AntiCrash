package me.alek.honey.netty.modelwrappers.suggestion;

import me.alek.honey.netty.modelwrappers.WrappedObject;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.structure.converters.ListConverter;

import java.util.List;

public class WrappedSuggestions extends WrappedObject<WrappedSuggestions> {

    public WrappedSuggestions(Object handle) {
        super(handle);
    }

    public ListConverter<Object, WrappedSuggestion> getSuggestions() {
        return getReflectionStructure()
                .withWeakType(List.class, Converters.getListConverterOf(WrappedSuggestion.class, Converters.getSuggestionConverter()))
                .withTarget(getHandle())
                .readField(0);
    }
}
