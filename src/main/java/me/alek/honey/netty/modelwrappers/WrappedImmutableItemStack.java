package me.alek.honey.netty.modelwrappers;

import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.codec.CodecVisitor;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.codec.Rewritable;
import me.alek.honey.netty.codec.TypeSerializer;
import me.alek.honey.netty.codec.types.Types;
import me.alek.honey.netty.codec.validation.CodecContext;
import me.alek.honey.netty.codec.validation.CodecTypeValidation;
import me.alek.honey.netty.tag.ItemTag;
import me.alek.honey.netty.tag.Visitors;
import me.alek.honey.netty.tag.component.ComponentMap;
import me.alek.honey.netty.tag.component.visitor.ComponentMapVisitor;
import me.alek.honey.netty.tag.nbt.AbstractTag;
import me.alek.honey.netty.tag.nbt.BookTag;
import me.alek.honey.netty.tag.nbt.CompoundTag;
import me.alek.honey.netty.tag.nbt.visitor.NBTVisitor;
import me.alek.honey.netty.tag.rewriter.InTagRewriter;
import me.alek.honey.util.protocol.NetProtocol;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.Optional;

public class WrappedImmutableItemStack implements Cloneable {

    public static class Serializer implements Rewritable.Serializer<WrappedImmutableItemStack, TypeSerializer.TypeContext.TwoArgs<CodecVisitor.Dynamic<?, ?>, CodecContext>> {

        private static final int EMPTY_ID_LEGACY = -1;

        @Override
        public Rewritable<WrappedImmutableItemStack> readRewritable(PacketDataSerializer buf, TypeContext.TwoArgs<CodecVisitor.Dynamic<?, ?>, CodecContext> context) {
            NetProtocol protocol = context.getSecond().protocol();
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
                return WrappedImmutableItemStack.readTag(context.getSecond(), buf, id, amount, durability, context.getFirst());
            } catch (DecoderException | IOException ex) {
                throw new DecoderException("Failed to decode item stack", ex);
            }
        }

