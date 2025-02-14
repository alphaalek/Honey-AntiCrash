package me.alek.honey.netty.tag.nbt;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.codec.TypeSerializer;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.tag.nbt.visitor.NBTVisitor;
import me.alek.honey.util.protocol.NetProtocol;
import org.bukkit.Bukkit;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NBTFactory {

    public static final String NAMED_COMPOUND_TAG_NAME = "";

    public static class Serializer implements TypeSerializer<AbstractTag<?, ?>, TypeSerializer.TypeContext.TwoArgs<NBTVisitor, Boolean>> {

        @Override
        public AbstractTag<?, ?> read(PacketDataSerializer buf, TypeContext.TwoArgs<NBTVisitor, Boolean> context) throws DecoderException {
            try (ByteBufInputStream stream = new ByteBufInputStream(buf)) {
                if (context.getSecond()) {
                    return NBTFactory.readCompoundTag(stream, (NBTVisitor.In) context.getFirst());
                } else {
                    return NBTFactory.readTag(stream, (NBTVisitor.In) context.getFirst());
                }
            } catch (DecoderException | IOException ex) {
                throw new DecoderException("Failed to read nbt tag", ex);
            }
        }

        @Override
        public PacketDataSerializer write(PacketDataSerializer buf, AbstractTag<?, ?> val, TypeContext.TwoArgs<NBTVisitor, Boolean> context) throws EncoderException {
            return write0(buf, val, context);
        }

        @SuppressWarnings("unchecked")
        public <TAG extends AbstractTag<?, TAG>> PacketDataSerializer write0(PacketDataSerializer buf, AbstractTag<?, ?> tag, TypeContext.TwoArgs<NBTVisitor, Boolean> context) throws EncoderException {
            try (ByteBufOutputStream stream = new ByteBufOutputStream(buf)) {
                if (context.getSecond()) {
                    return (PacketDataSerializer) NBTFactory.writeCompoundTag(stream, (CompoundTag) tag, (NBTVisitor.Out) context.getFirst()).buffer();
                } else {
                    return (PacketDataSerializer) NBTFactory.writeTag(stream, (TAG) tag, (NBTVisitor.Out) context.getFirst()).buffer();
                }
            } catch (DecoderException | IOException ex) {
                throw new EncoderException("Failed to write item tag", ex);
            }
        }
    }

    private static CompoundTag readCompoundTag(ByteBufInputStream stream, NBTVisitor.In visitor) throws IOException {
        byte id = stream.readByte();
        TagType type = TagType.getFromTypeId(id);
        if (type == TagType.END) {
            return null;
        }
        if (type != CompoundTag.TYPE.get()) {
            throw new IOException("Expected compound as root tag");
        }
        assert type != null;

        // named compound tag, skip these bytes
        if (visitor.protocol().isOlderThan(NetProtocol.v1_20_2)) {
            stream.skipBytes(stream.readUnsignedShort());
        }

        return readTag(stream, visitor, type);
    }

    private static AbstractTag<?, ?> readTag(ByteBufInputStream stream, NBTVisitor.In visitor) throws IOException {
        stream.mark(0);
        byte id = stream.readByte();
        TagType type = TagType.getFromTypeId(id);
        if (type == TagType.END) {
            return null;
        }
        assert type != null;

        if (type == CompoundTag.TYPE.get()) {
            stream.reset();
            return readCompoundTag(stream, visitor);
        } else {
            return readTag(stream, visitor, type);
        }
    }

    @SuppressWarnings("unchecked")
    private static <TAG extends AbstractTag<?, TAG>> TAG readTag(ByteBufInputStream stream, NBTVisitor.In visitor, TagType type) throws IOException {
        return (TAG) type.getCodec().read(new DataInputStream(stream), visitor.limiter(), visitor);
    }

    public static TagLimiter createDefaultTagLimiter() {
        return new TagLimiter.Default(
                TagLimiter.THIRTY_TWO_KILOBYTES,
                ConfigurationOptions.ItemAnalysis.NBT.MAX_NBT_NESTING,
                ConfigurationOptions.ItemAnalysis.NBT.MAX_NBT_COMPLEXITY,
                ConfigurationOptions.ItemAnalysis.NBT.MAX_NBT_BASES);
    }

    private static ByteBufOutputStream writeCompoundTag(ByteBufOutputStream stream, CompoundTag tag, NBTVisitor.Out visitor) throws IOException {
        stream.writeByte(CompoundTag.TYPE.get().getId());

        // named compound tag
        if (visitor.protocol().isOlderThan(NetProtocol.v1_20_2)) {
            stream.writeUTF(NAMED_COMPOUND_TAG_NAME);
        }

        // serialize tag
        CompoundTag.TYPE.get().<CompoundTag>getCodec().write(new DataOutputStream(stream), tag, visitor);
        return stream;
    }

    private static <TAG extends AbstractTag<?, TAG>> ByteBufOutputStream writeTag(ByteBufOutputStream stream, TAG tag, NBTVisitor.Out visitor) throws IOException {
        TagType type = tag.getType();
        if (type == CompoundTag.TYPE.get()) {
            return writeCompoundTag(stream, (CompoundTag) tag, visitor);
        }
        stream.writeByte(type.getId());

        // serialize tag
        type.<TAG>getCodec().write(new DataOutputStream(stream), tag, visitor);
        return stream;
    }

    public static int getAmountOfTags(AbstractTag<?, ?> tag) {
        if (tag instanceof ArrayTag) {
            int i = 0;
            for (AbstractTag<?, ?> tagIn : ((ArrayTag<?, ?, ?>) tag).getTags()) {
                i += getAmountOfTags(tagIn);
            }
            return i;
        } else if (tag instanceof CompoundTag) {
            int i = 0;
            for (AbstractTag<?, ?> tagIn : ((CompoundTag) tag).getValues()) {
                i += getAmountOfTags(tagIn);
            }
            return i;
        } else {
            return 1;
        }
    }

    public static int getComplexityOfTag(AbstractTag<?, ?> tag) {
        return getComplexityOfTag0(tag, 0);
    }

    private static int getComplexityOfTag0(AbstractTag<?, ?> tag, int complexity) {
        if (tag instanceof ArrayTag) {
            int max = complexity;
            for (AbstractTag<?, ?> tagIn : ((ArrayTag<?, ?, ?>) tag).getTags()) {
                int complexity0 = getComplexityOfTag0(tagIn, complexity + 1);
                if (complexity0 > max) {
                    max = complexity0;
                }
            }
            return max;
        } else if (tag instanceof CompoundTag) {
            int max = complexity;
            for (AbstractTag<?, ?> tagIn : ((CompoundTag) tag).getValues()) {
                int complexity0 = getComplexityOfTag0(tagIn, complexity + 1);
                if (complexity0 > max) {
                    max = complexity0;
                }
            }
            return max;
        } else {
            return complexity;
        }
    }

    public static <T extends AbstractTag<?, ?>> List<T> getAllTagsAs(AbstractTag<?, ?> tag, TagType type) {
        List<T> tags = new ArrayList<>();
        getAllTagsAs0(tag, type, tags);
        return tags;
    }

    @SuppressWarnings("unchecked")
    private static <T extends AbstractTag<?, ?>> void getAllTagsAs0(AbstractTag<?, ?> tag, TagType type, List<T> tags) {
        if (tag instanceof ArrayTag) {
            for (AbstractTag<?, ?> tagIn : ((ArrayTag<?, ?, ?>) tag).getTags()) {
                getAllTagsAs0(tagIn, type, tags);
            }
        } else if (tag instanceof CompoundTag) {
            for (AbstractTag<?, ?> tagIn : ((CompoundTag) tag).getValues()) {
                getAllTagsAs0(tagIn, type, tags);
            }
        }

        if (tag.getType() == type) {
            tags.add((T) tag);
        }
    }

    public static Object toLiteral(AbstractTag<?, ?> tag) {
        TagType type = tag.getType();
        if (type == TagType.ARRAY_LEGACY || type == TagType.ARRAY_MODERN) {
            ArrayTag<?, ?, ?> array = tag.asArray();
            List<Object> tags = array.getTags().stream()
                    .map(NBTFactory::toLiteral)
                    .collect(Collectors.toList());

            if (type == TagType.ARRAY_LEGACY) {
                Object literal = type.getInternalInvoker().invoke();

                ReflectStructure<?, ?> structure = ReflectStructure.Provider.acquireStructure(literal.getClass());
                structure.withType(List.class).withTarget(literal).writeField(0, tags);
                structure.withBytes().withTarget(literal).writeField(0, array.getTypeOf().getId());

                return literal;
            } else {
                return type.getInternalInvoker().invoke(tags, array.getTypeOf().getId());
            }
        } else if (type == TagType.COMPOUND_LEGACY || type == TagType.COMPOUND_MODERN) {
            CompoundTag compound = tag.asCompound();
            Map<String, Object> tags = compound.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> toLiteral(entry.getValue())));

            if (type == TagType.COMPOUND_LEGACY) {
                Object literal = type.getInternalInvoker().invoke();

                ReflectStructure.Provider.acquireStructure(literal.getClass())
                        .withType(Map.class).withTarget(literal)
                        .writeField(0, tags);

                return literal;
            } else {
                return type.getInternalInvoker().invoke(tags);
            }
        } else if (type == TagType.END) {
            return type.getInternalInvoker().invoke();
        } else {
            return type.getInternalInvoker().invoke(tag.asPrimitive().getValue());
        }
    }

    public static AbstractTag<?, ?> fromLiteral(Object literal) {
        TagType type = TagType.getFromInternalClass(literal.getClass());
        if (type == null) {
            return null; // not a nbt tag
        }

        Object val = ReflectStructure.Provider.acquireStructure(literal.getClass())
                .withType(type.getDataTypeClass()).withTarget(literal)
                .readField(0);
        if (val == null) {
            return null; // no value, and we don't want tags with null value
        }

        return createTagOfExpectedType(type, val);
    }

    public static AbstractTag<?, ?> createTag(Object value) {
        if (AbstractTag.class.isAssignableFrom(value.getClass())) {
            return (AbstractTag<?, ?>) value;
        }

        TagType type = TagType.getFromClass(value.getClass());
        if (type == null) {
            return null;
        } else {
            return createTag(type, value);
        }
    }

    public static AbstractTag<?, ?> createTagOfExpectedType(TagType type, Object value) {
        if (AbstractTag.class.isAssignableFrom(value.getClass())) {
            return (AbstractTag<?, ?>) value;
        }

        TagType got = TagType.getFromClass(value.getClass());
        if (got == null || !got.equals(type)) {
            return null;
        } else {
            return createTag(type, value);
        }
    }

    @SuppressWarnings("unchecked")
    public static AbstractTag<?, ?> createTag(TagType type, Object value) {
        if (type == ArrayTag.TYPE.get()) {
            List<?> array = (List<?>) value;
            TagType of = null;
            if (!array.isEmpty()) {
                Object first = array.get(0);
                of = TagType.getFromClass(first.getClass());
            }
            if (of == null) {
                of = TagType.END;
            }

            if (of == CompoundTag.TYPE.get() || of == ArrayTag.TYPE.get()) {
                return new ArrayTag<>(of, array);
            } else {
                return new PrimitiveArrayTag<>(of, array);
            }
        } else if (type == CompoundTag.TYPE.get()) {
            return new CompoundTag((Map<String, Object>) value);
        } else {
            return new PrimitiveTag<>(type, value);
        }
    }
}
