package me.alek.honey.configuration;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.item.ItemCheckRegistry;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;

public class ConfigurationOptions {

    private static FileConfiguration file;
    private static Map<PacketTypeEnum, VlSetting> floodSettings = new HashMap<>();
    private static Map<String, VlSetting> channelFloodSettings = new HashMap<>();
    private static Map<Class<? extends VlSetting>, VlSetting> vlSettingMap = new HashMap<>();

    public static void setFile(FileConfiguration file) {
        ConfigurationOptions.file = file;
    }

    public static Map<PacketTypeEnum, VlSetting> getFloodSettings() {
        return floodSettings;
    }

    public static Map<String, VlSetting> getChannelFloodSettings() {
        return channelFloodSettings;
    }

    public static List<Field> getItemAnalysisSettings() {
        List<Field> fields = new ArrayList<>();
        fields.addAll(Arrays.asList(ItemAnalysis.class.getDeclaredFields()));
        fields.addAll(Arrays.asList(ItemAnalysis.NBT.class.getDeclaredFields()));

        return fields;
    }

    public static void load() {
        floodSettings.clear();
        vlSettingMap.clear();
        load(ConfigurationOptions.class);
    }

    public static void load(Class<?> clazz) {
        if (file == null) {
            return;
        }
        if (clazz.isAnnotationPresent(Setting.class)) {
            try {
                clazz.getDeclaredMethod("load").invoke(null);
            } catch (Exception ignored) {
            }
        }
        if (clazz.isAnnotationPresent(Violation.class)) {
            if (VlSetting.class.isAssignableFrom(clazz)) {
                setIntervalBased((Class<? extends VlSetting>) clazz, clazz.getAnnotation(Violation.class).value());
            }
        }
        for (Class<?> nestedClass : clazz.getDeclaredClasses()) {
            if (!nestedClass.isAnnotationPresent(Setting.class)) {
                continue;
            }
            load(nestedClass);
        }
    }

    private static final Map<String, String> vlFieldLookup = new HashMap<String, String>(){{
            put("MAX_PER_INTERVAL", ".max-per-interval");
            put("INTERVAL", ".interval");
            put("VLS_AMOUNT", ".vls-amount");

            put("THRESHOLD", ".threshold");
            put("DECAY", ".decay");
            put ("DECAY_AMOUNT", ".decay-amount");

            put("CLEAR_BUFFER", ".clear-buffer");
            put("ENABLED", ".enabled");
    }};

    public static void setIntervalBased(Class<? extends VlSetting> clazz, String[] prefixPaths) {
        try {
            Constructor<? extends VlSetting> constructor = clazz.getDeclaredConstructor();
            VlSetting setting = constructor.newInstance();
            vlSettingMap.put(clazz, setting);

            if (clazz.isAnnotationPresent(Flood.class)) {
                Flood flood = clazz.getAnnotation(Flood.class);
                for (PacketTypeEnum packetType : flood.value()) {
                    floodSettings.put(packetType, setting);
                }
            }
            if (clazz.isAnnotationPresent(ChannelFlood.class)) {
                ChannelFlood flood = clazz.getAnnotation(ChannelFlood.class);
                channelFloodSettings.put(flood.value(), setting);
            }

            while (true) {
                for (Field field : clazz.getDeclaredFields()) {
                    for (String prefixPath : prefixPaths) {

                        String configPath = vlFieldLookup.get(field.getName());
                        if (configPath == null || file.get(prefixPath + configPath) == null) {
                            continue;
                        }
                        if (file.get(prefixPath + configPath).getClass() == Integer.class) {
                            field.setInt(setting, file.getInt(prefixPath + configPath));
                        } else if (file.get(prefixPath + configPath).getClass() == Boolean.class) {
                            field.setBoolean(setting, file.getBoolean(prefixPath + configPath));
                        }
                    }
                }
                try {
                    if (VlSetting.class.isAssignableFrom(clazz = (Class<? extends VlSetting>) clazz.getSuperclass())) {
                        continue;
                    }
                } catch (Exception ex) {
                }
                return;
            }
        } catch (Exception ex) {
        }
    }

