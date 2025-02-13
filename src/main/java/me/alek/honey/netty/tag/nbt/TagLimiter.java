package me.alek.honey.netty.tag.nbt;

import io.netty.handler.codec.DecoderException;
import me.alek.honey.netty.codec.types.Utf8String;

import java.io.DataInputStream;
import java.io.IOException;

public interface TagLimiter {

    int THIRTY_TWO_KILOBYTES = 1024 * 320;

    class UTF8Limiter implements TagLimiter {

        private final TagLimiter delegate;
        public final int maxLength;

        public UTF8Limiter(TagLimiter delegate, int maxLength) {
            this.delegate = delegate;
            this.maxLength = maxLength;
        }

        public String readString(DataInputStream stream) throws IOException {
            return Utf8String.readUTFInputStream(stream, this.maxLength);
        }

        @Override
        public void incrementBase() {
            this.delegate.incrementBase();
        }

        @Override
        public void incrementBytes(int bytes) {
            this.delegate.incrementBytes(bytes);
        }

        @Override
        public int incrementNestingDepth() {
            return this.delegate.incrementNestingDepth();
        }

        @Override
        public void incrementComplexity() {
            this.delegate.incrementComplexity();
        }

        @Override
        public void setNestingDepthLevel(int depth) {
            this.delegate.setNestingDepthLevel(depth);
        }
    }

    class Default implements TagLimiter {

        private final int maxBytes;
        private final int maxDepth;
        private final int maxComplexity;
        private final int maxBases;
        protected int bytes;
        private int depth;
        private int complexity;
        private int bases;

        public Default(int maxBytes, int maxDepth, int maxComplexity, int maxBases) {
            this.maxBytes = maxBytes;
            this.maxDepth = maxDepth;
            this.maxComplexity = maxComplexity;
            this.maxBases = maxBases;
        }

        @Override
        public void incrementBase() {
            this.bases++;

            if (this.bases > this.maxBases) {
                throw new DecoderException("Tried to read nbt with too many bases: " + this.bases);
            }
        }

        @Override
        public void incrementBytes(int bytes) {
            this.bytes += bytes;

            if (this.bytes > this.maxBytes) {
                throw new DecoderException("Tried to read too complex nbt: " + this.bytes);
            }
        }

        @Override
        public int incrementNestingDepth() {
            this.depth++;

            if (this.depth > this.maxDepth) {
                throw new DecoderException("Tried to read nbt which is too deep: " + this.depth);
            }

            return this.depth - 1;
        }

        @Override
        public void incrementComplexity() {
            this.complexity++;

            if (this.complexity > this.maxComplexity) {
                throw new DecoderException("Tried to read nbt which is too complex: " + this.complexity);
            }
        }

        @Override
        public void setNestingDepthLevel(int depth) {
            this.depth = depth;
        }
    }

    void incrementBase();

    void incrementBytes(int bytes);

    int incrementNestingDepth();

    void incrementComplexity();

    void setNestingDepthLevel(int depth);
}
