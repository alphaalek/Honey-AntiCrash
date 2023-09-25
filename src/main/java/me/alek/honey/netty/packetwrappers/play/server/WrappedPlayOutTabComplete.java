package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.IStructureModifier;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.modelwrappers.suggestion.WrappedSuggestion;
import me.alek.honey.netty.modelwrappers.suggestion.WrappedSuggestions;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.structure.converters.ListConverter;
import me.alek.honey.netty.utility.protocol.Protocol;
import me.alek.honey.netty.utility.reflect.FieldAccessor;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import org.bukkit.Bukkit;

import java.util.*;

public class WrappedPlayOutTabComplete extends WrappedPacket<WrappedPlayOutTabComplete> {

    private static final boolean IS_MODERN = Protocol.getProtocol().isNewerThanOrEqual(Protocol.v1_13);

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

    private Object[] getHandleFromArrayList(List<?> list) {
        final IStructureModifier<Object[]> modifier = ReflectStructureCache.acquireStructure(list.getClass())
                .withType(Object[].class).withTarget(list);
        return modifier.readField(2);
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
        final List<?> list = new ArrayList<>(Arrays.asList(array));
        list.removeIf(value -> Objects.isNull(value) || value.toString().equals("null"));

        return list.toArray();
    }

    public List<String> getControllableSuggestions() {
        return new ArrayList<String>() {

            @Override
            public boolean remove(Object o) {
                if (!(o instanceof String)) return false;

                final List<String> suggestions = new ArrayList<>(Arrays.asList(getSuggestions()));
                suggestions.remove(o);

                if (!IS_MODERN) {
                    getContainer().getStringArrays().writeField(0,
                            createStringArrayFromObjectArray(removeNullsFromArray(suggestions.toArray())));
                }
                else {
                    getSuggestionsHandle(true).removeConverted(new WrappedSuggestion((String)o));
                }
                return true;
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
