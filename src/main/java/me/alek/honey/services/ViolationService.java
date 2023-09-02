package me.alek.honey.services;

import com.google.common.util.concurrent.AtomicDouble;
import io.netty.channel.Channel;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.impl.FloodListener;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.configuration.PacketLimitHandlerType;
import me.alek.honey.netty.packet.CancellablePacketObserver;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import me.alek.honey.utility.Tuple2;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

    private static final Field[] VIOLATION_SETTING_FIELDS = ConfigurationOptions.VlSetting.class.getFields();

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
            for (AbstractPacketLimitHandler packetLimitHandler : violationContext.getPacketlimitMap().values()) {
                packetLimitHandler.reload();
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

    public AbstractPacketLimitHandler getLimits(Player player, CheckKey checkKey, ConfigurationOptions.VlSetting setting) {
        ViolationContext context = getViolationContext(player);
        return context.getLimits(checkKey, setting);
    }

    public boolean flag(
            Player player,
            Object handle,
            CancellablePacketObserver cancellablePacketObserver,
            CheckKey key,
            double amount,
            Runnable onViolation,
            Runnable onFlag,
            String violationName,
            ConfigurationOptions.VlSetting setting,
            CheckContextContainer.DevelopmentData developmentData,
            boolean ignoreSpike
    ) {
        return getLimits(player, key, setting)
                .flag(handle, cancellablePacketObserver, violationName, amount, onViolation, onFlag, developmentData, ignoreSpike);
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

        private final Map<CheckKey, AbstractPacketLimitHandler> packetLimitMap = new ConcurrentHashMap<>();
        private final Player player;

        public ViolationContext(Player player) {
            this.player = player;
        }

        public synchronized AbstractPacketLimitHandler getLimits(CheckKey key, ConfigurationOptions.VlSetting setting) {
            if (!packetLimitMap.containsKey(key)) {

                switch (setting.getPacketLimitHandlerType()) {

                    case SIMPLE:
                        packetLimitMap.put(key, new PacketSimpleHandler(player, setting));
                        break;
                    case INTERVAL:
                        packetLimitMap.put(key, new PacketIntervalHandler(player, setting));
                        break;
                    case SINGLE_OVERFLOW:
                        packetLimitMap.put(key, new PacketSingleOverflowHandler(player, setting));
                        break;
                    case BUFFER_OVERFLOW:
                        packetLimitMap.put(key, new PacketBufferOverflowHandler(player, setting));
                        break;
                    case BUFFER_THROTTLE:
                        packetLimitMap.put(key, new PacketBufferThrottleHandler(player, setting));
                        break;
                }
            }
            return packetLimitMap.get(key);
        }

        public Map<CheckKey, AbstractPacketLimitHandler> getPacketlimitMap() {
            return packetLimitMap;
        }

        public AbstractPacketLimitHandler getHandler(CheckKey key) {
            return packetLimitMap.get(key);
        }

        public Player getPlayer() {
            return player;
        }
    }

    public static class CheckKey extends Tuple2<Object, Object> implements Cloneable {

        public static CheckKey DEFAULT = new CheckKey(null, null);

        public CheckKey(Object object1, Object object2) {
            super(object1, object2);
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
            if (onViolation != null) onViolation.run();
            if (cancellablePacketObserver != null) cancellablePacketObserver.cancel();

            if (System.currentTimeMillis() - lastViolationClear.get() >= ConfigurationOptions.General.CLEAR_OLD_VIOLATIONS_TIMESTAMP) {
                violations.set(0);
                lastViolationClear.set(System.currentTimeMillis());
            }
            if (loggerService.isDebug() && handle != null)
                HoneyPlugin.get().getLoggerService().logDebug(LoggerService.LoggingStrategy.VIOLATE, handle, getPlayer().getName(), violationName);

            violations.incrementAndGet();
            if (developmentData != null) showCheckSettingsToDevelopmentData(developmentData);
            HoneyPlugin.get().getNotificationService().notify(getPlayer(), violationName, getSetting(), this, developmentData);

            if (violations.get() < getSetting().getVlsAmount()) return false;

            punish(getPlayer(), violationName);
            return true;
        }

        private void showCheckSettingsToDevelopmentData(CheckContextContainer.DevelopmentData developmentData) {
            developmentData.addEmpty();

            PacketLimitHandlerType handlerType = setting.getPacketLimitHandlerType();
            developmentData.addHeader("Settings", (handlerType == PacketLimitHandlerType.SIMPLE ? null : setting.getPacketLimitHandlerType().name().toLowerCase().replace("_", " ")));

            for (Field field : VIOLATION_SETTING_FIELDS) {

                try {
                    ConfigurationOptions.VlSettingImpl settingImpl = field.getAnnotation(ConfigurationOptions.VlSettingImpl.class);
                    if (settingImpl == null && field.getDeclaringClass() == ConfigurationOptions.VlSetting.class) continue;

                    Object value = field.get(setting);

                    // check if this field is associated with a setting value from this check
                    if (settingImpl != null && !setting.getPacketLimitHandlerType().getVlSettingImplTypes().contains(settingImpl.value())) {
                        continue;
                    }

                    // add the line to the development data
                    developmentData.addLine(field.getName() + ": " + value);

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        public int getViolations() {
            return violations.get();
        }

        public void reload() {
            this.lastViolationClear.set(System.currentTimeMillis());
        }

        public abstract boolean flag(Object handle, CancellablePacketObserver cancellablePacketObserver, String violationName, double amount, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, boolean ignoreSpike);
    }

    public class PacketSimpleHandler extends AbstractPacketLimitHandler {


        public PacketSimpleHandler(Player player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
        }

        @Override
        public boolean flag(Object handle, CancellablePacketObserver cancellablePacketObserver, String violationName, double amount, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, boolean ignoreSpike) {
            if (onFlag != null) onFlag.run();

            return violate(handle, cancellablePacketObserver, violationName, onViolation, developmentData);
        }
    }

    public class PacketSingleOverflowHandler extends AbstractPacketLimitHandler {

        private int threshold;
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
            if ((this.amount = amount) < threshold) return false;

            if (onFlag != null) onFlag.run();

            return violate(handle, cancellablePacketObserver, violationName, onViolation, developmentData);
        }

        public double getAmount() {
            return amount;
        }
    }

    public abstract class PacketBufferHandler extends AbstractPacketLimitHandler {

        protected int decayInterval;
        protected int decayAmount;
        protected int threshold;
        protected double bufferIndexCopy;
        protected boolean hasDecayed;

        protected final AtomicLong lastDecay = new AtomicLong();
        protected final AtomicDouble bufferIndex = new AtomicDouble();


        public PacketBufferHandler(Player player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
            lastDecay.set(System.currentTimeMillis());
            reload();
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
            bufferIndex.addAndGet(-decayAmount);
        }

        @Override
        public synchronized boolean flag(Object handle, CancellablePacketObserver cancellablePacketObserver, String violationName, double amount, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, boolean ignoreSpike) {
            hasDecayed = false;
            if (System.currentTimeMillis() - lastDecay.get() > decayInterval) {
                hasDecayed = true;
                lastDecay.set(System.currentTimeMillis());
                decay();
            }
            if (bufferIndex.get() < 0) bufferIndex.set(0);
            bufferIndex.addAndGet(amount);

            if (onFlag != null) onFlag.run();

            if (loggerService.isDebug())
                HoneyPlugin.get().getLoggerService().logDebug(LoggerService.LoggingStrategy.FLAG, handle, getPlayer().getName(), violationName);

            if (bufferIndex.get() < threshold) return false;

            bufferIndexCopy = bufferIndex.get();
            if (getSetting().isClearBuffer()) this.bufferIndex.set(0);

            return handle(handle, cancellablePacketObserver, violationName, onViolation, developmentData);
        }

        public double getBufferIndex() {
            return bufferIndexCopy;
        }

        public abstract boolean handle(Object handle, CancellablePacketObserver cancellablePacketObserver, String violationName, Runnable onViolation, CheckContextContainer.DevelopmentData developmentData);

    }

    public class PacketBufferThrottleHandler extends PacketBufferHandler {

        private int until;

        public PacketBufferThrottleHandler(Player player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
        }

        @Override
        public void reload() {
            until = getSetting().getUntil();

            super.reload();
        }

        @Override
        public void decay() {
            if (bufferIndex.get() < threshold) {
                super.decay();
                return;
            }
            double ratio = (int) ((bufferIndex.get() - threshold) / (decayAmount * 1.8));
            double amount = decayAmount * Math.pow(1.4, ratio);

            bufferIndex.addAndGet(-amount);
        }

        @Override
        public boolean handle(Object handle, CancellablePacketObserver cancellablePacketObserver, String violationName, Runnable onViolation, CheckContextContainer.DevelopmentData developmentData) {
            if (onViolation != null) onViolation.run();

            cancellablePacketObserver.cancel();

            if (bufferIndex.get() > until && getSetting().isEnabled()) violate(handle, cancellablePacketObserver, violationName, null, developmentData);
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
    }

    public class PacketIntervalHandler extends AbstractPacketLimitHandler {

        private int maxPerInterval;
        private int interval; // ms

        private volatile boolean violatedForInterval;
        private volatile boolean ignoringSpikeForInterval;
        private volatile long intervalStart;
        private final AtomicDouble flagged = new AtomicDouble();
        private volatile double flaggedCopy;

        public PacketIntervalHandler(Player player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
            maxPerInterval = setting.getMaxPerInterval();
            interval = setting.getInterval();
            intervalStart = System.currentTimeMillis();
        }

        public int getFlagged() {
            return (int) flaggedCopy;
        }

        public void reload() {
            this.maxPerInterval = getSetting().getMaxPerInterval();
            this.interval = getSetting().getInterval();
        }

        private synchronized void clearInterval() {
            violatedForInterval = false;
            ignoringSpikeForInterval = false;
            flagged.set(0);
            intervalStart = System.currentTimeMillis();
        }

        public synchronized boolean flag(Object handle, CancellablePacketObserver cancellablePacketObserver, String violationName, double amount, Runnable onViolation, Runnable onFlag, CheckContextContainer.DevelopmentData developmentData, boolean ignoreSpike) {
            if (System.currentTimeMillis() - intervalStart >= interval) {
                clearInterval();
            }
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

            flagged.addAndGet(amount);
            if (flagged.get() < maxPerInterval) return false;

            if (violatedForInterval) {
                cancellablePacketObserver.cancel();
                return true;
            }

            violatedForInterval = true;
            flaggedCopy = flagged.get();

            if (getSetting().isClearInterval()) clearInterval();

            return violate(handle, cancellablePacketObserver, violationName, onViolation, developmentData);
        }
    }
}
