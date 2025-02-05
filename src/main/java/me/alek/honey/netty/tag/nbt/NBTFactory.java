package me.alek.honey.netty.tag.nbt;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import me.alek.honey.netty.codec.CodecVisitor;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.codec.types.TypeSerializer;
import me.alek.honey.netty.structure.IStructureModifier;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.structure.converters.ListConverter;
import me.alek.honey.netty.tag.nbt.array.AbstractArrayTag;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.ConstructorInvoker;
import me.alek.honey.util.reflection.ItemFactory;
import me.alek.honey.util.reflection.MinecraftReflection;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class NBTFactory {

    private static final String NAMED_COMPOUND_TAG_NAME = "";

    private static final int SIXTY_FOUR_KILOBYTES = 1024 * 64;
    private static final int MAX_DEPTH = 8;
    private static final int MAX_COMPLEXITY = 256;

    public static class Serializer implements TypeSerializer<AbstractTag<?, ?, ?>, TypeSerializer.TypeContext.OneArg<CodecVisitor>> {

        @Override
        public AbstractTag<?, ?, ?> read(PacketDataSerializer buf, TypeContext.OneArg<CodecVisitor> context) throws DecoderException {
            try (ByteBufInputStream stream = new ByteBufInputStream(buf)) {
                return NBTFactory.read(stream, (TagVisitor) context.get());
            } catch (IOException ex) {
                throw new DecoderException("Failed to read nbt tag", ex);
            }
        }

        @Override
        public PacketDataSerializer write(PacketDataSerializer buf, AbstractTag<?, ?, ?> val, TypeContext.OneArg<CodecVisitor> context) throws EncoderException {
            return write0(buf, val, context);
        }

        @SuppressWarnings("unchecked")
        public <TAG extends AbstractTag<?, ?, TAG>> PacketDataSerializer write0(PacketDataSerializer buf, AbstractTag<?, ?, ?> tag, TypeContext.OneArg<CodecVisitor> context) throws EncoderException {
            try (ByteBufOutputStream stream = new ByteBufOutputStream(buf)) {
                return (PacketDataSerializer) NBTFactory.write(stream, (TAG) tag, (TagVisitor) context.get()).buffer();
            } catch (IOException ex) {
                throw new EncoderException("Failed to write item tag", ex);
            }
        }
    }

    private static AbstractTag<?, ?, ?> read(ByteBufInputStream stream, TagVisitor visitor) throws IOException {
        byte id = stream.readByte();
        TagType type = TagType.getFromTypeId(id);
        if (type != TagType.getFromCollection(TagType.Collection.COMPOUND)) {
            throw new IOException("Expected compound as root tag");
        }
        assert type != null;

        // named compound tag, skip these bytes
        if (visitor.protocol().isOlderThan(NetProtocol.v1_20_2)) {
            stream.skipBytes(stream.readUnsignedShort());
        }

        // deserialize tag
        return type.getCodec().read(new DataInputStream(stream),
                new TagLimiter(SIXTY_FOUR_KILOBYTES, MAX_DEPTH, MAX_COMPLEXITY), visitor);
    }

    private static <TAG extends AbstractTag<?, ?, TAG>> ByteBufOutputStream write(ByteBufOutputStream stream, TAG tag, TagVisitor visitor) throws IOException {
        TagType type = tag.getType();
        if (type != TagType.getFromCollection(TagType.Collection.COMPOUND)) {
            throw new IOException("Expected compound as root tag");
        }
        stream.writeByte(type.getTypeId());

        // named compound tag
        if (visitor.protocol().isOlderThan(NetProtocol.v1_20_2)) {
            stream.writeUTF(NAMED_COMPOUND_TAG_NAME);
        }

        // serialize tag
        type.<TAG>getCodec().write(new DataOutputStream(stream), tag, visitor);
        return stream;
    }

    public static int getAmountOfTags(AbstractTag<?, ?, ?> tag) {
        if (tag instanceof AbstractArrayTag) {
            int i = 0;
            for (AbstractTag<?, ?, ?> tagIn : ((AbstractArrayTag<?, ?, ?>) tag).getValues()) {
                i += getAmountOfTags(tagIn);
            }
            return i;
        } else if (tag instanceof CompoundTag) {
            int i = 0;
            for (AbstractTag<?, ?, ?> tagIn : ((CompoundTag) tag).getBases()) {
                i += getAmountOfTags(tagIn);
            }
            return i;
        } else {
            return 1;
        }
    }

    public static int getComplexityOfTag(AbstractTag<?, ?, ?> tag) {
        return getComplexityOfTag0(tag, 0);
    }

    private static int getComplexityOfTag0(AbstractTag<?, ?, ?> tag, int complexity) {
        if (tag instanceof AbstractArrayTag) {
            int max = complexity;
            for (AbstractTag<?, ?, ?> tagIn : ((AbstractArrayTag<?, ?, ?>) tag).getValues()) {
                int complexity0 = getComplexityOfTag0(tagIn, complexity + 1);
                if (complexity0 > max) {
                    max = complexity0;
                }
            }
            return max;
        } else if (tag instanceof CompoundTag) {
            int max = complexity;
            for (AbstractTag<?, ?, ?> tagIn : ((CompoundTag) tag).getBases()) {
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

    public static <T extends AbstractTag<?, ?, ?>> List<T> getAllTagsAs(AbstractTag<?, ?, ?> tag, TagType type) {
        List<T> tags = new ArrayList<>();
        getAllTagsAs0(tag, type, tags);
        return tags;
    }

    @SuppressWarnings("unchecked")
    private static <T extends AbstractTag<?, ?, ?>> void getAllTagsAs0(AbstractTag<?, ?, ?> tag, TagType type, List<T> tags) {
        if (tag instanceof AbstractArrayTag) {
            for (AbstractTag<?, ?, ?> tagIn : ((AbstractArrayTag<?, ?, ?>) tag).getValues()) {
                getAllTagsAs0(tagIn, type, tags);
            }
        } else if (tag instanceof CompoundTag) {
            for (AbstractTag<?, ?, ?> tagIn : ((CompoundTag) tag).getBases()) {
                getAllTagsAs0(tagIn, type, tags);
            }
        }

        if (tag.getType() == type) {
            tags.add((T) tag);
        }
    }

    private static final ReflectStructure<Object, ?> ITEM_STACK_STRUCTURE = new ReflectStructure<>(MinecraftReflection.getItemStackClass());
    private static final ReflectStructure<Object, AbstractTag<?, ?, ?>> TAG_STRUCTURE = ITEM_STACK_STRUCTURE
            .withType(MinecraftReflection.getNBTTagCompoundClass(), TagConverter.CONVERTER);

    public static CompoundTag fromItem(ItemStack item, boolean createIfNone) {
        if (item.getType() == Material.AIR) {
            return null;
        }

        Object nmsItem;
        try {
            nmsItem = ItemFactory.getHandleItem(item);
            if (nmsItem == null) {
                return null;
            }
        } catch (Exception ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to get handle item", ex);
            return null;
        }

        IStructureModifier<AbstractTag<?, ?, ?>> structureModifier = TAG_STRUCTURE.withTarget(nmsItem);
        AbstractTag<?, ?, ?> base = structureModifier.readField(0);
        if (base == null && createIfNone) {
            try {
                CompoundTag nbt = new CompoundTag();
                structureModifier.writeField(0, nbt);
                return nbt;
            } catch (Exception ex) {
                Bukkit.getLogger().log(Level.SEVERE, "Failed to create tag", ex);
            }
        }

        if (base != null && !base.getType().getCollection().equals(TagType.Collection.COMPOUND)) {
            throw new IllegalStateException("Item with invalid compound tag");
        }

        return (CompoundTag) base;
    }

    public static <T extends AbstractTag<K, V, T>, K, V> T fromDelegate(Object handle) {
        return copyFromType(getType(handle, false), handle);
    }

    public static <T extends AbstractTag<K, V, T>, K, V> T copyBase(T base) {
        return copyFromType(base.getType(), base.getHandle());
    }

    @SuppressWarnings("unchecked")
    public static <T extends AbstractTag<K, V, T>, K, V> T copyFromType(TagType type, Object handle) {
        ConstructorInvoker<? extends AbstractTag<?, ?, ?>> constructorInvoker = type.getBridgeWrapperInvoker();
        if (constructorInvoker == null) {
            return null;
        }

        if (type.getCollection() == TagType.Collection.ARRAY) {
            return (T) constructorInvoker.invoke(handle, getArrayType(handle));
        } else {
            return (T) constructorInvoker.invoke(handle);
        }
    }

    public static TagType getArrayType(Object handle) {
        IStructureModifier<Byte> byteModifier = ReflectStructure.Provider.acquireStructure(MinecraftReflection.getNBTTagListClass())
                .withBytes()
                .withTarget(handle);
        byte typeId = byteModifier.readField(0);
        if (typeId != 0) {
            return TagType.getFromTypeId(typeId);
        }

        IStructureModifier<ListConverter<Object, AbstractTag<?, ?, ?>>> arrayStructure = ReflectStructure.Provider.acquireStructure(handle.getClass())
                .withWeakType(List.class, Converters.<AbstractTag<?, ?, ?>>getNBTListConverter())
                .withTarget(handle);
        List<? extends AbstractTag<?, ?, ?>> array = arrayStructure.readFieldAndApplyMapping(0)
                .getDelegatedList();
        if (array.isEmpty()) {
            return null;
        }

        AbstractTag<?, ?, ?> value = array.get(0);
        if (value == null) {
            return null;
        }

        return value.getType();
    }

    public static TagType getType(Object handle, boolean ignoreNestedArrays) {
        IStructureModifier<Byte> structureModifier = ReflectStructure.Provider.acquireStructure(MinecraftReflection.getNBTBaseClass())
                .withBytes()
                .withTarget(handle);
        byte typeId;
        if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_17)) {
            typeId = structureModifier.invokeMethod(null, "getTypeId", 0);
        } else {
            typeId = structureModifier.invokeMethod(null,
                    (index, invoker) -> invoker.getHandle().getDeclaringClass().equals(MinecraftReflection.getNBTBaseClass()));
        }

        TagType type = TagType.getFromTypeId(typeId);
        if (type != null && type.getCollection() == TagType.Collection.ARRAY && !ignoreNestedArrays) {
            TagType arrayType = getArrayType(handle);
            if (arrayType.hasCollection()) {
                switch (arrayType.getCollection()) {
                    case COMPOUND:
                        return TagType.getFromCollection(TagType.Collection.ARRAY_OF_COMPOUND);
                    case ARRAY:
                        return TagType.getFromCollection(TagType.Collection.ARRAY_OF_ARRAY);
                }
            }
        }

        return type;
    }

    public static AbstractTag<?, ?, ?> createTag(Object value) {
        if (AbstractTag.class.isAssignableFrom(value.getClass())) {
            return (AbstractTag<?, ?, ?>) value;
        }

        TagType type = TagType.getFromClass(value.getClass());
        if (type == null) {
            return null;
        }
        return createTag(type, value);
    }

    public static AbstractTag<?, ?, ?> createTag(TagType type, Object value) {
        if (type.getCollection() == TagType.Collection.ARRAY) {
            List<?> array = (List<?>) value;
            TagType of = null;
            if (!array.isEmpty()) {
                Object first = array.get(0);
                if (List.class.isAssignableFrom(first.getClass())) {
                    type = TagType.getFromCollection(TagType.Collection.ARRAY_OF_ARRAY);
                } else if (Map.class.isAssignableFrom(first.getClass())) {
                    type = TagType.getFromCollection(TagType.Collection.ARRAY_OF_COMPOUND);
                } else {
                    of = TagType.getFromClass(first.getClass());
                }
            }

            if (type.getCollection() == TagType.Collection.ARRAY) {
                return type.getWrapperInvoker().invoke(of, array);
            } else {
                List<?> converted = array.stream()
                        .map(NBTFactory::createTag)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                return type.getWrapperInvoker().invoke(converted);
            }
        } else if (type.getCollection() == TagType.Collection.COMPOUND) {
            return type.getWrapperInvoker().invoke(value);
        } else {
            return type.getWrapperInvoker().invoke(type, value);
        }
    }
}
