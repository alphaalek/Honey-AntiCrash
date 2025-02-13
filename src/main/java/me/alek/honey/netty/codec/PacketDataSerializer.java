package me.alek.honey.netty.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufProcessor;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.codec.types.Types;
import me.alek.honey.netty.codec.validation.CodecTypeValidation;
import me.alek.honey.netty.codec.validation.CodecContext;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.tag.nbt.AbstractTag;
import me.alek.honey.netty.tag.nbt.TagLimiter;
import me.alek.honey.netty.tag.nbt.array.AbstractArrayTag;
import me.alek.honey.netty.tag.nbt.visitor.NBTVisitor;
import me.alek.honey.util.LazyInit;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.ConstructorInvoker;
import me.alek.honey.util.reflection.MinecraftReflection;
import me.alek.honey.util.reflection.Reflection;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.*;

public class PacketDataSerializer extends ByteBuf {

    private static final ConstructorInvoker<?> packetDataSerializerConstructor = Reflection.getConstructor(MinecraftReflection.getPacketDataSerializerClass(), ByteBuf.class);

    public static Object toNMSSerializer(ByteBuf bytebuf) {
        return packetDataSerializerConstructor.invoke(bytebuf);
    }

    private final ByteBuf delegate;
    private final LazyInit<Object> minecraftSerializer;

    public PacketDataSerializer() {
        this(Unpooled.buffer());
    }

    public PacketDataSerializer(ByteBuf buf) {
        this.delegate = buf;
        this.minecraftSerializer = new LazyInit<>(() -> toNMSSerializer(buf));
    }

    public ByteBuf getBuf() {
        return this.delegate;
    }

    public Object getSerializer() {
        return this.minecraftSerializer.get();
    }

    public WrappedBlockPosition readBlockPosition() {
        return WrappedBlockPosition.fromLong(readLong());
    }

    public int readVarInt() {
        return Types.VAR_INT.read(this);
    }

    public PacketDataSerializer writeVarInt(int val) {
        return Types.VAR_INT.write(this, val);
    }

    public PacketDataSerializer writeItemStack(PacketContainer<?> packet, WrappedImmutableItemStack stack) throws EncoderException {
        return writeItemStack(CodecContext.from(this, packet.getProtocol(), packet.getProtocolCreated()), stack);
    }
    public PacketDataSerializer writeItemStack(CodecContext context, WrappedImmutableItemStack stack) throws EncoderException {
        return Types.ITEM_STACK.write(this, stack, TypeSerializer.TypeContext.TwoArgs.of(CodecVisitor.Dynamic.OUT_DEFAULT, context));
    }

    public AbstractTag<?, ?, ?> readNBT(NBTVisitor.In visitor, boolean expectCompound) {
        return Types.NBT.read(this, TypeSerializer.TypeContext.TwoArgs.of(visitor, expectCompound));
    }

    public AbstractTag<?, ?, ?> readNBT(NBTVisitor.In visitor) {
        return readNBT(visitor, false);
    }

    public AbstractTag<?, ?, ?> readNBT(NetProtocol protocol, NetProtocol protocolCreated, TagLimiter limiter) {
        return readNBT(NBTVisitor.In.createDefault(limiter, protocol, protocolCreated, null));
    }

    public AbstractTag<?, ?, ?> readNBT(NetProtocol protocol, NetProtocol protocolCreated) {
        return readNBT(NBTVisitor.In.createDefault(protocol, protocolCreated, null));
    }

    public PacketDataSerializer writeNBT(NBTVisitor.Out visitor, AbstractTag<?, ?, ?> tag, boolean expectCompound) {
        return Types.NBT.write(this, tag, TypeSerializer.TypeContext.TwoArgs.of(visitor, expectCompound));
    }

    public PacketDataSerializer writeNBT(NBTVisitor.Out visitor, AbstractTag<?, ?, ?> tag) {
        return writeNBT(visitor, tag, false);
    }

