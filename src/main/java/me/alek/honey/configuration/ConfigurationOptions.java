package me.alek.honey.configuration;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.List;

public class ConfigurationOptions {

    private static FileConfiguration file;

    public static void setFile(FileConfiguration file) {
        ConfigurationOptions.file = file;
    }

    public static void load() {
        load(ConfigurationOptions.class);
    }

    public static void load(Class<?> clazz) {
        if (file == null) {
            return;
        }
        if (clazz.isAnnotationPresent(Setting.class)) {
            try {
                clazz.getDeclaredMethod("load").invoke(null);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        if (clazz.isAnnotationPresent(Interval.class)) {
            setIntervalBased(clazz, clazz.getAnnotation(Interval.class).value());
        }
        for (Class<?> nestedClass : clazz.getDeclaredClasses()) {
            if (!nestedClass.isAnnotationPresent(Setting.class)) {
                continue;
            }
            load(nestedClass);
        }
    }

    public static void setIntervalBased(Class<?> clazz, String prefixPath) {
        try {
            for (Field field : clazz.getDeclaredFields()) {
                switch (field.getName()) {
                    case "MAX_PER_INTERVAL": {
                        field.set(null, file.getInt(prefixPath + ".max-per-interval"));
                        continue;
                    }
                    case "INTERVAL": {
                        field.set(null, file.getInt(prefixPath + ".interval", 1000));
                        continue;
                    }
                    case "VLS_AMOUNT": {
                        field.set(null, file.getInt(prefixPath + ".vls-amount", 1));
                        continue;
                    }
                    case "KICK_PLAYER": {
                        field.set(null, file.getBoolean(prefixPath + ".kick-player", true));
                    }
                }
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
        String value();
    }

    @Setting
    public static class General {

        public static boolean CANCEL_ON_VIOLATION;
        public static String KICK_MESSAGE;

        public static void load() {
            CANCEL_ON_VIOLATION = file.getBoolean("cancel-on-violation");
            KICK_MESSAGE = file.getString("kick-message");
        }
    }

    @Setting
    @Interval("commands")
    public static class Commands {

        public static int VLS_AMOUNT;
        public static List<String> PROHIBITED_TAB_COMPLETE_COMMANDS;
        public static List<String> PROHIBITED_COMMANDS;

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
        public static boolean KICK_PLAYER;

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
    public static class BlockPlacement {

        public static double MAX_BLOCK_TO_PLAYER_DISTANCE;
        public static boolean KICK_PLAYER;

        public static void load() {
            MAX_BLOCK_TO_PLAYER_DISTANCE = file.getDouble("block-place.max-block-to-player-distance");
        }
    }

    @Setting
    public static class Velocity {

        @Setting
        @Interval("velocity.new-chunks")
        public static class NewChunks {

            public static int MAX_PER_INTERVAL;
            public static int INTERVAL;
            public static int VLS_AMOUNT;
            public static boolean KICK_PLAYER;

            public static void load() {}
        }

        @Setting
        @Interval("velocity.new-unloaded-chunks")
        public static class NewUnloadedChunks {

            public static int MAX_PER_INTERVAL;
            public static int INTERVAL;
            public static int VLS_AMOUNT;
            public static boolean KICK_PLAYER;

            public static void load() {}
        }

        @Setting
        @Interval("velocity.movement")
        public static class Movement {

            public static int MAX_2D_DISTANCE;
            public static int MAX_PER_INTERVAL;
            public static int INTERVAL;
            public static int VLS_AMOUNT;
            public static boolean KICK_PLAYER;

            public static void load() {
                MAX_2D_DISTANCE = file.getInt("velocity.movement.max-2d-distance");
            }
        }

        public static void load() {}
    }
}
