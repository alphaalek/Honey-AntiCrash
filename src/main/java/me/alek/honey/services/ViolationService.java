package me.alek.honey.services;

import com.google.common.util.concurrent.AtomicDouble;
import io.netty.channel.Channel;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.AbstractPacketListener;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.impl.FloodListener;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.configuration.PacketLimitHandlerType;
import me.alek.honey.netty.packet.CancellablePacketObserver;
import me.alek.honey.netty.utility.reflect.NMSUtils;
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
import java.util.stream.Collectors;

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
            sharedIgnoringSetting.interval = ConfigurationOptions.General.Exempt.SPIKE_INTERVAL;
        }

        // loop through all the players and update their packet limit handlers
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            // get the violation context of this player
            ViolationContext violationContext = violatedMap.get(uuid);

            // register a violation context instance to this player if he doesn't have any, although he should have
            if (violationContext == null) {
                addPlayer(player);
                continue;
            }

            // reload all the packet limit handlers for this player
            for (ViolationDistributor violationDistributor : violationContext.getPacketlimitMap().values()) {
                violationDistributor.reload();
            }
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

    public synchronized void addPlayer(Player player) {
        if (violatedMap.containsKey(player.getUniqueId()))
            return;

        violatedMap.put(player.getUniqueId(), new ViolationContext(player));
        punishing.remove(player.getUniqueId());
    }

    public void removePlayer(Player player) {
        violatedMap.remove(player.getUniqueId());
    }

    public synchronized ViolationContext getViolationContext(Player player) {
        if (!violatedMap.containsKey(player.getUniqueId()))
            addPlayer(player);

        return violatedMap.get(player.getUniqueId());
    }

    public ViolationDistributor getLimits(Player player, CheckKey checkKey, ConfigurationOptions.VlSetting setting) {
        ViolationContext context = getViolationContext(player);
        return context.getLimits(checkKey, setting);
    }

    public boolean flag(
            Player player,
            Object handle,
            CancellablePacketObserver cancellablePacketObserver,
            CheckKey violationContextKey,
            CheckKey checkKey,
            double amount,
            Runnable onViolation,
            Runnable onFlag,
            String violationName,
            ConfigurationOptions.VlSetting setting,
            CheckContextContainer.DevelopmentData developmentData,
            boolean ignoreSpike
    ) {
        return getLimits(player, violationContextKey, setting)
                .flag(checkKey, handle, cancellablePacketObserver, violationName, amount, onViolation, onFlag, developmentData, ignoreSpike);
    }

    public boolean flag(
            Player player,
            Object handle,
            CancellablePacketObserver cancellablePacketObserver,
            CheckKey violationContextKey,
            CheckKey checkKey,
            double amount,
            String violationName,
            ConfigurationOptions.VlSetting setting,
            CheckContextContainer.DevelopmentData developmentData,
            boolean ignoreSpike
    ) {
        return flag(player, handle, cancellablePacketObserver, violationContextKey, checkKey, amount, null, null, violationName, setting, developmentData, ignoreSpike);
    }

    public void punish(Player player, String violationName) {
        try {
            punishLock.lock();
            if (punishing.contains(player.getUniqueId())) {
                return;
            }
            punishing.add(player.getUniqueId());

            HoneyPlugin.get().getNotificationService().warn(player, violationName);
            HoneyPlugin.get().getLoggerService().logPunish(player.getName(), violationName);

            Channel channel = NMSUtils.getChannel(player);
            if (channel != null) {
                channel.eventLoop().execute(channel::close);
            }
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

                AbstractPacketLimitHandler packetLimitHandler = generatePacketLimitHandler(setting.getPacketLimitHandlerType(), player, setting);
                if (packetLimitHandler == null)
                    throw new RuntimeException("No packet limit handler could be created.");

                ViolationDistributor distributor;
                if (setting.getKeyedAnnotation() != null)
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
            if (setting.getKeyedAnnotation() == null || setting.getKeyedAnnotation().keys().length == 0) {
                return new HashMap<CheckKey, AbstractPacketLimitHandler>(){{
                    put(CheckKey.DEFAULT, handler);
                }};
            }
            final Map<CheckKey, AbstractPacketLimitHandler> map = new HashMap<>();
            for (final String key : setting.getKeyedAnnotation().keys()) {
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
            if (packetLimitHandlerDelegateLookup.size() == 0) return null;

            return packetLimitHandlerDelegateLookup.values().iterator().next();
        }

        public Player getPlayer() {
            return this.player;
        }

        public AbstractPacketLimitHandler getPacketLimitHandler(CheckKey key) {
            if (key == null) {
                return getAnyPacketLimitHandler();
            }
            return packetLimitHandlerDelegateLookup.getOrDefault(key, getAnyPacketLimitHandler());
        }

        public Map<CheckKey, AbstractPacketLimitHandler> getPacketLimitHandlers() {
            return packetLimitHandlerDelegateLookup;
        }

        public synchronized boolean flag(CheckKey checkKey, Object handle, CancellablePacketObserver cancellablePacketObserver, String violationName, double amount, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, boolean ignoreSpike) {
            AbstractPacketLimitHandler delegate = null;
            // if (violationName.equals("Netty.K"))
                // Bukkit.getLogger().info(packetLimitHandlerDelegateLookup.get(checkKey) + "");
            if (checkKey == null || !packetLimitHandlerDelegateLookup.containsKey(checkKey)) {
                if (packetLimitHandlerDelegateLookup.size() == 0) return false;

                delegate =
                    cachedDefaultPacketLimitHandler == null
                        ? (cachedDefaultPacketLimitHandler = packetLimitHandlerDelegateLookup.values().iterator().next())
                        : cachedDefaultPacketLimitHandler;
            }
            if (delegate == null)
                delegate = packetLimitHandlerDelegateLookup.get(checkKey);

            return delegate.flag(handle, cancellablePacketObserver, violationName, amount, onViolation, onFlag, developmentData, ignoreSpike);
        }

        public void reload() {
            for (final AbstractPacketLimitHandler packetLimitHandler : packetLimitHandlerDelegateLookup.values()) {
                packetLimitHandler.reload();
            }
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
            reload();
        }

        public Player getPlayer() {
            return player;
        }

        public ConfigurationOptions.VlSetting getSetting() {
            return setting;
        }

        public synchronized boolean violate(Object handle, CancellablePacketObserver cancellablePacketObserver, String violationName, Runnable onViolation, CheckContextContainer.DevelopmentData developmentData) {
            if (System.currentTimeMillis() - lastViolationClear.get() >= ConfigurationOptions.General.CLEAR_OLD_VIOLATIONS_TIMESTAMP) {
                violations.set(0);
                lastViolationClear.set(System.currentTimeMillis());
            }
            if (loggerService.isDebug() && handle != null)
                HoneyPlugin.get().getLoggerService().logDebug(LoggerService.LoggingStrategy.VIOLATE, handle, getPlayer().getName(), violationName);

            violations.incrementAndGet();
            if (developmentData != null) {
                AbstractPacketListener.applyQuotients(player, developmentData);
                applySettingsToDevelopmentData(developmentData);
            }
            HoneyPlugin.get().getNotificationService().notify(getPlayer(), violationName, getSetting(), this, developmentData);

            try {
                HoneyPlugin.get().getSQLManager().postViolation(player, violationName, ChatColor.stripColor(HoneyPlugin.get().getNotificationService().formatNotification(setting, this)), developmentData);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if (onViolation != null) onViolation.run();
            if (cancellablePacketObserver != null) cancellablePacketObserver.cancel();

            if (violations.get() < getSetting().getVlsAmount()) return false;

            punish(getPlayer(), violationName);
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

        public void reload() {
            this.lastViolationClear.set(System.currentTimeMillis());
        }

        public abstract boolean flag(Object handle, CancellablePacketObserver cancellablePacketObserver, String violationName, double amount, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, boolean ignoreSpike);

        public abstract PacketLimitHandlerType getType();
    }

    public class PacketSimpleHandler extends AbstractPacketLimitHandler {


        public PacketSimpleHandler(Player player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
        }

        @Override
        public boolean flag(Object handle, CancellablePacketObserver cancellablePacketObserver, String violationName, double amount, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, boolean ignoreSpike) {
            if (onFlag != null) onFlag.run();

            violate(handle, cancellablePacketObserver, violationName, onViolation, developmentData);
            return true;
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.SIMPLE;
        }
    }

    public class PacketSingleOverflowHandler extends AbstractPacketLimitHandler {

        private double threshold;
        private double amount;

        public PacketSingleOverflowHandler(Player player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
        }

        @Override
        public void reload() {
            threshold = getSetting().getThreshold();

            super.reload();
        }

        @Override
        public boolean flag(Object handle, CancellablePacketObserver cancellablePacketObserver, String violationName, double amount, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, boolean ignoreSpike) {
            if (onFlag != null) onFlag.run();

            if ((this.amount = amount) < threshold) return false;

            violate(handle, cancellablePacketObserver, violationName, onViolation, developmentData);
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

        protected int decayInterval;
        protected int decayAmount;
        protected double threshold;
        protected double bufferIndexCopy;
        protected boolean hasDecayed;

        protected long lastDecay = System.currentTimeMillis();
        protected double bufferIndex;


        public PacketBufferHandler(Player player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
        }

        @Override
        public void reload() {
            decayInterval = getSetting().getDecay();
            decayAmount = getSetting().getDecayAmount();
            threshold = getSetting().getThreshold();

            super.reload();
        }

        // put into its own method, so it can be overwritten by the buffer throttle
        public void decay() {
            bufferIndex -= decayAmount;
        }

        @Override
        public synchronized boolean flag(Object handle, CancellablePacketObserver cancellablePacketObserver, String violationName, double amount, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, boolean ignoreSpike) {
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
                HoneyPlugin.get().getLoggerService().logDebug(LoggerService.LoggingStrategy.FLAG, handle, getPlayer().getName(), violationName);

            if (bufferIndex < threshold) return false;

            bufferIndexCopy = bufferIndex;
            if (getSetting().isClearBuffer()) this.bufferIndex = 0;

            return handle(handle, cancellablePacketObserver, violationName, onViolation, developmentData);
        }

        public double getBufferIndex() {
            return bufferIndexCopy;
        }

        public abstract boolean handle(Object handle, CancellablePacketObserver cancellablePacketObserver, String violationName, Runnable onViolation, CheckContextContainer.DevelopmentData developmentData);

    }

    public class PacketBufferThrottleHandler extends PacketBufferHandler {

        private double until;

        public PacketBufferThrottleHandler(Player player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
        }

        @Override
        public void reload() {
            until = getSetting().getUntil();

            super.reload();
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
        public boolean handle(Object handle, CancellablePacketObserver cancellablePacketObserver, String violationName, Runnable onViolation, CheckContextContainer.DevelopmentData developmentData) {
            if (cancellablePacketObserver != null) cancellablePacketObserver.cancel();

            if (bufferIndex > until && getSetting().isEnabled())
                violate(handle, cancellablePacketObserver, violationName, null, developmentData);
            return true;
        }
    }

    public class PacketBufferOverflowHandler extends PacketBufferHandler {


        public PacketBufferOverflowHandler(Player player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
        }

        @Override
        public boolean handle(Object handle, CancellablePacketObserver cancellablePacketObserver, String violationName, Runnable onViolation, CheckContextContainer.DevelopmentData developmentData) {
            violate(handle, cancellablePacketObserver, violationName, onViolation, developmentData);
            return true;
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.BUFFER_OVERFLOW;
        }
    }

    public class PacketIntervalHandler extends AbstractPacketLimitHandler {

        private int maxPerInterval;
        private int interval; // ms

        private boolean violatedForInterval;
        private boolean ignoringSpikeForInterval;
        private long intervalStart = System.currentTimeMillis();
        private double flagged;
        private double flaggedCopy;

        public PacketIntervalHandler(Player player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
        }

        public int getFlagged() {
            return (int) flaggedCopy;
        }

        @Override
        public void reload() {
            this.maxPerInterval = getSetting().getMaxPerInterval();
            this.interval = getSetting().getInterval();

            super.reload();
        }

        private synchronized void clearInterval() {
            violatedForInterval = false;
            ignoringSpikeForInterval = false;
            flagged = 0;
            intervalStart = System.currentTimeMillis();
        }

        public synchronized boolean flag(Object handle, CancellablePacketObserver cancellablePacketObserver, String violationName, double amount, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, boolean ignoreSpike) {
            if (System.currentTimeMillis() - intervalStart >= interval)
                clearInterval();

            if (ignoreSpike) {
                clearInterval();
                ignoringSpikeForInterval = true;
            }
            if (ignoringSpikeForInterval) {
                if (violatedForInterval) cancellablePacketObserver.cancel();

                if (HoneyPlugin.get().getLoggerService().isDebug()) Bukkit.getLogger().info("DEBUG: ignoring packet spike for interval check " + violationName);
                return false;
            }
            if (onFlag != null) onFlag.run();

            flagged += amount;
            if (flagged < maxPerInterval) return false;

            if (cancellablePacketObserver != null) cancellablePacketObserver.cancel();

            if (violatedForInterval) return true;

            violatedForInterval = true;
            flaggedCopy = flagged;
            if (getSetting().isClearInterval()) clearInterval();

            violate(handle, cancellablePacketObserver, violationName, onViolation, developmentData);
            return true;
        }

        @Override
        public PacketLimitHandlerType getType() {
            return PacketLimitHandlerType.INTERVAL;
        }
    }

    public class PacketIntervalThrottleHandler extends AbstractPacketLimitHandler {

        private int interval;
        private double threshold;
        private double until;

        private double amount;
        private boolean currentlyCancelling;
        private long intervalStart = System.currentTimeMillis();

        public PacketIntervalThrottleHandler(Player player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
        }

        @Override
        public synchronized boolean flag(Object handle, CancellablePacketObserver cancellablePacketObserver, String violationName, double amount, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, boolean ignoreSpike) {
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
                    violate(handle, cancellablePacketObserver, violationName, onViolation, developmentData);
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

        @Override
        public void reload() {
            threshold = getSetting().getThreshold();
            until = getSetting().getUntil();
            interval = getSetting().getInterval();

            super.reload();
        }

        public double getAmount() {
            return amount;
        }
    }
}
