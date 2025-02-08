package me.alek.honey.netty.modelwrappers;

import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import io.netty.handler.codec.DecoderException;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.codec.CodecVisitor;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.codec.types.TypeSerializer;
import me.alek.honey.netty.codec.types.Types;
import me.alek.honey.netty.codec.validation.CodecTypeValidation;
import me.alek.honey.netty.codec.validation.DecodeContext;
import me.alek.honey.netty.tag.ItemTag;
import me.alek.honey.netty.tag.component.ComponentMap;
import me.alek.honey.netty.tag.nbt.AbstractTag;
import me.alek.honey.netty.tag.nbt.BookTag;
import me.alek.honey.netty.tag.nbt.CompoundTag;
import me.alek.honey.netty.tag.nbt.visitor.Visitors;
import me.alek.honey.util.protocol.NetProtocol;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.Optional;

public class WrappedImmutableItemStack implements Cloneable {

    public static class Serializer implements TypeSerializer<WrappedImmutableItemStack, TypeSerializer.TypeContext.TwoArgs<CodecVisitor, DecodeContext.StackContext>> {

        private static final int EMPTY_ID_LEGACY = -1;

        @Override
        public WrappedImmutableItemStack read(PacketDataSerializer buf, TypeContext.TwoArgs<CodecVisitor, DecodeContext.StackContext> context) {
            NetProtocol protocol = context.getFirst().protocol();
            int amount = -1;
            short durability = -1;
            if (protocol.isNewerThanOrEqual(NetProtocol.v1_20_5)) {
                amount = buf.readVarInt();
                if (amount < 1) {
                    return null;
                }
            } else if (protocol.isNewerThanOrEqual(NetProtocol.v1_13_2)
                    && !buf.readBoolean()) {
                return null;
            }

            int id;
            if (protocol.isNewerThanOrEqual(NetProtocol.v1_13_2)) {
                id = buf.readVarInt();
            } else {
                id = buf.readShort();
            }
            if (id == EMPTY_ID_LEGACY && protocol.isOlderThan(NetProtocol.v1_13_2)) {
                return null;
            }
            if (id < 0) {
                throw new DecoderException("Invalid item id: " + id);
            }

            if (protocol.isOlderThan(NetProtocol.v1_20_5)) {
                amount = buf.readByte();

                if (protocol.isOlderThan(NetProtocol.v1_13_2)) {
                    durability = buf.readShort();
                }
            }

            try {
                return WrappedImmutableItemStack.read(context.getSecond(), buf, id, amount, durability, context.getFirst());
            } catch (Exception ex) {
                throw new DecoderException("Failed to decode item stack", ex);
            }
        }

        @Override
        public PacketDataSerializer write(PacketDataSerializer buf, WrappedImmutableItemStack stack, TypeContext.TwoArgs<CodecVisitor, DecodeContext.StackContext> context) {
            NetProtocol protocol = context.getFirst().protocol();
            if (protocol.isNewerThanOrEqual(NetProtocol.v1_20_5)) {
                buf.writeVarInt(stack.getAmount());
                if (stack.getAmount() < 1) {
                    return buf;
                }
            } else if (protocol.isNewerThanOrEqual(NetProtocol.v1_13_2)) {
                if (stack.getAmount() < 1) {
                    buf.writeBoolean(false);
                    return buf;
                } else {
                    buf.writeBoolean(true);
                }
            } else {
                if (stack.getAmount() < 1) {
                    buf.writeShort(EMPTY_ID_LEGACY);
                    return buf;
                }
            }

            int id = stack.getType().getId(protocol.toPECVersion());
            if (protocol.isNewerThanOrEqual(NetProtocol.v1_13_2)) {
                buf.writeVarInt(id);
            } else {
                buf.writeShort(id);
            }

            if (protocol.isOlderThan(NetProtocol.v1_20_5)) {
                buf.writeByte(stack.getAmount());

                if (protocol.isOlderThan(NetProtocol.v1_13_2)) {
                    buf.writeShort(stack.getDurability());
                }
            }

            CodecVisitor visitor = context.getFirst();
            if (visitor.replacedByRegistried()) {
                CodecVisitor.Factory<?> factory = CodecVisitor.Factory.OUT_REGISTRY.get(stack.getType());
                if (factory != null) {
                    visitor = factory.create(protocol, context.getFirst().protocolCreated(), stack.getType());
                }
            }

            if (protocol.isNewerThanOrEqual(NetProtocol.v1_20_5)) {
                Types.OUT_COMPONENT_MAP.write(buf, stack.getTag().asComponentMap().orElse(ComponentMap.EMPTY), TypeContext.TwoArgs.of(stack.getType(), protocol));
            } else {
                Types.NBT.write(buf, stack.getTag().asNBT().orElse(CompoundTag.EMPTY), TypeContext.OneArg.of(visitor));
            }

            return buf;
        }
    }

    static {
        CodecTypeValidation.ITEM_STACK.addValidation(WrappedImmutableItemStack::validate);
        CodecTypeValidation.ITEM_STACK_PRE_TAG.addValidation(WrappedImmutableItemStack::validatePreTag);

        Visitors.load();
    }

