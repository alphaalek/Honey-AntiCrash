package me.alek.honey.netty.structure.converters;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.alek.honey.netty.structure.IStructureModifier;
import me.alek.honey.netty.codec.LegacyProtocol;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.modelwrappers.WrappedChunkCoordinates;
import me.alek.honey.netty.modelwrappers.objects.WrappedRenderData;
import me.alek.honey.netty.modelwrappers.suggestion.WrappedSuggestion;
import me.alek.honey.netty.modelwrappers.suggestion.WrappedSuggestions;
import me.alek.honey.netty.tag.nbt.AbstractTag;
import me.alek.honey.netty.tag.nbt.TagConverter;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.converters.wrapper.SimpleListConverter;
import me.alek.honey.netty.structure.converters.wrapper.SimpleMapConverter;
import me.alek.honey.netty.structure.converters.wrapper.SimpleWrapperConverter;
import me.alek.honey.util.reflection.ConstructorInvoker;
import me.alek.honey.util.reflection.ItemFactory;
import me.alek.honey.util.reflection.NMS;
import me.alek.honey.util.reflection.Reflection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.checkerframework.checker.units.qual.C;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Converters {

    private static final Map<Class<?>, JavaConverter<?, ?>> CONVERTER_CACHE = new HashMap<>();
    private static final Map<Class<?>, ListConverter<?, ?>> LIST_CONVERTER_CACHE = new HashMap<>();
    private static final Map<Class<?>, MapConverter<?, ?, ?>> MAP_CONVERTER_CACHE = new HashMap<>();

    @SuppressWarnings("unchecked")
    private static <R, V> JavaConverter<R, V> getConverter(Class<?> clazz, Function<? super Class<?>, ? extends JavaConverter<?, ?>> orElse) {
        return (JavaConverter<R, V>) CONVERTER_CACHE.computeIfAbsent(clazz, orElse);
    }

    @SuppressWarnings("unchecked")
    private static <R, V, D> MapConverter<R, V, D> getMapConverter(Class<?> clazz, Function<? super Class<?>, ? extends MapConverter<R, V, D>> orElse) {
        return (MapConverter<R, V, D>) MAP_CONVERTER_CACHE.computeIfAbsent(clazz, orElse);
    }

    @SuppressWarnings("unchecked")
    private static <R, V> ListConverter<R, V> getListConverter(Class<?> clazz, Function<? super Class<?>, ? extends ListConverter<R, V>> orElse) {
        return (ListConverter<R, V>) LIST_CONVERTER_CACHE.computeIfAbsent(clazz, orElse);
    }

    public static JavaConverter<Object, WrappedBlockPosition> getBlockPositionConverter() {

        return getConverter(WrappedBlockPosition.class, (clazz) -> new JavaConverter<Object, WrappedBlockPosition>() {
            @Override
            public WrappedBlockPosition convertDelegate(Object delegate, Object... params) {
                if (delegate == null) {
                    return null;
                }

                if (!NMS.isBlockPosition(delegate)) {
                    return null;
                }

                ReflectStructure<Object, ?> blockPositionStructure = ReflectStructure.Provider.acquireStructure(NMS.getBaseBlockPositionClass());
                IStructureModifier<Integer> intStructure = blockPositionStructure.withInts().withTarget(delegate);
                if (intStructure.fieldSize() < 3) {
                    throw new RuntimeException("Reflection error");
                }

                return new WrappedBlockPosition(intStructure.readField(0), intStructure.readField(1), intStructure.readField(2));
            }

            @Override
            public Object convertDeclaring(WrappedBlockPosition blockPosition) {
                if (blockPosition == null) {
                    return null;
                }

                ConstructorInvoker<Object> blockPositionConstructor = Reflection.getConstructor(
                        NMS.getBlockPositionClass(), int.class, int.class, int.class);
                try {
                    return blockPositionConstructor.invoke(
                            blockPosition.getX(), blockPosition.getY(), blockPosition.getZ());
                } catch (Exception ex) {
                    throw new RuntimeException("Reflection error");
                }
            }
        });
    }

    public static JavaConverter<Object, WrappedRenderData> getRenderDataConverter() {
        return getConverter(WrappedRenderData.class, (clazz) -> new SimpleWrapperConverter<>(WrappedRenderData.class));
    }

    public static JavaConverter<Object, PacketDataSerializer> getPacketDataSerializerConverter() {
        return new JavaConverter<Object, PacketDataSerializer>() {
            @Override
            public PacketDataSerializer convertDelegate(Object delegate, Object... params) {
                ByteBuf buf = ReflectStructure.Provider.acquireStructure(NMS.getPacketDataSerializerClass())
                        .withType(ByteBuf.class).withTarget(delegate)
                        .readField(0);
                return new PacketDataSerializer(buf);
            }

            @Override
            public Object convertDeclaring(PacketDataSerializer object) {
                return object.getSerializer();
            }
        };
    }

    public static JavaConverter<Enum<?>, LegacyProtocol> getLegacyProtocolConverter() {
        return getConverter(LegacyProtocol.class, (clazz) -> new SimpleWrapperConverter<>(LegacyProtocol.class, PacketBound.class));
    }

    public static JavaConverter<Object, WrappedSuggestions> getSuggestionsConverter() {
        return getConverter(WrappedSuggestions.class, (clazz) -> new SimpleWrapperConverter<>(WrappedSuggestions.class));
    }

    public static JavaConverter<Object, WrappedSuggestion> getSuggestionConverter() {
        return getConverter(WrappedSuggestion.class, (clazz) -> new SimpleWrapperConverter<>(WrappedSuggestion.class));
    }

    public static MapConverter<String, Object, AbstractTag<?, ?, ?>> getNBTCompoundConverter() {
        return getMapConverterWithoutCache(TagConverter.CONVERTER);
    }

    @SuppressWarnings("unchecked")
    public static <T> ListConverter<Object, T> getNBTListConverter() {
        return (ListConverter<Object, T>) getListConverterWithoutCache(TagConverter.CONVERTER);
    }

    public static <K, T> ListConverter<K, T> getListConverterOf(Class<?> clazz, JavaConverter<K, T> converter) {
        return getListConverter(clazz, (clazz1) -> new SimpleListConverter<>(converter));
    }

    public static <K, T> ListConverter<K, T> getListConverterWithoutCache(JavaConverter<K, T> converter) {
        return new SimpleListConverter<>(converter);
    }

    // maps
    public static <K, T, D> MapConverter<K, T, D> getMapConverterOf(Class<?> clazz, JavaConverter<T, D> converter) {
        return getMapConverter(clazz, (clazz1) -> new SimpleMapConverter<>(converter));
    }

    public static <K, T, D> MapConverter<K, T, D> getMapConverterWithoutCache(JavaConverter<T, D> converter) {
        return new SimpleMapConverter<>(converter);
    }
}
