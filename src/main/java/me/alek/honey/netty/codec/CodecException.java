package me.alek.honey.netty.codec;

public class CodecException extends Exception {

    private final int packetID;

    public CodecException(int id, String message) {
        super(message);
        this.packetID = id;
    }

    public CodecException(int id, Exception ex) {
        super(ex);
        this.packetID = id;
    }

    public int getPacketID() {
        return packetID;
    }
}
