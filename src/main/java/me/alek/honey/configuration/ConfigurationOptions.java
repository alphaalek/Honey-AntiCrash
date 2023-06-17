package me.alek.honey.configuration;

import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import org.bukkit.configuration.file.FileConfiguration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigurationOptions {

    private static FileConfiguration file;
    private static Map<PacketTypeEnum, ConfigurationOptions.IntervalSetting> floodSettings = new HashMap<>();

    public static void setFile(FileConfiguration file) {
        ConfigurationOptions.file = file;
    }

    public static Map<PacketTypeEnum, IntervalSetting> getFloodSettings() {
        return floodSettings;
    }

    public static void load() {
        floodSettings.clear();
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
        if (clazz.isAnnotationPresent(Interval.class)) {
            if (IntervalSetting.class.isAssignableFrom(clazz)) {
                setIntervalBased(clazz, clazz.getAnnotation(Interval.class).value());
            }
        }
        for (Class<?> nestedClass : clazz.getDeclaredClasses()) {
            if (!nestedClass.isAnnotationPresent(Setting.class)) {
                continue;
            }
            load(nestedClass);
        }
    }

    public static void setIntervalBased(Class<?> clazz, String[] prefixPaths) {
        try {
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            IntervalSetting setting = (IntervalSetting) constructor.newInstance();

            if (clazz.isAnnotationPresent(Flood.class)) {
                Flood flood = clazz.getAnnotation(Flood.class);
                for (PacketTypeEnum packetType : flood.value()) {
                    floodSettings.put(packetType, setting);
                }
            }
            while (true) {
                for (Field field : clazz.getDeclaredFields()) {
                    for (String prefixPath : prefixPaths) {

                        switch (field.getName()) {
                            case "MAX_PER_INTERVAL": {
                                if (file.get(prefixPath + ".max-per-interval") == null) {
                                    continue;
                                }
                                field.set(setting, file.getInt(prefixPath + ".max-per-interval"));
                                continue;
                            }
                            case "INTERVAL": {
                                if (file.get(prefixPath + ".interval") == null) {
                                    continue;
                                }
                                field.set(setting, file.getInt(prefixPath + ".interval"));
                                continue;
                            }
                            case "VLS_AMOUNT": {
                                if (file.get(prefixPath + ".vls-amount") == null) {
                                    continue;
                                }
                                field.set(setting, file.getInt(prefixPath + ".vls-amount"));
                            }
                            case "ENABLED": {
                                if (file.get(prefixPath + ".enabled") == null) {
                                    continue;
                                }
                                field.set(setting, file.getBoolean(prefixPath + ".vls-amount"));
                            }
                        }
                    }
                }
                if (IntervalSetting.class.isAssignableFrom(clazz = clazz.getSuperclass())) {
                    continue;
                }
                return;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    private @interface Setting {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    private @interface Interval {
        String[] value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    private @interface Flood {

        PacketType.Play.Client[] value();
    }

    public abstract static class IntervalSetting {

        public int MAX_PER_INTERVAL = 1;
        public int INTERVAL = 0;
        public int VLS_AMOUNT = 1;
        private boolean ENABLED;

        public IntervalSetting() {}

        public int getMaxPerInterval() {
            return MAX_PER_INTERVAL;
        }

        public int getInterval() {
            return INTERVAL;
        }

        public int getVlsAmount() {
            return VLS_AMOUNT;
        }

        public boolean isEnabled() {
            return ENABLED;
        }
    }

    @Setting
    public static class General {

        public static int CLEAR_OLD_VIOLATIONS_TIMESTAMP;
        public static String KICK_MESSAGE;
        public static boolean BYPASS_PERMISSION_ENABLED;
        public static String BYPASS_PERMISSION;
        public static String NOTIFY_PERMISSION;

        public static void load() {
            CLEAR_OLD_VIOLATIONS_TIMESTAMP = file.getInt("clear-old-violations-timestamp");
            KICK_MESSAGE = file.getString("kick-message");
            BYPASS_PERMISSION_ENABLED = file.getBoolean("bypass-permission-enabled");
            BYPASS_PERMISSION = file.getString("bypass-permission");
            NOTIFY_PERMISSION = file.getString("notify-permission");
        }
    }

    @Setting
    public static class PacketLogger {

        public static boolean ENABLED;
        public static boolean CONSOLE;
        public static boolean DETAILED;
        public static boolean TRUNCATE_OLD_LOGS;
        public static List<String> IGNORED_PLAYERS;
        public static List<String> IGNORED_PACKETS;

        public static void load() {
            ENABLED = file.getBoolean("packet-logger.enabled");
            CONSOLE = file.getBoolean("packet-logger.console");
            DETAILED = file.getBoolean("packet-logger.detailed");
            TRUNCATE_OLD_LOGS = file.getBoolean("packet-logger.truncate-old-logs");
            IGNORED_PLAYERS = file.getStringList("packet-logger.ignored-players");
            IGNORED_PACKETS = file.getStringList("packet-logger.ignored-packets");
        }
    }

    @Setting
    @Interval("commands")
    public static class Commands extends IntervalSetting {

        public static List<String> PROHIBITED_TAB_COMPLETE_COMMANDS;
        public static List<String> PROHIBITED_COMMANDS;

        private static Commands instance;
        public Commands() {
            instance = this;
        }
        public static Commands get() {
            return instance;
        }

        public static void load() {
            PROHIBITED_TAB_COMPLETE_COMMANDS = file.getStringList("commands.prohibited-tab-complete-commands");
            PROHIBITED_COMMANDS = file.getStringList("commands.prohibited-commands");
        }
    }

    @Setting
    public static class NBT {

        public static int MAX_NBT_LENGTH;
        public static int MAX_FIREWORK_NBT_LENGTH;
        public static int MAX_FIREWORK_CHARGE_NBT_LENGTH;
        public static int MAX_BOOK_NBT_LENGTH;
        public static int MAX_BOOK_PAGES;
        public static int MAX_BOOK_PAGE_LENGTH;
        public static int MAX_NBT_BASES;
        public static int MAX_NBT_NESTING;
        public static boolean CHECK_ILLEGAL_SKULL_TEXTURES;

        public static void load() {
            MAX_NBT_LENGTH = file.getInt("nbt.max-nbt-length");

            MAX_FIREWORK_NBT_LENGTH = file.getInt("nbt.max-firework-nbt-length");
            MAX_FIREWORK_CHARGE_NBT_LENGTH = file.getInt("nbt.max-firework-charge-nbt-length");

            MAX_BOOK_NBT_LENGTH = file.getInt("nbt.max-book-nbt-length");
            MAX_BOOK_PAGES = file.getInt("nbt.max-book-pages");
            MAX_BOOK_PAGE_LENGTH = file.getInt("nbt.max-book-page-length");

            MAX_NBT_BASES = file.getInt("nbt.max-nbt-bases");
            MAX_NBT_NESTING = file.getInt("nbt.max-nbt-nesting");

            CHECK_ILLEGAL_SKULL_TEXTURES = file.getBoolean("nbt.check-illegal-skull-textures");
        }
    }

    @Setting
    public static class Items {

        public static int MAX_CHEST_DATA;

        public static int MAX_ENCHANTMENT_LEVEL;
        public static int MAX_AMOUNT;

        public static int MAX_DISPLAYNAME_LENGTH;
        public static int MAX_TOTAL_LORE_LENGTH;

        public static void load() {
            MAX_CHEST_DATA = file.getInt("items.max-chest-data");

            MAX_ENCHANTMENT_LEVEL = file.getInt("items.max-enchantment-level");
            MAX_AMOUNT = file.getInt("items.max-amount");

            MAX_DISPLAYNAME_LENGTH = file.getInt("items.max-displayname-length");
            MAX_TOTAL_LORE_LENGTH = file.getInt("items.max-total-lore-length");
        }
    }

    @Setting
    @Interval("custom-payload")
    public static class CustomPayload {

        public static int MIN_BYTEBUF_CAPACITY;
        public static int MAX_BYTEBUF_CAPACITY;
        public static int MIN_BYTEBUF_REFCNT;
        public static int MAX_BYTEBUF_REFCNT;
        public static int MAX_SERIALIZED_ITEM_NBT_LENGTH;
        public static int MAX_PLUGIN_CHANNELS;

        public static void load() {
            MIN_BYTEBUF_CAPACITY = file.getInt("custom-payload.min-bytebuf-capacity");
            MAX_BYTEBUF_CAPACITY = file.getInt("custom-payload.max-bytebuf-capacity");

            MIN_BYTEBUF_REFCNT = file.getInt("custom-payload.min-bytebuf-refcnt");
            MAX_BYTEBUF_REFCNT = file.getInt("custom-payload.max-bytebuf-refcnt");

            MAX_SERIALIZED_ITEM_NBT_LENGTH = file.getInt("custom-payload.max-serialized-item-nbt-length");
            MAX_PLUGIN_CHANNELS = file.getInt("custom-payload.max-plugin-channels");
        }
    }

    @Setting
    @Interval("block-place")
    public static class BlockPlacement extends IntervalSetting {
        public static double MAX_BLOCK_TO_PLAYER_DISTANCE;

        private static BlockPlacement instance;
        public BlockPlacement() {
            instance = this;
        }
        public static BlockPlacement get() {
            return instance;
        }

        public static void load() {
            MAX_BLOCK_TO_PLAYER_DISTANCE = file.getDouble("block-place.max-block-to-player-distance");
        }
    }

    @Setting
    public static class Velocity {

        @Setting
        @Interval("velocity.new-chunks")
        public static class NewChunks extends IntervalSetting {

            private static NewChunks instance;

            public NewChunks() {
                instance = this;
            }

            public static NewChunks get() {
                return instance;
            }
        }

        @Setting
        @Interval("velocity.new-unloaded-chunks")
        public static class NewUnloadedChunks extends IntervalSetting {

            private static NewUnloadedChunks instance;
            public NewUnloadedChunks() {
                instance = this;
            }
            public static NewUnloadedChunks get() {
                return instance;
            }

        }

        @Setting
        @Interval("velocity.movement")
        public static class Movement extends IntervalSetting {

            public static int MAX_2D_DISTANCE;

            private static Movement instance;
            public Movement() {
                instance = this;
            }
            public static Movement get() {
                return instance;
            }

            public static void load() {
                MAX_2D_DISTANCE = file.getInt("velocity.movement.max-2d-distance");
            }
        }

        @Setting
        public static class Height {

            public static boolean ENABLED;
            public static int MAX_HEIGHT;

            public static void load() {
                ENABLED = file.getBoolean("velocity.height.enabled");
                MAX_HEIGHT = file.getInt("velocity.height.max-height");
            }
        }

        @Setting
        public static class Yaw {

            public static int MAX_YAW;
            public static int MIN_YAW;

            public static void load () {
                MAX_YAW = file.getInt("velocity.yaw.max-yaw");
                MIN_YAW = file.getInt("velocity.yaw.min-yaw");
            }
        }
    }

    @Setting
    public static class Netty {

        public static int MAX_BYTES_PER_PACKET;
        public static boolean BLOCK_HEAVY_COMPRESSED_PACKETS;

        public static void load() {
            MAX_BYTES_PER_PACKET = file.getInt("netty.max-bytes-per-packet");
            BLOCK_HEAVY_COMPRESSED_PACKETS = file.getBoolean("netty.block-heavy-compressed");
        }
    }

    @Setting
    @Interval("packet-flood")
    public static class PacketFlood extends IntervalSetting {

        @Flood(PacketType.Play.Client.ABILITIES)
        @Setting
        @Interval({"packet-flood", "packet-flood.abilities"})
        public static class Abilities extends IntervalSetting {

        }

        @Flood(PacketType.Play.Client.ARM_ANIMATION)
        @Setting
        @Interval({"packet-flood", "packet-flood.arm-animation"})
        public static class ArmAnimation extends IntervalSetting {

        }

        @Flood(PacketType.Play.Client.AUTO_RECIPE)
        @Setting
        @Interval({"packet-flood", "packet-flood.auto-recipe"})
        public static class AutoRecipe extends IntervalSetting {

        }

        @Flood(PacketType.Play.Client.BLOCK_DIG)
        @Setting
        @Interval({"packet-flood", "packet-flood.block-dig"})
        public static class BlockDig extends IntervalSetting {

        }

        @Flood(PacketType.Play.Client.BLOCK_PLACE)
        @Setting
        @Interval({"packet-flood", "packet-flood.block-place"})
        public static class BlockPlace extends IntervalSetting {

        }

        @Flood(PacketType.Play.Client.CLIENT_COMMAND)
        @Setting
        @Interval({"packet-flood", "packet-flood.client-command"})
        public static class ClientCommand extends IntervalSetting {

        }

        @Flood(PacketType.Play.Client.CLOSE_WINDOW)
        @Setting
        @Interval({"packet-flood", "packet-flood.close-window"})
        public static class CloseWindow extends IntervalSetting {

        }


        @Flood(PacketType.Play.Client.CHAT)
        @Setting
        @Interval({"packet-flood", "packet-flood.chat"})
        public static class Chat extends IntervalSetting {

        }

        @Flood(PacketType.Play.Client.CUSTOM_PAYLOAD)
        @Setting
        @Interval({"packet-flood", "packet-flood.custom-payload"})
        public static class CustomPayload extends IntervalSetting {

        }

        @Flood(PacketType.Play.Client.HELD_ITEM_SLOT)
        @Setting
        @Interval({"packet-flood", "packet-flood.held-item-slot"})
        public static class HeldItemSlot extends IntervalSetting {

        }

        @Flood(PacketType.Play.Client.ENCHANT_ITEM)
        @Setting
        @Interval({"packet-flood", "packet-flood.enchant-item"})
        public static class EnchantItem extends IntervalSetting {

        }

        @Flood(PacketType.Play.Client.ENTITY_ACTION)
        @Setting
        @Interval({"packet-flood", "packet-flood.entity-action"})
        public static class EntityAction extends IntervalSetting {

        }

        @Flood(PacketType.Play.Client.KEEP_ALIVE)
        @Setting
        @Interval({"packet-flood", "packet-flood.keep-alive"})
        public static class KeepAlive extends IntervalSetting {

        }

        @Flood(PacketType.Play.Client.LOOK)
        @Setting
        @Interval({"packet-flood", "packet-flood.look"})
        public static class Look extends IntervalSetting {

        }

        @Flood(PacketType.Play.Client.FLYING)
        @Setting
        @Interval({"packet-flood", "packet-flood.flying"})
        public static class Flying extends IntervalSetting {

        }

        @Flood(PacketType.Play.Client.SET_CREATIVE_SLOT)
        @Setting
        @Interval({"packet-flood", "packet-flood.set-creative-slot"})
        public static class SetCreativeSlot extends IntervalSetting {

        }

        @Flood(PacketType.Play.Client.SETTINGS)
        @Setting
        @Interval({"packet-flood", "packet-flood.settings"})
        public static class Settings extends IntervalSetting {

        }

        @Flood(PacketType.Play.Client.SPECTATE)
        @Setting
        @Interval({"packet-flood", "packet-flood.spectate"})
        public static class Spectate extends IntervalSetting {

        }

        @Flood(PacketType.Play.Client.STEER_VEHICLE)
        @Setting
        @Interval({"packet-flood", "packet-flood.steer-vehicle"})
        public static class SteerVehicle extends IntervalSetting {

        }

        @Flood({PacketType.Play.Client.POSITION, PacketType.Play.Client.POSITION_LOOK})
        @Setting
        @Interval({"packet-flood", "packet-flood.position"})
        public static class Position extends IntervalSetting {

        }

        @Flood(PacketType.Play.Client.UPDATE_SIGN)
        @Setting
        @Interval({"packet-flood", "packet-flood.update-sign"})
        public static class UpdateSign extends IntervalSetting {

        }

        @Flood(PacketType.Play.Client.USE_ITEM)
        @Setting
        @Interval({"packet-flood", "packet-flood.use-item"})
        public static class UseItem extends IntervalSetting {

        }

        @Flood(PacketType.Play.Client.USE_ENTITY)
        @Setting
        @Interval({"packet-flood", "packet-flood.use-entity"})
        public static class UseEntity extends IntervalSetting {

        }

        @Flood(PacketType.Play.Client.TAB_COMPLETE)
        @Setting
        @Interval({"packet-flood", "packet-flood.tab-complete"})
        public static class TabComplete extends IntervalSetting {

        }

        @Flood(PacketType.Play.Client.WINDOW_CLICK)
        @Setting
        @Interval({"packet-flood", "packet-flood.window-click"})
        public static class WindowClick extends IntervalSetting {

        }
    }
}
