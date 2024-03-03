package me.alek.honey.services;

import com.google.common.collect.Sets;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.AbstractPacketListener;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.impl.FloodListener;
import me.alek.honey.checks.types.HoneyCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.configuration.PacketLimitHandlerType;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.CancellablePacketObserver;
import me.alek.honey.netty.packet.EventloopState;
import me.alek.honey.netty.packet.bundle.PacketBundleFilter;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.utility.BungeeMessenger;
import me.alek.honey.utility.Tuple2;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ViolationService {

    private static ViolationService INSTANCE;

    public ViolationService() { INSTANCE = this; }

    public void reload() {

        // update the setting for the ignoring flood handlers
        ConfigurationOptions.VlSetting sharedIgnoringSetting = FloodListener.getSharedIgnoringSetting();
        if (sharedIgnoringSetting != null) {
            // set the interval of this setting to be the spike interval specified in the configuration
            sharedIgnoringSetting.interval = ConfigurationOptions.SpikeFlood.SPIKE_INTERVAL;
        }

        // loop through all the players and update their packet limit handlers
        for (HoneyPlayer player : HoneyPlayer.getAllPlayers()) {
            player.violationContext = new ViolationContext(player);
        }
    }

    public ViolationDistributor getDistributor(HoneyPlayer player, CheckKey checkKey, ConfigurationOptions.VlSetting setting) {
        return player.violationContext.getLimits(checkKey, setting);
    }

    public boolean flag(HoneyPlayer player,
                        Object handle,
                        CancellablePacketObserver cancellablePacketObserver,
                        CheckKey violationContextKey,
                        CheckKey checkKey,
                        double amount,
                        Runnable onPunish,
                        Runnable onViolation,
                        Runnable onFlag,
                        HoneyCheck check,
                        ConfigurationOptions.VlSetting setting,
                        CheckContextContainer.DevelopmentData developmentData,
                        boolean ignoreSpike
    ) {
        return getDistributor(player, violationContextKey, setting)
                .flag(checkKey, handle, cancellablePacketObserver, check, amount, onPunish, onViolation, onFlag, developmentData, ignoreSpike);
    }

    public boolean flag(HoneyPlayer player,
                        Object handle,
                        CancellablePacketObserver cancellablePacketObserver,
                        CheckKey violationContextKey,
                        CheckKey checkKey,
                        double amount,
                        HoneyCheck check,
                        ConfigurationOptions.VlSetting setting,
                        CheckContextContainer.DevelopmentData developmentData,
                        boolean ignoreSpike
    ) {
        return flag(player, handle, cancellablePacketObserver, violationContextKey, checkKey, amount, null, null, null, check, setting, developmentData, ignoreSpike);
    }

    public synchronized void punish(Runnable onPunish, HoneyPlayer player, String violationName, CheckKey checkKey) {
        if (onPunish != null) onPunish.run();

        if (player.punished) return;

        player.channel.eventLoop().execute(() -> {
            if (player.channel.isActive()) {
                player.channel.close();
            }
        });
        BungeeMessenger.sendPluginMessage(player.player, "kickPlayer", player.name, "Disconnected");
        Bukkit.getServer().getScheduler().runTask(HoneyPlugin.get().getPlugin(), () -> player.player.kickPlayer("Disconnected"));

        HoneyPlugin.get().getNotificationService().warn(player, violationName, checkKey);

        player.punished = true;
    }

    public static class ViolationContext {

        private final Map<CheckKey, ViolationDistributor> packetLimitMap = new ConcurrentHashMap<>();
        private final HoneyPlayer player;

        public ViolationContext(HoneyPlayer player) {
            this.player = player;
        }

        public ViolationDistributor getLimits(CheckKey key, ConfigurationOptions.VlSetting setting) {
            if (!packetLimitMap.containsKey(key)) {

                final AbstractPacketLimitHandler packetLimitHandler = generatePacketLimitHandler(setting.getPacketLimitHandlerType(), player, setting);
                if (packetLimitHandler == null)
                    throw new RuntimeException("No packet limit handler could be created.");

                final ViolationDistributor distributor;
                if (setting.keyedAnnotation != null)
                    distributor = new ViolationDistributor(player,
                            new KeyedViolationLookupGenerator(packetLimitHandler, setting));
                else
                    distributor = new ViolationDistributor(player, packetLimitHandler);
                packetLimitMap.put(key, distributor);
            }
            return packetLimitMap.get(key);
        }

        public Map<CheckKey, ViolationDistributor> getPacketlimitMap() {
            return packetLimitMap;
        }

        public ViolationDistributor getHandler(CheckKey key) {
            return packetLimitMap.get(key);
        }

        public HoneyPlayer getPlayer() {
            return player;
        }
    }

    private static AbstractPacketLimitHandler generatePacketLimitHandler(PacketLimitHandlerType type, HoneyPlayer player, ConfigurationOptions.VlSetting setting) {
        switch (type) {
            case SIMPLE:
                return new PacketSimpleHandler(player, setting);
            case INTERVAL:
                return new PacketIntervalHandler(player, setting);
            case INTERVAL_THROTTLE:
                return new PacketIntervalThrottleHandler(player, setting);
            case SINGLE_OVERFLOW:
                return new PacketSingleOverflowHandler(player, setting);
            case BUFFER_OVERFLOW:
                return new PacketBufferOverflowHandler(player, setting);
            case BUFFER_THROTTLE:
                return new PacketBufferThrottleHandler(player, setting);
            case TRIGGER_THROTTLE:
                return new PacketTriggerThrottleHandler(player, setting);
            case CHIPSTATE_THROTTLE:
                return new PacketChipstateThrottleHandler(player, setting);
            default:
                return null;
        }
    }

    public static class CheckKey extends Tuple2<Object, Object> implements Cloneable {

        public static CheckKey DEFAULT = new CheckKey(null, null);

        public CheckKey(Object object1, Object object2) {
            super(object1, object2);
        }

        public CheckKey(Object object1) {
            super(object1, null);
        }

        public CheckKey() {
            super(UUID.randomUUID(), UUID.randomUUID());
        }

        public static CheckKey of(Object object1, Object object2) {
            return new CheckKey(object1, object2);
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public CheckKey clone() {
            try {
                return (CheckKey) super.clone();
            } catch (Exception ex) {
                throw new AssertionError();
            }
        }
    }

    public static class KeyedViolationLookupGenerator {

        private final AbstractPacketLimitHandler handler;
        private final ConfigurationOptions.VlSetting setting;

        public KeyedViolationLookupGenerator(AbstractPacketLimitHandler handler, ConfigurationOptions.VlSetting setting) {
            this.handler = handler;
            this.setting = setting;
        }

        public Map<CheckKey, AbstractPacketLimitHandler> generateMap(HoneyPlayer player) {
            if (setting.keyedAnnotation == null || setting.keyedAnnotation.keys().length == 0) {
                return new HashMap<CheckKey, AbstractPacketLimitHandler>(){{
                    put(CheckKey.DEFAULT, handler);
                }};
            }
            final Map<CheckKey, AbstractPacketLimitHandler> map = new HashMap<>();
            for (final String key : setting.keyedAnnotation.keys()) {
                map.put(new CheckKey(key), generatePacketLimitHandler(handler.getType(), player, setting));
            }
            return map;
        }
    }

    public static class ViolationDistributor {

        private final HoneyPlayer player;
        private final Map<CheckKey, AbstractPacketLimitHandler> packetLimitHandlerDelegateLookup = new HashMap<>();
        private AbstractPacketLimitHandler cachedDefaultPacketLimitHandler;

        public ViolationDistributor(HoneyPlayer player, KeyedViolationLookupGenerator generator) {
            this.player = player;
            packetLimitHandlerDelegateLookup.putAll(generator.generateMap(player));
        }

        public ViolationDistributor(HoneyPlayer player, AbstractPacketLimitHandler defaultPacketLimitHandler) {
            this.player = player;
            packetLimitHandlerDelegateLookup.put(CheckKey.DEFAULT, defaultPacketLimitHandler);
        }

        public AbstractPacketLimitHandler getAnyPacketLimitHandler() {
            if (packetLimitHandlerDelegateLookup.isEmpty()) return null;

            return packetLimitHandlerDelegateLookup.values().iterator().next();
        }

        public HoneyPlayer getPlayer() {
            return this.player;
        }

        public AbstractPacketLimitHandler getPacketLimitHandler(CheckKey key) {
            return packetLimitHandlerDelegateLookup.getOrDefault(key, getAnyPacketLimitHandler());
        }

        public Map<CheckKey, AbstractPacketLimitHandler> getPacketLimitHandlers() {
            return packetLimitHandlerDelegateLookup;
        }

        public boolean flag(CheckKey checkKey, Object handle, CancellablePacketObserver cancellablePacketObserver,
                                         HoneyCheck check, double amount, Runnable onPunish, Runnable onViolation, Runnable onFlag,
                                         CheckContextContainer.DevelopmentData developmentData, boolean ignoreSpike) {
            AbstractPacketLimitHandler delegate = null;
            if (checkKey == null || !packetLimitHandlerDelegateLookup.containsKey(checkKey)) {
                if (packetLimitHandlerDelegateLookup.isEmpty()) return false;

                delegate =
                    cachedDefaultPacketLimitHandler == null
                        ? (cachedDefaultPacketLimitHandler = packetLimitHandlerDelegateLookup.values().iterator().next())
                        : cachedDefaultPacketLimitHandler;
            }
            if (delegate == null)
                delegate = packetLimitHandlerDelegateLookup.get(checkKey);

            return delegate.flag(handle, cancellablePacketObserver, check, amount, onPunish, onViolation, onFlag, developmentData, ignoreSpike, checkKey);
        }
    }

    public static abstract class AbstractPacketLimitHandler {

        private final HoneyPlayer player;
        private final ConfigurationOptions.VlSetting setting;

        private final AtomicLong lastViolationClear = new AtomicLong();
        private final AtomicInteger violations = new AtomicInteger();

        public AbstractPacketLimitHandler(HoneyPlayer player, ConfigurationOptions.VlSetting setting) {
            this.player = player;
            this.setting = setting;

            this.lastViolationClear.set(System.currentTimeMillis());
        }

        public HoneyPlayer getPlayer() {
            return player;
        }

        public ConfigurationOptions.VlSetting getSetting() {
            return setting;
        }

        public void doFormatStuff(HoneyCheck check, CheckContextContainer.DevelopmentData developmentData, CheckKey checkKey) {
            if (ConfigurationOptions.PacketDump.VLS_ENABLED) {

                PacketBundleFilter filter = check.getDumpFilter();
                if (filter != null && filter.getAcceptedPacketTypes() != null) {

                    List<PacketContainer<?>> dumpedPackets = player.packetBundleSaver
                            .get(Sets.newHashSet(filter.getAcceptedPacketTypes()), filter.size(), true);

                    if (dumpedPackets != null) {
                        HoneyPlugin.get().getLoggerService().dumpPackets(player, dumpedPackets);
                    }
                }
            }
            if (developmentData != null) {
                AbstractPacketListener.applyQuotients(player, developmentData);
                applySettingsToDevelopmentData(developmentData);
            }

            HoneyPlugin.get().getNotificationService().notify(getPlayer(), check.getName(), checkKey, getSetting(), this, developmentData);

            try {
                HoneyPlugin.get().getSQLManager().postViolation(
                        player, check.getName() + (checkKey == null ? "" : " (" + checkKey.getT1() + ")"),
                        ChatColor.stripColor(HoneyPlugin.get().getNotificationService().formatNotification(setting, this)), developmentData);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        public boolean violate(CancellablePacketObserver cancellablePacketObserver, HoneyCheck check, Runnable onPunish, Runnable onViolation, CheckContextContainer.DevelopmentData developmentData, CheckKey checkKey) {
            if (System.currentTimeMillis() - lastViolationClear.get() >= ConfigurationOptions.General.CLEAR_OLD_VIOLATIONS_TIMESTAMP) {
                violations.set(0);
                lastViolationClear.set(System.currentTimeMillis());
            }

            if (onViolation != null) onViolation.run();
            cancellablePacketObserver.cancel();

            violations.incrementAndGet();

            doFormatStuff(check, developmentData, checkKey);

            if (violations.get() < getSetting().vlsAmount) return false;

            INSTANCE.punish(onPunish, getPlayer(), check.getName(), checkKey);
            return true;
        }

        private void applySettingsToDevelopmentData(CheckContextContainer.DevelopmentData developmentData) {
            developmentData.addEmpty();

            PacketLimitHandlerType handlerType = setting.getPacketLimitHandlerType();
            developmentData.addHeader("Settings", (handlerType == PacketLimitHandlerType.SIMPLE ? null : setting.getPacketLimitHandlerType().name().toLowerCase().replace("_", " ")));

            for (Map.Entry<String, Object> valueEntry : setting.getValues()) {

                 // add the line to the development data
                 developmentData.addLine(valueEntry.getKey() + ": " +valueEntry.getValue());
            }
        }

        public int getViolations() {
            return violations.get();
        }

        public abstract boolean flag(Object handle, CancellablePacketObserver cancellablePacketObserver, HoneyCheck check, double amount, Runnable onPunish, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, boolean ignoreSpike, CheckKey checkKey);

        public abstract PacketLimitHandlerType getType();
    }

    public static class PacketChipstateThrottleHandler extends AbstractPacketLimitHandler {

        public PacketChipstateThrottleHandler(HoneyPlayer player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
        }

        @Override
        public boolean flag(Object handle, CancellablePacketObserver cancellablePacketObserver, HoneyCheck check, double amount, Runnable onPunish, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, boolean ignoreSpike, CheckKey checkKey) {
            if (amount == 1) {
                if (onFlag != null) onFlag.run();
                cancellablePacketObserver.cancel();

                return true;
            }
            return false;
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.CHIPSTATE_THROTTLE;
        }
    }

    public static class PacketTriggerThrottleHandler extends AbstractPacketLimitHandler {

        private double lastFlag;

        public PacketTriggerThrottleHandler(HoneyPlayer player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
        }

        @Override
        public boolean flag(Object handle, CancellablePacketObserver cancellablePacketObserver, HoneyCheck check, double amount, Runnable onPunish, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, boolean ignoreSpike, CheckKey checkKey) {
            if (amount >= 1 || System.currentTimeMillis() - lastFlag < getSetting().throttleAfter) {
                if (onFlag != null) onFlag.run();

                if (amount >= 1) lastFlag = System.currentTimeMillis();
                cancellablePacketObserver.cancel();

                return true;
            }
            return false;
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.TRIGGER_THROTTLE;
        }
    }

    public static class PacketSimpleHandler extends AbstractPacketLimitHandler {


        public PacketSimpleHandler(HoneyPlayer player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
        }

        @Override
        public boolean flag(Object handle, CancellablePacketObserver cancellablePacketObserver, HoneyCheck check, double amount, Runnable onPunish, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, boolean ignoreSpike, CheckKey checkKey) {
            if (onFlag != null) onFlag.run();

            violate(cancellablePacketObserver, check, onPunish, onViolation, developmentData, checkKey);
            return true;
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.SIMPLE;
        }
    }

    public static class PacketSingleOverflowHandler extends AbstractPacketLimitHandler {

        private double amount;

        public PacketSingleOverflowHandler(HoneyPlayer player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
        }

        @Override
        public boolean flag(Object handle, CancellablePacketObserver cancellablePacketObserver, HoneyCheck check, double amount, Runnable onPunish, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, boolean ignoreSpike, CheckKey checkKey) {
            if (onFlag != null) onFlag.run();

            if ((this.amount = amount) < getSetting().threshold) return false;

            violate(cancellablePacketObserver, check, onPunish, onViolation, developmentData, checkKey);
            return true;
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.SINGLE_OVERFLOW;
        }

        public double getAmount() {
            return amount;
        }
    }

    public static abstract class PacketBufferHandler extends AbstractPacketLimitHandler {

        protected double bufferIndexCopy;
        protected boolean hasDecayed;

        protected long lastDecay = System.currentTimeMillis();
        protected double bufferIndex;


        public PacketBufferHandler(HoneyPlayer player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
        }

        // put into its own method, so it can be overwritten by the buffer throttle
        public void decay() {
            bufferIndex -= getSetting().decayAmount;
        }

        @Override
        public boolean flag(Object handle, CancellablePacketObserver cancellablePacketObserver, HoneyCheck check, double amount, Runnable onPunish, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, boolean ignoreSpike, CheckKey checkKey) {
            hasDecayed = false;
            if (System.currentTimeMillis() - lastDecay > getSetting().decay) {
                hasDecayed = true;
                lastDecay = System.currentTimeMillis();
                decay();
            }
            if (bufferIndex < 0) bufferIndex = 0;
            bufferIndex += amount;

            if (onFlag != null) onFlag.run();

            if (bufferIndex < getSetting().threshold) return false;

            bufferIndexCopy = bufferIndex;
            if (getSetting().clearBuffer) this.bufferIndex = 0;

            return handle(handle, cancellablePacketObserver, check, onPunish, onViolation, developmentData, checkKey);
        }

        public double getBufferIndex() {
            return bufferIndexCopy;
        }

        public abstract boolean handle(Object handle, CancellablePacketObserver cancellablePacketObserver, HoneyCheck check, Runnable onPunish, Runnable onViolation, CheckContextContainer.DevelopmentData developmentData, CheckKey checkKey);

    }

    public static class PacketBufferThrottleHandler extends PacketBufferHandler {


        public PacketBufferThrottleHandler(HoneyPlayer player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.BUFFER_THROTTLE;
        }

        @Override
        public void decay() {
            if (bufferIndex < getSetting().threshold) {
                super.decay();
                return;
            }
            double ratio = (int) ((bufferIndex - getSetting().threshold) / (getSetting().decayAmount * 1.8));
            double amount = getSetting().decayAmount * Math.pow(1.4, ratio);

            bufferIndex -= amount;
        }

        @Override
        public boolean handle(Object handle, CancellablePacketObserver cancellablePacketObserver, HoneyCheck check, Runnable onPunish, Runnable onViolation, CheckContextContainer.DevelopmentData developmentData, CheckKey checkKey) {
            cancellablePacketObserver.cancel();

            if (bufferIndex > getSetting().until && getSetting().isEnabled())
                violate(cancellablePacketObserver, check, null, null, developmentData, checkKey);
            return true;
        }
    }

    public static class PacketBufferOverflowHandler extends PacketBufferHandler {


        public PacketBufferOverflowHandler(HoneyPlayer player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
        }

        @Override
        public boolean handle(Object handle, CancellablePacketObserver cancellablePacketObserver, HoneyCheck check, Runnable onPunish, Runnable onViolation, CheckContextContainer.DevelopmentData developmentData, CheckKey checkKey) {
            violate(cancellablePacketObserver, check, onPunish, onViolation, developmentData, checkKey);
            return true;
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.BUFFER_OVERFLOW;
        }
    }

    public static class PacketIntervalHandler extends AbstractPacketLimitHandler {

        private boolean violatedForInterval;
        private boolean ignoringSpikeForInterval;
        private long intervalStart = System.currentTimeMillis();
        private double flagged;
        public double flaggedCopy;

        public PacketIntervalHandler(HoneyPlayer player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
        }

        private synchronized void clearInterval() {
            violatedForInterval = false;
            ignoringSpikeForInterval = false;
            flagged = 0;
            intervalStart = System.currentTimeMillis();
        }

        public boolean flag(Object handle, CancellablePacketObserver cancellablePacketObserver, HoneyCheck check, double amount, Runnable onPunish, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, boolean ignoreSpike, CheckKey checkKey) {
            if (System.currentTimeMillis() - intervalStart >= getSetting().interval)
                clearInterval();

            if (ignoreSpike) {
                clearInterval();
                ignoringSpikeForInterval = true;
            }
            if (ignoringSpikeForInterval) {
                if (HoneyPlugin.get().getLoggerService().isDebug())
                    HoneyPlugin.get().getLoggerService().getLogger().info("DEBUG: ignoring packet spike for interval check " + check);
                return false;
            }
            if (onFlag != null) onFlag.run();

            flagged += amount;
            if (flagged < getSetting().maxPerInterval) return false;

            cancellablePacketObserver.cancel();

            if (violatedForInterval) return true;

            violatedForInterval = true;
            flaggedCopy = flagged;
            if (getSetting().clearInterval) clearInterval();

            violate(cancellablePacketObserver, check, onPunish, onViolation, developmentData, checkKey);
            return true;
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.INTERVAL;
        }
    }

    public static class PacketIntervalThrottleHandler extends AbstractPacketLimitHandler {

        private double amount;
        private boolean currentlyCancelling;
        private long intervalStart = System.currentTimeMillis();

        public PacketIntervalThrottleHandler(HoneyPlayer player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
        }

        @Override
        public boolean flag(Object handle, CancellablePacketObserver cancellablePacketObserver, HoneyCheck check, double amount, Runnable onPunish, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, boolean ignoreSpike, CheckKey checkKey) {
            if (currentlyCancelling) {
                if (System.currentTimeMillis() - intervalStart > getSetting().interval) {
                    intervalStart = System.currentTimeMillis();
                    currentlyCancelling = false;
                }
                else cancellablePacketObserver.cancel();
            }
            if ((this.amount = amount) > getSetting().threshold) {
                intervalStart = System.currentTimeMillis();

                if (amount > getSetting().until && !currentlyCancelling) {
                    violate(cancellablePacketObserver, check, onPunish, onViolation, developmentData, checkKey);
                }
                currentlyCancelling = true;
                return true;
            }
            return false;
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.INTERVAL_THROTTLE;
        }

        public double getAmount() {
            return amount;
        }
    }
}
