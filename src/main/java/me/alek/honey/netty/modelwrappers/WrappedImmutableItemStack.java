package me.alek.honey.netty.modelwrappers;

import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import io.netty.handler.codec.DecoderException;
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
import me.alek.honey.netty.tag.nbt.TagVisitor;
import me.alek.honey.util.protocol.NetProtocol;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.Optional;

public class WrappedImmutableItemStack implements Cloneable {

    public static class Serializer implements TypeSerializer<WrappedImmutableItemStack, TypeSerializer.TypeContext.OneArg<CodecVisitor>> {

        @Override
        public WrappedImmutableItemStack read(PacketDataSerializer buf, TypeContext.OneArg<CodecVisitor> context) {
            NetProtocol protocol = context.get().protocol();
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
            if (id < 0) {
                throw new DecoderException("Invalid item id");
            }

            if (protocol.isOlderThan(NetProtocol.v1_20_5)) {
                amount = buf.readByte();

                if (protocol.isOlderThan(NetProtocol.v1_13_2)) {
                    durability = buf.readShort();
                }
            }

            try {
                return WrappedImmutableItemStack.read(protocol, buf, id, amount, durability, context.get());
            } catch (Exception ex) {
                throw new DecoderException("Failed to decode item stack", ex);
            }
        }

        @Override
        public PacketDataSerializer write(PacketDataSerializer buf, WrappedImmutableItemStack stack, TypeContext.OneArg<CodecVisitor> context) {
            NetProtocol protocol = context.get().protocol();
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

            if (protocol.isNewerThanOrEqual(NetProtocol.v1_20_5)) {
                Types.OUT_COMPONENT_MAP.write(buf, stack.getTag().asComponentMap().orElse(ComponentMap.EMPTY), TypeContext.TwoArgs.of(stack.getType(), protocol));
            } else {
                Types.NBT.write(buf, stack.getTag().asNBT().orElse(CompoundTag.EMPTY), TypeContext.OneArg.of(TagVisitor.Out.createDefault(protocol)));
            }

            return buf;
        }
    }

    static {
        CodecTypeValidation.ITEM_STACK.addValidation(WrappedImmutableItemStack::validate);
    }

    public static void validate(WrappedImmutableItemStack item, DecodeContext<?> context) {
        if (item.getType() == ItemTypes.WRITABLE_BOOK || item.getType() == ItemTypes.WRITTEN_BOOK) {
            BookTag.validateBook(item, context);
        }
    }

    public static WrappedImmutableItemStack read(NetProtocol protocol, PacketDataSerializer serializer, int itemId, int amount, short durability, CodecVisitor visitor) throws IOException {
        ItemType type = ItemTypes.getById(protocol.toPECVersion(), itemId);
        if (type == null) {
            return null; // no type found
        }

        ItemTag tag;
        if (protocol.isNewerThanOrEqual(NetProtocol.v1_20_5)) {
            // wrap component map
            tag = Types.IN_COMPONENT_MAP.read(serializer, TypeSerializer.TypeContext.TwoArgs.of(type, protocol));
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
