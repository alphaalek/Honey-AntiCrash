package me.alek.honey.configuration;

import com.google.common.collect.Sets;
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
import java.util.concurrent.CountDownLatch;

public class ConfigurationOptions {

    private static CountDownLatch loadingLatch = new CountDownLatch(0);
    private static volatile boolean loading;
    private static FileConfiguration file;
    private static final Map<PacketTypeEnum, VlSetting> floodSettings = new HashMap<>();
    private static final Map<Class<? extends VlSetting>, VlSetting> vlSettings = new HashMap<>();

    public static Map<PacketTypeEnum, VlSetting> getFloodSettings() {
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

    public static List<Field> getItemAnalysisFields() {
        List<Field> fields = new ArrayList<>();
        fields.addAll(Arrays.asList(ItemAnalysis.class.getDeclaredFields()));
        fields.addAll(Arrays.asList(ItemAnalysis.NBT.class.getDeclaredFields()));

        return fields;
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
            if (VlSetting.class.isAssignableFrom(clazz)) {
                Violation violation = clazz.getAnnotation(Violation.class);
                setViolationSetting((Class<? extends VlSetting>) clazz, violation.packetHandler(), violation.prefixPaths());
            }
        }
        for (Class<?> nestedClass : clazz.getDeclaredClasses()) {
            if (!nestedClass.isAnnotationPresent(Setting.class)) {
                continue;
            }
            load(nestedClass);
        }
    }

