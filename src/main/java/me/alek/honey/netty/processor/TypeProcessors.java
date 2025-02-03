package me.alek.honey.netty.processor;

import me.alek.honey.netty.processor.impl.BrandProcessor;
import me.alek.honey.netty.processor.impl.HandshakeProcessor;
import me.alek.honey.netty.processor.impl.InventoryProcessor;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.processor.impl.LoginProcessor;

public class TypeProcessors {

    public static void loadImpl() {
        HandshakeProcessor.load();
        BrandProcessor.load();
        LoginProcessor.load();
    }

    public static final InventoryProcessor<?> INVENTORY = new InventoryProcessor<>();
}