    public static void validate(WrappedImmutableItemStack item, DecodeContext context) {
        if (item.getType() == ItemTypes.WRITABLE_BOOK || item.getType() == ItemTypes.WRITTEN_BOOK) {
            BookTag.validateBook(item, context);
        }
    }

    public static void validatePreTag(ItemType type, DecodeContext.StackContext context) {
        if (context.maxMissingBytes() != -1) {
            int forTag = context.serializer().readableBytes() - context.maxMissingBytes();
            if (type == ItemTypes.WRITABLE_BOOK || type == ItemTypes.WRITTEN_BOOK) {
                if (forTag > ConfigurationOptions.ItemAnalysis.NBT.getMaxBookNBTLength(context.protocolCreated())) {
                    throw new DecoderException("Tried to read a book with too big nbt: " + forTag + " (at max)");
                }
            }

            if (forTag > ConfigurationOptions.ItemAnalysis.NBT.getMaxItemNBTLength(context.protocolCreated())) {
                throw new DecoderException("Tried to read too big nbt: " + forTag + " (at max)");
            }
        }
    }

    public static WrappedImmutableItemStack read(DecodeContext.StackContext context, PacketDataSerializer serializer, int itemId, int amount, short durability, CodecVisitor visitor) throws IOException {
        NetProtocol protocol = context.protocol();
        NetProtocol protocolCreated = context.protocolCreated();
        ItemType type = ItemTypes.getById(protocol.toPECVersion(), itemId);
        if (type == null) {
            return null; // no type found
        }

        if (visitor.replacedByRegistried()) {
            CodecVisitor.Factory<?> factory = CodecVisitor.Factory.IN_REGISTRY.get(type);
            if (factory != null) {
                visitor = factory.create(protocol, protocolCreated, type);
            }
        }

        CodecTypeValidation.ITEM_STACK_PRE_TAG.validate(type, context);

        ItemTag tag;
        if (protocol.isNewerThanOrEqual(NetProtocol.v1_20_5)) {
            NetProtocol componentProtocol;
            if (protocolCreated.isOlderThan(NetProtocol.v1_20_5)) {
                // rewritten by ViaRewind
                componentProtocol = protocol;
            } else {
                componentProtocol = protocolCreated;
            }
            // wrap component map
            tag = Types.IN_COMPONENT_MAP.read(serializer, TypeSerializer.TypeContext.TwoArgs.of(type, componentProtocol));
        } else {
            // wrap nbt
            tag = Optional.ofNullable(
                            Types.NBT.read(serializer, TypeSerializer.TypeContext.OneArg.of(visitor)))
                    .map(AbstractTag::asCompound)
                    .orElse(null);
        }

        if (type == ItemTypes.AIR) {
            // done reading this item stack, but just ignore all other components if the type is air
            return AIR.clone();
        }

        // create stack
        if (durability != -1) {
            // before item update
            return new WrappedImmutableItemStack(protocol, type, amount, durability, tag);
        } else {
            return new WrappedImmutableItemStack(protocol, type, amount, tag);
        }
    }

    public static final WrappedImmutableItemStack AIR = new WrappedImmutableItemStack();

    private final NetProtocol protocol;
    private final ItemType type;
    private ItemTag tag = ItemTag.EMPTY;
    private int amount;
    private short durability = -1; // until 1.13

    public WrappedImmutableItemStack(NetProtocol protocol, ItemType type, int amount, ItemTag tag) {
        this.protocol = protocol;
        this.type = Optional.ofNullable(type).orElse(ItemTypes.AIR);
        this.amount = amount;
        if (tag != null) {
            this.tag = tag;
        }

        if (protocol.isOlderThan(NetProtocol.v1_13_2)) {
            this.durability = 0;
        }
    }

    public WrappedImmutableItemStack(NetProtocol protocol, ItemType type, int amount, short durability, ItemTag tag) {
        this(protocol, type, amount, tag);
        this.durability = durability;

        if (protocol.isNewerThanOrEqual(NetProtocol.v1_13_2)) {
            throw new UnsupportedOperationException();
        }
    }

    private WrappedImmutableItemStack() {
        this.type = ItemTypes.AIR;
        this.protocol = NetProtocol.UNKNOWN;
    }

    public NetProtocol getProtocol() {
        return this.protocol;
    }

    public boolean hasTag() {
        return !this.tag.isEmpty();
    }

    public ItemTag getTag() {
        return this.tag;
    }

    public void setTag(ItemTag tag) {
        this.tag = tag;
        if (this.tag == null) {
            this.tag = ItemTag.EMPTY;
        }
    }

    public int getAmount() {
        return this.amount;
    }

    public ItemType getType() {
        return this.type;
    }

    public short getDurability() {
        if (this.protocol.isNewerThanOrEqual(NetProtocol.v1_13)) {
            throw new UnsupportedOperationException();
        }
        return durability;
    }

    public boolean isSimilar(ItemStack stack) {
        // TODO: fix
        return false;
    }

    public boolean isSimilar(WrappedImmutableItemStack stack) {
        return this.type.equals(stack.getType()) && (!hasTag() && !stack.hasTag() || this.tag.isSimilar(stack.getTag()));
    }

    @Override
    public String toString() {
        // TODO: format
        return super.toString();
    }

    @Override
    public WrappedImmutableItemStack clone() {
        try {
            return (WrappedImmutableItemStack) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
