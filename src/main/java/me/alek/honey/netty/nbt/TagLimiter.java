package me.alek.honey.netty.nbt;

import io.netty.handler.codec.DecoderException;

public class TagLimiter {

    private final int maxBytes;
    private final int maxDepth;
    private final int maxComplexity;
    private int bytes;
    private int depth;
    private int complexity;

    public TagLimiter(int maxBytes, int maxDepth, int maxComplexity) {
        this.maxBytes = maxBytes;
        this.maxDepth = maxDepth;
        this.maxComplexity = maxComplexity;
    }

    public void incrementBytes(int bytes) {
        this.bytes += bytes;

        if (this.bytes > this.maxBytes) {
            throw new DecoderException("Tried to read nbt with too many bytes: " + this.bytes);
        }
    }

    public int incrementNestingDepth() {
        this.depth++;

        if (this.depth > this.maxDepth) {
            throw new DecoderException("Tried to read nbt which is too deep: " + this.depth);
        }

        return this.depth - 1;
    }

    public void incrementComplexity() {
        this.complexity++;

        if (this.complexity > this.maxComplexity) {
            throw new DecoderException("Tried to read nbt which is too complex: " + this.complexity);
        }
    }

    public void setNestingDepthLevel(int depth) {
        this.depth = depth;
    }
}
