package me.alek.honey.configuration;

import com.google.common.collect.Sets;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.ConstructorInvoker;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class ConfigurationOptions {

    private static CountDownLatch loadingLatch = new CountDownLatch(0);
    private static volatile boolean loading;
    private static FileConfiguration file;
    private static final Map<PacketType, ViolationSetting> floodSettings = new HashMap<>();
    private static final Map<Class<? extends ViolationSetting>, ViolationSetting> vlSettings = new HashMap<>();

    public static Map<PacketType, ViolationSetting> getFloodSettings() {
        if (loading) {
            try {
                loadingLatch.await();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return floodSettings;
    }

    public static void setFile(FileConfiguration file) {
        ConfigurationOptions.file = file;
    }

    public static boolean load() {
        Bukkit.getLogger().info("Loading configuration...");
        try {
            loadingLatch.await();
            loading = true;
            loadingLatch = new CountDownLatch(1);

            floodSettings.clear();
            load(ConfigurationOptions.class);

        } catch (Exception ex) {
            ex.printStackTrace();
            loadingLatch.countDown();
            loading = false;
            return true;
        } finally {
            loadingLatch.countDown();
            loading = false;
        }
        return false;
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
            if (ViolationSetting.class.isAssignableFrom(clazz)) {
                Violation violation = clazz.getAnnotation(Violation.class);
                setViolationSetting((Class<? extends ViolationSetting>) clazz, violation.packetHandler(), violation.prefixPaths());
            }
        }
        for (Class<?> nestedClass : clazz.getDeclaredClasses()) {
            if (!nestedClass.isAnnotationPresent(Setting.class)) {
                continue;
            }
            load(nestedClass);
        }
    }

    @SuppressWarnings({"unchecked", "UnstableApiUsage"})
    public static void setViolationSetting(Class<? extends ViolationSetting> clazz, PacketLimitHandlerType type, String[] prefixPaths) {
        Class<? extends ViolationSetting> classCopy = clazz;
        ViolationSetting setting = vlSettings.computeIfAbsent(clazz, __ -> {
            try {
                return new ConstructorInvoker<>(classCopy).invoke();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return null;
        });
        if (setting == null) {
            return;
        }

        setting.packetLimitHandlerType = type;
        vlSettings.put(clazz, setting);
        // checks if this violation setting is a flood setting, so we can register it to the map of flood settings
        if (clazz.isAnnotationPresent(Flood.class)) {
            Flood flood = clazz.getAnnotation(Flood.class);

            for (PacketType packetType : flood.value()) {
                floodSettings.put(packetType, setting);
            }
        }
        if (clazz.isAnnotationPresent(Keyed.class)) {
            setting.keyedAnnotation = clazz.getAnnotation(Keyed.class);
        }

        Set<ViolationSettingType> requiredVlSettingImplTypes = new HashSet<>();
        if (setting instanceof VlRequiredContentsSetting) {
            VlRequiredContentsSetting vlRequiredContentsSetting = (VlRequiredContentsSetting) setting;
            requiredVlSettingImplTypes.addAll(vlRequiredContentsSetting.getRequiredTypes());
        }
        // check if the configuration is misconfigured or just from an old version
        for (String prefixPath : prefixPaths) {
            // check if this memory section exists, if it does not then disable the plugin
            if (file.get(prefixPath) == null) {
                Bukkit.getLogger().severe("Config setting misconfiguration (" + clazz + "), missing: " + prefixPath);
                throw new RuntimeException();
            }
            Iterator<ViolationSettingType> requiredVlSettingImplTypesIterator = requiredVlSettingImplTypes.iterator();

            // loop through all the required paths for this violation setting, and check if they are present
            while (requiredVlSettingImplTypesIterator.hasNext()) {
                ViolationSettingType vlSettingImplType = requiredVlSettingImplTypesIterator.next();

                String requiredPath = prefixPath + "." + vlSettingImplType.getPathName();
                // check if this required path is in the memory section
                if (file.get(requiredPath) != null) {
                    // remove the required value if is present
                    requiredVlSettingImplTypesIterator.remove();
                }
            }
        }
        // check if there is anymore required values left which have not been removed (because they were present)
        if (!requiredVlSettingImplTypes.isEmpty()) {
            Bukkit.getLogger().severe("Config setting misconfiguration (" + clazz + "), missing: " + requiredVlSettingImplTypes);
            throw new RuntimeException();
        }

        // set the values of the violation setting
        for (;;) {
            for (Field field : clazz.getDeclaredFields()) {
                // check if this field is associated with a setting value
                VlSettingImpl vlSettingImpl = field.getAnnotation(VlSettingImpl.class);
                if (vlSettingImpl == null) {
                    continue;
                }

                for (String prefixPath : prefixPaths) {
                    String configPath = prefixPath + "." + vlSettingImpl.value().getPathName();
                    Object value = file.get(configPath);
                    // check if this value is not present
                    if (value == null) {
                        continue;
                    }

                    try {
                        if (value.getClass() == Double.class) {
                            field.setDouble(setting, file.getDouble(configPath));
                        } else if (value.getClass() == Integer.class) {
                            field.setInt(setting, file.getInt(configPath));
                        } else if (value.getClass() == Boolean.class) {
                            field.setBoolean(setting, file.getBoolean(configPath));
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
            // check further for subclasses which are also violation settings
            try {
                if (!ViolationSetting.class.isAssignableFrom(clazz = (Class<? extends ViolationSetting>) clazz.getSuperclass())) {
                    return;
                }
            } catch (Exception ignored) {
            }
        }
    }

    public static ViolationSetting getVlSetting(Class<? extends ViolationSetting> clazz) {
        if (loading) {
            try {
                loadingLatch.await();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        if (!vlSettings.containsKey(clazz)) {
            HoneyPlugin.get().resetConfiguration();
            Bukkit.getPluginManager().disablePlugin(HoneyPlugin.get().getPlugin());

            throw new RuntimeException("You have a misconfigured config.yml. The file is now deleted, you just have to restart the server.");
        }
        return vlSettings.get(clazz);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Setting {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Keyed {

        int amount();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Violation {
        String[] prefixPaths();

        PacketLimitHandlerType packetHandler();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    private @interface Flood {

        PacketTypes.Play.Client[] value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface VlSettingImpl {

        ViolationSettingType value();
    }

    public enum ViolationSettingType {
        MAX_PER_INTERVAL, INTERVAL, MIN_VLS, VLS_AMOUNT, THRESHOLD, UNTIL, DECAY, DECAY_AMOUNT, MIN, MAX, ENABLED, THROTTLE_AFTER;

        public String getPathName() {
            return name().toLowerCase().replace("_", "-");
        }
    }

    public interface VlRequiredContentsSetting {

        Set<ViolationSettingType> getRequiredTypes();
    }

    public interface VlMaxRequiredContentsSetting extends VlRequiredContentsSetting {

        default Set<ViolationSettingType> getRequiredTypes() {
            return Sets.newHashSet(
                    ViolationSettingType.VLS_AMOUNT,
                    ViolationSettingType.MAX
            );
        }
    }

    public interface VlSimpleMaxRequiredContentsSetting extends VlRequiredContentsSetting {

        default Set<ViolationSettingType> getRequiredTypes() {
            return Collections.singleton(ViolationSettingType.MAX);
        }
    }

    public interface VlTriggerThrottleRequiredContentsSetting extends VlRequiredContentsSetting {

        default Set<ViolationSettingType> getRequiredTypes() {
            return Sets.newHashSet(
                    ViolationSettingType.THROTTLE_AFTER
            );
        }
    }

    public interface VlIntervalRequiredContentsSetting extends VlRequiredContentsSetting {

        default Set<ViolationSettingType> getRequiredTypes() {
            return Sets.newHashSet(
                    ViolationSettingType.MAX_PER_INTERVAL,
                    ViolationSettingType.INTERVAL
            );
        }
    }

    public interface VlBufferOverflowRequiredContentsSetting extends VlRequiredContentsSetting {

        default Set<ViolationSettingType> getRequiredTypes() {
            return Sets.newHashSet(
                    ViolationSettingType.DECAY,
                    ViolationSettingType.DECAY_AMOUNT,
                    ViolationSettingType.THRESHOLD
            );
        }
    }

    public interface VlBufferThrottleRequiredContentsSetting extends VlRequiredContentsSetting {

        default Set<ViolationSettingType> getRequiredTypes() {
            return Sets.newHashSet(
                    ViolationSettingType.DECAY,
                    ViolationSettingType.DECAY_AMOUNT,
                    ViolationSettingType.UNTIL,
                    ViolationSettingType.THRESHOLD
            );
        }
    }

    public interface VlSingleOverflowRequiredContentsSetting extends VlRequiredContentsSetting {

        default Set<ViolationSettingType> getRequiredTypes() {
            return Sets.newHashSet(
                    ViolationSettingType.THRESHOLD
            );
        }
    }

    public static class ViolationSetting implements Cloneable {

        @VlSettingImpl(ViolationSettingType.MAX_PER_INTERVAL)
        public double maxPerInterval = 1;
        @VlSettingImpl(ViolationSettingType.INTERVAL)
        public int interval;
        @VlSettingImpl(ViolationSettingType.MIN_VLS)
        public int minVls = 1;
        @VlSettingImpl(ViolationSettingType.VLS_AMOUNT)
        public int vlsAmount = 1;
        @VlSettingImpl(ViolationSettingType.THRESHOLD)
        public double threshold;
        @VlSettingImpl(ViolationSettingType.UNTIL)
        public double until;
        @VlSettingImpl(ViolationSettingType.DECAY)
        public int decay;
        @VlSettingImpl(ViolationSettingType.THROTTLE_AFTER)
        public int throttleAfter = 500;
        @VlSettingImpl(ViolationSettingType.DECAY_AMOUNT)
        public int decayAmount;
        @VlSettingImpl(ViolationSettingType.MIN)
        public int min;
        @VlSettingImpl(ViolationSettingType.MAX)
        public int max;

        @VlSettingImpl(ViolationSettingType.ENABLED)
        public boolean enabled = true;
        public Keyed keyedAnnotation;

        private PacketLimitHandlerType packetLimitHandlerType;

        public ViolationSetting(PacketLimitHandlerType type) {
            this.packetLimitHandlerType = type;
        }

        public ViolationSetting() {
            this(PacketLimitHandlerType.INTERVAL);
        }

        public boolean isEnabled() {
            return this.enabled;
        }

        public PacketLimitHandlerType getPacketLimitHandlerType() {
            return this.packetLimitHandlerType;
        }

        @Override
        public ViolationSetting clone() {
            try {
                return (ViolationSetting) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }

        public Set<Map.Entry<String, Object>> getValues() {
            Map<String, Object> valueMap = new HashMap<>();
            for (Field field : getClass().getFields()) {
                try {
                    ConfigurationOptions.VlSettingImpl settingImpl = field.getAnnotation(ConfigurationOptions.VlSettingImpl.class);
                    boolean isViolationSetting = field.getDeclaringClass() == ViolationSetting.class;
                    if (settingImpl == null && isViolationSetting) {
                        continue;
                    }

                    Object value = field.get(this);
                    // check if this field is associated with a setting value from this check
                    if (value != null) {
                        if (isViolationSetting && !getPacketLimitHandlerType().getVlSettingImplTypes().contains(settingImpl.value())){
                            continue;
                        }

                        valueMap.put(field.getName(), value);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            return valueMap.entrySet();
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();

            for (Map.Entry<String, Object> valueEntry : getValues()) {
                builder.append(valueEntry.getKey()).append("=").append(valueEntry.getValue()).append(",");
            }

            return builder.substring(0, builder.length() - 1);
        }
    }

    @Setting
    public static class General {

        public static int CLEAR_OLD_VIOLATIONS_TIMESTAMP;
        public static String PREFIX;
        public static boolean BYPASS_PERMISSION_ENABLED;
        public static boolean BYPASS_PERMISSION_SERVER_THREAD_ENABLED;
        public static boolean HIDE_COMMAND_ENABLED;
        public static String HIDE_COMMAND_MESSAGE;
        public static String HIDE_BYPASS_PERMISSION;
        public static String BYPASS_PERMISSION;
        public static String NOTIFY_PERMISSION;
        public static String PASTEBIN_DEVELOPER_KEY;

        public static void load() {
            CLEAR_OLD_VIOLATIONS_TIMESTAMP = file.getInt("clear-old-violations-timestamp");
            PREFIX = file.getString("messages.prefix", "&6Honey &8»");

            HIDE_COMMAND_ENABLED = file.getBoolean("hide-command");
            HIDE_COMMAND_MESSAGE = file.getString("hide-command-message", "SPIGOT_DEFAULT");
            HIDE_BYPASS_PERMISSION = file.getString("hide-bypass-permission", "honey.hide.bypass");
            BYPASS_PERMISSION_ENABLED = file.getBoolean("bypass-permission-enabled");
            BYPASS_PERMISSION_SERVER_THREAD_ENABLED = file.getBoolean("bypass-permission-serverthread-tracking");
            BYPASS_PERMISSION = file.getString("bypass-permission", "honey.bypass");
            NOTIFY_PERMISSION = file.getString("notify-permission", "honey.notify");

            PASTEBIN_DEVELOPER_KEY = file.getString("pastebin-developer-key");
        }
    }

    @Setting
    public static class Webhook {

        public static String URL;
        public static boolean DETAILED;

        public static void load() {
            URL = file.getString("webhook.url");
            DETAILED = file.getBoolean("webhook.detailed", true);
        }
    }

    @Setting
    public static class PacketDump {

        public static boolean HEAVY_PACKET_DUMP_ENABLED;
        public static double MIN_SERVER_HANDLE_PACKET_DUMP;

        public static boolean HEAVY_SERVER_THREAD_DUMP_ENABLED;
        public static double MIN_SERVER_HANDLE_THREAD_DUMP;

        public static void load() {
            HEAVY_PACKET_DUMP_ENABLED = file.getBoolean("packet-dump.heavy-packet.enabled");
            MIN_SERVER_HANDLE_PACKET_DUMP = file.getDouble("packet-dump.heavy-packet.min-server-handle");

            HEAVY_SERVER_THREAD_DUMP_ENABLED = file.getBoolean("packet-dump.server-thread-dump.enabled");
            MIN_SERVER_HANDLE_THREAD_DUMP = file.getDouble("packet-dump.server-thread-dump.min-server-handle");
        }
    }

    @Setting
    public static class Decoder {

        public static boolean USE_DEFAULT;
        public static boolean DISABLE_ITEM_TAG_BLOCK_PLACE;
        public static boolean DISABLE_ITEM_TAG_WINDOW_CLICK;
        public static boolean DISABLE_CREATIVE_SLOT_FOR_NON_CREATIVE;

        public static void load() {
            USE_DEFAULT = file.getBoolean("decoder.use-default");
            DISABLE_ITEM_TAG_BLOCK_PLACE = file.getBoolean("decoder.disable-tag.block-place");
            DISABLE_ITEM_TAG_WINDOW_CLICK = file.getBoolean("decoder.disable-tag.window-click");
            DISABLE_CREATIVE_SLOT_FOR_NON_CREATIVE = file.getBoolean("decoder.disable-creative-slot-for-non-creative");
        }
    }
    @Setting
    public static class Messages {

        public static boolean BASE_COMMAND_FEATURE_INFOS;
        public static String VIOLATION_MESSAGE;
        public static String PUNISH_MESSAGE;

        public static String PACKET_FILTER_PRE;
        public static String PACKET_FILTER_CANCEL;
        public static String PACKET_FILTER_POSTPONE;
        public static boolean PACKET_FILTER_ENABLED;
        public static int PACKET_FILTER_MIN_CANCEL;
        public static int PACKET_FILTER_MIN_POSTPONE;

        public static void load() {
            BASE_COMMAND_FEATURE_INFOS = file.getBoolean("messages.base-command-feature-infos");
            VIOLATION_MESSAGE = file.getString("messages.violation");
            PUNISH_MESSAGE = file.getString("messages.punish");

            PACKET_FILTER_PRE = file.getString("messages.packet-filter-action-bar.pre");
            PACKET_FILTER_CANCEL = file.getString("messages.packet-filter-action-bar.cancel");
            PACKET_FILTER_POSTPONE = file.getString("messages.packet-filter-action-bar.postpone");
            PACKET_FILTER_ENABLED = file.getBoolean("messages.packet-filter-action-bar.enabled");
            PACKET_FILTER_MIN_CANCEL = file.getInt("messages.packet-filter-action-bar.min-cancelled");
            PACKET_FILTER_MIN_POSTPONE = file.getInt("messages.packet-filter-action-bar.min-postponed");
        }
    }

    @Setting
    public static class Postpone {

        public static boolean ENABLED;
        public static int MAX_POSTPONED_PACKETS;
        public static int MAX_POSTPONED_TICKS;
        public static long MAX_POSITION_TIME_REGISTER;
        public static double RATE_IGNORE_TERMINATOR;
        public static double MAX_PER_X_TICK;
        public static double MAX_PER_X_AMOUNT;
        public static double MAX_RATE_5S;
        public static double MAX_RATE_10S;
        public static double MAX_RATE_ACCEL;
        public static double MAX_RATE_ACCEL_QUOTIENT;
        public static double MIN_RATE_5S_FOR_QUOTIENT;

        public static void load() {
            ENABLED = file.getBoolean("postpone.enabled");
            MAX_POSTPONED_PACKETS = file.getInt("postpone.max-postponed-packets");
            MAX_POSTPONED_TICKS = file.getInt("postpone.max-postponed-execution-ticks");
            MAX_POSITION_TIME_REGISTER = file.getLong("max-position-time-register");
            RATE_IGNORE_TERMINATOR = file.getDouble("postpone.rate-ignore-terminator");
            MAX_PER_X_TICK = file.getDouble("postpone.max-per-x-tick");
            MAX_PER_X_AMOUNT = file.getDouble("postpone.max-per-x-amount");
            MAX_RATE_5S = file.getDouble("postpone.max-rate-5s");
            MAX_RATE_10S = file.getDouble("postpone.max-rate-10s");
            MAX_RATE_ACCEL = file.getDouble("postpone.max-rate-accel");
            MAX_RATE_ACCEL_QUOTIENT = file.getDouble("postpone.max-rate-accel-quotient");
            MIN_RATE_5S_FOR_QUOTIENT = file.getDouble("postpone.min-rate-5s-for-quotient");
        }
    }

    @Setting
    public static class SQL {

        public static boolean ENABLED;
        public static String HOST;
        public static int PORT;
        public static String USERNAME;
        public static String PASSWORD;
        public static String DATABASE;

        public static void load() {
            ENABLED = file.getBoolean("mysql.enabled");
            HOST = file.getString("mysql.host");
            PORT = file.getInt("mysql.port");
            USERNAME = file.getString("mysql.username");
            PASSWORD = file.getString("mysql.password");
            DATABASE = file.getString("mysql.name");
        }
    }

    @Setting
    public static class Monitor {

        public static boolean ENABLE_PACKET_LOGGER;
        public static long MESSAGE_INTERVAL;
        public static List<String> IGNORED_PACKETS;

        public static void load() {
            ENABLE_PACKET_LOGGER = file.getBoolean("monitor.enable-packet-logger");
            MESSAGE_INTERVAL = file.getLong("monitor.message-interval");
            IGNORED_PACKETS = file.getStringList("monitor.ignored-packets");
        }
    }

    @Setting
    public static class PacketLogger {

        public static boolean ENABLED;
        public static boolean DETAILED;
        public static boolean FILTER_TOO_MUCH_DATA;
        public static boolean EARLY;
        public static boolean DEBUG;
        public static boolean DEBUG_PACKETS_CANCELLED;
        public static boolean FORMAT_ARRAYS;
        public static List<String> IGNORED_PLAYERS;
        public static List<String> IGNORED_PACKETS;

        public static void load() {
            ENABLED = file.getBoolean("packet-logger.enabled");
            DETAILED = file.getBoolean("packet-logger.detailed");
            FORMAT_ARRAYS = file.getBoolean("packet-logger.format-arrays");
            FILTER_TOO_MUCH_DATA = file.getBoolean("packet-logger.filter-too-much-data");
            EARLY = file.getBoolean("packet-logger.early");
            DEBUG = file.getBoolean("packet-logger.debug-mode");
            DEBUG_PACKETS_CANCELLED = file.getBoolean("packet-logger.debug-packets-cancelled");
            IGNORED_PLAYERS = file.getStringList("packet-logger.ignored-players");
            IGNORED_PACKETS = file.getStringList("packet-logger.ignored-packets");
        }
    }

    @Setting
    @Violation(prefixPaths = "commands", packetHandler = PacketLimitHandlerType.SIMPLE)
    public static class Commands extends ViolationSetting {

        public static List<String> PROHIBITED_TAB_COMPLETE_COMMANDS;
        public static List<String> PROHIBITED_COMMANDS_LITERAL;
        public static List<String> PROHIBITED_COMMANDS_REGEX;
        public static List<String> FALLBACK_PREFIXES;

        public static void load() {
            PROHIBITED_TAB_COMPLETE_COMMANDS = file.getStringList("commands.prohibited-tab-complete-commands");
            PROHIBITED_COMMANDS_LITERAL = file.getStringList("commands.prohibited-commands.literal");
            PROHIBITED_COMMANDS_REGEX = file.getStringList("commands.prohibited-commands.regex");
            FALLBACK_PREFIXES = file.getStringList("commands.fallback-prefixes");
        }
    }

    @Setting
    public static class ItemAnalysis {

        public static int MAX_ENCHANTMENT_LEVEL;
        public static int MAX_AMOUNT;
        public static int MAX_DISPLAYNAME_LENGTH;
        public static int MAX_TOTAL_LORE_LENGTH;
        public static int MAX_FIREWORK_EXPLOSIONS;
        public static int MAX_BANNER_PATTERNS;
        public static boolean CHECK_ILLEGAL_SKULL_TEXTURES;
        public static boolean CHECK_CUSTOM_POTION_EFFECTS;
        public static boolean CHECK_UNBREAKABLE_ITEMS;
        public static boolean CHECK_ATTRIBUTE_MODIFIERS;
        public static boolean CHECK_BOOK_NO_TEXT_COMPONENTS;

        public static void load() {
            MAX_ENCHANTMENT_LEVEL = file.getInt("item-analysis.max-enchantment-level");
            MAX_AMOUNT = file.getInt("item-analysis.max-amount");

            MAX_DISPLAYNAME_LENGTH = file.getInt("item-analysis.max-displayname-length");
            MAX_TOTAL_LORE_LENGTH = file.getInt("item-analysis.max-total-lore-length");

            MAX_FIREWORK_EXPLOSIONS = file.getInt("item-analysis.max-firework-explosions");
            MAX_BANNER_PATTERNS = file.getInt("item-analysis.max-banner-patterns");
            CHECK_ILLEGAL_SKULL_TEXTURES = file.getBoolean("item-analysis.check-illegal-skull-textures");

            CHECK_CUSTOM_POTION_EFFECTS = file.getBoolean("item-analysis.disable-custom-potion-effects");
            CHECK_UNBREAKABLE_ITEMS = file.getBoolean("item-analysis.disable-unbreakable-items");
            CHECK_ATTRIBUTE_MODIFIERS = file.getBoolean("item-analysis.check-attribute-modifiers");
            CHECK_BOOK_NO_TEXT_COMPONENTS = file.getBoolean("item-analysis.disable-book-no-text-components");
        }

        @Setting
        public static class General {

            public static String CONFISCATION_STRATEGY;
            public static List<String> IGNORED_PLAYERS;
            public static boolean ENABLED;

            public static void load() {
                CONFISCATION_STRATEGY = file.getString("item-analysis.general.confiscation-strategy");
                IGNORED_PLAYERS = file.getStringList("item-analysis.general.ignored-players");
                ENABLED = file.getBoolean("item-analysis.general.enabled");
            }
        }

        @Setting
        public static class NBT {

            public static int MAX_FIREWORK_NBT_LENGTH;

            public static int MAX_BOOK_NBT_LENGTH_MODERN;
            public static int MAX_BOOK_NBT_LENGTH_LEGACY;
            public static int MAX_BOOK_PAGES_MODERN;
            public static int MAX_BOOK_PAGES_LEGACY;
            public static int MAX_BOOK_PAGE_LENGTH;
            public static int MAX_BOOK_FORMATTED_PAGE_LENGTH;
            public static int MAX_BOOK_AUTHOR_LENGTH;
            public static int MAX_BOOK_TITLE_LENGTH;

            public static int MAX_NBT_LENGTH_MODERN;
            public static int MAX_NBT_LENGTH_LEGACY;
            public static int MAX_NBT_STRING_LENGTH;
            public static int MAX_NBT_KEY_LENGTH;
            public static int MAX_NBT_BASES;
            public static int MAX_NBT_NESTING;
            public static int MAX_NBT_COMPLEXITY;

            public static void load() {
                MAX_FIREWORK_NBT_LENGTH = file.getInt("item-analysis.nbt.max-firework-nbt-length");

                MAX_BOOK_NBT_LENGTH_MODERN = file.getInt("item-analysis.nbt.max-book-nbt-length.modern");
                MAX_BOOK_NBT_LENGTH_LEGACY = file.getInt("item-analysis.nbt.max-book-nbt-length.legacy");
                MAX_BOOK_PAGE_LENGTH = file.getInt("item-analysis.nbt.max-book-page-length");
                MAX_BOOK_FORMATTED_PAGE_LENGTH = file.getInt("item-analysis.nbt.max-book-formatted-page-length");
                MAX_BOOK_PAGES_MODERN = file.getInt("item-analysis.nbt.max-book-pages.modern");
                MAX_BOOK_PAGES_LEGACY = file.getInt("item-analysis.nbt.max-book-pages.legacy");
                MAX_BOOK_AUTHOR_LENGTH = file.getInt("item-analysis.nbt.max-book-author-length");
                MAX_BOOK_TITLE_LENGTH = file.getInt("item-analysis.nbt.max-book-title-length");

                MAX_NBT_LENGTH_MODERN = file.getInt("item-analysis.nbt.max-nbt-length.modern");
                MAX_NBT_LENGTH_LEGACY = file.getInt("item-analysis.nbt.max-nbt-length.legacy");
                MAX_NBT_STRING_LENGTH = file.getInt("item-analysis.nbt.max-nbt-string-length");
                MAX_NBT_KEY_LENGTH = file.getInt("item-analysis.nbt.max-nbt-key-length");
                MAX_NBT_BASES = file.getInt("item-analysis.nbt.max-nbt-bases");
                MAX_NBT_NESTING = file.getInt("item-analysis.nbt.max-nbt-nesting");
                MAX_NBT_COMPLEXITY = file.getInt("item-analysis.nbt.max-nbt-complexity");
            }

            public static int getMaxBookPages(NetProtocol protocol) {
                return protocol.isNewerThanOrEqual(NetProtocol.v1_14) ? MAX_BOOK_PAGES_MODERN : MAX_BOOK_PAGES_LEGACY;
            }

            public static int getMaxBookNBTLength(NetProtocol protocol) {
                return protocol.isNewerThanOrEqual(NetProtocol.v1_14) ? MAX_BOOK_NBT_LENGTH_MODERN : MAX_BOOK_NBT_LENGTH_LEGACY;
            }

            public static int getMaxItemNBTLength(NetProtocol protocol) {
                return protocol.isNewerThanOrEqual(NetProtocol.v1_14) ? MAX_NBT_LENGTH_MODERN : MAX_NBT_LENGTH_LEGACY;
            }
        }
    }

    @Setting
    public static class Print {

        public static boolean BLOCK_MESSAGE;

        public void load() {
            BLOCK_MESSAGE = file.getBoolean("print.block-message-on-flag");
        }

        @Setting
        @Violation(prefixPaths = "print.a", packetHandler = PacketLimitHandlerType.BUFFER_OVERFLOW)
        public static class PrintA extends ViolationSetting implements VlBufferOverflowRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "print.b", packetHandler = PacketLimitHandlerType.SINGLE_OVERFLOW)
        public static class PrintB extends ViolationSetting implements VlSingleOverflowRequiredContentsSetting {

        }
    }

    @Setting
    public static class Netty {

        @Setting
        @Violation(prefixPaths = "netty.b", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class NettyB extends ViolationSetting {

        }

        @Setting
        @Violation(prefixPaths = "netty.d", packetHandler = PacketLimitHandlerType.SINGLE_OVERFLOW)
        public static class NettyD extends ViolationSetting implements VlSingleOverflowRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "netty.e", packetHandler = PacketLimitHandlerType.BUFFER_THROTTLE)
        public static class NettyE extends ViolationSetting implements VlBufferThrottleRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "netty.f", packetHandler = PacketLimitHandlerType.BUFFER_THROTTLE)
        public static class NettyF extends ViolationSetting implements VlBufferThrottleRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "netty.g", packetHandler = PacketLimitHandlerType.SINGLE_OVERFLOW)
        public static class NettyG extends ViolationSetting implements VlSingleOverflowRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "netty.h", packetHandler = PacketLimitHandlerType.SINGLE_OVERFLOW)
        public static class NettyH extends ViolationSetting implements VlSingleOverflowRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "netty.i", packetHandler = PacketLimitHandlerType.BUFFER_THROTTLE)
        public static class NettyI extends ViolationSetting implements VlBufferThrottleRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "netty.k", packetHandler = PacketLimitHandlerType.TRIGGER_THROTTLE)
        @Keyed(amount = 2)
        public static class NettyK extends ViolationSetting implements VlTriggerThrottleRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "netty.l", packetHandler = PacketLimitHandlerType.INTERVAL)
        public static class NettyL extends ViolationSetting implements VlIntervalRequiredContentsSetting {

            @Override
            public Set<ViolationSettingType> getRequiredTypes() {
                return Sets.newHashSet(
                        ViolationSettingType.MAX_PER_INTERVAL,
                        ViolationSettingType.INTERVAL,
                        ViolationSettingType.VLS_AMOUNT,
                        ViolationSettingType.MAX,
                        ViolationSettingType.MIN
                );
            }
        }

        @Setting
        @Violation(prefixPaths = "netty.m", packetHandler = PacketLimitHandlerType.INTERVAL)
        public static class NettyM extends ViolationSetting implements VlIntervalRequiredContentsSetting {

            @Override
            public Set<ViolationSettingType> getRequiredTypes() {
                return Sets.newHashSet(
                        ViolationSettingType.MAX_PER_INTERVAL,
                        ViolationSettingType.INTERVAL,
                        ViolationSettingType.VLS_AMOUNT,
                        ViolationSettingType.MIN
                );
            }
        }

        @Setting
        @Violation(prefixPaths = "netty.n", packetHandler = PacketLimitHandlerType.INTERVAL)
        public static class NettyN extends ViolationSetting implements VlIntervalRequiredContentsSetting {

            @Override
            public Set<ViolationSettingType> getRequiredTypes() {
                return Sets.newHashSet(
                        ViolationSettingType.MAX_PER_INTERVAL,
                        ViolationSettingType.INTERVAL,
                        ViolationSettingType.VLS_AMOUNT,
                        ViolationSettingType.MIN
                );
            }
        }

        @Setting
        @Violation(prefixPaths = "netty.o", packetHandler = PacketLimitHandlerType.INTERVAL)
        public static class NettyO extends ViolationSetting implements VlIntervalRequiredContentsSetting {

            @Override
            public Set<ViolationSettingType> getRequiredTypes() {
                return Sets.newHashSet(
                        ViolationSettingType.MAX_PER_INTERVAL,
                        ViolationSettingType.INTERVAL,
                        ViolationSettingType.VLS_AMOUNT,
                        ViolationSettingType.MAX,
                        ViolationSettingType.MIN
                );
            }
        }
    }

    @Setting
    public static class Payload {

        @Setting
        @Violation(prefixPaths = "custom-payload.a", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class PayloadA extends ViolationSetting {

        }

        @Setting
        @Violation(prefixPaths = "custom-payload.b", packetHandler = PacketLimitHandlerType.SINGLE_OVERFLOW)
        public static class PayloadB extends ViolationSetting implements VlSingleOverflowRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "custom-payload.c", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class PayloadC extends ViolationSetting {

        }

        @Setting
        @Violation(prefixPaths = "custom-payload.d", packetHandler = PacketLimitHandlerType.MAX)
        public static class PayloadD extends ViolationSetting implements VlSimpleMaxRequiredContentsSetting {

        }
    }

    @Setting
    public static class HeldItem {

        @Setting
        @Violation(prefixPaths = "held-item.a", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class HeldItemA extends ViolationSetting {

        }
    }

    @Setting
    public static class Sign {

        @Setting
        @Violation(prefixPaths = "sign.a", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class SignA extends ViolationSetting {

        }
    }

    @Setting
    public static class WindowClick {

        @Setting
        @Violation(prefixPaths = "window-click.a", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class WindowA extends ViolationSetting {

        }

        @Setting
        @Violation(prefixPaths = "window-click.b", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class WindowB extends ViolationSetting {

        }

        @Setting
        @Violation(prefixPaths = "window-click.c", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class WindowC extends ViolationSetting {

        }

        @Setting
        @Violation(prefixPaths = "window-click.d", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class WindowD extends ViolationSetting {

        }
    }

    @Setting
    public static class PickItem {

        @Setting
        @Violation(prefixPaths = "pick-item.a", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class PickItemA extends ViolationSetting {

        }
    }

    @Setting
    public static class BlockInteract {

        @Setting
        @Violation(prefixPaths = "block-interact.a", packetHandler = PacketLimitHandlerType.SINGLE_OVERFLOW)
        public static class BlockInteractA extends ViolationSetting implements VlSimpleMaxRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "block-interact.b", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class BlockInteractB extends ViolationSetting {

        }

        @Setting
        @Violation(prefixPaths = "block-interact.c", packetHandler = PacketLimitHandlerType.MAX)
        public static class BlockInteractC extends ViolationSetting {

        }
    }

    @Setting
    public static class TabComplete {

        @Setting
        @Violation(prefixPaths = "tab-complete.a", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class TabA extends ViolationSetting {

        }

        @Setting
        @Violation(prefixPaths = "tab-complete.b", packetHandler = PacketLimitHandlerType.BUFFER_THROTTLE)
        public static class TabB extends ViolationSetting implements VlBufferThrottleRequiredContentsSetting {

        }

        @Setting
        @Keyed(amount = 2)
        @Violation(prefixPaths = "tab-complete.c", packetHandler = PacketLimitHandlerType.SINGLE_OVERFLOW)
        public static class TabC extends ViolationSetting implements VlSingleOverflowRequiredContentsSetting {

            public static int LABEL_THRESHOLD;

            public static void load() {
                LABEL_THRESHOLD = file.getInt("tab-complete.c.label-threshold", 64);
            }
        }
    }

    @Setting
    public static class Movement {

        @Setting
        @Violation(prefixPaths = "position.a", packetHandler = PacketLimitHandlerType.INTERVAL)
        public static class MovementA extends ViolationSetting implements VlIntervalRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "position.b", packetHandler = PacketLimitHandlerType.MAX)
        @Keyed(amount = 2)
        public static class MovementB extends ViolationSetting implements VlMaxRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "position.c", packetHandler = PacketLimitHandlerType.BUFFER_OVERFLOW)
        public static class MovementC extends ViolationSetting implements VlBufferOverflowRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "position.d", packetHandler = PacketLimitHandlerType.INTERVAL)
        public static class MovementD extends ViolationSetting implements VlIntervalRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "position.e", packetHandler = PacketLimitHandlerType.INTERVAL)
        public static class MovementE extends ViolationSetting implements VlIntervalRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "position.f", packetHandler = PacketLimitHandlerType.INTERVAL)
        @Keyed(amount = 3)
        public static class MovementF extends ViolationSetting implements VlIntervalRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "position.g", packetHandler = PacketLimitHandlerType.INTERVAL)
        public static class MovementG extends ViolationSetting implements VlIntervalRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "position.h", packetHandler = PacketLimitHandlerType.INTERVAL)
        @Keyed(amount = 2)
        public static class MovementH extends ViolationSetting implements VlIntervalRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "position.i", packetHandler = PacketLimitHandlerType.INTERVAL)
        public static class MovementI extends ViolationSetting implements VlIntervalRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "position.j", packetHandler = PacketLimitHandlerType.INTERVAL)
        public static class MovementJ extends ViolationSetting implements VlRequiredContentsSetting {

            @Override
            public Set<ViolationSettingType> getRequiredTypes() {
                return Sets.newHashSet(
                        ViolationSettingType.MAX,
                        ViolationSettingType.MIN,
                        ViolationSettingType.INTERVAL,
                        ViolationSettingType.MAX_PER_INTERVAL,
                        ViolationSettingType.VLS_AMOUNT
                );
            }
        }
    }

    @Setting
    public static class Rotation {

        @Setting
        @Violation(prefixPaths = "rotation.a", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class RotationA extends ViolationSetting {

        }

        @Setting
        @Violation(prefixPaths = "rotation.b", packetHandler = PacketLimitHandlerType.SINGLE_OVERFLOW)
        @Keyed(amount = 2)
        public static class RotationB extends ViolationSetting implements VlSingleOverflowRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "rotation.c", packetHandler = PacketLimitHandlerType.BUFFER_OVERFLOW)
        @Keyed(amount = 2)
        public static class RotationC extends ViolationSetting implements VlBufferOverflowRequiredContentsSetting {

        }
    }

    @Setting
    public static class Creative {

        @Setting
        @Violation(prefixPaths = "set-creative-slot.a", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class CreativeA extends ViolationSetting {

        }

        @Setting
        @Violation(prefixPaths = "set-creative-slot.b", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class CreativeB extends ViolationSetting {

        }

        @Setting
        @Violation(prefixPaths = "set-creative-slot.c", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class CreativeC extends ViolationSetting {

        }
    }

    @Setting
    public static class Chat {

        @Setting
        @Violation(prefixPaths = "chat.a", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class ChatA extends ViolationSetting {

        }

        @Setting
        @Violation(prefixPaths = "chat.b", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class ChatB extends ViolationSetting {

        }

    }

    @Setting
    public static class PacketFlood extends ViolationSetting {

        @Setting
        @Violation(prefixPaths = "packet-flood.a", packetHandler = PacketLimitHandlerType.INTERVAL)
        public static class FloodA extends ViolationSetting implements VlIntervalRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "packet-flood.b", packetHandler = PacketLimitHandlerType.INTERVAL)
        public static class FloodB extends ViolationSetting implements VlIntervalRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "packet-flood.c", packetHandler = PacketLimitHandlerType.INTERVAL)
        public static class FloodC extends ViolationSetting implements VlIntervalRequiredContentsSetting {

        }

        @Setting
        public static class FloodD {

            @Flood(PacketTypes.Play.Client.PLAYER_ABILITIES)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.abilities"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class Abilities extends ViolationSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketTypes.Play.Client.SWING)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.arm-animation"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class ArmAnimation extends ViolationSetting implements VlIntervalRequiredContentsSetting {
            }

            @Flood(PacketTypes.Play.Client.AUTO_RECIPE)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.auto-recipe"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class AutoRecipe extends ViolationSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketTypes.Play.Client.PLAYER_DIGGING)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.block-dig"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class BlockDig extends ViolationSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketTypes.Play.Client.BLOCK_PLACE)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.block-place"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class BlockPlace extends ViolationSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketTypes.Play.Client.CLIENT_COMMAND)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.client-command"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class ClientCommand extends ViolationSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketTypes.Play.Client.CLOSE_WINDOW)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.close-window"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class CloseWindow extends ViolationSetting implements VlIntervalRequiredContentsSetting {

            }


            @Flood(PacketTypes.Play.Client.CHAT_MESSAGE)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.chat"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class Chat extends ViolationSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketTypes.Play.Client.CUSTOM_PAYLOAD)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.custom-payload"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class CustomPayload extends ViolationSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketTypes.Play.Client.HELD_ITEM_CHANGE)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.held-item-slot"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class HeldItemSlot extends ViolationSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketTypes.Play.Client.CLICK_WINDOW_BUTTON)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.enchant-item"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class EnchantItem extends ViolationSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketTypes.Play.Client.ENTITY_ACTION)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.entity-action"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class EntityAction extends ViolationSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketTypes.Play.Client.KEEP_ALIVE)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.keep-alive"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class KeepAlive extends ViolationSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketTypes.Play.Client.PLAYER_ROTATION)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.look"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class Look extends ViolationSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketTypes.Play.Client.PLAYER_MOVEMENT)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.flying"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class Flying extends ViolationSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketTypes.Play.Client.CREATIVE_INVENTORY_ACTION)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.set-creative-slot"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class SetCreativeSlot extends ViolationSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketTypes.Play.Client.SPECTATE)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.spectate"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class Spectate extends ViolationSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketTypes.Play.Client.STEER_VEHICLE)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.steer-vehicle"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class SteerVehicle extends ViolationSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood({PacketTypes.Play.Client.PLAYER_POSITION, PacketTypes.Play.Client.PLAYER_POS_ROT})
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.position"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class Position extends ViolationSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketTypes.Play.Client.TAB_COMPLETE)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.tab-complete"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class TabComplete extends ViolationSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketTypes.Play.Client.TRANSACTION)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.transaction"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class Transaction extends ViolationSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketTypes.Play.Client.UPDATE_SIGN)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.update-sign"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class UpdateSign extends ViolationSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketTypes.Play.Client.USE_ITEM)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.use-item"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class UseItem extends ViolationSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketTypes.Play.Client.INTERACT_ENTITY)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.use-entity"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class UseEntity extends ViolationSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketTypes.Play.Client.CLICK_WINDOW)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.window-click"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class WindowClick extends ViolationSetting implements VlIntervalRequiredContentsSetting {

            }
        }

        @Setting
        @Violation(prefixPaths = "packet-flood.e", packetHandler = PacketLimitHandlerType.INTERVAL)
        public static class FloodE extends ViolationSetting implements VlIntervalRequiredContentsSetting {

        }
    }
}