        @Override
        public PacketDataSerializer write(PacketDataSerializer buf, WrappedImmutableItemStack stack, TypeContext.TwoArgs<CodecVisitor.Dynamic<?, ?>, CodecContext> context) {
            NetProtocol protocol = context.getSecond().protocol();
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

            WrappedImmutableItemStack.writeTag(context.getFirst(), context.getSecond(), buf, stack);

            return buf;
        }
    }

    static {
        CodecTypeValidation.ITEM_STACK.addValidation(WrappedImmutableItemStack::validate);
        CodecTypeValidation.ITEM_STACK_PRE_TAG.addValidation(WrappedImmutableItemStack::validatePreTag);

        Visitors.load();
    }

    public static void validate(WrappedImmutableItemStack item, CodecContext context) {
        if (item.getType() == ItemTypes.WRITABLE_BOOK || item.getType() == ItemTypes.WRITTEN_BOOK) {
            BookTag.validateBook(item, context);
        }
    }

    public static void validatePreTag(ItemType type, CodecContext.Transformer context) {
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

    public static void writeTag(CodecVisitor.Dynamic<?, ?> visitorFactory, CodecContext context, PacketDataSerializer serializer, WrappedImmutableItemStack stack) {
        writeTag(visitorFactory, context, serializer, stack.getType(), stack.getTag());
    }

    public static void writeTag(CodecVisitor.Dynamic<?, ?> visitorFactory, CodecContext context, PacketDataSerializer serializer, ItemType type, ItemTag tag) {
        NetProtocol protocol = context.protocol();
        NetProtocol protocolCreated = context.protocolCreated();

        if (visitorFactory.replaceByRegistered()) {
            CodecVisitor.Dynamic<?, ?> registryVisitorFactory = CodecVisitor.Dynamic.OUT_REGISTRY.get(type);
            if (registryVisitorFactory != null) {
                visitorFactory = registryVisitorFactory;
            }
        }
        CodecVisitor visitor = visitorFactory.create(protocol, protocolCreated, type);

        if (protocol.isNewerThanOrEqual(NetProtocol.v1_20_5)) {
            Types.COMPONENT_MAP.write(serializer, tag.asComponentMap().orElse(ComponentMap.EMPTY), TypeSerializer.TypeContext.OneArg.of((ComponentMapVisitor) visitor));
        } else {
            serializer.writeNBT((NBTVisitor.Out) visitor, tag.asNBT().get(), true);
        }
    }

    public static Rewritable<WrappedImmutableItemStack> readTag(CodecContext context, PacketDataSerializer serializer, int itemId, int amount, short durability, CodecVisitor.Dynamic<?, ?> visitorFactory) throws IOException {
        NetProtocol protocol = context.protocol();
        NetProtocol protocolCreated = context.protocolCreated();
        ItemType type = ItemTypes.getById(protocol.toPECVersion(), itemId);
        if (type == null) {
            return null; // no type found
        }

        if (context instanceof CodecContext.Transformer) {
            CodecTypeValidation.ITEM_STACK_PRE_TAG.validate(type, (CodecContext.Transformer) context);
        }

        if (visitorFactory.replaceByRegistered()) {
            CodecVisitor.Dynamic<?, ?> registryVisitorFactory = CodecVisitor.Dynamic.IN_REGISTRY.get(type);
            if (registryVisitorFactory != null) {
                visitorFactory = registryVisitorFactory;
            }
        }
        CodecVisitor visitor = visitorFactory.create(protocol, protocolCreated, type);

        int preTagRidx = serializer.readerIndex();
        ItemTag tag;
        if (protocol.isNewerThanOrEqual(NetProtocol.v1_20_5)) {
            // wrap component map
            tag = Types.COMPONENT_MAP.read(serializer, TypeSerializer.TypeContext.OneArg.of((ComponentMapVisitor) visitor));
        } else {
            // wrap nbt
            tag = Optional.ofNullable(
                            serializer.readNBT((NBTVisitor.In) visitor, true))
                    .map(AbstractTag::asCompound)
                    .orElse(null);
        }

        if (type == ItemTypes.AIR) {
            // done reading this item stack, but just ignore all other components if the type is air
            return Rewritable.noAction(AIR.clone());
        }

        Rewritable.Result<PacketDataSerializer> rewritten;
        if (tag != null && !tag.isEmpty()) {
            // rewrite this tag
            Rewritable<ItemTag> rewritableTag = rewriteTag(context, type, preTagRidx, serializer, tag);
            tag = rewritableTag.get();
            rewritten = rewritableTag.rewritten();
        } else {
            rewritten = Rewritable.Result.noRewritten();
        }

        WrappedImmutableItemStack stack;
        // create stack
        if (durability != -1) {
            // before 1.13
            stack = new WrappedImmutableItemStack(protocol, type, amount, durability, tag);
        } else {
            stack = new WrappedImmutableItemStack(protocol, type, amount, tag);
        }

        return Rewritable.rewritten(stack, rewritten);
    }

    @SuppressWarnings("unchecked")
    public static <TAG extends ItemTag> Rewritable<TAG> rewriteTag(CodecContext context, ItemType type, int preTagRidx, PacketDataSerializer serializer, TAG itemTag) {
        InTagRewriter<TAG> rewriter;
        if (itemTag.asNBT().isPresent()) {
            rewriter = (InTagRewriter<TAG>) InTagRewriter.NBTRewriter.REGISTRY.get(type);
        } else {
            rewriter = (InTagRewriter<TAG>) InTagRewriter.ComponentMapRewriter.REGISTRY.get(type);
        }

        if (rewriter != null) {
            Rewritable.Result<TAG> result = rewriter.rewrite(context.protocol(), InTagRewriter.TagRewritingContext.of(itemTag, context));
            if (result != null && result.isRewritten()) {
                TAG tag = result.get();
                return rewriteTag0(context, tag, type, preTagRidx, serializer);
            }
        }

        return Rewritable.noAction(itemTag);
    }

    public static <TAG extends ItemTag> Rewritable<TAG> rewriteTag0(CodecContext context, TAG tag, ItemType type, int preTagRidx, PacketDataSerializer serializer) {
        // temporarily reserve the end of this serializer
        ByteBuf reserved = serializer.readBytes(serializer.readableBytes());

        // slice the serializer we will rewrite to, and mark the writer index at the end
        PacketDataSerializer rewritten = new PacketDataSerializer();
        rewritten.writeBytes(serializer.slice(0, preTagRidx));

        // write the rewritten tag to the rewritten buffer, and mark the reader index for expanding
        writeTag(CodecVisitor.Dynamic.OUT_DEFAULT, context, rewritten, type, tag);
        rewritten.readerIndex(rewritten.writerIndex());

        // write the reserved bytes to the rewritten buffer
        rewritten.writeBytes(reserved);
        // release the reserved buffer
        reserved.release();

        return Rewritable.rewritten(tag, Rewritable.Result.rewritten(rewritten));
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
