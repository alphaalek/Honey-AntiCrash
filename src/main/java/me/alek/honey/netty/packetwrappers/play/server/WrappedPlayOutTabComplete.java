package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.modelwrappers.suggestion.WrappedSuggestion;
import me.alek.honey.netty.modelwrappers.suggestion.WrappedSuggestions;
import me.alek.honey.netty.packetwrappers.ObjectWrappedPacket;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.structure.converters.ListConverter;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.NMS;

import java.util.*;
import java.util.stream.Collectors;

public class WrappedPlayOutTabComplete extends ObjectWrappedPacket<WrappedPlayOutTabComplete> {

    private static final boolean IS_MODERN = NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_13);

    public WrappedPlayOutTabComplete(Object rawPacket, PacketContainer<WrappedPlayOutTabComplete> packetContainer) {
        super(rawPacket, packetContainer);
    }

    private ReflectStructure<Object, ?> getSuggestionsStructure() {
        return ReflectStructure.Provider.acquireStructure(NMS.getSuggestionsClass());
    }

    private WrappedSuggestions getWrappedSuggestionsObject() {
        return getSuggestionsStructure()
                .withType(NMS.getSuggestionsClass(), Converters.getSuggestionsConverter())
                .withTarget(getHandle())
                .readField(0);
    }

    private ListConverter<Object, WrappedSuggestion> getModernSuggestionsHandle(boolean controlDeclaring) {
        return getWrappedSuggestionsObject().getSuggestions(controlDeclaring);
    }

    private String[] createStringArrayFromObjectArray(Object[] objects) {
        if (objects instanceof String[]) return (String[]) objects;

        final String[] array = new String[objects.length];
        for (int i = 0; i < objects.length; i++) {
            array[i] = (String) objects[i];
        }
        return array;
    }

    private Object[] removeNullsFromArray(Object[] array) {
        Set<Object> set = Arrays.stream(array).collect(Collectors.toSet());
        set.removeAll(Collections.singleton(null));
        return set.toArray();
    }

    public void removeSuggestion(String suggestion) {
        if (!IS_MODERN) {
            List<String> suggestions = new ArrayList<>(Arrays.asList(getSuggestions()));
            suggestions.remove(suggestion);
            getContainer().getStructure().getStringArrays().writeField(0, createStringArrayFromObjectArray(removeNullsFromArray(suggestions.toArray())));
        } else {
            getModernSuggestionsHandle(true).removeConverted(new WrappedSuggestion(suggestion));
        }
    }

    public String[] getSuggestions() {
        if (!IS_MODERN) {
            return getContainer().getStructure().getStringArrays().readField(0);
        }

        ListConverter<Object, WrappedSuggestion> suggestionsHandle = getModernSuggestionsHandle(true);
        String[] suggestionsArray = new String[suggestionsHandle.size()];
        int i = 0;
        for (final WrappedSuggestion suggestion : suggestionsHandle) {
            suggestionsArray[i++] = suggestion.getText();
        }

        return suggestionsArray;
    }

    public int getSuggestionsSize() {
        if (!IS_MODERN) {
            return getSuggestions().length;
        }

        ListConverter<Object, WrappedSuggestion> suggestionsHandle = getModernSuggestionsHandle(false);
        return suggestionsHandle.size();
    }
}
