package me.alek.honey.netty.tag.component;

import com.github.retrooper.packetevents.protocol.component.ComponentType;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import io.netty.handler.codec.DecoderException;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.codec.types.Type;
import me.alek.honey.netty.tag.ItemTag;
import me.alek.honey.util.protocol.NetProtocol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ComponentMap implements ItemTag {

    public static class ProtocolType implements Type.In<ComponentMap, Type.TypeContext.OneArg<ItemType>> {

        public static ComponentMap.ProtocolType forProtocol(NetProtocol protocol) {
            return new ComponentMap.ProtocolType(protocol);
        }

        private final NetProtocol protocol;

        public ProtocolType(NetProtocol protocol) {
            this.protocol = protocol;
        }

        @SuppressWarnings("unchecked")
        @Override
        public ComponentMap read(PacketDataSerializer serializer, TypeContext.OneArg<ItemType> context) {
            int present = serializer.readVarInt();
            int absent = serializer.readVarInt();
            if (present == 0 && absent == 0) {
                return null;
            }
            if (absent < 0 || present < 0) {
                throw new DecoderException("Invalid component map");
            }

            List<ComponentType<?>> types = VersionComponentTypes.getComponents(this.protocol);

            ItemType itemType = context.get();
            List<Val<?>> bases = itemType.getComponents().getDelegate().entrySet().stream()
                    .map(e -> new ComponentMap.Val<>((ComponentType<Object>) e.getKey(), e.getValue(), ComponentSource.BASE))
                    .collect(Collectors.toList());
            if (absent > bases.size()) {
                throw new DecoderException("Too many absent tags");
            }
            if (present > types.size() - bases.size()) {
                throw new DecoderException("Too many present tags");
            }

            PacketWrapper<?> wrapper = PacketWrapper.createUniversalPacketWrapper(serializer);
            wrapper.setClientVersion(this.protocol.toPECVersion());
            wrapper.setServerVersion(this.protocol.toPESVersion());

            ComponentMap map = new ComponentMap(bases);
            for (int i = 0; i < present; i++) {
                ComponentType<Object> type = readType(serializer);
                if (types.contains(type) &&
                        Optional.ofNullable(map.get(type))
                                .map(Val::source)
                                .orElse(ComponentSource.BASE)
                                .equals(ComponentSource.BASE)) {
                    ComponentMap.validateComponentType(type, itemType);

                    Object val = type.read(wrapper);
                    map.set(type, val);
                } else {
                    throw new DecoderException("Invalid present component type");
                }
            }
            for (int i = 0; i < absent; i++) {
                ComponentType<?> type = readType(serializer);
                if (map.has(type) && types.contains(type)) {
                    map.remove(readType(serializer));
                } else {
                    throw new DecoderException("Invalid absent component type");
                }
            }

            ComponentMap.validateComponentMap(map, itemType);

            return map;
        }

        @SuppressWarnings("unchecked")
        private ComponentType<Object> readType(PacketDataSerializer serializer) {
            int id = serializer.readVarInt();
            return (ComponentType<Object>)
                    ComponentTypes.getRegistry().getById(this.protocol.toPECVersion(), id);
        }
    }

    private static final ComponentMap EMPTY = new ComponentMap();

    private final Map<ComponentType<?>, Val<?>> delegate = new HashMap<>();

    public ComponentMap(List<Val<?>> bases) {
        this.delegate.putAll(bases.stream().collect(Collectors.toMap(Val::type, v -> v)));
    }

    public ComponentMap() {

    }

    @Override
    public Representation<ComponentMap> asComponentMap() {
        return Representation.as(this);
    }

    @Override
    public boolean isEmpty() {
        return this.delegate.isEmpty();
    }

    @Override
    public int size() {
        return this.delegate.size();
    }

    @SuppressWarnings("unchecked")
    public <T> Val<T> get(ComponentType<T> type) {
        return (Val<T>) this.delegate.get(type);
    }

    @SuppressWarnings("unchecked")
    public <T> T getVal(ComponentType<T> type) {
        Val<T> val = (Val<T>) this.delegate.get(type);
        if (val == null) {
            return null;
        }

        return val.delegate;
    }

    public List<Val<?>> getAllOf(ComponentSource source) {
        return this.delegate.values().stream().filter(v -> v.source.equals(source)).collect(Collectors.toList());
    }

    public List<Val<?>> getAllPatches() {
        return getAllOf(ComponentSource.PATCH);
    }

    public List<Val<?>> getAllBases() {
        return getAllOf(ComponentSource.BASE);
    }

    public <T> void set(ComponentType<T> type, T value) {
        this.delegate.put(type, new Val<>(type, value, ComponentSource.PATCH));
    }

    public boolean has(ComponentType<?> type) {
        return this.delegate.containsKey(type);
    }

    public boolean has(ComponentType<?> type, ComponentSource source) {
        return has(type) && this.delegate.get(type).source().equals(source);
    }

    public void remove(ComponentType<?> type) {
        this.delegate.remove(type);
    }

    public static class Val<T> {

        private final ComponentType<T> type;
        private final T delegate;
        private final ComponentSource source;

        public Val(ComponentType<T> type, T delegate, ComponentSource source) {
            this.type = type;
            this.delegate = delegate;
            this.source = source;
        }

        public ComponentType<T> type() {
            return this.type;
        }

        public T get() {
            return this.delegate;
        }

        public ComponentSource source() {
            return this.source;
        }
    }

    public static void validateComponentMap(ComponentMap map, ItemType type) {
        if (map.has(ComponentTypes.FOOD) && !map.has(ComponentTypes.CONSUMABLE) && !type.hasAttribute(ItemTypes.ItemAttribute.EDIBLE)) {
            throw new IllegalStateException("Illegal food component");
        }

        if (map.has(ComponentTypes.MAX_DAMAGE) && map.has(ComponentTypes.MAX_STACK_SIZE)) {
            throw new IllegalStateException("Illegal combination of max_damage and max_stack_size components");
        }
    }

    public static void validateComponentType(ComponentType<?> componentType, ItemType type) {
        if (componentType.equals(ComponentTypes.BANNER_PATTERNS)
                && !type.equals(ItemTypes.SHIELD)
                && !type.equals(ItemTypes.BLACK_BANNER)
                && !type.equals(ItemTypes.BLUE_BANNER)
                && !type.equals(ItemTypes.BROWN_BANNER)
                && !type.equals(ItemTypes.GRAY_BANNER)
                && !type.equals(ItemTypes.CYAN_BANNER)
                && !type.equals(ItemTypes.GREEN_BANNER)
                && !type.equals(ItemTypes.LIME_BANNER)
                && !type.equals(ItemTypes.LIGHT_BLUE_BANNER)
                && !type.equals(ItemTypes.LIGHT_BLUE_BANNER)
                && !type.equals(ItemTypes.MAGENTA_BANNER)
                && !type.equals(ItemTypes.ORANGE_BANNER)
                && !type.equals(ItemTypes.PINK_BANNER)
                && !type.equals(ItemTypes.PURPLE_BANNER)
                && !type.equals(ItemTypes.RED_BANNER)
                && !type.equals(ItemTypes.WHITE_BANNER)
                && !type.equals(ItemTypes.YELLOW_BANNER)) {
            throw new IllegalStateException("Illegal banner_patterns component");
        }

        if (componentType.equals(ComponentTypes.BASE_COLOR) && !type.equals(ItemTypes.SHIELD)) {
            throw new IllegalStateException("Illegal base_color component");
        }

        if (componentType.equals(ComponentTypes.BEES) && !type.equals(ItemTypes.BEEHIVE) && !type.equals(ItemTypes.BEE_NEST)) {
            throw new IllegalStateException("Illegal bees component");
        }

        if (componentType.equals(ComponentTypes.BUCKET_ENTITY_DATA)
                && !type.equals(ItemTypes.COD_BUCKET)
                && !type.equals(ItemTypes.AXOLOTL_BUCKET)
                && !type.equals(ItemTypes.PUFFERFISH_BUCKET)
                && !type.equals(ItemTypes.SALMON_BUCKET)
                && !type.equals(ItemTypes.TADPOLE_BUCKET)
                && !type.equals(ItemTypes.TROPICAL_FISH_BUCKET)) {
            throw new IllegalStateException("Illegal bucket_entity_data component");
        }

        if (componentType.equals(ComponentTypes.BUNDLE_CONTENTS)
                && !type.equals(ItemTypes.BUNDLE)
                && !type.equals(ItemTypes.BROWN_BUNDLE)
                && !type.equals(ItemTypes.BLACK_BUNDLE)
                && !type.equals(ItemTypes.BLUE_BUNDLE)
                && !type.equals(ItemTypes.CYAN_BUNDLE)
                && !type.equals(ItemTypes.GRAY_BUNDLE)
                && !type.equals(ItemTypes.GREEN_BUNDLE)
                && !type.equals(ItemTypes.LIGHT_BLUE_BUNDLE)
                && !type.equals(ItemTypes.LIGHT_GRAY_BUNDLE)
                && !type.equals(ItemTypes.LIME_BUNDLE)
                && !type.equals(ItemTypes.MAGENTA_BUNDLE)
                && !type.equals(ItemTypes.ORANGE_BUNDLE)
                && !type.equals(ItemTypes.PINK_BUNDLE)
                && !type.equals(ItemTypes.RED_BUNDLE)
                && !type.equals(ItemTypes.WHITE_BUNDLE)
                && !type.equals(ItemTypes.YELLOW_BUNDLE)
                && !type.equals(ItemTypes.PURPLE_BUNDLE)) {
            throw new IllegalStateException("Illegal bundle_contents component");
        }

        if (componentType.equals(ComponentTypes.CHARGED_PROJECTILES) && !type.equals(ItemTypes.CROSSBOW)) {
            throw new IllegalStateException("Illegal charged_projectiles component");
        }

        if (componentType.equals(ComponentTypes.DEBUG_STICK_STATE) && !type.equals(ItemTypes.DEBUG_STICK)) {
            throw new IllegalStateException("Illegal debug_stick component");
        }

        if (componentType.equals(ComponentTypes.DEATH_PROTECTION) && !type.equals(ItemTypes.TOTEM_OF_UNDYING)) {
            throw new IllegalStateException("Illegal death_protection component");
        }

        if (componentType.equals(ComponentTypes.DYED_COLOR)
                && !type.equals(ItemTypes.LEATHER_BOOTS)
                && !type.equals(ItemTypes.LEATHER_LEGGINGS)
                && !type.equals(ItemTypes.LEATHER_CHESTPLATE)
                && !type.equals(ItemTypes.LEATHER_HELMET)
                && !type.equals(ItemTypes.LEATHER_HORSE_ARMOR)) {
            throw new IllegalStateException("Illegal dyed_color component");
        }

        if (componentType.equals(ComponentTypes.FIREWORK_EXPLOSION) && !type.equals(ItemTypes.FIREWORK_STAR)) {
            throw new IllegalStateException("Illegal firework_explosion component");
        }

        if (componentType.equals(ComponentTypes.FIREWORKS) && !type.equals(ItemTypes.FIREWORK_ROCKET)) {
            throw new IllegalStateException("Illegal fireworks component");
        }

        if (componentType.equals(ComponentTypes.INSTRUMENT) && !type.equals(ItemTypes.GOAT_HORN)) {
            throw new IllegalStateException("Illegal instrument component");
        }

        if (componentType.equals(ComponentTypes.LODESTONE_TRACKER) && !type.equals(ItemTypes.COMPASS)) {
            throw new IllegalStateException("Illegal lodestone_tracker component");
        }

        if ((componentType.equals(ComponentTypes.MAP_ID)
                || componentType.equals(ComponentTypes.MAP_COLOR)
                || componentType.equals(ComponentTypes.MAP_DECORATIONS)
                )
                && !type.equals(ItemTypes.MAP) && !type.equals(ItemTypes.FILLED_MAP)) {
            throw new IllegalStateException("Illegal map component");
        }

        if (componentType.equals(ComponentTypes.OMINOUS_BOTTLE_AMPLIFIER) && !type.equals(ItemTypes.OMINOUS_BOTTLE)) {
            throw new IllegalStateException("Illegal ominous_bottle_amplifier component");
        }

        if (componentType.equals(ComponentTypes.POT_DECORATIONS) && !type.equals(ItemTypes.DECORATED_POT)) {
            throw new IllegalStateException("Illegal pot_decorations component");
        }

        if (componentType.equals(ComponentTypes.POTION_CONTENTS)
                && !type.equals(ItemTypes.POTION)
                && !type.equals(ItemTypes.SPLASH_POTION)
                && !type.equals(ItemTypes.LINGERING_POTION)
                && !type.equals(ItemTypes.TIPPED_ARROW)) {
            throw new IllegalStateException("Illegal potion_contents component");
        }

        if (componentType.equals(ComponentTypes.PROFILE) && !type.equals(ItemTypes.PLAYER_HEAD)) {
            throw new IllegalStateException("Illegal profile component");
        }

        if (componentType.equals(ComponentTypes.RECIPES) && !type.equals(ItemTypes.KNOWLEDGE_BOOK)) {
            throw new IllegalStateException("Illegal recipes component");
        }

        if (componentType.equals(ComponentTypes.STORED_ENCHANTMENTS) && !type.equals(ItemTypes.ENCHANTED_BOOK)) {
            throw new IllegalStateException("Illegal stored_enchantments component");
        }

        if (componentType.equals(ComponentTypes.SUSPICIOUS_STEW_EFFECTS) && !type.equals(ItemTypes.SUSPICIOUS_STEW)) {
            throw new IllegalStateException("Illegal suspicious_stew_effects component");
        }

        if (componentType.equals(ComponentTypes.TRIM)
                && !type.equals(ItemTypes.LEATHER_BOOTS)
                && !type.equals(ItemTypes.LEATHER_LEGGINGS)
                && !type.equals(ItemTypes.LEATHER_CHESTPLATE)
                && !type.equals(ItemTypes.LEATHER_HELMET)
                && !type.equals(ItemTypes.IRON_BOOTS)
                && !type.equals(ItemTypes.IRON_LEGGINGS)
                && !type.equals(ItemTypes.IRON_CHESTPLATE)
                && !type.equals(ItemTypes.IRON_HELMET)
                && !type.equals(ItemTypes.CHAINMAIL_BOOTS)
                && !type.equals(ItemTypes.CHAINMAIL_LEGGINGS)
                && !type.equals(ItemTypes.CHAINMAIL_CHESTPLATE)
                && !type.equals(ItemTypes.CHAINMAIL_HELMET)
                && !type.equals(ItemTypes.GOLDEN_BOOTS)
                && !type.equals(ItemTypes.GOLDEN_LEGGINGS)
                && !type.equals(ItemTypes.GOLDEN_CHESTPLATE)
                && !type.equals(ItemTypes.GOLDEN_HELMET)
                && !type.equals(ItemTypes.DIAMOND_BOOTS)
                && !type.equals(ItemTypes.DIAMOND_LEGGINGS)
                && !type.equals(ItemTypes.DIAMOND_CHESTPLATE)
                && !type.equals(ItemTypes.DIAMOND_HELMET)
                && !type.equals(ItemTypes.NETHERITE_BOOTS)
                && !type.equals(ItemTypes.NETHERITE_LEGGINGS)
                && !type.equals(ItemTypes.NETHERITE_CHESTPLATE)
                && !type.equals(ItemTypes.NETHERITE_HELMET)) {
            throw new IllegalStateException("Illegal trim component");
        }

        if (componentType.equals(ComponentTypes.WRITABLE_BOOK_CONTENT) && !type.equals(ItemTypes.WRITABLE_BOOK)) {
            throw new IllegalStateException("Illegal writable_book_content component");
        }

        if (componentType.equals(ComponentTypes.WRITTEN_BOOK_CONTENT) && !type.equals(ItemTypes.WRITTEN_BOOK)) {
            throw new IllegalStateException("Illegal written_book_content component");
        }
    }
}
