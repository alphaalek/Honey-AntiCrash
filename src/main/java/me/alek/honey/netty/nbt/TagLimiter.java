package me.alek.honey.netty.nbt;

import io.netty.handler.codec.DecoderException;

public class TagLimiter {

    private final int maxBytes;
    private final int maxDepth;
    private int bytes;
    private int depth;

    public TagLimiter(int maxBytes, int maxDepth) {
        this.maxBytes = maxBytes;
        this.maxDepth = maxDepth;
    }

    public void incrementBytes(int bytes) {
        this.bytes += bytes;

        if (this.bytes > this.maxBytes) {
            throw new DecoderException("Tried to read nbt with too many bytes: " + this.bytes);
        }
    }

    public int incrementComplexity() {
        this.depth++;

        if (this.depth > this.maxDepth) {
            throw new DecoderException("Tried to read too complex nbt: " + this.depth);
        }

        return this.depth - 1;
    }

    public void setComplexity(int depth) {
        this.depth = depth;
    }
}
