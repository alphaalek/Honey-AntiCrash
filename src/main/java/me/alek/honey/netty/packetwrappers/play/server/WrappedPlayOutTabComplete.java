package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.modelwrappers.suggestion.WrappedSuggestion;
import me.alek.honey.netty.modelwrappers.suggestion.WrappedSuggestions;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.structure.converters.ListConverter;
import me.alek.honey.netty.utility.protocol.Protocol;
import me.alek.honey.netty.utility.reflect.NMSUtils;

public class WrappedPlayOutTabComplete extends WrappedPacket<WrappedPlayOutTabComplete> {

    private static final boolean IS_MODERN = Protocol.getProtocol().isNewerThanOrEqual(Protocol.v1_13);

    public WrappedPlayOutTabComplete(Object rawPacket, PacketContainer<WrappedPlayOutTabComplete> packetContainer) {
        super(rawPacket, packetContainer);
    }

    private ReflectStructure<Object, ?> getSuggestionsStructure() {
        return ReflectStructureCache.acquireStructure(NMSUtils.getSuggestionsClass());
    }

    private WrappedSuggestions getWrappedSuggestions() {
        return getSuggestionsStructure()
                .withType(NMSUtils.getSuggestionsClass(), Converters.getSuggestionsConverter())
                .withTarget(getHandle())
                .readField(0);
    }

    private ListConverter<Object, WrappedSuggestion> getSuggestionsHandle() {
        return getWrappedSuggestions().getSuggestions(false);
    }

    public String[] getSuggestions() {
        if (!IS_MODERN) {
            return getContainer().getStringArrays().readField(0);
        }
        ListConverter<Object, WrappedSuggestion> suggestionsHandle = getSuggestionsHandle();

        String[] suggestionsArray = new String[suggestionsHandle.size()];
        int i = 0;
        for (WrappedSuggestion suggestion : suggestionsHandle) {
            suggestionsArray[i++] = suggestion.getText();
        }

        return suggestionsArray;
    }

    public int getSuggestionsSize() {
        if (!IS_MODERN) {
            return getSuggestions().length;
        }
        ListConverter<Object, WrappedSuggestion> suggestionsHandle = getSuggestionsHandle();

        return suggestionsHandle.size();
    }
}
