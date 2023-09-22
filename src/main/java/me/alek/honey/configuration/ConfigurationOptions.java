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
    private static FileConfiguration file;
    private static final Map<PacketTypeEnum, VlSetting> floodSettings = new HashMap<>();
    private static final Map<String, VlSetting> channelFloodSettings = new HashMap<>();
    private static final Map<Class<? extends VlSetting>, VlSetting> vlSettingMap = new HashMap<>();

    public static Map<PacketTypeEnum, VlSetting> getFloodSettings() {
        return floodSettings;
    }

    public static Map<String, VlSetting> getChannelFloodSettings() {
        return channelFloodSettings;
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

    public static boolean isLoading() {
        return loadingLatch.getCount() == 1;
    }

    public static boolean load() {
        try {
            // venter på threads der allerede loader
            loadingLatch.await();

            // laver en ny barrier
            loadingLatch = new CountDownLatch(1);

            floodSettings.clear();
            load(ConfigurationOptions.class);

        } catch (Exception ex) {
            ex.printStackTrace();
            return true;

        } finally {

            loadingLatch.countDown();
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

    public static void setViolationSetting(Class<? extends VlSetting> clazz, PacketLimitHandlerType packetLimitHandlerType, String[] prefixPaths) {
        boolean misconfiguredConfiguration = false;
        try {
            // makes a new violation setting instance and register it
            final Class<? extends VlSetting> clazzCopy = clazz;
            final VlSetting setting = vlSettingMap.computeIfAbsent(clazz, (d) -> {
                try {
                    final Constructor<? extends VlSetting> constructor = clazzCopy.getDeclaredConstructor();
                    return constructor.newInstance();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return null;
            });
            if (setting == null) return;
            setting.packetLimitHandlerType = packetLimitHandlerType;

            vlSettingMap.put(clazz, setting);

            // checks if this violation setting is a flood setting, so we can register it to the map of flood settings
            if (clazz.isAnnotationPresent(Flood.class)) {
                Flood flood = clazz.getAnnotation(Flood.class);

                for (PacketTypeEnum packetType : flood.value()) {
                    floodSettings.put(packetType, setting);
                }
            }
            // just like the condition above, checks if it is a custom payload channel flood setting instead
            if (clazz.isAnnotationPresent(ChannelFlood.class)) {
                ChannelFlood flood = clazz.getAnnotation(ChannelFlood.class);
                channelFloodSettings.put(flood.value(), setting);
            }
            if (clazz.isAnnotationPresent(Keyed.class)) {
                Keyed keyed = clazz.getAnnotation(Keyed.class);
                setting.keyedAnnotation = keyed;
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
                    HoneyPlugin.get().resetConfiguration();
                    Bukkit.getPluginManager().disablePlugin(HoneyPlugin.get().getPlugin());
                    misconfiguredConfiguration = true;

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
                HoneyPlugin.get().resetConfiguration();
                Bukkit.getPluginManager().disablePlugin(HoneyPlugin.get().getPlugin());
                misconfiguredConfiguration = true;

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

                        if (value.getClass() == Double.class) {
                            field.setDouble(setting, file.getDouble(configPath));
                        }
                        else if (value.getClass() == Integer.class) {
                            if (field.getType() == Double.class)
                                field.setDouble(setting, file.getInt(configPath));
                            else
                                field.setInt(setting, file.getInt(configPath));
                        }
                        else if (value.getClass() == Boolean.class) {
                            field.setBoolean(setting, file.getBoolean(configPath));
                        }
                    }
                }
                // check further for subclasses which are also violation settings
                try {
                    if (VlSetting.class.isAssignableFrom(clazz = (Class<? extends VlSetting>) clazz.getSuperclass())) {
                        continue;
                    }
                } catch (Exception ex) {
                }
                return;
            }
        } catch (Exception ex) {
            Bukkit.getLogger().severe("You have a misconfigured config.yml. The file is now deleted, you just have to restart the server.");
            if (misconfiguredConfiguration) throw new RuntimeException(ex.getCause());
        }
    }

    public static VlSetting getVlSetting(Class<? extends VlSetting> clazz) {
        // check if the configuration options are still loading, and if it is, wait for it to be done.
        try {
            if (isLoading()) loadingLatch.await();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (!Bukkit.getPluginManager().isPluginEnabled(HoneyPlugin.get().getPlugin())) {
            throw new RuntimeException(
                    "Honey is proceeding to run although it is turned off! What is happening!? " +
                    "Some plugin (not Bukkit) may have disabled Honey without doing it properly. " +
                    "Check your server plugins for malware and related things, or plugins" +
                    " disabling/loading other plugins during runtime."
            );
        }
        if (!vlSettingMap.containsKey(clazz)) {
            HoneyPlugin.get().resetConfiguration();
            Bukkit.getPluginManager().disablePlugin(HoneyPlugin.get().getPlugin());

            throw new RuntimeException("You have a misconfigured config.yml. The file is now deleted, you just have to restart the server.");
        }
        return vlSettingMap.get(clazz);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    private @interface Setting {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Keyed {

        String[] keys();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    private @interface Violation {
        String[] prefixPaths();

        PacketLimitHandlerType packetHandler();
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

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface VlSettingImpl {

        VlSettingImplType value();
    }

   public enum VlSettingImplType {
        MAX_PER_INTERVAL, INTERVAL, VLS_AMOUNT, THRESHOLD, UNTIL, DECAY, DECAY_AMOUNT, CLEAR_BUFFER, CLEAR_INTERVAL, ENABLED;

        public String getPathName() {
            return name().toLowerCase().replace("_", "-");
        }
    }

    public interface VlRequiredContentsSetting {

        Set<VlSettingImplType> getRequiredTypes();

    }

    public interface VlSimpleRequiredContentsSetting extends VlRequiredContentsSetting {

        default Set<VlSettingImplType> getRequiredTypes() {
            return Collections.singleton(VlSettingImplType.VLS_AMOUNT);
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
        public int maxPerInterval = 1;
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
        @VlSettingImpl(VlSettingImplType.DECAY_AMOUNT)
        public int decayAmount;

        @VlSettingImpl(VlSettingImplType.CLEAR_BUFFER)
        public boolean clearBuffer;
        @VlSettingImpl(VlSettingImplType.CLEAR_INTERVAL)
        public boolean clearInterval;
        @VlSettingImpl(VlSettingImplType.ENABLED)
        public boolean enabled = true;
        public Keyed keyedAnnotation;

        private PacketLimitHandlerType packetLimitHandlerType;

        public VlSetting(PacketLimitHandlerType packetLimitHandlerType) { this.packetLimitHandlerType = packetLimitHandlerType; }

        public VlSetting() { this(PacketLimitHandlerType.INTERVAL); }

        public int getMaxPerInterval() { return maxPerInterval; }

        public int getInterval() { return interval; }

        public int getVlsAmount() { return vlsAmount; }

        public double getThreshold() { return threshold; }

        public double getUntil() { return until; }

        public int getDecay() { return decay; }

        public int getDecayAmount() { return decayAmount; }

        public boolean isEnabled() { return enabled; }

        public Keyed getKeyedAnnotation() { return keyedAnnotation; }

        public boolean isClearInterval() { return clearInterval; }

        public boolean isClearBuffer() { return clearBuffer; }

        public PacketLimitHandlerType getPacketLimitHandlerType() { return packetLimitHandlerType; }

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
        public static String BYPASS_PERMISSION;
        public static String NOTIFY_PERMISSION;
        public static String PASTEBIN_DEVELOPER_KEY;

        public static void load() {
            CLEAR_OLD_VIOLATIONS_TIMESTAMP = file.getInt("clear-old-violations-timestamp", 120000);
            PREFIX = file.getString("prefix", "&6Honey &8»");

            BYPASS_PERMISSION_ENABLED = file.getBoolean("bypass-permission-enabled", false);
            BYPASS_PERMISSION = file.getString("bypass-permission", "honey.bypass");
            NOTIFY_PERMISSION = file.getString("notify-permission", "honey.notify");

            PASTEBIN_DEVELOPER_KEY = file.getString("pastebin-developer-key");
        }

        @Setting
        public static class Exempt {

            public static int MIN_TPS;
            public static int MAX_PING;
            public static int MAX_TIME_BETWEEN;
            public static int SPIKE_INTERVAL;

            public static void load() {
                MIN_TPS = file.getInt("exempt.min-tps", 18);
                MAX_PING = file.getInt("exempt.max-ping", 70);
                MAX_TIME_BETWEEN = file.getInt("exempt.max-time-between", 550);
                SPIKE_INTERVAL = file.getInt("exempt.spike-interval", 700);
            }
        }

        @Setting
        public static class SQL {

            public static boolean ENABLED;
            public static String HOST;
            public static int PORT;
            public static String USERNAME;
            public static String PASSWORD;
            public static String DATBASE;

            public static void load() {
                ENABLED = file.getBoolean("mysql.enabled");
                HOST = file.getString("mysql.host");
                PORT = file.getInt("mysql.port");
                USERNAME = file.getString("mysql.username");
                PASSWORD = file.getString("mysql.password");
                DATBASE = file.getString("mysql.name");
            }
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
            ENABLED = file.getBoolean("packet-logger.enabled", false);
            DETAILED = file.getBoolean("packet-logger.detailed", false);
            DEBUG = file.getBoolean("packet-logger.debug-mode", false);
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
            MAX_CONTAINER_DATA = file.getInt("item-analysis.max-container-data", 250000);

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

            public static void load() {
                CONFISCATION_STRATEGY = file.getString("item-analysis.general.confiscation-strategy");
                IGNORED_PLAYERS = file.getStringList("item-analysis.general.ignored-players");
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
                MAX_NBT_LENGTH = file.getInt("item-analysis.nbt.max-nbt-length", 15000);
                MAX_NBT_STRING_LENGTH = file.getInt("item-analysis.nbt.max-nbt-string-length", 800);

                MAX_FIREWORK_NBT_LENGTH = file.getInt("item-analysis.nbt.max-firework-nbt-length", 2500);

                MAX_BOOK_NBT_LENGTH = file.getInt("item-analysis.nbt.max-book-nbt-length", 13500);
                MAX_BOOK_PAGE_LENGTH = file.getInt("item-analysis.nbt.max-book-page-length", 256);
                MAX_BOOK_PAGES = file.getInt("item-analysis.nbt.max-book-pages", 50);
                MAX_BOOK_TITLE_LENGTH = file.getInt("item-analysis.nbt.max-book-title-length", 32);
                MAX_BOOK_AUTHOR_LENGTH = file.getInt("item-analysis.nbt.max-book-author-length", 16);

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
        public static class PayloadC extends VlSetting {

            public static int MAX;

            public static void load() {
                MAX = file.getInt("custom-payload.c.max", 6000);
            }
        }

        @Setting
        @Violation(prefixPaths = "custom-payload.d", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class PayloadD extends VlSetting {

        }

        @Setting
        @Violation(prefixPaths = "custom-payload.e", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class PayloadE extends VlSetting {

            public static int MAX;

            public static void load() {
                MAX = file.getInt("custom-payload.e.max", 110);
            }
        }

        @Setting
        @Violation(prefixPaths = "custom-payload.f", packetHandler = PacketLimitHandlerType.INTERVAL)
        public static class PayloadF extends VlSetting {

            @Setting
            @Violation(prefixPaths = {"custom-payload.f", "custom-payload.f.adv-cmd"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            @ChannelFlood("MC|AdvCmd")
            public static class AdvCmd extends VlSetting implements VlIntervalRequiredContentsSetting {

            }

            @Setting
            @Violation(prefixPaths = {"custom-payload.f", "custom-payload.f.auto-cmd"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            @ChannelFlood("MC|AutoCmd")
            public static class AutoCmd extends VlSetting implements VlIntervalRequiredContentsSetting {

            }

            @Setting
            @Violation(prefixPaths = {"custom-payload.f", "custom-payload.f.beacon"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            @ChannelFlood("MC|Beacon")
            public static class Beacon extends VlSetting implements VlIntervalRequiredContentsSetting {

            }

            @Setting
            @Violation(prefixPaths = {"custom-payload.f", "custom-payload.f.b-edit"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            @ChannelFlood("MC|BEdit")
            public static class BEdit extends VlSetting implements VlIntervalRequiredContentsSetting {

            }

            @Setting
            @Violation(prefixPaths = {"custom-payload.f", "custom-payload.f.b-open"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            @ChannelFlood("MC|BOpen")
            public static class BOpen extends VlSetting implements VlIntervalRequiredContentsSetting {

            }

            @Setting
            @Violation(prefixPaths = {"custom-payload.f", "custom-payload.f.brand"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            @ChannelFlood("MC|Brand")
            public static class Brand extends VlSetting implements VlIntervalRequiredContentsSetting {

            }

            @Setting
            @Violation(prefixPaths = {"custom-payload.f", "custom-payload.f.b-sign"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            @ChannelFlood("MC|BSign")
            public static class BSign extends VlSetting implements VlIntervalRequiredContentsSetting {

            }

            @Setting
            @Violation(prefixPaths = {"custom-payload.f", "custom-payload.f.register"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            @ChannelFlood("REGISTER")
            public static class Register extends VlSetting implements VlIntervalRequiredContentsSetting {

            }

            @Setting
            @Violation(prefixPaths = {"custom-payload.f", "custom-payload.f.unregister"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            @ChannelFlood("UNREGISTER")
            public static class Unregister extends VlSetting implements VlIntervalRequiredContentsSetting {

            }
        }

        @Setting
        @Violation(prefixPaths = "custom-payload.g", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class PayloadG extends VlSetting {

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
    }

    @Setting
    public static class BlockInteract {

        @Setting
        @Violation(prefixPaths = "block-interact.a", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class BlockInteractA extends VlSetting implements VlSimpleRequiredContentsSetting {

            public static int MAX;

            public static void load() {
                MAX = file.getInt("block-interact.a.max", 15);
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
        @Violation(prefixPaths = "block-interact.e", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class BlockInteractE extends VlSetting implements VlSimpleRequiredContentsSetting {

            public static int MAX;

            public static void load() {
                MAX = file.getInt("block-interact.e.max", 15);
            }
        }

        @Setting
        @Violation(prefixPaths = "block-interact.f", packetHandler = PacketLimitHandlerType.INTERVAL)
        public static class BlockInteractF extends VlSetting implements VlIntervalRequiredContentsSetting {

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
    }

    @Setting
    public static class Position {

        @Setting
        @Violation(prefixPaths = "position.a", packetHandler = PacketLimitHandlerType.INTERVAL)
        public static class PositionA extends VlSetting implements VlIntervalRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "position.b", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class PositionB extends VlSetting implements VlSimpleRequiredContentsSetting {

            public static double MAX;

            public static void load() {
                MAX = file.getDouble("position.b.max", 15);
            }
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
        @Keyed(keys = {"horizontal_1", "horizontal_2", "vertical"})
        public static class PositionF extends VlSetting implements VlIntervalRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "position.g", packetHandler = PacketLimitHandlerType.INTERVAL)
        public static class PositionG extends VlSetting implements VlIntervalRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "position.h", packetHandler = PacketLimitHandlerType.INTERVAL)
        @Keyed(keys = {"2d", "y"})
        public static class PositionH extends VlSetting implements VlIntervalRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "position.i", packetHandler = PacketLimitHandlerType.INTERVAL)
        public static class PositionI extends VlSetting implements VlIntervalRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "position.j", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class PositionJ extends VlSetting {

            public static int MAX;
            public static int MIN;

            public static void load() {
                MAX = file.getInt("position.a.max", 2048);
                MIN = file.getInt("position.a.min", -256);
            }
        }
    }

    @Setting
    public static class Rotation {

        @Setting
        @Violation(prefixPaths = "rotation.a", packetHandler = PacketLimitHandlerType.SINGLE_OVERFLOW)
        public static class RotationA extends VlSetting implements VlSingleOverflowRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "rotation.b", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class RotationB extends VlSetting implements VlSimpleRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "rotation.c", packetHandler = PacketLimitHandlerType.BUFFER_OVERFLOW)
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

        @Setting
        @Violation(prefixPaths = "netty.a", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class NettyA extends VlSetting implements VlSimpleRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "netty.b", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class NettyB extends VlSetting {

        }

        @Setting
        @Violation(prefixPaths = "netty.c", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class NettyC extends VlSetting {

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

        @Setting
        @Violation(prefixPaths = "netty.j", packetHandler = PacketLimitHandlerType.SIMPLE)
        @Keyed(keys = {"stq", "ntq"})
        public static class NettyJ extends VlSetting implements VlSimpleRequiredContentsSetting {

            public static double MAX_QUOTIENT;

            public static void load() {
                MAX_QUOTIENT = file.getDouble("netty.j.max-quotient", 10d);
            }
        }

        @Setting
        @Violation(prefixPaths = "netty.k", packetHandler = PacketLimitHandlerType.INTERVAL_THROTTLE)
        @Keyed(keys = {"stq", "ntq"})
        public static class NettyK extends VlSetting implements VlIntervalThrottleRequiredContentsSetting {

        }
    }

    @Setting
    public static class UseEntity {

        @Setting
        @Violation(prefixPaths = "use-entity.a", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class UseEntityA extends VlSetting implements VlSimpleRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "use-entity.b", packetHandler = PacketLimitHandlerType.SIMPLE)
        public static class UseEntityB extends VlSetting implements VlSimpleRequiredContentsSetting {

            public static double MAX_X;
            public static double MAX_Y;
            public static double MAX_Z;

            public static void load() {
                MAX_X = file.getDouble("use-entity.b.max-x", 16.0);
                MAX_Y = file.getDouble("use-entity.b.max-y", 12.6);
                MAX_Z = file.getDouble("use-entity.b.max-z", 16.0);
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
        @Violation(prefixPaths = "packet-flood.common.b", packetHandler = PacketLimitHandlerType.INTERVAL)
        public static class FloodB extends VlSetting implements VlIntervalRequiredContentsSetting {

        }

        @Setting
        @Violation(prefixPaths = "packet-flood.common.c", packetHandler = PacketLimitHandlerType.INTERVAL)
        public static class FloodC extends VlSetting implements VlIntervalRequiredContentsSetting {

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

            @Flood(PacketType.Play.Client.TAB_COMPLETE)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.tab-complete"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class TabComplete extends VlSetting implements VlIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.WINDOW_CLICK)
            @Setting
            @Violation(prefixPaths = {"packet-flood.d", "packet-flood.d.window-click"}, packetHandler = PacketLimitHandlerType.INTERVAL)
            public static class WindowClick extends VlSetting implements VlIntervalRequiredContentsSetting {

            }
        }
    }
}
