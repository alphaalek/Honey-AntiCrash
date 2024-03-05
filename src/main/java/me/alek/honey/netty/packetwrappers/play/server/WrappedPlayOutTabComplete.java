package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.modelwrappers.suggestion.WrappedSuggestion;
import me.alek.honey.netty.modelwrappers.suggestion.WrappedSuggestions;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.structure.converters.ListConverter;
import me.alek.honey.netty.utility.NetProtocol;
import me.alek.honey.netty.utility.NMSUtils;

import java.util.*;
import java.util.stream.Collectors;

public class WrappedPlayOutTabComplete extends WrappedPacket<WrappedPlayOutTabComplete> {

    private static final boolean IS_MODERN = NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_13);

    public WrappedPlayOutTabComplete(Object rawPacket, PacketContainer<WrappedPlayOutTabComplete> packetContainer) {
        super(rawPacket, packetContainer);
    }

    private ReflectStructure<Object, ?> getSuggestionsStructure() {
        return ReflectStructureCache.acquireStructure(NMSUtils.getSuggestionsClass());
    }

    private WrappedSuggestions getWrappedSuggestionsObject() {
        return getSuggestionsStructure()
                .withType(NMSUtils.getSuggestionsClass(), Converters.getSuggestionsConverter())
                .withTarget(getHandle())
                .readField(0);
    }

    private ListConverter<Object, WrappedSuggestion> getSuggestionsHandle(boolean controlDeclaring) {
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

    public List<String> getControllableSuggestions() {
        return new ArrayList<String>() {

            @Override
            public boolean remove(Object o) {
                if (!(o instanceof String)) return false;

                if (!IS_MODERN) {
                    final List<String> suggestions = new ArrayList<>(Arrays.asList(getSuggestions()));
                    boolean removed = suggestions.remove(o);

                    getContainer().getStringArrays().writeField(0,
                            createStringArrayFromObjectArray(removeNullsFromArray(suggestions.toArray())));
                    return removed;
                }
                else {
                    return getSuggestionsHandle(true).removeConverted(new WrappedSuggestion((String)o));
                }
            }

            @Override
            public boolean contains(Object value) {
                return Arrays.asList(getSuggestions()).contains(value);
            }
        };
    }

    public String[] getSuggestions(boolean controlDeclaring) {
        if (!IS_MODERN) {
            return getContainer().getStringArrays().readField(0);
        }
        final ListConverter<Object, WrappedSuggestion> suggestionsHandle = getSuggestionsHandle(controlDeclaring);

        final String[] suggestionsArray = new String[suggestionsHandle.size()];
        int i = 0;
        for (final WrappedSuggestion suggestion : suggestionsHandle) {
            suggestionsArray[i++] = suggestion.getText();
        }

        return suggestionsArray;
    }

    public String[] getSuggestions() {
        return getSuggestions(false);
    }

    public int getSuggestionsSize() {
        if (!IS_MODERN) {
            return getSuggestions().length;
        }
        ListConverter<Object, WrappedSuggestion> suggestionsHandle = getSuggestionsHandle(false);

        return suggestionsHandle.size();
    }
}
