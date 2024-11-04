package me.alek.honey.netty.nbt;

import io.netty.buffer.ByteBufInputStream;
import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.codec.types.Type;
import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;
import me.alek.honey.netty.nbt.array.AbstractArrayTag;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.structure.converters.ListConverter;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.ConstructorInvoker;
import me.alek.honey.util.reflection.ItemReflection;
import me.alek.honey.util.reflection.NMS;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NBTFactory {

    private static final int SIXTY_FOUR_KILOBYTES = 1024 * 64;
    private static final int MAX_DEPTH = 8;
    private static final int MAX_COMPLEXITY = 256;

    public static class ProtocolType implements Type.In<AbstractTag<?, ?, ?>> {

        public static Type.In<AbstractTag<?, ?, ?>> forProtocol(NetProtocol protocol) {
            return new NBTFactory.ProtocolType(protocol);
        }

        private final NetProtocol protocol;

        public ProtocolType(NetProtocol protocol) {
            this.protocol = protocol;
        }

        @Override
        public AbstractTag<?, ?, ?> read(PacketDataSerializer buf, Object... params) {
            try (ByteBufInputStream stream = new ByteBufInputStream(buf)) {
                return NBTFactory.read(stream, protocol);
            } catch (IOException ex) {
                return null;
            }
        }
    }

    private static AbstractTag<?, ?, ?> read(ByteBufInputStream stream, NetProtocol protocol) throws IOException {
        byte id = stream.readByte();
        TagType type = TagType.getFromTypeId(id);
        if (type == TagType.END || type == null) {
            return null;
        }
        if (type != TagType.getFromCollection(TagType.Collection.COMPOUND)) {
            throw new IOException("Expected compound as root tag");
        }

        // named compound tag, skip these bytes
        if (protocol.isOlderThan(NetProtocol.v1_20_2)) {
            stream.skipBytes(stream.readUnsignedShort());
        }

        // deserialize tag
        return type.getCodec().read(new DataInputStream(stream),
                new TagLimiter(SIXTY_FOUR_KILOBYTES, MAX_DEPTH, MAX_COMPLEXITY));
    }

    public static int getAmountOfTags(AbstractTag<?, ?, ?> tag) {
        if (tag instanceof AbstractArrayTag) {
            int i = 0;
            for (AbstractTag<?, ?, ?> tagIn : ((AbstractArrayTag<?, ?>)tag))  {
                i += getAmountOfTags(tagIn);
            }
            return i;
        } else if (tag instanceof CompoundTag) {
            int i = 0;
            for (AbstractTag<?, ?, ?> tagIn : ((CompoundTag)tag).getBases()) {
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
            for (AbstractTag<?, ?, ?> tagIn : ((AbstractArrayTag<?, ?>)tag)) {
                int complexity0 = getComplexityOfTag0(tagIn, complexity + 1);
                if (complexity0 > max) {
                    max = complexity0;
                }
            }
            return max;
        } else if (tag instanceof CompoundTag) {
            int max = complexity;
            for (AbstractTag<?, ?, ?> tagIn : ((CompoundTag)tag).getBases()) {
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

    private static <T extends AbstractTag<?, ?, ?>> void getAllTagsAs0(AbstractTag<?, ?, ?> tag, TagType type, List<T> tags) {
        if (tag instanceof AbstractArrayTag) {
            for (AbstractTag<?, ?, ?> tagIn : ((AbstractArrayTag<?, ?>)tag))  {
                getAllTagsAs0(tagIn, type, tags);
            }
        } else if (tag instanceof CompoundTag) {
            for (AbstractTag<?, ?, ?> tagIn : ((CompoundTag)tag).getBases()) {
                getAllTagsAs0(tagIn, type, tags);
            }
        }

        if (tag.getType() == type) {
            tags.add((T) tag);
        }
    }

    public static CompoundTag fromItem(ItemStack item, boolean createIfNone, boolean forceCreation) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }

        Object nmsItem = ItemReflection.getHandleItem(item);
        if (nmsItem == null) {
            return null;
        }

        ReflectStructure<Object, ?> reflectStructure = ReflectStructure.Provider.acquireStructure(nmsItem.getClass());
        IStructureModifier<AbstractTag<?, ?, ?>> structureModifier = reflectStructure
                .withTypeWithoutCache(NMS.getNBTTagCompoundClass(), Converters.getNBTBaseConverter(forceCreation))
                .withTarget(nmsItem);

        AbstractTag<?, ?, ?> base = structureModifier.readField(0);
        if (base == null && createIfNone) {
            try {
                Object handle = NMS.getNBTTagCompoundClass().getDeclaredConstructor().newInstance();

                CompoundTag nbt = new CompoundTag(handle, forceCreation);
                structureModifier.writeField(0, nbt);
                return nbt;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return (CompoundTag) base;
    }

    public static <T extends AbstractTag<K, V, T>, K, V> T fromDelegate(Object handle, boolean forceCreation) {
        return copyFromType(getType(handle, false, forceCreation), handle, forceCreation);
    }

    public static <T extends AbstractTag<K, V, T>, K, V> T copyBase(T base, boolean forceCreation) {
        return copyFromType(base.getType(), base.getHandle(), forceCreation);
    }

    @SuppressWarnings("unchecked")
    public static <T extends AbstractTag<K, V, T>, K, V> T copyFromType(TagType type, Object handle, boolean forceCreation) {
        if (type == null) {
            return null;
        }

        ConstructorInvoker<? extends AbstractTag<?, ?, ?>> constructorInvoker = type.getWrappedInvoker();
        if (constructorInvoker == null) {
            return null;
        }

        if (type.getCollection() == TagType.Collection.ARRAY) {
            return (T) constructorInvoker.invoke(handle, getArrayType(handle, forceCreation), forceCreation);
        } else {
            return (T) constructorInvoker.invoke(handle, forceCreation);
        }
    }

    public static TagType getArrayType(Object handle, boolean forceCreation) {
        if (handle == null) {
            return null;
        }

        IStructureModifier<Byte> byteModifier = ReflectStructure.Provider.acquireStructure(NMS.getNBTTagListClass())
                .withBytes()
                .withTarget(handle);
        byte typeId = byteModifier.readField(0);
        if (typeId != 0) {
            return TagType.getFromTypeId(typeId);
        }

        IStructureModifier<ListConverter<Object, AbstractTag<?, ?, ?>>> arrayStructure = ReflectStructure.Provider.acquireStructure(handle.getClass())
            .withWeakType(List.class, Converters.<AbstractTag<?, ?, ?>>getNBTListConverter(forceCreation))
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

    public static byte getTypeId(IStructureModifier<Byte> structureModifier) {
        if (structureModifier == null) {
            return -1;
        }

        if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_17)) {
            return structureModifier.invokeMethod(null, "getTypeId", 0);
        } else {
            return structureModifier.invokeMethod(null,
                    (index, invoker) -> invoker.getHandle().getDeclaringClass().equals(NMS.getNBTBaseClass()));
        }
    }

    public static TagType getType(Object handle, boolean ignoreNestedArrays, boolean forceCreation) {
        if (handle == null) {
            return null;
        }

        IStructureModifier<Byte> structureModifier = ReflectStructure.Provider.acquireStructure(NMS.getNBTBaseClass())
                .withBytes()
                .withTarget(handle);

        byte typeId = getTypeId(structureModifier);

        TagType type = TagType.getFromTypeId(typeId);
        if (type != null && type.getCollection() == TagType.Collection.ARRAY && !ignoreNestedArrays) {
            TagType arrayType = getArrayType(handle, forceCreation);
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
}
