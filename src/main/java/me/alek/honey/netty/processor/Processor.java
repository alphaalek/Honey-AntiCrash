package me.alek.honey.netty.processor;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.PacketAdapter;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.util.Array;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class Processor<WP extends WrappedPacket<WP>> extends PacketAdapter<WP> {

    private final Array<List<Consumer<PacketContainer<WP>>>> listeners = new Array<>();

    public void add(PacketType type, Consumer<PacketContainer<WP>> listener) {
        List<Consumer<PacketContainer<WP>>> listeners = this.listeners.get(type.getHoneyID(), LinkedList::new);
        listeners.add(listener);
    }


}
