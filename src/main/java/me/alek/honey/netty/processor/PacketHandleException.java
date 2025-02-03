package me.alek.honey.netty.processor;

public class PacketHandleException extends RuntimeException {

    public PacketHandleException(String msg, Throwable thrown) {
        super(msg, thrown);
    }
}