    public static void setViolationSetting(Class<? extends VlSetting> clazz, PacketLimitHandlerType type, String[] prefixPaths) {
        final Class<? extends VlSetting> classCopy = clazz;
        final VlSetting setting = vlSettings.computeIfAbsent(clazz, (d) -> {
            try {
                final Constructor<? extends VlSetting> constructor = classCopy.getDeclaredConstructor();
                return constructor.newInstance();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return null;
        });
        if (setting == null) return;
        setting.packetLimitHandlerType = type;

        vlSettings.put(clazz, setting);

        // checks if this violation setting is a flood setting, so we can register it to the map of flood settings
        if (clazz.isAnnotationPresent(Flood.class)) {
            Flood flood = clazz.getAnnotation(Flood.class);

            for (PacketTypeEnum packetType : flood.value()) {
                floodSettings.put(packetType, setting);
            }
        }
        if (clazz.isAnnotationPresent(Keyed.class)) {
            setting.keyedAnnotation = clazz.getAnnotation(Keyed.class);
        }

        final Set<VlSettingImplType> requiredVlSettingImplTypes = new HashSet<>();
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
            Iterator<VlSettingImplType> requiredVlSettingImplTypesIterator = requiredVlSettingImplTypes.iterator();

            // loop through all the required paths for this violation setting, and check if they are present
            while (requiredVlSettingImplTypesIterator.hasNext()) {
                VlSettingImplType vlSettingImplType = requiredVlSettingImplTypesIterator.next();

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
        while (true) {
            for (Field field : clazz.getDeclaredFields()) {

                // check if this field is associated with a setting value
                VlSettingImpl vlSettingImpl = field.getAnnotation(VlSettingImpl.class);
                if (vlSettingImpl == null) continue;

                for (String prefixPath : prefixPaths) {
                    final String configPath = prefixPath + "." + vlSettingImpl.value().getPathName();
                    final Object value = file.get(configPath);

                    // check if this value is not present or if this field is not an interval check setting
                    if (value == null) continue;

                    try {
                        if (value.getClass() == Double.class)
                            field.setDouble(setting, file.getDouble(configPath));

                        else if (value.getClass() == Integer.class)
                            field.setInt(setting, file.getInt(configPath));

                        else if (value.getClass() == Boolean.class)
                            field.setBoolean(setting, file.getBoolean(configPath));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
            // check further for subclasses which are also violation settings
            try {
                if (VlSetting.class.isAssignableFrom(clazz = (Class<? extends VlSetting>) clazz.getSuperclass())) {
                    continue;
                }
            } catch (Exception ex) {}
            return;
        }
    }

    public static VlSetting getVlSetting(Class<? extends VlSetting> clazz) {
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

        PacketType.Play.Client[] value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ItemAnalysisSetting {

        ItemCheckRegistry value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface VlSettingImpl {

        VlSettingImplType value();
    }

    public enum VlSettingImplType {
        MAX_PER_INTERVAL, INTERVAL, VLS_AMOUNT, THRESHOLD, UNTIL, DECAY, DECAY_AMOUNT, CLEAR_BUFFER, CLEAR_INTERVAL, MIN, MAX, ENABLED, THROTTLE_AFTER;

        public String getPathName() {
            return name().toLowerCase().replace("_", "-");
        }
    }

    public interface VlRequiredContentsSetting {

        Set<VlSettingImplType> getRequiredTypes();

    }

    public interface VlMaxRequiredContentsSetting extends VlRequiredContentsSetting {

        default Set<VlSettingImplType> getRequiredTypes() {
            return Sets.newHashSet(
                    VlSettingImplType.VLS_AMOUNT,
                    VlSettingImplType.MAX
            );
        }
    }

    public interface VlSimpleMaxRequiredContentsSetting extends VlRequiredContentsSetting {

        default Set<VlSettingImplType> getRequiredTypes() {
            return Collections.singleton(VlSettingImplType.MAX);
        }
    }

    public interface VlSimpleRequiredContentsSetting extends VlRequiredContentsSetting {

        default Set<VlSettingImplType> getRequiredTypes() {
            return Collections.singleton(VlSettingImplType.VLS_AMOUNT);
        }
    }

    public interface VlTriggerThrottleRequiredContentsSetting extends VlRequiredContentsSetting {

        default Set<VlSettingImplType> getRequiredTypes() {
            return Sets.newHashSet(
                    VlSettingImplType.THROTTLE_AFTER,
                    VlSettingImplType.VLS_AMOUNT
            );
        }
    }

    public interface VlIntervalRequiredContentsSetting extends VlRequiredContentsSetting {

        default Set<VlSettingImplType> getRequiredTypes() {
            return Sets.newHashSet(
                    VlSettingImplType.MAX_PER_INTERVAL,
                    VlSettingImplType.INTERVAL,
                    VlSettingImplType.VLS_AMOUNT
            );
        }
    }

    public interface VlIntervalThrottleRequiredContentsSetting extends VlRequiredContentsSetting {

        default Set<VlSettingImplType> getRequiredTypes() {
            return Sets.newHashSet(
                    ConfigurationOptions.VlSettingImplType.INTERVAL,
                    ConfigurationOptions.VlSettingImplType.THRESHOLD,
                    ConfigurationOptions.VlSettingImplType.UNTIL,
                    ConfigurationOptions.VlSettingImplType.VLS_AMOUNT
            );
        }
    }

    public interface VlBufferOverflowRequiredContentsSetting extends VlRequiredContentsSetting {

        default Set<VlSettingImplType> getRequiredTypes() {
            return Sets.newHashSet(
                    VlSettingImplType.DECAY,
                    VlSettingImplType.DECAY_AMOUNT,
                    VlSettingImplType.THRESHOLD,
                    VlSettingImplType.VLS_AMOUNT
            );
        }
    }

    public interface VlBufferThrottleRequiredContentsSetting extends VlRequiredContentsSetting {

        default Set<VlSettingImplType> getRequiredTypes() {
            return Sets.newHashSet(
                    VlSettingImplType.DECAY,
                    VlSettingImplType.DECAY_AMOUNT,
                    VlSettingImplType.UNTIL,
                    VlSettingImplType.THRESHOLD,
                    VlSettingImplType.VLS_AMOUNT
            );
        }
    }

    public interface VlSingleOverflowRequiredContentsSetting extends VlRequiredContentsSetting {

        default Set<VlSettingImplType> getRequiredTypes() {
            return Sets.newHashSet(
                    VlSettingImplType.THRESHOLD,
                    VlSettingImplType.VLS_AMOUNT
            );
        }
    }

    public static class VlSetting implements Cloneable {

        @VlSettingImpl(VlSettingImplType.MAX_PER_INTERVAL)
        public double maxPerInterval = 1;
        @VlSettingImpl(VlSettingImplType.INTERVAL)
        public int interval;
        @VlSettingImpl(VlSettingImplType.VLS_AMOUNT)
        public int vlsAmount = 1;
        @VlSettingImpl(VlSettingImplType.THRESHOLD)
        public double threshold;
        @VlSettingImpl(VlSettingImplType.UNTIL)
        public double until;
        @VlSettingImpl(VlSettingImplType.DECAY)
        public int decay;
        @VlSettingImpl(VlSettingImplType.THROTTLE_AFTER)
        public int throttleAfter = 500;
        @VlSettingImpl(VlSettingImplType.DECAY_AMOUNT)
        public int decayAmount;
        @VlSettingImpl(VlSettingImplType.MIN)
        public int min;
        @VlSettingImpl(VlSettingImplType.MAX)
        public int max;

        @VlSettingImpl(VlSettingImplType.CLEAR_BUFFER)
        public boolean clearBuffer;
        @VlSettingImpl(VlSettingImplType.CLEAR_INTERVAL)
        public boolean clearInterval;
        @VlSettingImpl(VlSettingImplType.ENABLED)
        public boolean enabled = true;
        public Keyed keyedAnnotation;

        private PacketLimitHandlerType packetLimitHandlerType;

        public VlSetting(PacketLimitHandlerType packetLimitHandlerType) {
            this.packetLimitHandlerType = packetLimitHandlerType;
        }

        public VlSetting() {
            this(PacketLimitHandlerType.INTERVAL);
        }

        public boolean isEnabled() {
            return enabled;
        }

        public PacketLimitHandlerType getPacketLimitHandlerType() {
            return packetLimitHandlerType;
        }

        @Override
        public VlSetting clone() {
            try {
                return (VlSetting) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }

        public Set<Map.Entry<String, Object>> getValues() {
            Map<String, Object> valueMap = new HashMap<>();

            for (Field field : getClass().getFields()) {

                try {
                    ConfigurationOptions.VlSettingImpl settingImpl = field.getAnnotation(ConfigurationOptions.VlSettingImpl.class);
                    if (settingImpl == null && field.getDeclaringClass() == ConfigurationOptions.VlSetting.class) continue;

                    Object value = field.get(this);

                    // check if this field is associated with a setting value from this check
                    if (settingImpl != null && value != null) {
                        if (!getPacketLimitHandlerType().getVlSettingImplTypes().contains(settingImpl.value())) continue;

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
        public static boolean HIDE_COMMAND_ENABLED;
        public static String HIDE_COMMAND_MESSAGE;
        public static String HIDE_BYPASS_PERMISSION;
        public static String BYPASS_PERMISSION;
        public static String NOTIFY_PERMISSION;
        public static String PASTEBIN_DEVELOPER_KEY;

        public static void load() {
            CLEAR_OLD_VIOLATIONS_TIMESTAMP = file.getInt("clear-old-violations-timestamp", 120000);
            PREFIX = file.getString("messages.prefix", "&6Honey &8»");

            HIDE_COMMAND_ENABLED = file.getBoolean("hide-command", false);
            HIDE_COMMAND_MESSAGE = file.getString("hide-command-message", "SPIGOT_DEFAULT");
            HIDE_BYPASS_PERMISSION = file.getString("hide-bypass-permission", "honey.hide.bypass");
            BYPASS_PERMISSION_ENABLED = file.getBoolean("bypass-permission-enabled", false);
            BYPASS_PERMISSION = file.getString("bypass-permission", "honey.bypass");
            NOTIFY_PERMISSION = file.getString("notify-permission", "honey.notify");

            PASTEBIN_DEVELOPER_KEY = file.getString("pastebin-developer-key");
        }
    }

    @Setting
    public static class PacketDump {

        public static boolean VLS_ENABLED;
        public static List<String> VLS_FILTER_ALL;

        public static boolean HEAVY_PACKET_DUMP_ENABLED;
        public static double MIN_SERVER_HANDLE_PACKET_DUMP;

        public static boolean HEAVY_SERVER_THREAD_DUMP_ENABLED;
        public static double MIN_SERVER_HANDLE_THREAD_DUMP;

        public static void load() {
            VLS_ENABLED = file.getBoolean("packet-dump.enabled", true);
            VLS_FILTER_ALL = file.getStringList("packet-dump.filter-all");

            HEAVY_PACKET_DUMP_ENABLED = file.getBoolean("packet-dump.heavy-packet.enabled", true);
            MIN_SERVER_HANDLE_PACKET_DUMP = file.getDouble("packet-dump.heavy-packet.min-server-handle", 5d);

            HEAVY_SERVER_THREAD_DUMP_ENABLED = file.getBoolean("packet-dump.server-thread-dump.enabled", true);
            MIN_SERVER_HANDLE_THREAD_DUMP = file.getDouble("packet-dump.server-thread-dump.min-server-handle", 500d);
        }
    }

    @Setting
    public static class AsyncDecoder {

        public static boolean ENABLED;
        public static int MIN_WORKER_THREADS;
        public static int MIN_PENDING_TASK_FOR_QUARANTINE;
        public static double MIN_TIME_FOR_QUARANTINE;

        public static void load() {
            ENABLED = file.getBoolean("async-decoder.enabled");
            MIN_WORKER_THREADS = file.getInt("async-decoder.min-worker-threads");
            MIN_PENDING_TASK_FOR_QUARANTINE = file.getInt("async-decoder.min-pending-task-size-for-quarantine");
            MIN_TIME_FOR_QUARANTINE = file.getDouble("async-decoder.min-time-for-quarantine");
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

        public static String QUARANTINED_ASYNC_WORKER;
        public static boolean QUARANTINED_ASYNC_WORKER_ENABLED;
        public static int QUARANTINED_ASYNC_WORKER_MIN_QUARANTINED;

        public static void load() {
            BASE_COMMAND_FEATURE_INFOS = file.getBoolean("messages.base-command-feature-infos", true);
            VIOLATION_MESSAGE = file.getString("messages.violation", "%prefix% &7%player% %network%&7 failed &e%check% &8&o(hover)&7. %context%");
            PUNISH_MESSAGE = file.getString("messages.punish", "%prefix% &7%player% was kicked for &e%check%&7.");

            PACKET_FILTER_PRE = file.getString("messages.packet-filter-action-bar.pre", "&cPacket filter:");
            PACKET_FILTER_CANCEL = file.getString("messages.packet-filter-action-bar.cancel", "Cancelling... (%amount%)");
            PACKET_FILTER_POSTPONE = file.getString("messages.packet-filter-action-bar.postpone", "Postponing... (%amount%) (current: %current_amount%)");
            PACKET_FILTER_ENABLED = file.getBoolean("messages.packet-filter-action-bar.enabled", true);
            PACKET_FILTER_MIN_CANCEL = file.getInt("messages.packet-filter-action-bar.min-cancelled", 10);
            PACKET_FILTER_MIN_POSTPONE = file.getInt("messages.packet-filter-action-bar.min-postponed", 30);

            QUARANTINED_ASYNC_WORKER = file.getString("messages.quarantined-async-worker-action-bar.message", "&cAsync worker moved to quarantine... (%time%ms)");
            QUARANTINED_ASYNC_WORKER_ENABLED = file.getBoolean("messages.quarantined-async-worker-action-bar.enabled", true);
            QUARANTINED_ASYNC_WORKER_MIN_QUARANTINED = file.getInt("messages.quarantined-async-worker-action-bar.min-quarantined", 100);
        }
    }

    @Setting
    public static class Postpone {

        public static boolean ENABLED;
        public static int MAX_POSTPONED_PACKETS;
        public static int MAX_POSTPONED_TICKS;
        public static int MAX_RETRY_AMOUNT;
        public static List<String> MAX_QUEUE_SIZE_IGNORED;
        public static double RATE_IGNORE_TERMINATOR;
        public static double MAX_PER_X_TICK;
        public static double MAX_PER_X_AMOUNT;
        public static double MAX_RATE_5S;
        public static double MAX_RATE_10S;
        public static double MAX_RATE_ACCEL;
        public static double MAX_RATE_ACCEL_QUOTIENT;
        public static double MIN_RATE_5S_FOR_QUOTIENT;

        public static void load() {
            ENABLED = file.getBoolean("postpone.enabled", true);
            MAX_POSTPONED_PACKETS = file.getInt("postpone.max-postponed-packets", 150);
            MAX_POSTPONED_TICKS = file.getInt("postpone.max-postponed-execution-ticks", 100);
            MAX_RETRY_AMOUNT = file.getInt("postpone.max-retry", 30);
            if (file.get("postpone.max-queue-size-ignored") == null) {
                MAX_QUEUE_SIZE_IGNORED = Arrays.asList(
                        "PacketPlayInFlying",
                        "PacketPlayInLook",
                        "PacketPlayInPosition",
                        "PacketPlayInPositionLook",
                        "PacketPlayInSetCreativeSlot",
                        "PacketPlayInWindowClick");
            } else {
                MAX_QUEUE_SIZE_IGNORED = file.getStringList("max-queue-size-ignored");
            }
            RATE_IGNORE_TERMINATOR = file.getDouble("postpone.rate-ignore-terminator", 10d);
            MAX_PER_X_TICK = file.getDouble("postpone.max-per-x-tick", 75d);
            MAX_PER_X_AMOUNT = file.getDouble("postpone.max-per-x-amount", 5);
            MAX_RATE_5S = file.getDouble("postpone.max-rate-5s", 90d);
            MAX_RATE_10S = file.getDouble("postpone.max-rate-10s", 75d);
            MAX_RATE_ACCEL = file.getDouble("postpone.max-rate-accel", 150d);
            MAX_RATE_ACCEL_QUOTIENT = file.getDouble("postpone.max-rate-accel-quotient", 12.5d);
            MIN_RATE_5S_FOR_QUOTIENT = file.getDouble("postpone.min-rate-5s-for-quotient", 32.5d);
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
            ENABLE_PACKET_LOGGER = file.getBoolean("monitor.enable-packet-logger", true);
            MESSAGE_INTERVAL = file.getLong("monitor.message-interval", 500L);
            IGNORED_PACKETS = file.getStringList("monitor.ignored-packets");
        }
    }

    @Setting
    public static class PacketLogger {

        public static boolean ENABLED;
        public static boolean DETAILED;
        public static boolean EARLY;
        public static boolean DEBUG;
        public static boolean DEBUG_PACKETS_CANCELLED;
        public static boolean FORMAT_ARRAYS;
        public static List<String> IGNORED_PLAYERS;
        public static List<String> IGNORED_PACKETS;

        public static void load() {
            ENABLED = file.getBoolean("packet-logger.enabled", false);
            DEBUG = file.getBoolean("packet-logger.debug-mode", false);
            EARLY = file.getBoolean("packet-logger.early", false);
            DEBUG_PACKETS_CANCELLED = file.getBoolean("packet-logger.debug-packets-cancelled", false);
            FORMAT_ARRAYS = file.getBoolean("packet-logger.format-arrays", true);
            DETAILED = file.getBoolean("packet-logger.detailed", true);
            IGNORED_PLAYERS = file.getStringList("packet-logger.ignored-players");
            IGNORED_PACKETS = file.getStringList("packet-logger.ignored-packets");
        }
    }

    @Setting
    @Violation(prefixPaths = "commands", packetHandler = PacketLimitHandlerType.SIMPLE)
    public static class Commands extends VlSetting implements VlSimpleRequiredContentsSetting {

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
        @ItemAnalysisSetting(ItemCheckRegistry.MAX_ENCHANTMENT_LEVEL)
        public static int MAX_ENCHANTMENT_LEVEL;
        @ItemAnalysisSetting(ItemCheckRegistry.MAX_AMOUNT)
        public static int MAX_AMOUNT;
        @ItemAnalysisSetting(ItemCheckRegistry.MAX_DISPLAYNAME_LENGTH)
        public static int MAX_DISPLAYNAME_LENGTH;
        @ItemAnalysisSetting(ItemCheckRegistry.MAX_TOTAL_LORE_LENGTH)
        public static int MAX_TOTAL_LORE_LENGTH;
        @ItemAnalysisSetting(ItemCheckRegistry.MAX_FIREWORK_EXPLOSIONS)
        public static int MAX_FIREWORK_EXPLOSIONS;
        @ItemAnalysisSetting(ItemCheckRegistry.MAX_BANNER_PATTERNS)
        public static int MAX_BANNER_PATTERNS;
        @ItemAnalysisSetting(ItemCheckRegistry.CUSTOM_POTION_EFFECTS)
        public static boolean CHECK_CUSTOM_POTION_EFFECTS;
        @ItemAnalysisSetting(ItemCheckRegistry.UNBREAKABLE_ITEM)
        public static boolean CHECK_UNBREAKABLE_ITEMS;
        @ItemAnalysisSetting(ItemCheckRegistry.ATTRIBUTE_MODIFIERS)
        public static boolean CHECK_ATTRIBUTE_MODIFIERS;

        public static void load() {
            MAX_ENCHANTMENT_LEVEL = file.getInt("item-analysis.max-enchantment-level", 256);
            MAX_AMOUNT = file.getInt("item-analysis.max-amount", 64);

            MAX_DISPLAYNAME_LENGTH = file.getInt("item-analysis.max-displayname-length", 100);
            MAX_TOTAL_LORE_LENGTH = file.getInt("item-analysis.max-total-lore-length", 2000);

            MAX_FIREWORK_EXPLOSIONS = file.getInt("item-analysis.max-firework-explosions", 17);
            MAX_BANNER_PATTERNS = file.getInt("item-analysis.max-banner-patterns", 6);

            CHECK_CUSTOM_POTION_EFFECTS = file.getBoolean("item-analysis.disable-custom-potion-effects", false);
            CHECK_UNBREAKABLE_ITEMS = file.getBoolean("item-analysis.disable-unbreakable-items", false);
            CHECK_ATTRIBUTE_MODIFIERS = file.getBoolean("item-analysis.check-attribute-modifiers", false);
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

            @ItemAnalysisSetting(ItemCheckRegistry.MAX_NBT_LENGTH)
            public static int MAX_NBT_LENGTH;
            @ItemAnalysisSetting(ItemCheckRegistry.MAX_NBT_STRING_LENGTH)
            public static int MAX_NBT_STRING_LENGTH;
            @ItemAnalysisSetting(ItemCheckRegistry.MAX_FIREWORK_NBT_LENGTH)
            public static int MAX_FIREWORK_NBT_LENGTH;
            @ItemAnalysisSetting(ItemCheckRegistry.MAX_BOOK_NBT_LENGTH)
            public static int MAX_BOOK_NBT_LENGTH;
            @ItemAnalysisSetting(ItemCheckRegistry.MAX_BOOK_PAGES)
            public static int MAX_BOOK_PAGES;
            @ItemAnalysisSetting(ItemCheckRegistry.MAX_BOOK_PAGE_LENGTH)
            public static int MAX_BOOK_PAGE_LENGTH;
            @ItemAnalysisSetting(ItemCheckRegistry.MAX_BOOK_AUTHOR_LENGTH)
            public static int MAX_BOOK_AUTHOR_LENGTH;
            @ItemAnalysisSetting(ItemCheckRegistry.MAX_BOOK_TITLE_LENGTH)
            public static int MAX_BOOK_TITLE_LENGTH;
            @ItemAnalysisSetting(ItemCheckRegistry.MAX_NBT_BASES)
            public static int MAX_NBT_BASES;
            @ItemAnalysisSetting(ItemCheckRegistry.MAX_NBT_NESTING)
            public static int MAX_NBT_NESTING;
            @ItemAnalysisSetting(ItemCheckRegistry.ILLEGAL_SKULL_TEXTURES)
            public static boolean CHECK_ILLEGAL_SKULL_TEXTURES;

            public static void load() {
                MAX_NBT_LENGTH = file.getInt("item-analysis.nbt.max-nbt-length", 15000);
                MAX_NBT_STRING_LENGTH = file.getInt("item-analysis.nbt.max-nbt-string-length", 800);

                MAX_FIREWORK_NBT_LENGTH = file.getInt("item-analysis.nbt.max-firework-nbt-length", 2500);

                MAX_BOOK_NBT_LENGTH = file.getInt("item-analysis.nbt.max-book-nbt-length", 13500);
                MAX_BOOK_PAGE_LENGTH = file.getInt("item-analysis.nbt.max-book-page-length", 256);
                MAX_BOOK_PAGES = file.getInt("item-analysis.nbt.max-book-pages", 50);
                MAX_BOOK_AUTHOR_LENGTH = file.getInt("item-analysis.nbt.max-book-author-length", 16);
                MAX_BOOK_TITLE_LENGTH = file.getInt("item-analysis.nbt.max-book-title-length", 32);

                MAX_NBT_BASES = file.getInt("item-analysis.nbt.max-nbt-bases", 80);
                MAX_NBT_NESTING = file.getInt("item-analysis.nbt.max-nbt-nesting", 4);

                CHECK_ILLEGAL_SKULL_TEXTURES = file.getBoolean("item-analysis.nbt.check-illegal-skull-textures", true);
            }
        }
    }

    @Setting
    public static class CustomPayload {

        @Setting
        @Violation(prefixPaths = "custom-payload.a", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class PayloadA extends VlSetting {

        }

        @Setting
        @Violation(prefixPaths = "custom-payload.b", packetHandler = PacketLimitHandlerType.SINGLE_OVERFLOW)
        public static class PayloadB extends VlSetting implements VlSingleOverflowRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "custom-payload.c", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class PayloadC extends VlSetting implements VlSimpleMaxRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "custom-payload.d", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class PayloadD extends VlSetting {

        }

        @Setting
        @Violation(prefixPaths = "custom-payload.e", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class PayloadE extends VlSetting implements VlSimpleMaxRequiredContentsSetting {

        }
    }

    @Setting
    public static class HeldItem {

        @Setting
        @Violation(prefixPaths = "held-item.a", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class HeldItemA extends VlSetting implements VlSimpleRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "held-item.b", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class HeldItemB extends VlSetting {

        }
    }

    @Setting
    public static class WindowClick {

        @Setting
        @Violation(prefixPaths = "window-click.a", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class WindowA extends VlSetting implements VlSimpleRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "window-click.b", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class WindowB extends VlSetting {

        }

        @Setting
        @Violation(prefixPaths = "window-click.c", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class WindowC extends VlSetting implements VlSimpleRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "window-click.d", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class WindowD extends VlSetting implements VlSimpleRequiredContentsSetting {

        }
    }

    @Setting
    public static class BlockInteract {

        @Setting
        @Violation(prefixPaths = "block-interact.a", packetHandler = PacketLimitHandlerType.INTERVAL)
        public static class BlockInteractA extends VlSetting implements VlRequiredContentsSetting {

            private static Set<VlSettingImplType> getRequiredContentTypes() {
                return Sets.newHashSet(
                        VlSettingImplType.MAX,
                        VlSettingImplType.MAX_PER_INTERVAL,
                        VlSettingImplType.INTERVAL,
                        VlSettingImplType.VLS_AMOUNT
                );
            }

            @Override
            public Set<VlSettingImplType> getRequiredTypes() {
                return getRequiredContentTypes();
            }
        }

        @Setting
        @Violation(prefixPaths = "block-interact.b", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class BlockInteractB extends VlSetting {

        }

        @Setting
        @Violation(prefixPaths = "block-interact.c", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class BlockInteractC extends VlSetting implements VlSimpleRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "block-interact.d", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class BlockInteractD extends VlSetting implements VlSimpleRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "block-interact.e", packetHandler = PacketLimitHandlerType.INTERVAL)
        public static class BlockInteractE extends VlSetting implements VlRequiredContentsSetting {

            @Override
            public Set<VlSettingImplType> getRequiredTypes() {
                return BlockInteractA.getRequiredContentTypes();
            }
        }
    }

    @Setting
    public static class TabComplete {

        @Setting
        @Violation(prefixPaths = "tab-complete.a", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class TabA extends VlSetting {

        }

        @Setting
        @Violation(prefixPaths = "tab-complete.b", packetHandler = PacketLimitHandlerType.BUFFER_THROTTLE)
        public static class TabB extends VlSetting implements VlBufferThrottleRequiredContentsSetting {

        }

        @Setting
        @Keyed(amount = 2)
        @Violation(prefixPaths = "tab-complete.c", packetHandler = PacketLimitHandlerType.SINGLE_OVERFLOW)
        public static class TabC extends VlSetting implements VlSingleOverflowRequiredContentsSetting {

            public static int LABEL_THRESHOLD;

            public static void load() {
                LABEL_THRESHOLD = file.getInt("tab-complete.c.label-threshold", 64);
            }
        }
    }

    @Setting
    public static class Position {

        @Setting
        @Violation(prefixPaths = "position.a", packetHandler = PacketLimitHandlerType.INTERVAL)
        public static class PositionA extends VlSetting implements VlIntervalRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "position.b", packetHandler = PacketLimitHandlerType.SIMPLE)
        @Keyed(amount = 2)
        public static class PositionB extends VlSetting implements VlMaxRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "position.c", packetHandler = PacketLimitHandlerType.BUFFER_OVERFLOW)
        public static class PositionC extends VlSetting implements VlBufferOverflowRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "position.d", packetHandler = PacketLimitHandlerType.INTERVAL)
        public static class PositionD extends VlSetting implements VlIntervalRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "position.e", packetHandler = PacketLimitHandlerType.INTERVAL)
        public static class PositionE extends VlSetting implements VlIntervalRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "position.f", packetHandler = PacketLimitHandlerType.INTERVAL)
        @Keyed(amount = 3)
        public static class PositionF extends VlSetting implements VlIntervalRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "position.g", packetHandler = PacketLimitHandlerType.INTERVAL)
        public static class PositionG extends VlSetting implements VlIntervalRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "position.h", packetHandler = PacketLimitHandlerType.INTERVAL)
        @Keyed(amount = 2)
        public static class PositionH extends VlSetting implements VlIntervalRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "position.i", packetHandler = PacketLimitHandlerType.INTERVAL)
        public static class PositionI extends VlSetting implements VlIntervalRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "position.j", packetHandler = PacketLimitHandlerType.INTERVAL)
        public static class PositionJ extends VlSetting implements VlRequiredContentsSetting {

            @Override
            public Set<VlSettingImplType> getRequiredTypes() {
                return Sets.newHashSet(
                        VlSettingImplType.MAX,
                        VlSettingImplType.MIN,
                        VlSettingImplType.INTERVAL,
                        VlSettingImplType.MAX_PER_INTERVAL,
                        VlSettingImplType.VLS_AMOUNT
                );
            }
        }
    }

    @Setting
    public static class Rotation {

        @Setting
        @Violation(prefixPaths = "rotation.a", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class RotationA extends VlSetting implements VlSimpleRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "rotation.b", packetHandler = PacketLimitHandlerType.SINGLE_OVERFLOW)
        @Keyed(amount = 2)
        public static class RotationB extends VlSetting implements VlSingleOverflowRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "rotation.c", packetHandler = PacketLimitHandlerType.BUFFER_OVERFLOW)
        @Keyed(amount = 2)
        public static class RotationC extends VlSetting implements VlBufferOverflowRequiredContentsSetting {

        }
    }

    @Setting
    public static class Creative {

        @Setting
        @Violation(prefixPaths = "set-creative-slot.a", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class CreativeA extends VlSetting implements VlSimpleRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "set-creative-slot.b", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class CreativeB extends VlSetting implements VlSimpleRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "set-creative-slot.c", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class CreativeC extends VlSetting {

        }
    }

    @Setting
    public static class Chat {

        @Setting
        @Violation(prefixPaths = "chat.a", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class ChatA extends VlSetting {

        }

        @Setting
        @Violation(prefixPaths = "chat.b", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class ChatB extends VlSetting {

        }

    }

    @Setting
    public static class Netty {

        /* @Setting */
        @Violation(prefixPaths = "netty.a", packetHandler = PacketLimitHandlerType.CHIPSTATE_THROTTLE)
        public static class NettyA extends VlSetting {

            public static int MAX_PACKETS;
            public static double MAX_HANDLE;

            /*
            public static void load() {
                MAX_PACKETS = file.getInt("netty.a.max-packets", 10);
                MAX_HANDLE = file.getDouble("netty.a.max-handle", 25.0);
            }
            */
        }

        @Setting
        @Violation(prefixPaths = "netty.b", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class NettyB extends VlSetting {

        }

        /* @Setting */
        @Violation(prefixPaths = "netty.c", packetHandler = PacketLimitHandlerType.TRIGGER_THROTTLE)
        public static class NettyC extends VlSetting /* implements VlTriggerThrottleRequiredContentsSetting */ {

        }

        @Setting
        @Violation(prefixPaths = "netty.d", packetHandler = PacketLimitHandlerType.SINGLE_OVERFLOW)
        public static class NettyD extends VlSetting implements VlSingleOverflowRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "netty.e", packetHandler = PacketLimitHandlerType.BUFFER_THROTTLE)
        public static class NettyE extends VlSetting implements VlBufferThrottleRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "netty.f", packetHandler = PacketLimitHandlerType.BUFFER_THROTTLE)
        public static class NettyF extends VlSetting implements VlBufferThrottleRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "netty.g", packetHandler = PacketLimitHandlerType.SINGLE_OVERFLOW)
        public static class NettyG extends VlSetting implements VlSingleOverflowRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "netty.h", packetHandler = PacketLimitHandlerType.SINGLE_OVERFLOW)
        public static class NettyH extends VlSetting implements VlSingleOverflowRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "netty.i", packetHandler = PacketLimitHandlerType.BUFFER_THROTTLE)
        public static class NettyI extends VlSetting implements VlBufferThrottleRequiredContentsSetting {

        }

        /* @Setting */
        @Violation(prefixPaths = "netty.j", packetHandler = PacketLimitHandlerType.TRIGGER_THROTTLE)
        @Keyed(amount = 3)
        public static class NettyJ extends VlSetting /* implements VlTriggerThrottleRequiredContentsSetting */ {

        }

        @Setting
        @Violation(prefixPaths = "netty.k", packetHandler = PacketLimitHandlerType.TRIGGER_THROTTLE)
        @Keyed(amount = 2)
        public static class NettyK extends VlSetting implements VlTriggerThrottleRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "netty.l", packetHandler = PacketLimitHandlerType.INTERVAL)
        public static class NettyL extends VlSetting implements VlIntervalRequiredContentsSetting {

            @Override
            public Set<VlSettingImplType> getRequiredTypes() {
                return Sets.newHashSet(
                        VlSettingImplType.MAX_PER_INTERVAL,
                        VlSettingImplType.INTERVAL,
                        VlSettingImplType.VLS_AMOUNT,
                        VlSettingImplType.MAX
                );
            }

        }

        @Setting
        @Violation(prefixPaths = "netty.m", packetHandler = PacketLimitHandlerType.INTERVAL)
        public static class NettyM extends VlSetting implements VlIntervalRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "netty.n", packetHandler = PacketLimitHandlerType.INTERVAL)
        public static class NettyN extends VlSetting implements VlIntervalRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "netty.o", packetHandler = PacketLimitHandlerType.INTERVAL)
        public static class NettyO extends VlSetting implements VlIntervalRequiredContentsSetting {

            @Override
            public Set<VlSettingImplType> getRequiredTypes() {
                return Sets.newHashSet(
                        VlSettingImplType.MAX_PER_INTERVAL,
                        VlSettingImplType.INTERVAL,
                        VlSettingImplType.VLS_AMOUNT,
                        VlSettingImplType.MAX
                );
            }
        }
    }

    @Setting
    public static class PacketFlood extends VlSetting {

        @Setting
        @Violation(prefixPaths = "packet-flood.a", packetHandler = PacketLimitHandlerType.INTERVAL)
        public static class FloodA extends VlSetting implements VlIntervalRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "packet-flood.b", packetHandler = PacketLimitHandlerType.INTERVAL)
        public static class FloodB extends VlSetting implements VlIntervalRequiredContentsSetting {

        }

        @Setting
        public static class FloodD {

            @Flood(PacketType.Play.Client.ABILITIES)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.abilities"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class Abilities extends VlSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.ARM_ANIMATION)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.arm-animation"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class ArmAnimation extends VlSetting implements VlIntervalRequiredContentsSetting {
            }

            @Flood(PacketType.Play.Client.AUTO_RECIPE)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.auto-recipe"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class AutoRecipe extends VlSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.BLOCK_DIG)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.block-dig"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class BlockDig extends VlSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.BLOCK_PLACE)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.block-place"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class BlockPlace extends VlSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.CLIENT_COMMAND)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.client-command"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class ClientCommand extends VlSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.CLOSE_WINDOW)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.close-window"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class CloseWindow extends VlSetting implements VlIntervalRequiredContentsSetting {

            }


            @Flood(PacketType.Play.Client.CHAT)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.chat"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class Chat extends VlSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.CUSTOM_PAYLOAD)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.custom-payload"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class CustomPayload extends VlSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.HELD_ITEM_SLOT)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.held-item-slot"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class HeldItemSlot extends VlSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.ENCHANT_ITEM)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.enchant-item"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class EnchantItem extends VlSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.ENTITY_ACTION)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.entity-action"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class EntityAction extends VlSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.KEEP_ALIVE)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.keep-alive"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class KeepAlive extends VlSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.LOOK)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.look"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class Look extends VlSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.FLYING)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.flying"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class Flying extends VlSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.SET_CREATIVE_SLOT)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.set-creative-slot"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class SetCreativeSlot extends VlSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.SPECTATE)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.spectate"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class Spectate extends VlSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.STEER_VEHICLE)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.steer-vehicle"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class SteerVehicle extends VlSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood({PacketType.Play.Client.POSITION, PacketType.Play.Client.POSITION_LOOK})
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.position"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class Position extends VlSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.TAB_COMPLETE)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.tab-complete"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class TabComplete extends VlSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.TRANSACTION)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.transaction"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class Transaction extends VlSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.UPDATE_SIGN)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.update-sign"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class UpdateSign extends VlSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.USE_ITEM)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.use-item"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class UseItem extends VlSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.USE_ENTITY)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.use-entity"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class UseEntity extends VlSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.WINDOW_CLICK)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.window-click"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class WindowClick extends VlSetting implements VlIntervalRequiredContentsSetting {

            }
        }
    }
}
