package me.alek.honey.configuration;

import org.bukkit.configuration.file.FileConfiguration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ConfigurationOptions {

    private static FileConfiguration file;
    private static List<ConfigurationOptions.IntervalSetting> floodSettings = new ArrayList<>();

    public static void setFile(FileConfiguration file) {
        ConfigurationOptions.file = file;
    }

    public static List<ConfigurationOptions.IntervalSetting> getFloodSettings() {
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
                floodSettings.add(setting);
            }
            while (true) {
                for (Field field : clazz.getDeclaredFields()) {
                    for (String prefixPath : prefixPaths) {

                        switch (field.getName()) {
                            case "MAX_PER_INTERVAL": {
                                field.set(setting, file.getInt(prefixPath + ".max-per-interval"));
                                continue;
                            }
                            case "INTERVAL": {
                                field.set(setting, file.getInt(prefixPath + ".interval", 1000));
                                continue;
                            }
                            case "VLS_AMOUNT": {
                                field.set(setting, file.getInt(prefixPath + ".vls-amount", 1));
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

    }

    public abstract static class IntervalSetting {

        public int MAX_PER_INTERVAL = 1;
        public int INTERVAL = -1;
        public int VLS_AMOUNT = 1;

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
        public static int MAX_CHEST_DATA;
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

            MAX_CHEST_DATA = file.getInt("nbt.max-chest-data");

            CHECK_ILLEGAL_SKULL_TEXTURES = file.getBoolean("nbt.check-illegal-skull-textures");
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
    }

    @Setting
    @Interval("packet-flood")
    public static class PacketFlood extends IntervalSetting {

        @Flood
        @Setting
        @Interval({"packet-flood.arm-animation", "packet-flood"})
        public static class ArmAnimation extends IntervalSetting {

        }

        @Flood
        @Setting
        @Interval({"packet-flood.auto-recipe", "packet-flood"})
        public static class AutoRecipe extends IntervalSetting {

        }
    }
}
