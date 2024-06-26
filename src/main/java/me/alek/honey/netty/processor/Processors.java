package me.alek.honey.netty.processor;

import me.alek.honey.packetwrappers.WrappedPacket;

public class Processors {

    public static void dummyLoad() {
        // nothing to do, just a dummy method to load the class into memory and initialize the processors
    }

    public static final InventoryProcessor<? extends WrappedPacket<?>> INVENTORY = new InventoryProcessor<>();
}