    public PacketDataSerializer writeNBT(NetProtocol protocol, NetProtocol protocolCreated, AbstractTag<?, ?, ?> tag) {
        return writeNBT(NBTVisitor.Out.createDefault(protocol, protocolCreated, null), tag);
    }

    public Rewritable<WrappedImmutableItemStack> readItemStack(CodecVisitor.Dynamic<?, ?> visitorFactory, CodecContext context) throws StackDecodingException {
        try {
            return Optional.ofNullable(Types.ITEM_STACK.readRewritable(this, TypeSerializer.TypeContext.TwoArgs.of(visitorFactory, context)))
                    .orElse(Rewritable.noAction(WrappedImmutableItemStack.AIR));
        } catch (DecoderException ex) {
            throw new StackDecodingException(ex);
        }
    }

    public Rewritable<WrappedImmutableItemStack> readItemStack(CodecVisitor.Dynamic<?, ?> visitorFactory, NetProtocol protocol, NetProtocol protocolCreated, int maxMissingBytes) throws StackDecodingException {
        return readItemStack(visitorFactory, CodecContext.StackContextImpl.from(this, protocol, protocolCreated, maxMissingBytes));
    }

    public Rewritable<WrappedImmutableItemStack> readItemStackSafely(CodecVisitor.Dynamic<?, ?> visitorFactory, NetProtocol protocol, NetProtocol protocolCreated, int maxMissingBytes) throws StackDecodingException {
        Rewritable<WrappedImmutableItemStack> stack = readItemStack(visitorFactory, protocol, protocolCreated, maxMissingBytes);
        if (stack.get() != WrappedImmutableItemStack.AIR) {
            try {
                CodecTypeValidation.ITEM_STACK.validate(stack.get(), CodecContext.from(this, protocol, protocolCreated));
            } catch (DecoderException ex) {
                throw new StackDecodingException(ex);
            }
        }

        return stack;
    }

    public Rewritable<WrappedImmutableItemStack> readItemStackSafely(PacketContainer<?> packet, int maxMissingBytes) throws StackDecodingException {
        return readItemStackSafely(CodecVisitor.Dynamic.IN_DEFAULT, packet.getProtocol(), packet.getProtocolCreated(), maxMissingBytes);
    }

    public Rewritable<WrappedImmutableItemStack> readItemStackSafely(PacketContainer<?> packet) throws StackDecodingException {
        return readItemStackSafely(packet, -1);
    }

