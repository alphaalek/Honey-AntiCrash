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

    public static List<Field> getItemAnalysisSettings() {
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
            vlSettingMap.clear();
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
                setViolationSetting((Class<? extends VlSetting>) clazz, clazz.getAnnotation(Violation.class).value());
            }
        }
        for (Class<?> nestedClass : clazz.getDeclaredClasses()) {
            if (!nestedClass.isAnnotationPresent(Setting.class)) {
                continue;
            }
            load(nestedClass);
        }
    }

    public static void setViolationSetting(Class<? extends VlSetting> clazz, String[] prefixPaths) {
        boolean misconfiguredConfiguration = false;
        try {
            // makes a new violation setting instance and register it
            final Constructor<? extends VlSetting> constructor = clazz.getDeclaredConstructor();
            final VlSetting setting = constructor.newInstance();
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
                    Bukkit.getPluginManager().disablePlugin(HoneyPlugin.get());
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
                Bukkit.getPluginManager().disablePlugin(HoneyPlugin.get());
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

                        if (value.getClass() == Integer.class) {
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

        if (!Bukkit.getPluginManager().isPluginEnabled(HoneyPlugin.get())) {
            throw new RuntimeException(
                    "Honey is proceeding to run although it is turned off! What is happening!? " +
                    "Some plugin (not Bukkit) may have disabled Honey without doing it properly. " +
                    "Check your server plugins for malware and related things, or plugins" +
                    " disabling/loading other plugins during runtime."
            );
        }
        if (!vlSettingMap.containsKey(clazz)) {
            HoneyPlugin.get().resetConfiguration();
            Bukkit.getPluginManager().disablePlugin(HoneyPlugin.get());

            throw new RuntimeException("You have a misconfigured config.yml. The file is now deleted, you just have to restart the server.");
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

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    private @interface VlSettingImpl {

        VlSettingImplType value();
    }

    private enum VlSettingImplType {
        MAX_PER_INTERVAL, INTERVAL, VLS_AMOUNT, THRESHOLD, UNTIL, DECAY, DECAY_AMOUNT, CLEAR_BUFFER, CLEAR_INTERVAL, ENABLED;

        public String getPathName() {
            return name().toLowerCase().replace("_", "-");
        }
    }

    public interface VlRequiredContentsSetting {

        Set<VlSettingImplType> getRequiredTypes();

    }

    public interface VlSimpleSingletonRequiredContentsSetting extends VlRequiredContentsSetting {

        default Set<VlSettingImplType> getRequiredTypes() {
            return Collections.singleton(VlSettingImplType.VLS_AMOUNT);
        }
    }

    public interface VlSimpleIntervalRequiredContentsSetting extends VlRequiredContentsSetting {

        default Set<VlSettingImplType> getRequiredTypes() {
            return Sets.newHashSet(
                    VlSettingImplType.MAX_PER_INTERVAL,
                    VlSettingImplType.INTERVAL,
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
        public int threshold;
        @VlSettingImpl(VlSettingImplType.UNTIL)
        public int until;
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
        private final PacketLimitHandlerType packetLimitHandlerType;

        public VlSetting(PacketLimitHandlerType packetLimitHandlerType) { this.packetLimitHandlerType = packetLimitHandlerType; }

        public VlSetting() { this(PacketLimitHandlerType.INTERVAL); }

        public int getMaxPerInterval() { return maxPerInterval; }

        public int getInterval() { return interval; }

        public int getVlsAmount() { return vlsAmount; }

        public int getThreshold() { return threshold; }

        public int getUntil() { return until; }

        public int getDecay() { return decay; }

        public int getDecayAmount() { return decayAmount; }

        public boolean isEnabled() { return enabled; }

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
    }

    @Setting
    public static class General {

        public static int CLEAR_OLD_VIOLATIONS_TIMESTAMP;
        public static String PREFIX;
        public static boolean BYPASS_PERMISSION_ENABLED;
        public static String BYPASS_PERMISSION;
        public static String NOTIFY_PERMISSION;

        public static void load() {
            CLEAR_OLD_VIOLATIONS_TIMESTAMP = file.getInt("clear-old-violations-timestamp", 120000);
            PREFIX = file.getString("prefix", "&6Honey &8»");

            BYPASS_PERMISSION_ENABLED = file.getBoolean("bypass-permission-enabled", false);
            BYPASS_PERMISSION = file.getString("bypass-permission", "honey.bypass");
            NOTIFY_PERMISSION = file.getString("notify-permission", "honey.notify");
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
        @Violation("custom-payload.a")
        public static class PayloadA extends VlSetting {

        }

        @Setting
        @Violation("custom-payload.b")
        public static class PayloadB extends VlSetting {

            public static int MAX;

            public static void load() {
                MAX = file.getInt("custom-payload.b.max", 10000);
            }
        }

        @Setting
        @Violation("custom-payload.c")
        public static class PayloadC extends VlSetting {

            public static int MAX;

            public static void load() {
                MAX = file.getInt("custom-payload.c.max", 6000);
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
                MAX = file.getInt("custom-payload.e.max", 110);
            }
        }

        @Setting
        @Violation("custom-payload.f")
        public static class PayloadF extends VlSetting {

            @Setting
            @Violation({"custom-payload.f", "custom-payload.f.adv-cmd"})
            @ChannelFlood("MC|AdvCmd")
            public static class AdvCmd extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

            }

            @Setting
            @Violation({"custom-payload.f", "custom-payload.f.auto-cmd"})
            @ChannelFlood("MC|AutoCmd")
            public static class AutoCmd extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

            }

            @Setting
            @Violation({"custom-payload.f", "custom-payload.f.beacon"})
            @ChannelFlood("MC|Beacon")
            public static class Beacon extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

            }

            @Setting
            @Violation({"custom-payload.f", "custom-payload.f.b-edit"})
            @ChannelFlood("MC|BEdit")
            public static class BEdit extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

            }

            @Setting
            @Violation({"custom-payload.f", "custom-payload.f.b-open"})
            @ChannelFlood("MC|BOpen")
            public static class BOpen extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

            }

            @Setting
            @Violation({"custom-payload.f", "custom-payload.f.brand"})
            @ChannelFlood("MC|Brand")
            public static class Brand extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

            }

            @Setting
            @Violation({"custom-payload.f", "custom-payload.f.b-sign"})
            @ChannelFlood("MC|BSign")
            public static class BSign extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

            }

            @Setting
            @Violation({"custom-payload.f", "custom-payload.f.register"})
            @ChannelFlood("REGISTER")
            public static class Register extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

            }

            @Setting
            @Violation({"custom-payload.f", "custom-payload.f.unregister"})
            @ChannelFlood("UNREGISTER")
            public static class Unregister extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

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
        public static class HeldItemA extends VlSetting implements VlSimpleSingletonRequiredContentsSetting {

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
        public static class WindowA extends VlSetting implements VlSimpleSingletonRequiredContentsSetting {

        }

        @Setting
        @Violation("window-click.b")
        public static class WindowB extends VlSetting {

        }

        @Setting
        @Violation("window-click.c")
        public static class WindowC extends VlSetting implements VlSimpleSingletonRequiredContentsSetting {

        }
    }

    @Setting
    public static class BlockInteract {

        @Setting
        @Violation("block-interact.a")
        public static class BlockInteractA extends VlSetting implements VlSimpleSingletonRequiredContentsSetting {

            public static int MAX;

            public static void load() {
                MAX = file.getInt("block-interact.a.max", 15);
            }
        }

        @Setting
        @Violation("block-interact.b")
        public static class BlockInteractB extends VlSetting {

        }

        @Setting
        @Violation("block-interact.c")
        public static class BlockInteractC extends VlSetting implements VlSimpleSingletonRequiredContentsSetting {

        }

        @Setting
        @Violation("block-interact.d")
        public static class BlockInteractD extends VlSetting implements VlSimpleSingletonRequiredContentsSetting {

        }

        @Setting
        @Violation("block-interact.e")
        public static class BlockInteractE extends VlSetting implements VlSimpleSingletonRequiredContentsSetting {

            public static int MAX;

            public static void load() {
                MAX = file.getInt("block-interact.e.max", 15);
            }

        }
    }

    @Setting
    public static class TabComplete {

        @Setting
        @Violation("tab-complete.a")
        public static class TabA extends VlSetting {

        }

        @Setting
        @Violation("tab-complete.b")
        public static class TabB extends VlSetting implements VlRequiredContentsSetting {

            public TabB() { super (PacketLimitHandlerType.BUFFER_THROTTLE); }

            @Override
            public Set<VlSettingImplType> getRequiredTypes() {
                return Sets.newHashSet(
                        VlSettingImplType.THRESHOLD,
                        VlSettingImplType.UNTIL,
                        VlSettingImplType.DECAY,
                        VlSettingImplType.DECAY_AMOUNT,
                        VlSettingImplType.VLS_AMOUNT
                );
            }
        }
    }

    @Setting
    public static class Position {

        @Setting
        @Violation("position.a")
        public static class PositionA extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

        }

        @Setting
        @Violation("position.b")
        public static class PositionB extends VlSetting implements VlSimpleSingletonRequiredContentsSetting {

            public static double MAX;
            public static double MIN;

            public static void load() {
                MAX = file.getDouble("position.b.max", 15);
                MIN = file.getDouble("position.b.min", 0.00001);
            }
        }

        @Setting
        @Violation("position.c")
        public static class PositionC extends VlSetting implements VlRequiredContentsSetting {

            public PositionC() {
                super(PacketLimitHandlerType.BUFFER_OVERFLOW);
            }

            @Override
            public Set<VlSettingImplType> getRequiredTypes() {
                return Sets.newHashSet(
                        VlSettingImplType.THRESHOLD,
                        VlSettingImplType.DECAY,
                        VlSettingImplType.DECAY_AMOUNT,
                        VlSettingImplType.VLS_AMOUNT
                );
            }
        }

        @Setting
        @Violation("position.d")
        public static class PositionD extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

        }

        @Setting
        @Violation("position.e")
        public static class PositionE extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

        }

        @Setting
        @Violation("position.f")
        public static class PositionF extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

        }

        @Setting
        @Violation("position.g")
        public static class PositionG extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

        }

        @Setting
        @Violation("position.h")
        public static class PositionH extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

        }

        @Setting
        @Violation("position.i")
        public static class PositionI extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

        }

        @Setting
        @Violation("position.j")
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
        @Violation("rotation.a")
        public static class RotationA extends VlSetting implements VlSimpleSingletonRequiredContentsSetting {

            public static int MAX;

            public static void load() {
                MAX = file.getInt("rotation.a.max", 500);
            }
        }

        @Setting
        @Violation("rotation.b")
        public static class RotationB extends VlSetting implements VlSimpleSingletonRequiredContentsSetting {

        }

        @Setting
        @Violation("rotation.c")
        public static class RotationC extends VlSetting implements VlRequiredContentsSetting {

            public RotationC() { super(PacketLimitHandlerType.BUFFER_OVERFLOW); }

            @Override
            public Set<VlSettingImplType> getRequiredTypes() {
                return Sets.newHashSet(
                        VlSettingImplType.THRESHOLD,
                        VlSettingImplType.DECAY,
                        VlSettingImplType.DECAY_AMOUNT,
                        VlSettingImplType.VLS_AMOUNT
                );
            }
        }
    }

    @Setting
    public static class Creative {

        @Setting
        @Violation("set-creative-slot.a")
        public static class CreativeA extends VlSetting implements VlSimpleSingletonRequiredContentsSetting {

        }

        @Setting
        @Violation("set-creative-slot.b")
        public static class CreativeB extends VlSetting implements VlSimpleSingletonRequiredContentsSetting {

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
        public static class NettyB extends VlSetting implements VlSimpleSingletonRequiredContentsSetting {

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
        public static class NettyE extends VlSetting implements VlRequiredContentsSetting {

            public NettyE() { super(PacketLimitHandlerType.SINGLE_OVERFLOW); }

            @Override
            public Set<VlSettingImplType> getRequiredTypes() {
                return Collections.singleton(VlSettingImplType.THRESHOLD);
            }
        }

        @Setting
        @Violation("netty.f")
        public static class NettyF extends VlSetting implements VlRequiredContentsSetting {

            public NettyF() {
                super(PacketLimitHandlerType.BUFFER_OVERFLOW);
            }

            @Override
            public Set<VlSettingImplType> getRequiredTypes() {
                return Sets.newHashSet(
                        VlSettingImplType.THRESHOLD,
                        VlSettingImplType.DECAY,
                        VlSettingImplType.DECAY_AMOUNT
                );
            }
        }

        @Setting
        @Violation("netty.g")
        public static class NettyG extends VlSetting implements VlRequiredContentsSetting {

            public NettyG() { super(PacketLimitHandlerType.BUFFER_THROTTLE); }

            @Override
            public Set<VlSettingImplType> getRequiredTypes() {
                return Sets.newHashSet(
                        VlSettingImplType.UNTIL,
                        VlSettingImplType.THRESHOLD,
                        VlSettingImplType.DECAY,
                        VlSettingImplType.DECAY_AMOUNT
                );
            }
        }

        @Setting
        @Violation("netty.h")
        public static class NettyH extends VlSetting implements VlRequiredContentsSetting {

            public NettyH() { super(PacketLimitHandlerType.SINGLE_OVERFLOW); }

            @Override
            public Set<VlSettingImplType> getRequiredTypes() {
                return Collections.singleton(VlSettingImplType.THRESHOLD);
            }
        }

        @Setting
        @Violation("netty.i")
        public static class NettyI extends VlSetting {

            public static int MAX;

            public static void load() {
                MAX = file.getInt("netty.i.max", 550);
            }
        }

        @Setting
        @Violation("netty.j")
        public static class NettyJ extends VlSetting implements VlRequiredContentsSetting {

            public NettyJ() { super(PacketLimitHandlerType.BUFFER_THROTTLE); }

            @Override
            public Set<VlSettingImplType> getRequiredTypes() {
                return Sets.newHashSet(
                        VlSettingImplType.THRESHOLD,
                        VlSettingImplType.UNTIL,
                        VlSettingImplType.DECAY,
                        VlSettingImplType.DECAY_AMOUNT
                );
            }
        }
    }

    @Setting
    public static class UseEntity {

        @Setting
        @Violation("use-entity.a")
        public static class UseEntityA extends VlSetting implements VlSimpleSingletonRequiredContentsSetting {

        }

        @Setting
        @Violation("use-entity.b")
        public static class UseEntityB extends VlSetting implements VlSimpleSingletonRequiredContentsSetting {

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
        @Violation("packet-flood.common.a")
        public static class FloodA extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

        }

        @Setting
        @Violation("packet-flood.common.b")
        public static class FloodB extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

        }

        @Setting
        @Violation("packet-flood.c")
        public static class FloodC extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

        }

        @Setting
        public static class FloodD {

            @Flood(PacketType.Play.Client.ABILITIES)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.abilities"})
            public static class Abilities extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.ARM_ANIMATION)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.arm-animation"})
            public static class ArmAnimation extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {
            }

            @Flood(PacketType.Play.Client.AUTO_RECIPE)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.auto-recipe"})
            public static class AutoRecipe extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.BLOCK_DIG)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.block-dig"})
            public static class BlockDig extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.BLOCK_PLACE)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.block-place"})
            public static class BlockPlace extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.CLIENT_COMMAND)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.client-command"})
            public static class ClientCommand extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.CLOSE_WINDOW)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.close-window"})
            public static class CloseWindow extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

            }


            @Flood(PacketType.Play.Client.CHAT)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.chat"})
            public static class Chat extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.CUSTOM_PAYLOAD)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.custom-payload"})
            public static class CustomPayload extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.HELD_ITEM_SLOT)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.held-item-slot"})
            public static class HeldItemSlot extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.ENCHANT_ITEM)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.enchant-item"})
            public static class EnchantItem extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.ENTITY_ACTION)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.entity-action"})
            public static class EntityAction extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.KEEP_ALIVE)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.keep-alive"})
            public static class KeepAlive extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.LOOK)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.look"})
            public static class Look extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.FLYING)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.flying"})
            public static class Flying extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.SET_CREATIVE_SLOT)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.set-creative-slot"})
            public static class SetCreativeSlot extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.SPECTATE)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.spectate"})
            public static class Spectate extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.STEER_VEHICLE)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.steer-vehicle"})
            public static class SteerVehicle extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

            }

            @Flood({PacketType.Play.Client.POSITION, PacketType.Play.Client.POSITION_LOOK})
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.position"})
            public static class Position extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.UPDATE_SIGN)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.update-sign"})
            public static class UpdateSign extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.USE_ITEM)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.use-item"})
            public static class UseItem extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.USE_ENTITY)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.use-entity"})
            public static class UseEntity extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.TAB_COMPLETE)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.tab-complete"})
            public static class TabComplete extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

            }

            @Flood(PacketType.Play.Client.WINDOW_CLICK)
            @Setting
            @Violation({"packet-flood.d", "packet-flood.d.window-click"})
            public static class WindowClick extends VlSetting implements VlSimpleIntervalRequiredContentsSetting {

            }
        }
    }
}