    public static VlSetting getVlSetting(Class<? extends VlSetting> clazz) {
        if (!vlSettingMap.containsKey(clazz)) {
            Bukkit.getLogger().severe("Du har skrevet noget vrøvl i config.yml! Slet den og lad den generere en ny!");
            Bukkit.getPluginManager().disablePlugin(HoneyPlugin.get());
        }
        return vlSettingMap.get(clazz);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    private @interface Setting {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    private @interface Violation {
        String[] value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    private @interface Flood {

        PacketType.Play.Client[] value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    private @interface ChannelFlood {

        String value();

    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ItemAnalysisSetting {

        ItemCheckRegistry value();
    }

    public abstract static class VlSetting implements Cloneable {

        public int MAX_PER_INTERVAL = 1;
        public int INTERVAL;
        public int VLS_AMOUNT = 1;
        public int THRESHOLD;
        public int DECAY;
        public int DECAY_AMOUNT;

        public boolean CLEAR_BUFFER;
        public boolean ENABLED = true;
        private final boolean USE_BUFFER;

        public VlSetting(boolean useBuffer) {
            USE_BUFFER = useBuffer;
        }

        public VlSetting() {
            this(false);
        }
        public int getMaxPerInterval() {
            return MAX_PER_INTERVAL;
        }

        public int getInterval() {
            return INTERVAL;
        }

        public int getVlsAmount() {
            return VLS_AMOUNT;
        }

        public int getThreshold() {
            return THRESHOLD;
        }

        public int getDecay() {
            return DECAY;
        }

        public int getDecayAmount() {
            return DECAY_AMOUNT;
        }

        public boolean isEnabled() {
            return ENABLED;
        }

        public boolean isClearBuffer() {
            return CLEAR_BUFFER;
        }

        public boolean isUseBuffer() {
            return USE_BUFFER;
        }

        @Override
        public VlSetting clone() {
            try {
                return (VlSetting) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }
    }

    @Setting
    public static class General {

        public static int CLEAR_OLD_VIOLATIONS_TIMESTAMP;
        public static String KICK_MESSAGE;
        public static String PREFIX;
        public static boolean BYPASS_PERMISSION_ENABLED;
        public static String BYPASS_PERMISSION;
        public static String NOTIFY_PERMISSION;

        public static void load() {
            CLEAR_OLD_VIOLATIONS_TIMESTAMP = file.getInt("clear-old-violations-timestamp");
            KICK_MESSAGE = file.getString("kick-message");
            PREFIX = file.getString("prefix");

            BYPASS_PERMISSION_ENABLED = file.getBoolean("bypass-permission-enabled");
            BYPASS_PERMISSION = file.getString("bypass-permission");
            NOTIFY_PERMISSION = file.getString("notify-permission");
        }
    }

    @Setting
    public static class PacketLogger {

        public static boolean ENABLED;
        public static boolean DETAILED;
        public static boolean DEBUG;
        public static List<String> IGNORED_PLAYERS;
        public static List<String> IGNORED_PACKETS;

        public static void load() {
            ENABLED = file.getBoolean("packet-logger.enabled");
            DETAILED = file.getBoolean("packet-logger.detailed");
            DEBUG = file.getBoolean("packet-logger.debug-mode");
            IGNORED_PLAYERS = file.getStringList("packet-logger.ignored-players");
            IGNORED_PACKETS = file.getStringList("packet-logger.ignored-packets");
        }
    }

    @Setting
    @Violation("commands")
    public static class Commands extends VlSetting {

        public static List<String> PROHIBITED_TAB_COMPLETE_COMMANDS;
        public static List<String> PROHIBITED_COMMANDS;
        public static List<String> FALLBACK_PREFIXES;

        public static void load() {
            PROHIBITED_TAB_COMPLETE_COMMANDS = file.getStringList("commands.prohibited-tab-complete-commands");
            PROHIBITED_COMMANDS = file.getStringList("commands.prohibited-commands");
            FALLBACK_PREFIXES = file.getStringList("commands.fallback-prefixes");
        }
    }

    @Setting
    public static class ItemAnalysis {

        @ItemAnalysisSetting(ItemCheckRegistry.MAX_CONTAINER_CHECK)
        public static int MAX_CONTAINER_DATA;
        @ItemAnalysisSetting(ItemCheckRegistry.MAX_ENCHANTMENT_LEVEL_CHECK)
        public static int MAX_ENCHANTMENT_LEVEL;
        @ItemAnalysisSetting(ItemCheckRegistry.MAX_AMOUNT_CHECK)
        public static int MAX_AMOUNT;
        @ItemAnalysisSetting(ItemCheckRegistry.MAX_DISPLAYNAME_LENGTH_CHECK)
        public static int MAX_DISPLAYNAME_LENGTH;
        @ItemAnalysisSetting(ItemCheckRegistry.MAX_TOTAL_LORE_LENGTH_CHECK)
        public static int MAX_TOTAL_LORE_LENGTH;
        @ItemAnalysisSetting(ItemCheckRegistry.MAX_FIREWORK_EXPLOSIONS)
        public static int MAX_FIREWORK_EXPLOSIONS;
        @ItemAnalysisSetting(ItemCheckRegistry.MAX_BANNER_PATTERNS)
        public static int MAX_BANNER_PATTERNS;
        @ItemAnalysisSetting(ItemCheckRegistry.CUSTOM_POTION_EFFECTS_CHECK)
        public static boolean CHECK_CUSTOM_POTION_EFFECTS;
        @ItemAnalysisSetting(ItemCheckRegistry.UNBREAKABLE_ITEM_CHECK)
        public static boolean CHECK_UNBREAKABLE_ITEMS;
        @ItemAnalysisSetting(ItemCheckRegistry.ATTRIBUTE_MODIFIERS_CHECK)
        public static boolean CHECK_ATTRIBUTE_MODIFIERS;

        public static void load() {
            MAX_CONTAINER_DATA = file.getInt("item-analysis.max-container-data");

            MAX_ENCHANTMENT_LEVEL = file.getInt("item-analysis.max-enchantment-level");
            MAX_AMOUNT = file.getInt("item-analysis.max-amount");

            MAX_DISPLAYNAME_LENGTH = file.getInt("item-analysis.max-displayname-length");
            MAX_TOTAL_LORE_LENGTH = file.getInt("item-analysis.max-total-lore-length");

            MAX_FIREWORK_EXPLOSIONS = file.getInt("item-analysis.max-firework-explosions");
            MAX_BANNER_PATTERNS = file.getInt("item-analysis.max-banner-patterns");

            CHECK_CUSTOM_POTION_EFFECTS = file.getBoolean("item-analysis.disable-custom-potion-effects");
            CHECK_UNBREAKABLE_ITEMS = file.getBoolean("item-analysis.disable-unbreakable-items");
            CHECK_ATTRIBUTE_MODIFIERS = file.getBoolean("item-analysis.check-attribute-modifiers");
        }

        @Setting
        public static class General {

            public static String CONFISCATION_STRATEGY;
            public static List<String> IGNORED_PLAYERS;

            public static void load() {
                CONFISCATION_STRATEGY = file.getString("item-analysis.general.confiscation-strategy");
                IGNORED_PLAYERS = file.getStringList("item-analysis.general.ignored-players");
            }

        }

        @Setting
        public static class NBT {

            @ItemAnalysisSetting(ItemCheckRegistry.MAX_NBT_LENGTH)
            public static int MAX_NBT_LENGTH;
            @ItemAnalysisSetting(ItemCheckRegistry.MAX_FIREWORK_NBT_LENGTH)
            public static int MAX_FIREWORK_NBT_LENGTH;
            @ItemAnalysisSetting(ItemCheckRegistry.MAX_BOOK_NBT_LENGTH)
            public static int MAX_BOOK_NBT_LENGTH;
            @ItemAnalysisSetting(ItemCheckRegistry.MAX_BOOK_PAGES)
            public static int MAX_BOOK_PAGES;
            @ItemAnalysisSetting(ItemCheckRegistry.MAX_BOOK_PAGE_LENGTH)
            public static int MAX_NBT_STRING_LENGTH;
            @ItemAnalysisSetting(ItemCheckRegistry.MAX_BOOK_TITLE_LENGTH)
            public static int MAX_BOOK_TITLE_LENGTH;
            @ItemAnalysisSetting(ItemCheckRegistry.MAX_BOOK_AUHTOR_LENGTH)
            public static int MAX_BOOK_AUTHOR_LENGTH;
            @ItemAnalysisSetting(ItemCheckRegistry.MAX_NBT_BASES)
            public static int MAX_NBT_BASES;
            @ItemAnalysisSetting(ItemCheckRegistry.MAX_NBT_NESTING)
            public static int MAX_NBT_NESTING;
            @ItemAnalysisSetting(ItemCheckRegistry.ILLEGAL_SKULL_TEXTURES)
            public static boolean CHECK_ILLEGAL_SKULL_TEXTURES;

            public static void load() {
                MAX_NBT_LENGTH = file.getInt("item-analysis.nbt.max-nbt-length");
                MAX_NBT_STRING_LENGTH = file.getInt("item-analysis.nbt.max-nbt-string-length");

                MAX_FIREWORK_NBT_LENGTH = file.getInt("item-analysis.nbt.max-firework-nbt-length");

                MAX_BOOK_NBT_LENGTH = file.getInt("item-analysis.nbt.max-book-nbt-length");
                MAX_BOOK_PAGES = file.getInt("item-analysis.nbt.max-book-pages");
                MAX_BOOK_TITLE_LENGTH = file.getInt("item-analysis.nbt.max-book-title-length");
                MAX_BOOK_AUTHOR_LENGTH = file.getInt("item-analysis.nbt.max-book-author-length");

                MAX_NBT_BASES = file.getInt("item-analysis.nbt.max-nbt-bases");
                MAX_NBT_NESTING = file.getInt("item-analysis.nbt.max-nbt-nesting");

                CHECK_ILLEGAL_SKULL_TEXTURES = file.getBoolean("item-analysis.nbt.check-illegal-skull-textures");
            }
        }
    }

    @Setting
    public static class CustomPayload {

        @Setting
        @Violation("custom-payload.a")
        public static class PayloadA extends VlSetting {

        }

        @Setting
        @Violation("custom-payload.b")
        public static class PayloadB extends VlSetting {

            public static int MAX;

            public static void load() {
                MAX = file.getInt("custom-payload.b.max");
            }
        }

        @Setting
        @Violation("custom-payload.c")
        public static class PayloadC extends VlSetting {

            public static int MAX;

            public static void load() {
                MAX = file.getInt("custom-payload.c.max");
            }
        }

        @Setting
        @Violation("custom-payload.d")
        public static class PayloadD extends VlSetting {

        }

        @Setting
        @Violation("custom-payload.e")
        public static class PayloadE extends VlSetting {

            public static int MAX;

            public static void load() {
                MAX = file.getInt("custom-payload.e.max");
            }
        }

        @Setting
        @Violation("custom-payload.f")
        public static class PayloadF extends VlSetting {

            @Setting
            @Violation({"custom-payload.f", "custom-payload.f.adv-cmd"})
            @ChannelFlood("MC|AdvCmd")
            public static class AdvCmd extends VlSetting {

            }

            @Setting
            @Violation({"custom-payload.f", "custom-payload.f.auto-cmd"})
            @ChannelFlood("MC|AutoCmd")
            public static class AutoCmd extends VlSetting {

            }

            @Setting
            @Violation({"custom-payload.f", "custom-payload.f.beacon"})
            @ChannelFlood("MC|Beacon")
            public static class Beacon extends VlSetting {

            }

            @Setting
            @Violation({"custom-payload.f", "custom-payload.f.b-edit"})
            @ChannelFlood("MC|BEdit")
            public static class BEdit extends VlSetting {

            }

            @Setting
            @Violation({"custom-payload.f", "custom-payload.f.b-open"})
            @ChannelFlood("MC|BOpen")
            public static class BOpen extends VlSetting {

            }

            @Setting
            @Violation({"custom-payload.f", "custom-payload.f.brand"})
            @ChannelFlood("MC|Brand")
            public static class Brand extends VlSetting {

            }

            @Setting
            @Violation({"custom-payload.f", "custom-payload.f.b-sign"})
            @ChannelFlood("MC|BSign")
            public static class BSign extends VlSetting {

            }

            @Setting
            @Violation({"custom-payload.f", "custom-payload.f.register"})
            @ChannelFlood("REGISTER")
            public static class Register extends VlSetting {

            }

            @Setting
            @Violation({"custom-payload.f", "custom-payload.f.unregister"})
            @ChannelFlood("UNREGISTER")
            public static class Unregister extends VlSetting {

            }
        }

        @Setting
        @Violation("custom-payload.g")
        public static class PayloadG extends VlSetting {

        }
    }

    @Setting
    public static class HeldItem {

        @Setting
        @Violation("held-item.a")
        public static class HeldItemA extends VlSetting {

        }

        @Setting
        @Violation("held-item.b")
        public static class HeldItemB extends VlSetting {

        }
    }

    @Setting
    public static class WindowClick {

        @Setting
        @Violation("window-click.a")
        public static class WindowA extends VlSetting {

        }

        @Setting
        @Violation("window-click.b")
        public static class WindowB extends VlSetting {

        }

        @Setting
        @Violation("window-click.c")
        public static class WindowC extends VlSetting {

        }

        @Setting
        @Violation("window-click.d")
        public static class WindowD extends VlSetting {

        }
    }

    @Setting
    public static class BlockPlace {

        @Setting
        @Violation("block-place.a")
        public static class PlaceA extends VlSetting {

            public static int MAX;

            public static void load() {
                MAX = file.getInt("block-place.a.max");
            }
        }

        @Setting
        @Violation("block-place.b")
        public static class PlaceB extends VlSetting {

        }

        @Setting
        @Violation("block-place.c")
        public static class PlaceC extends VlSetting {

        }

        @Setting
        @Violation("block-place.d")
        public static class PlaceD extends VlSetting {

        }
    }

    @Setting
    public static class Position {

        @Setting
        @Violation("position.a")
        public static class PositionA extends VlSetting {

            public static int MAX;
            public static int MIN;

            public static void load() {
                MAX = file.getInt("position.a.max");
                MIN = file.getInt("position.a.min");
            }
        }

        @Setting
        @Violation("position.b")
        public static class PositionB extends VlSetting {

            public static double MAX;
            public static double MIN;

            public static void load() {
                MAX = file.getDouble("position.b.max");
                MIN = file.getDouble("position.b.min");
            }
        }

        @Setting
        @Violation("position.c")
        public static class PositionC extends VlSetting {

            public PositionC() {
                super(true);
            }
        }

        @Setting
        @Violation("position.d")
        public static class PositionD extends VlSetting {

        }

        @Setting
        @Violation("position.e")
        public static class PositionE extends VlSetting {

        }

        @Setting
        @Violation("position.f")
        public static class PositionF extends VlSetting {

        }
    }

    @Setting
    public static class Rotation {

        @Setting
        @Violation("rotation.a")
        public static class RotationA extends VlSetting {

            public static int MAX;

            public void load() {
                MAX = file.getInt("rotation.a.max");
            }
        }

        @Setting
        @Violation("rotation.b")
        public static class RotationB extends VlSetting {

        }

        @Setting
        @Violation("rotation.c")
        public static class RotationC extends VlSetting {

            public static int MAX;
            public static int MIN;

            public void load() {
                MAX = file.getInt("rotation.c.max");
                MIN = file.getInt("rotation.c.max");
            }
        }
    }

    @Setting
    public static class Creative {

        @Setting
        @Violation("set-creative-slot.a")
        public static class CreativeA extends VlSetting {

        }

        @Setting
        @Violation("set-creative-slot.b")
        public static class CreativeB extends VlSetting {

        }

        @Setting
        @Violation("set-creative-slot.c")
        public static class CreativeC extends VlSetting {

        }
    }

    @Setting
    public static class Chat {

        @Setting
        @Violation("chat.a")
        public static class ChatA extends VlSetting {

        }

        @Setting
        @Violation("chat.b")
        public static class ChatB extends VlSetting {

        }

    }

    @Setting
    public static class Netty {

        @Setting
        @Violation("netty.a")
        public static class NettyA extends VlSetting {

        }

        @Setting
        @Violation("netty.b")
        public static class NettyB extends VlSetting {

        }

        @Setting
        @Violation("netty.c")
        public static class NettyC extends VlSetting {

        }

        @Setting
        @Violation("netty.d")
        public static class NettyD extends VlSetting {

        }

        @Setting
        @Violation("netty.e")
        public static class NettyE extends VlSetting {

            public static int MAX;

            public NettyE() {
                super(true);
            }

            public static void load() {
                MAX = file.getInt("netty.e.max");
            }
        }

        @Setting
        @Violation("netty.f")
        public static class NettyF extends VlSetting {

            public NettyF() {
                super(true);
            }
        }

        @Setting
        @Violation("netty.g")
        public static class NettyG extends VlSetting {

        }

        @Setting
        @Violation("netty.h")
        public static class NettyH extends VlSetting {

        }
    }

    @Setting
    public static class PacketFlood extends VlSetting {

        @Setting
        @Violation("packet-flood.common.a")
        public static class FloodA extends VlSetting {

        }

        @Setting
        @Violation("packet-flood.common.b")
        public static class FloodB extends VlSetting {

        }

        @Setting
        @Violation("packet-flood.c")
        public static class FloodC extends VlSetting {

        }

        @Setting
        public static class FloodD {

            @Flood(PacketType.Play.Client.ABILITIES)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.abilities"})
            public static class Abilities extends VlSetting {

            }

            @Flood(PacketType.Play.Client.ARM_ANIMATION)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.arm-animation"})
            public static class ArmAnimation extends VlSetting {
            }

            @Flood(PacketType.Play.Client.AUTO_RECIPE)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.auto-recipe"})
            public static class AutoRecipe extends VlSetting {

            }

            @Flood(PacketType.Play.Client.BLOCK_DIG)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.block-dig"})
            public static class BlockDig extends VlSetting {

            }

            @Flood(PacketType.Play.Client.BLOCK_PLACE)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.block-place"})
            public static class BlockPlace extends VlSetting {

            }

            @Flood(PacketType.Play.Client.CLIENT_COMMAND)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.client-command"})
            public static class ClientCommand extends VlSetting {

            }

            @Flood(PacketType.Play.Client.CLOSE_WINDOW)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.close-window"})
            public static class CloseWindow extends VlSetting {

            }


            @Flood(PacketType.Play.Client.CHAT)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.chat"})
            public static class Chat extends VlSetting {

            }

            @Flood(PacketType.Play.Client.CUSTOM_PAYLOAD)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.custom-payload"})
            public static class CustomPayload extends VlSetting {

            }

            @Flood(PacketType.Play.Client.HELD_ITEM_SLOT)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.held-item-slot"})
            public static class HeldItemSlot extends VlSetting {

            }

            @Flood(PacketType.Play.Client.ENCHANT_ITEM)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.enchant-item"})
            public static class EnchantItem extends VlSetting {

            }

            @Flood(PacketType.Play.Client.ENTITY_ACTION)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.entity-action"})
            public static class EntityAction extends VlSetting {

            }

            @Flood(PacketType.Play.Client.KEEP_ALIVE)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.keep-alive"})
            public static class KeepAlive extends VlSetting {

            }

            @Flood(PacketType.Play.Client.LOOK)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.look"})
            public static class Look extends VlSetting {

            }

            @Flood(PacketType.Play.Client.FLYING)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.flying"})
            public static class Flying extends VlSetting {

            }

            @Flood(PacketType.Play.Client.SET_CREATIVE_SLOT)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.set-creative-slot"})
            public static class SetCreativeSlot extends VlSetting {

            }

            @Flood(PacketType.Play.Client.SPECTATE)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.spectate"})
            public static class Spectate extends VlSetting {

            }

            @Flood(PacketType.Play.Client.STEER_VEHICLE)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.steer-vehicle"})
            public static class SteerVehicle extends VlSetting {

            }

            @Flood({PacketType.Play.Client.POSITION, PacketType.Play.Client.POSITION_LOOK})
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.position"})
            public static class Position extends VlSetting {

            }

            @Flood(PacketType.Play.Client.UPDATE_SIGN)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.update-sign"})
            public static class UpdateSign extends VlSetting {

            }

            @Flood(PacketType.Play.Client.USE_ITEM)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.use-item"})
            public static class UseItem extends VlSetting {

            }

            @Flood(PacketType.Play.Client.USE_ENTITY)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.use-entity"})
            public static class UseEntity extends VlSetting {

            }

            @Flood(PacketType.Play.Client.TAB_COMPLETE)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.tab-complete"})
            public static class TabComplete extends VlSetting {

            }

            @Flood(PacketType.Play.Client.WINDOW_CLICK)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.window-click"})
            public static class WindowClick extends VlSetting {

            }
        }
    }
}
