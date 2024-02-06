package me.alek.honey.services;

import com.google.common.collect.Sets;
import io.netty.channel.Channel;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.AbstractPacketListener;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.impl.FloodListener;
import me.alek.honey.checks.types.HoneyCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.configuration.PacketLimitHandlerType;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.injector.DuplexChannelHandler;
import me.alek.honey.netty.packet.CancellablePacketObserver;
import me.alek.honey.netty.packet.PacketBundleFilter;
import me.alek.honey.netty.packet.PacketBundleSaver;
import me.alek.honey.netty.utility.NMSUtils;
import me.alek.honey.utility.BungeeMessenger;
import me.alek.honey.utility.Tuple2;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class ViolationService {

    private final Map<UUID, ViolationContext> violatedMap = new ConcurrentHashMap<>();
    private final List<UUID> punishing = new CopyOnWriteArrayList<>();
    private final LoggerService loggerService = HoneyPlugin.get().getLoggerService();
    private final ReentrantLock punishLock = new ReentrantLock();

    public void reload() {

        // update the setting for the ignoring flood handlers
        ConfigurationOptions.VlSetting sharedIgnoringSetting = FloodListener.getSharedIgnoringSetting();
        if (sharedIgnoringSetting != null) {
            // set the interval of this setting to be the spike interval specified in the configuration
            sharedIgnoringSetting.interval = ConfigurationOptions.Exempt.SPIKE_INTERVAL;
        }

        // loop through all the players and update their packet limit handlers
        for (Player player : Bukkit.getOnlinePlayers()) {
            violatedMap.remove(player.getUniqueId());
        }
    }

    public List<UUID> getPunishing() {
        return punishing;
    }

    public Map<UUID, ViolationContext> getViolatedMap() {
        return violatedMap;
    }

    public boolean isPunishing(Player player) {
        if (player == null)
            return false;

        if (punishing.isEmpty())
            return false; // optimized

        return punishing.contains(player.getUniqueId());
    }

    public synchronized void add(Player player) {
        if (violatedMap.containsKey(player.getUniqueId()))
            return;

        violatedMap.put(player.getUniqueId(), new ViolationContext(player));
        punishing.remove(player.getUniqueId());
    }

    public void remove(Player player) {
        violatedMap.remove(player.getUniqueId());
    }

    public synchronized ViolationContext getViolationContext(Player player) {
        if (!violatedMap.containsKey(player.getUniqueId()))
            add(player);

        return violatedMap.get(player.getUniqueId());
    }

    public ViolationDistributor getLimits(Player player, CheckKey checkKey, ConfigurationOptions.VlSetting setting) {
        final ViolationContext context = getViolationContext(player);
        return context.getLimits(checkKey, setting);
    }

    public boolean flag(
            Player player,
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
        return getLimits(player, violationContextKey, setting)
                .flag(checkKey, handle, cancellablePacketObserver, check, amount, onPunish, onViolation, onFlag, developmentData, ignoreSpike);
    }

    public boolean flag(
            Player player,
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

    public void punish(Runnable onPunish, Player player, String violationName, CheckKey checkKey) {
        if (onPunish != null) onPunish.run();
        try {
            punishLock.lock();
            if (punishing.contains(player.getUniqueId())) {
                return;
            }
            punishing.add(player.getUniqueId());

            loggerService.logToMonitors(player, null);

            Channel channel = NMSUtils.getChannel(player);
            if (channel != null) {
                channel.eventLoop().execute(() -> {
                    BungeeMessenger.sendPluginMessage(player, "kickPlayer", player.getName(), "Disconnected");
                    Bukkit.getServer().getScheduler().runTask(HoneyPlugin.get().getPlugin(), () -> player.kickPlayer("Disconnected"));

                    if (channel.isActive()) {
                        channel.close();
                    }
                });
            }
            HoneyPlugin.get().getNotificationService().warn(player, violationName, checkKey);
            HoneyPlugin.get().getLoggerService().logPunish(player.getName(), violationName);

        } finally {
            punishLock.unlock();
        }
        new BukkitRunnable() {

            @Override
            public void run() {
                if (!player.isOnline()) {
                    punishing.remove(player.getUniqueId());
                }
            }
        }.runTaskLaterAsynchronously(HoneyPlugin.get().getPlugin(), 5L);
    }

    public class ViolationContext {

        private final Map<CheckKey, ViolationDistributor> packetLimitMap = new ConcurrentHashMap<>();
        private final Player player;

        public ViolationContext(Player player) {
            this.player = player;
        }

        public synchronized ViolationDistributor getLimits(CheckKey key, ConfigurationOptions.VlSetting setting) {
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

        public Player getPlayer() {
            return player;
        }
    }

    private AbstractPacketLimitHandler generatePacketLimitHandler(PacketLimitHandlerType type, Player player, ConfigurationOptions.VlSetting setting) {
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

    public class KeyedViolationLookupGenerator {

        private final AbstractPacketLimitHandler handler;
        private final ConfigurationOptions.VlSetting setting;

        public KeyedViolationLookupGenerator(AbstractPacketLimitHandler handler, ConfigurationOptions.VlSetting setting) {
            this.handler = handler;
            this.setting = setting;
        }

        public Map<CheckKey, AbstractPacketLimitHandler> generateMap(Player player) {
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

        private final Player player;
        private final Map<CheckKey, AbstractPacketLimitHandler> packetLimitHandlerDelegateLookup = new HashMap<>();
        private AbstractPacketLimitHandler cachedDefaultPacketLimitHandler;

        public ViolationDistributor(Player player, KeyedViolationLookupGenerator generator) {
            this.player = player;
            packetLimitHandlerDelegateLookup.putAll(generator.generateMap(player));
        }

        public ViolationDistributor(Player player, AbstractPacketLimitHandler defaultPacketLimitHandler) {
            this.player = player;
            packetLimitHandlerDelegateLookup.put(CheckKey.DEFAULT, defaultPacketLimitHandler);
        }

        public AbstractPacketLimitHandler getAnyPacketLimitHandler() {
            if (packetLimitHandlerDelegateLookup.isEmpty()) return null;

            return packetLimitHandlerDelegateLookup.values().iterator().next();
        }

        public Player getPlayer() {
            return this.player;
        }

        public AbstractPacketLimitHandler getPacketLimitHandler(CheckKey key) {
            return packetLimitHandlerDelegateLookup.getOrDefault(key, getAnyPacketLimitHandler());
        }

        public Map<CheckKey, AbstractPacketLimitHandler> getPacketLimitHandlers() {
            return packetLimitHandlerDelegateLookup;
        }

        public synchronized boolean flag(CheckKey checkKey, Object handle, CancellablePacketObserver cancellablePacketObserver,
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

    public abstract class AbstractPacketLimitHandler {

        private final Player player;
        private final ConfigurationOptions.VlSetting setting;

        private final AtomicLong lastViolationClear = new AtomicLong();
        private final AtomicInteger violations = new AtomicInteger();

        public AbstractPacketLimitHandler(Player player, ConfigurationOptions.VlSetting setting) {
            this.player = player;
            this.setting = setting;

            this.lastViolationClear.set(System.currentTimeMillis());
        }

        public Player getPlayer() {
            return player;
        }

        public ConfigurationOptions.VlSetting getSetting() {
            return setting;
        }

        public synchronized void doFormatStuff(HoneyCheck check, CheckContextContainer.DevelopmentData developmentData, CheckKey checkKey) {
            if (ConfigurationOptions.PacketDump.ENABLED) {
                PacketBundleFilter filter = check.getDumpFilter();
                List<PacketContainer<?>> dumpedPackets = DuplexChannelHandler.getPacketBundleSavers().get(player.getUniqueId())
                        .get(Sets.newHashSet(filter.getAcceptedPacketTypes()), filter.size());

                if (dumpedPackets != null) {
                    HoneyPlugin.get().getLoggerService().dumpPackets(dumpedPackets);
                }
            }
            if (developmentData != null) {
                AbstractPacketListener.applyQuotients(player, developmentData);
                applySettingsToDevelopmentData(developmentData);
            }

            HoneyPlugin.get().getNotificationService().notify(getPlayer(), check.getName(), checkKey, getSetting(), this, developmentData);

            try {
                HoneyPlugin.get().getSQLManager().postViolation(player, check.getName(), ChatColor.stripColor(HoneyPlugin.get().getNotificationService().formatNotification(setting, this)), developmentData);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        public synchronized boolean violate(Object handle, CancellablePacketObserver cancellablePacketObserver, HoneyCheck check, Runnable onPunish, Runnable onViolation, CheckContextContainer.DevelopmentData developmentData, CheckKey checkKey) {
            if (System.currentTimeMillis() - lastViolationClear.get() >= ConfigurationOptions.General.CLEAR_OLD_VIOLATIONS_TIMESTAMP) {
                violations.set(0);
                lastViolationClear.set(System.currentTimeMillis());
            }
            if (loggerService.isDebug() && handle != null)
                HoneyPlugin.get().getLoggerService().logDebug(LoggerService.LoggingStrategy.VIOLATE, handle, getPlayer().getName(), check.getName());

            if (onViolation != null) onViolation.run();
            if (cancellablePacketObserver != null) cancellablePacketObserver.cancel();

            violations.incrementAndGet();

            PacketBundleFilter filter = check.getDumpFilter();
            doFormatStuff(check, developmentData, checkKey);

            if (violations.get() < getSetting().vlsAmount) return false;

            punish(onPunish, getPlayer(), check.getName(), checkKey);
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

    public class PacketTriggerThrottleHandler extends AbstractPacketLimitHandler {

        private double lastFlag;
        private boolean hasShownFlag;

        public PacketTriggerThrottleHandler(Player player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
        }

        @Override
        public boolean flag(Object handle, CancellablePacketObserver cancellablePacketObserver, HoneyCheck check, double amount, Runnable onPunish, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, boolean ignoreSpike, CheckKey checkKey) {
            if (amount == 1 || System.currentTimeMillis() - lastFlag < getSetting().throttleAfter) {
                if (onFlag != null) onFlag.run();

                if (amount == 1) lastFlag = System.currentTimeMillis();
                if (cancellablePacketObserver != null) cancellablePacketObserver.cancel();

                if (!hasShownFlag)
                    doFormatStuff(check, developmentData, checkKey);
                hasShownFlag = true;
                return true;
            }
            hasShownFlag = false;
            return false;
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.TRIGGER_THROTTLE;
        }
    }

    public class PacketSimpleHandler extends AbstractPacketLimitHandler {


        public PacketSimpleHandler(Player player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
        }

        @Override
        public boolean flag(Object handle, CancellablePacketObserver cancellablePacketObserver, HoneyCheck check, double amount, Runnable onPunish, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, boolean ignoreSpike, CheckKey checkKey) {
            if (onFlag != null) onFlag.run();

            violate(handle, cancellablePacketObserver, check, onPunish, onViolation, developmentData, checkKey);
            return true;
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.SIMPLE;
        }
    }

    public class PacketSingleOverflowHandler extends AbstractPacketLimitHandler {

        private final double threshold;
        private double amount;

        public PacketSingleOverflowHandler(Player player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
            threshold = getSetting().threshold;
        }

        @Override
        public boolean flag(Object handle, CancellablePacketObserver cancellablePacketObserver, HoneyCheck check, double amount, Runnable onPunish, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, boolean ignoreSpike, CheckKey checkKey) {
            if (onFlag != null) onFlag.run();

            if ((this.amount = amount) < threshold) return false;

            violate(handle, cancellablePacketObserver, check, onPunish, onViolation, developmentData, checkKey);
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

    public abstract class PacketBufferHandler extends AbstractPacketLimitHandler {

        protected final int decayInterval;
        protected final int decayAmount;
        protected final double threshold;
        protected double bufferIndexCopy;
        protected boolean hasDecayed;

        protected long lastDecay = System.currentTimeMillis();
        protected double bufferIndex;


        public PacketBufferHandler(Player player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);

            decayInterval = getSetting().decay;
            decayAmount = getSetting().decayAmount;
            threshold = getSetting().threshold;
        }

        // put into its own method, so it can be overwritten by the buffer throttle
        public void decay() {
            bufferIndex -= decayAmount;
        }

        @Override
        public synchronized boolean flag(Object handle, CancellablePacketObserver cancellablePacketObserver, HoneyCheck check, double amount, Runnable onPunish, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, boolean ignoreSpike, CheckKey checkKey) {
            hasDecayed = false;
            if (System.currentTimeMillis() - lastDecay > decayInterval) {
                hasDecayed = true;
                lastDecay = System.currentTimeMillis();
                decay();
            }
            if (bufferIndex < 0) bufferIndex = 0;
            bufferIndex += amount;

            if (onFlag != null) onFlag.run();

            if (loggerService.isDebug())
                HoneyPlugin.get().getLoggerService().logDebug(LoggerService.LoggingStrategy.FLAG, handle, getPlayer().getName(), check.getName());

            if (bufferIndex < threshold) return false;

            bufferIndexCopy = bufferIndex;
            if (getSetting().clearBuffer) this.bufferIndex = 0;

            return handle(handle, cancellablePacketObserver, check, onPunish, onViolation, developmentData, checkKey);
        }

        public double getBufferIndex() {
            return bufferIndexCopy;
        }

        public abstract boolean handle(Object handle, CancellablePacketObserver cancellablePacketObserver, HoneyCheck check, Runnable onPunish, Runnable onViolation, CheckContextContainer.DevelopmentData developmentData, CheckKey checkKey);

    }

    public class PacketBufferThrottleHandler extends PacketBufferHandler {

        private final double until;

        public PacketBufferThrottleHandler(Player player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
            until = getSetting().until;
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.BUFFER_THROTTLE;
        }

        @Override
        public void decay() {
            if (bufferIndex < threshold) {
                super.decay();
                return;
            }
            double ratio = (int) ((bufferIndex - threshold) / (decayAmount * 1.8));
            double amount = decayAmount * Math.pow(1.4, ratio);

            bufferIndex -= amount;
        }

        @Override
        public boolean handle(Object handle, CancellablePacketObserver cancellablePacketObserver, HoneyCheck check, Runnable onPunish, Runnable onViolation, CheckContextContainer.DevelopmentData developmentData, CheckKey checkKey) {
            if (cancellablePacketObserver != null) cancellablePacketObserver.cancel();

            if (bufferIndex > until && getSetting().isEnabled())
                violate(handle, cancellablePacketObserver, check, null, null, developmentData, checkKey);
            return true;
        }
    }

    public class PacketBufferOverflowHandler extends PacketBufferHandler {


        public PacketBufferOverflowHandler(Player player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
        }

        @Override
        public boolean handle(Object handle, CancellablePacketObserver cancellablePacketObserver, HoneyCheck check, Runnable onPunish, Runnable onViolation, CheckContextContainer.DevelopmentData developmentData, CheckKey checkKey) {
            violate(handle, cancellablePacketObserver, check, onPunish, onViolation, developmentData, checkKey);
            return true;
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.BUFFER_OVERFLOW;
        }
    }

    public class PacketIntervalHandler extends AbstractPacketLimitHandler {

        private final double maxPerInterval;
        private final int interval;

        private boolean violatedForInterval;
        private boolean ignoringSpikeForInterval;
        private long intervalStart = System.currentTimeMillis();
        private double flagged;
        public double flaggedCopy;

        public PacketIntervalHandler(Player player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);

            this.maxPerInterval = getSetting().maxPerInterval;
            this.interval = getSetting().interval;
        }

        private synchronized void clearInterval() {
            violatedForInterval = false;
            ignoringSpikeForInterval = false;
            flagged = 0;
            intervalStart = System.currentTimeMillis();
        }

        public synchronized boolean flag(Object handle, CancellablePacketObserver cancellablePacketObserver, HoneyCheck check, double amount, Runnable onPunish, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, boolean ignoreSpike, CheckKey checkKey) {
            if (System.currentTimeMillis() - intervalStart >= interval)
                clearInterval();

            if (ignoreSpike) {
                clearInterval();
                ignoringSpikeForInterval = true;
            }
            if (ignoringSpikeForInterval) {
                if (cancellablePacketObserver != null) cancellablePacketObserver.cancel();

                if (HoneyPlugin.get().getLoggerService().isDebug())
                    HoneyPlugin.get().getLoggerService().getLogger().info("DEBUG: ignoring packet spike for interval check " + check);
                return false;
            }
            if (onFlag != null) onFlag.run();

            flagged += amount;
            if (flagged < maxPerInterval) return false;

            if (cancellablePacketObserver != null) cancellablePacketObserver.cancel();

            if (violatedForInterval) return true;

            violatedForInterval = true;
            flaggedCopy = flagged;
            if (getSetting().clearInterval) clearInterval();

            violate(handle, cancellablePacketObserver, check, onPunish, onViolation, developmentData, checkKey);
            return true;
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.INTERVAL;
        }
    }

    public class PacketIntervalThrottleHandler extends AbstractPacketLimitHandler {

        private final int interval;
        private final double threshold;
        private final double until;

        private double amount;
        private boolean currentlyCancelling;
        private long intervalStart = System.currentTimeMillis();

        public PacketIntervalThrottleHandler(Player player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);

            threshold = getSetting().threshold;
            until = getSetting().until;
            interval = getSetting().interval;
        }

        @Override
        public synchronized boolean flag(Object handle, CancellablePacketObserver cancellablePacketObserver, HoneyCheck check, double amount, Runnable onPunish, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, boolean ignoreSpike, CheckKey checkKey) {
            if (currentlyCancelling) {
                if (System.currentTimeMillis() - intervalStart > interval) {
                    intervalStart = System.currentTimeMillis();
                    currentlyCancelling = false;
                }
                else cancellablePacketObserver.cancel();
            }
            if ((this.amount = amount) > threshold) {
                intervalStart = System.currentTimeMillis();

                if (amount > until && !currentlyCancelling) {
                    violate(handle, cancellablePacketObserver, check, onPunish, onViolation, developmentData, checkKey);
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