    public <K, V> Map<K, V> readMap(Supplier<K> keyFunction, Supplier<V> valueFunction) {
        int size = readVarInt();
        Map<K, V> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            K key = keyFunction.get();
            V value = valueFunction.get();
            map.put(key, value);
        }
        return map;
    }

    public <K, V> void writeMap(Map<K, V> map, Consumer<K> keyConsumer, Consumer<V> valueConsumer) {
        writeVarInt(map.size());
        for (Map.Entry<K, V> entry : map.entrySet()) {
            K key = entry.getKey();
            V value = entry.getValue();
            keyConsumer.accept(key);
            valueConsumer.accept(value);
        }
    }

    public UUID readUUID() {
        return new UUID(this.readLong(), this.readLong());
    }

    public String readString() {
        return readString(ConfigurationOptions.ItemAnalysis.NBT.MAX_NBT_STRING_LENGTH);
    }

    public String readString(int i) {
        return Types.UTF8.read(this, TypeSerializer.TypeContext.OneArg.of(i));
    }

    public PacketDataSerializer writeString(String val) {
        return writeString(val, Short.MAX_VALUE);
    }

    public PacketDataSerializer writeString(String val, int max) {
        return Types.UTF8.write(this, val, TypeSerializer.TypeContext.OneArg.of(max));
    }

    public byte[] readByteArray(int maxLength) {
        int len = this.readVarInt();
        if (len > maxLength) {
            throw new RuntimeException("The received byte array length is longer than maximum allowed (" + len + " > " + maxLength + ")");
        } else {
            return this.readBytes(len).array();
        }
    }

    public int capacity() {
        return this.delegate.capacity();
    }

    public PacketDataSerializer capacity(int i) {
        this.delegate.capacity(i);
        return this;
    }

    public int maxCapacity() {
        return this.delegate.maxCapacity();
    }

    public ByteBufAllocator alloc() {
        return this.delegate.alloc();
    }

    public ByteOrder order() {
        return this.delegate.order();
    }

    public PacketDataSerializer order(ByteOrder byteorder) {
        this.delegate.order(byteorder);
        return this;
    }

    public PacketDataSerializer unwrap() {
        this.delegate.unwrap();
        return this;
    }

    public boolean isDirect() {
        return this.delegate.isDirect();
    }

    public int readerIndex() {
        return this.delegate.readerIndex();
    }

    public PacketDataSerializer readerIndex(int i) {
        this.delegate.readerIndex(i);
        return this;
    }

    public int writerIndex() {
        return this.delegate.writerIndex();
    }

    public PacketDataSerializer writerIndex(int i) {
        this.delegate.writerIndex(i);
        return this;
    }

    public PacketDataSerializer setIndex(int i, int j) {
        this.delegate.setIndex(i, j);
        return this;
    }

    public int readableBytes() {
        return this.delegate.readableBytes();
    }

    public int writableBytes() {
        return this.delegate.writableBytes();
    }

    public int maxWritableBytes() {
        return this.delegate.maxWritableBytes();
    }

    public boolean isReadable() {
        return this.delegate.isReadable();
    }

    public boolean isReadable(int i) {
        return this.delegate.isReadable(i);
    }

    public boolean isWritable() {
        return this.delegate.isWritable();
    }

    public boolean isWritable(int i) {
        return this.delegate.isWritable(i);
    }

    public PacketDataSerializer clear() {
        this.delegate.clear();
        return this;
    }

    public PacketDataSerializer markReaderIndex() {
        this.delegate.markReaderIndex();
        return this;
    }

    public PacketDataSerializer resetReaderIndex() {
        this.delegate.resetReaderIndex();
        return this;
    }

    public PacketDataSerializer markWriterIndex() {
        this.delegate.markWriterIndex();
        return this;
    }

    public PacketDataSerializer resetWriterIndex() {
        this.delegate.resetWriterIndex();
        return this;
    }

    public PacketDataSerializer discardReadBytes() {
        this.delegate.discardReadBytes();
        return this;
    }

    public PacketDataSerializer discardSomeReadBytes() {
        this.delegate.discardSomeReadBytes();
        return this;
    }

    public PacketDataSerializer ensureWritable(int i) {
        this.delegate.ensureWritable(i);
        return this;
    }

    public int ensureWritable(int i, boolean flag) {
        return this.delegate.ensureWritable(i, flag);
    }

    public boolean getBoolean(int i) {
        return this.delegate.getBoolean(i);
    }

    public byte getByte(int i) {
        return this.delegate.getByte(i);
    }

    public short getUnsignedByte(int i) {
        return this.delegate.getUnsignedByte(i);
    }

    public short getShort(int i) {
        return this.delegate.getShort(i);
    }

    public int getUnsignedShort(int i) {
        return this.delegate.getUnsignedShort(i);
    }

    public int getMedium(int i) {
        return this.delegate.getMedium(i);
    }

    public int getUnsignedMedium(int i) {
        return this.delegate.getUnsignedMedium(i);
    }

    public int getInt(int i) {
        return this.delegate.getInt(i);
    }

    public long getUnsignedInt(int i) {
        return this.delegate.getUnsignedInt(i);
    }

    public long getLong(int i) {
        return this.delegate.getLong(i);
    }

    public char getChar(int i) {
        return this.delegate.getChar(i);
    }

    public float getFloat(int i) {
        return this.delegate.getFloat(i);
    }

    public double getDouble(int i) {
        return this.delegate.getDouble(i);
    }

    public ByteBuf getBytes(int i, ByteBuf bytebuf) {
        return this.delegate.getBytes(i, bytebuf);
    }

    public ByteBuf getBytes(int i, ByteBuf bytebuf, int j) {
        return this.delegate.getBytes(i, bytebuf, j);
    }

    public ByteBuf getBytes(int i, ByteBuf bytebuf, int j, int k) {
        return this.delegate.getBytes(i, bytebuf, j, k);
    }

    public ByteBuf getBytes(int i, byte[] abyte) {
        return this.delegate.getBytes(i, abyte);
    }

    public ByteBuf getBytes(int i, byte[] abyte, int j, int k) {
        return this.delegate.getBytes(i, abyte, j, k);
    }

    public ByteBuf getBytes(int i, ByteBuffer bytebuffer) {
        return this.delegate.getBytes(i, bytebuffer);
    }

    public ByteBuf getBytes(int i, OutputStream outputstream, int j) throws IOException {
        return this.delegate.getBytes(i, outputstream, j);
    }

    public int getBytes(int i, GatheringByteChannel gatheringbytechannel, int j) throws IOException {
        return this.delegate.getBytes(i, gatheringbytechannel, j);
    }

    public PacketDataSerializer setBoolean(int i, boolean flag) {
        this.delegate.setBoolean(i, flag);
        return this;
    }

    public PacketDataSerializer setByte(int i, int j) {
        this.delegate.setByte(i, j);
        return this;
    }

    public PacketDataSerializer setShort(int i, int j) {
        this.delegate.setShort(i, j);
        return this;
    }

    public PacketDataSerializer setMedium(int i, int j) {
        this.delegate.setMedium(i, j);
        return this;
    }

    public PacketDataSerializer setInt(int i, int j) {
        this.delegate.setInt(i, j);
        return this;
    }

    public PacketDataSerializer setLong(int i, long j) {
        this.delegate.setLong(i, j);
        return this;
    }

    public PacketDataSerializer setChar(int i, int j) {
        this.delegate.setChar(i, j);
        return this;
    }

    public PacketDataSerializer setFloat(int i, float f) {
        this.delegate.setFloat(i, f);
        return this;
    }

    public PacketDataSerializer setDouble(int i, double d0) {
        this.delegate.setDouble(i, d0);
        return this;
    }

    public PacketDataSerializer setBytes(int i, ByteBuf bytebuf) {
        this.delegate.setBytes(i, bytebuf);
        return this;
    }

    public PacketDataSerializer setBytes(int i, ByteBuf bytebuf, int j) {
        this.delegate.setBytes(i, bytebuf, j);
        return this;
    }

    public PacketDataSerializer setBytes(int i, ByteBuf bytebuf, int j, int k) {
        this.delegate.setBytes(i, bytebuf, j, k);
        return this;
    }

    public PacketDataSerializer setBytes(int i, byte[] abyte) {
        this.delegate.setBytes(i, abyte);
        return this;
    }

    public PacketDataSerializer setBytes(int i, byte[] abyte, int j, int k) {
        this.delegate.setBytes(i, abyte, j, k);
        return this;
    }

    public PacketDataSerializer setBytes(int i, ByteBuffer bytebuffer) {
        this.delegate.setBytes(i, bytebuffer);
        return this;
    }

    public int setBytes(int i, InputStream inputstream, int j) throws IOException {
        return this.delegate.setBytes(i, inputstream, j);
    }

    public int setBytes(int i, ScatteringByteChannel scatteringbytechannel, int j) throws IOException {
        return this.delegate.setBytes(i, scatteringbytechannel, j);
    }

    public PacketDataSerializer setZero(int i, int j) {
        this.delegate.setZero(i, j);
        return this;
    }

    public boolean readBoolean() {
        return this.delegate.readBoolean();
    }

    public byte readByte() {
        return this.delegate.readByte();
    }

    public short readUnsignedByte() {
        return this.delegate.readUnsignedByte();
    }

    public short readShort() {
        return this.delegate.readShort();
    }

    public int readUnsignedShort() {
        return this.delegate.readUnsignedShort();
    }

    public int readMedium() {
        return this.delegate.readMedium();
    }

    public int readUnsignedMedium() {
        return this.delegate.readUnsignedMedium();
    }

    public int readInt() {
        return this.delegate.readInt();
    }

    public long readUnsignedInt() {
        return this.delegate.readUnsignedInt();
    }

    public long readLong() {
        return this.delegate.readLong();
    }

    public char readChar() {
        return this.delegate.readChar();
    }

    public float readFloat() {
        return this.delegate.readFloat();
    }

    public double readDouble() {
        return this.delegate.readDouble();
    }

    public ByteBuf readBytes(int i) {
        return this.delegate.readBytes(i);
    }

    public ByteBuf readSlice(int i) {
        return this.delegate.readSlice(i);
    }

    public ByteBuf readBytes(ByteBuf bytebuf) {
        return this.delegate.readBytes(bytebuf);
    }

    public ByteBuf readBytes(ByteBuf bytebuf, int i) {
        return this.delegate.readBytes(bytebuf, i);
    }

    public ByteBuf readBytes(ByteBuf bytebuf, int i, int j) {
        return this.delegate.readBytes(bytebuf, i, j);
    }

    public ByteBuf readBytes(byte[] abyte) {
        return this.delegate.readBytes(abyte);
    }

    public ByteBuf readBytes(byte[] abyte, int i, int j) {
        return this.delegate.readBytes(abyte, i, j);
    }

    public ByteBuf readBytes(ByteBuffer bytebuffer) {
        return this.delegate.readBytes(bytebuffer);
    }

    public ByteBuf readBytes(OutputStream outputstream, int i) throws IOException {
        return this.delegate.readBytes(outputstream, i);
    }

    public int readBytes(GatheringByteChannel gatheringbytechannel, int i) throws IOException {
        return this.delegate.readBytes(gatheringbytechannel, i);
    }

    public PacketDataSerializer skipBytes(int i) {
        this.delegate.skipBytes(i);
        return this;
    }

    public PacketDataSerializer writeBoolean(boolean flag) {
        this.delegate.writeBoolean(flag);
        return this;
    }

    public PacketDataSerializer writeByte(int i) {
        this.delegate.writeByte(i);
        return this;
    }

    public PacketDataSerializer writeShort(int i) {
        this.delegate.writeShort(i);
        return this;
    }

    public PacketDataSerializer writeMedium(int i) {
        this.delegate.writeMedium(i);
        return this;
    }

    public PacketDataSerializer writeInt(int i) {
        this.delegate.writeInt(i);
        return this;
    }

    public PacketDataSerializer writeLong(long i) {
        this.delegate.writeLong(i);
        return this;
    }

    public PacketDataSerializer writeChar(int i) {
        this.delegate.writeChar(i);
        return this;
    }

    public PacketDataSerializer writeFloat(float f) {
        this.delegate.writeFloat(f);
        return this;
    }

    public PacketDataSerializer writeDouble(double d0) {
        this.delegate.writeDouble(d0);
        return this;
    }

    public PacketDataSerializer writeBytes(ByteBuf bytebuf) {
        this.delegate.writeBytes(bytebuf);
        return this;
    }

    public PacketDataSerializer writeBytes(ByteBuf bytebuf, int i) {
        this.delegate.writeBytes(bytebuf, i);
        return this;
    }

    public PacketDataSerializer writeBytes(ByteBuf bytebuf, int i, int j) {
        this.delegate.writeBytes(bytebuf, i, j);
        return this;
    }

    public PacketDataSerializer writeBytes(byte[] abyte) {
        this.delegate.writeBytes(abyte);
        return this;
    }

    public PacketDataSerializer writeBytes(byte[] abyte, int i, int j) {
        this.delegate.writeBytes(abyte, i, j);
        return this;
    }

    public PacketDataSerializer writeBytes(ByteBuffer bytebuffer) {
        this.delegate.writeBytes(bytebuffer);
        return this;
    }

    public int writeBytes(InputStream inputstream, int i) throws IOException {
        return this.delegate.writeBytes(inputstream, i);
    }

    public int writeBytes(ScatteringByteChannel scatteringbytechannel, int i) throws IOException {
        return this.delegate.writeBytes(scatteringbytechannel, i);
    }

    public PacketDataSerializer writeZero(int i) {
        this.delegate.writeZero(i);
        return this;
    }

    public int indexOf(int i, int j, byte b0) {
        return this.delegate.indexOf(i, j, b0);
    }

    public int bytesBefore(byte b0) {
        return this.delegate.bytesBefore(b0);
    }

    public int bytesBefore(int i, byte b0) {
        return this.delegate.bytesBefore(i, b0);
    }

    public int bytesBefore(int i, int j, byte b0) {
        return this.delegate.bytesBefore(i, j, b0);
    }

    @Override
    public int forEachByte(ByteBufProcessor byteBufProcessor) {
        return this.delegate.forEachByte(byteBufProcessor);
    }

    @Override
    public int forEachByte(int i, int i1, ByteBufProcessor byteBufProcessor) {
        return this.delegate.forEachByte(i, i1, byteBufProcessor);
    }

    @Override
    public int forEachByteDesc(ByteBufProcessor byteBufProcessor) {
        return this.delegate.forEachByteDesc(byteBufProcessor);
    }

    @Override
    public int forEachByteDesc(int i, int i1, ByteBufProcessor byteBufProcessor) {
        return this.delegate.forEachByteDesc(i, i1, byteBufProcessor);
    }

    public ByteBuf copy() {
        return this.delegate.copy();
    }

    public ByteBuf copy(int i, int j) {
        return this.delegate.copy(i, j);
    }

    public ByteBuf slice() {
        return this.delegate.slice();
    }

    public ByteBuf slice(int i, int j) {
        return this.delegate.slice(i, j);
    }

    public ByteBuf duplicate() {
        return this.delegate.duplicate();
    }

    public int nioBufferCount() {
        return this.delegate.nioBufferCount();
    }

    public ByteBuffer nioBuffer() {
        return this.delegate.nioBuffer();
    }

    public ByteBuffer nioBuffer(int i, int j) {
        return this.delegate.nioBuffer(i, j);
    }

    public ByteBuffer internalNioBuffer(int i, int j) {
        return this.delegate.internalNioBuffer(i, j);
    }

    public ByteBuffer[] nioBuffers() {
        return this.delegate.nioBuffers();
    }

    public ByteBuffer[] nioBuffers(int i, int j) {
        return this.delegate.nioBuffers(i, j);
    }

    public boolean hasArray() {
        return this.delegate.hasArray();
    }

    public byte[] array() {
        return this.delegate.array();
    }

    public int arrayOffset() {
        return this.delegate.arrayOffset();
    }

    public boolean hasMemoryAddress() {
        return this.delegate.hasMemoryAddress();
    }

    public long memoryAddress() {
        return this.delegate.memoryAddress();
    }

    public String toString(Charset charset) {
        return this.delegate.toString(charset);
    }

    public String toString(int i, int j, Charset charset) {
        return this.delegate.toString(i, j, charset);
    }

    public int hashCode() {
        return this.delegate.hashCode();
    }

    public boolean equals(Object object) {
        return this.delegate.equals(object);
    }

    public int compareTo(ByteBuf bytebuf) {
        return this.delegate.compareTo(bytebuf);
    }

    public String toString() {
        return this.delegate.toString();
    }

    public PacketDataSerializer retain(int i) {
        this.delegate.retain(i);
        return this;
    }

    public PacketDataSerializer retain() {
        this.delegate.retain();
        return this;
    }

    public int refCnt() {
        return this.delegate.refCnt();
    }

    public boolean release() {
        return this.delegate.release();
    }

    public boolean release(int i) {
        return this.delegate.release(i);
    }
}
