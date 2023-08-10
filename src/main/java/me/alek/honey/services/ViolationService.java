package me.alek.honey.services;

import com.google.common.util.concurrent.AtomicDouble;
import io.netty.channel.Channel;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import me.alek.honey.utility.Tuple2;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

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

    public void reload() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            ViolationContext violationContext = violatedMap.get(uuid);
            if (violationContext == null) {
                addPlayer(player);
                continue;
            }
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
            CheckKey key,
            double amount,
            Runnable onViolation,
            String violationName,
            ConfigurationOptions.VlSetting setting
    ) {
        return getLimits(player, key, setting)
                .flag(handle, violationName, amount, onViolation);
    }

    public void notifySingleOverflowStop(Player player, CheckKey key, ConfigurationOptions.VlSetting setting) {
        AbstractPacketLimitHandler packetLimitHandler = getLimits(player, key, setting);
        if (packetLimitHandler instanceof PacketSingleOverflowHandler) {

            ((PacketSingleOverflowHandler)packetLimitHandler).notifyStop();
        }
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
        }.runTaskLaterAsynchronously(HoneyPlugin.get(), 5L);
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

        public Player getPlayer() {
            return player;
        }
    }

    public static class CheckKey extends Tuple2<Object, Object> implements Cloneable {

        public static CheckKey DEFAULT = new CheckKey(null, null);
        private boolean useBuffer;

        public CheckKey(Object object1, Object object2) {
            super(object1, object2);
        }

        public static CheckKey of(Object object1, Object object2) {
            return new CheckKey(object1, object2);
        }

        public CheckKey setBuffer(boolean buffer) {
            useBuffer = buffer;
            return this;
        }

        @Override
        public int hashCode() {
            if (useBuffer) {
                return super.hashCode() << 5 ^ 2;
            }
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

        private AtomicLong lastViolationClear = new AtomicLong();
        private AtomicInteger violations = new AtomicInteger();

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

        public synchronized boolean violate(Object handle, String violationName, Runnable onViolation) {
            if (onViolation != null) {
                onViolation.run();
            }
            if (System.currentTimeMillis() - lastViolationClear.get() >= ConfigurationOptions.General.CLEAR_OLD_VIOLATIONS_TIMESTAMP) {
                violations.set(0);
                lastViolationClear.set(System.currentTimeMillis());
            }
            if (loggerService.isDebug()) {
                HoneyPlugin.get().getLoggerService().logDebug(LoggerService.LoggingStrategy.VIOLATE, handle, getPlayer().getName(), violationName, this, getSetting());
            }
            violations.incrementAndGet();
            HoneyPlugin.get().getNotificationService().notify(getPlayer(), violationName, getSetting(), this);
            if (violations.get() < getSetting().getVlsAmount()) {
                return false;
            }
            punish(getPlayer(), violationName);
            return true;
        }

        public void violate(Object handle, String violationName) {
            violate(handle, violationName, null);
        }

        public int getViolations() {
            return violations.get();
        }

        public void reload() {
            this.lastViolationClear.set(System.currentTimeMillis());
        }

        public abstract boolean flag(Object handle, String violationName, double amount, Runnable onViolation);
    }

    public class PacketSingleOverflowHandler extends AbstractPacketLimitHandler {

        private int threshold;

        private final ThreadLocal<AtomicDouble> index = new ThreadLocal<>();

        public PacketSingleOverflowHandler(Player player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
        }

        @Override
        public void reload() {
            threshold = getSetting().getThreshold();

            super.reload();
        }

        @Override
        public boolean flag(Object handle, String violationName, double amount, Runnable onViolation) {
            synchronized (index) {
                AtomicDouble index = this.index.get();
                if (index == null) {
                    this.index.set(new AtomicDouble());
                    index = this.index.get();
                }
                index.addAndGet(amount);

                if (index.get() < threshold) return false;

                return violate(handle, violationName, onViolation);
            }
        }

        public void notifyStop() {
            synchronized (index) {
                index.remove();
            }
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
        public synchronized boolean flag(Object handle, String violationName, double amount, Runnable onViolation) {
            hasDecayed = false;
            if (System.currentTimeMillis() - lastDecay.get() > decayInterval) {
                hasDecayed = true;
                lastDecay.set(System.currentTimeMillis());
                decay();
            }
            if (bufferIndex.get() < 0) bufferIndex.set(0);
            bufferIndex.addAndGet(amount);

            if (loggerService.isDebug())
                HoneyPlugin.get().getLoggerService().logDebug(LoggerService.LoggingStrategy.FLAG, handle, getPlayer().getName(), violationName, this, getSetting());

            if (bufferIndex.get() < threshold) return false;

            bufferIndexCopy = bufferIndex.get();
            if (getSetting().isClearBuffer()) {
                this.bufferIndex.set(0);
            }
            return handle(handle, violationName, onViolation);
        }

        public double getBufferIndex() {
            return bufferIndexCopy;
        }

        public abstract boolean handle(Object handle, String violationName, Runnable onViolation);

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
            double ratio = (int) ((bufferIndex.get() - threshold) / decayAmount);
            double amount = decayAmount * Math.pow(1.75, ratio);

            bufferIndex.addAndGet(-amount);
        }

        @Override
        public boolean handle(Object handle, String violationName, Runnable onViolation) {
            if (onViolation != null) onViolation.run();

            if (bufferIndex.get() > until) violate(handle, violationName, null);
            return true;
        }
    }

    public class PacketBufferOverflowHandler extends PacketBufferHandler {


        public PacketBufferOverflowHandler(Player player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
        }

        @Override
        public boolean handle(Object handle, String violationName, Runnable onViolation) {
            return violate(handle, violationName, onViolation);
        }
    }

    public class PacketIntervalHandler extends AbstractPacketLimitHandler {

        private int maxPerInterval;
        private int interval; // ms

        private volatile boolean violatedForInterval;
        private volatile long intervalStart;
        private volatile int flagged;
        private volatile int flaggedCopy;

        public PacketIntervalHandler(Player player, ConfigurationOptions.VlSetting setting) {
            super(player, setting);
            maxPerInterval = setting.getMaxPerInterval();
            interval = setting.getInterval();
            intervalStart = System.currentTimeMillis();
        }

        public int getFlagged() {
            return flaggedCopy;
        }

        public void reload() {
            this.maxPerInterval = getSetting().getMaxPerInterval();
            this.interval = getSetting().getInterval();
        }

        private void clearInterval() {
            violatedForInterval = false;
            flagged = 0;
            intervalStart = System.currentTimeMillis();
        }

        public synchronized boolean flag(Object handle, String violationName, double amount, Runnable onViolation) {
            if (System.currentTimeMillis() - intervalStart >= interval) {
                clearInterval();
            }
            flagged += amount;
            if (loggerService.isDebug()) {
                HoneyPlugin.get().getLoggerService().logDebug(LoggerService.LoggingStrategy.FLAG, handle, getPlayer().getName(), violationName, this, getSetting());
            }

            if (flagged < maxPerInterval) {
                return false;
            }
            if (violatedForInterval) {
                return true;
            }
            violatedForInterval = true;
            flaggedCopy = flagged;
            if (getSetting().isClearInterval()) {
                clearInterval();
            }
            return violate(handle, violationName, onViolation);
        }
    }
}
