package me.alek.honey.tickloop.packet;

import me.alek.honey.util.Array;
import me.alek.honey.util.Provider;

public class PacketTaskFactoryProvider implements Provider.ProviderGenericInput<PacketTaskFactory> {

    @Override
    public PacketTaskFactory get(Array<Object> array) {
        return new PacketTaskFactory((boolean) array.get(0));
    }
}
