package me.alek.honey.services;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
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
            for (PacketLimitHandler packetLimitHandler : violationContext.getPacketlimitMap().values()) {
                packetLimitHandler.reload();
            }
        }
    }

    public List<UUID> getPunishing() {
        return punishing;
    }

    public boolean isPunishing(Player player) {
        if (player == null) {
            return false;
        }
        if (punishing.isEmpty()) {
            return false; // optimized
        }
        return punishing.contains(player.getUniqueId());
    }

    public void addPlayer(Player player) {
        if (violatedMap.containsKey(player.getUniqueId())) {
            return;
        }
        violatedMap.put(player.getUniqueId(), new ViolationContext(player));
        punishing.remove(player.getUniqueId());
    }

    public void removePlayer(Player player) {
        violatedMap.remove(player.getUniqueId());
    }

    public ViolationContext getViolationContext(Player player) {
        if (!violatedMap.containsKey(player.getUniqueId())) {
            addPlayer(player);
        }
        return violatedMap.get(player.getUniqueId());
    }

    public PacketLimitHandler getLimits(
            Player player,
            CheckKey checkKey,
            ConfigurationOptions.VlSetting setting
    ) {
        ViolationContext context = getViolationContext(player);
        return context.getLimits(checkKey, setting);
    }

    public void flag(
            Player player,
            PacketContainer<? extends WrappedPacket<?>> packet,
            CheckKey key,
            Runnable onViolation,
            String violationName,
            ConfigurationOptions.VlSetting setting
    ) {
        getLimits(player, key, setting)
                .flag(packet.getHandle(), violationName, onViolation);
    }

    public void flag(
            Player player,
            PacketContainer<? extends WrappedPacket<?>> packet,
            PacketCheck<? extends WrappedPacket<?>> check,
            Runnable onViolation,
            String violationName,
            ConfigurationOptions.VlSetting setting
    ) {
        getLimits(player, new CheckKey(check, packet.getType()), setting)
                .flag(packet.getHandle(), violationName, onViolation);
    }

    public void punish(Player player, String violationName, ConfigurationOptions.VlSetting setting, PacketLimitHandler packetLimitHandler) {
        try {
            punishLock.lock();
            if (punishing.contains(player.getUniqueId())) {
                return;
            }
            punishing.add(player.getUniqueId());

            /*Object handle = PacketFactory.createPacket(PacketType.Play.Server.KICK_DISCONNECT);
            PacketContainer<WrappedPlayOutKickDisconnect> packetContainer =
                    new InternalPacketContainer<>(handle, player, NMSUtils.getChannel(player), null, PacketType.Play.Server.KICK_DISCONNECT);
            WrappedPlayOutKickDisconnect packet = PacketWrapperFactory.playOutKickDisconnect(handle, packetContainer);
            packet.getContainer() // chat base components write

            HoneyPlugin.get().getPacketLibrary().getProxy().writePacket(player, handle);*/

            HoneyPlugin.get().getNotificationService().warn(player, violationName, setting, packetLimitHandler);
            HoneyPlugin.get().getLoggerService().logPunish(player.getName(), violationName);

            Channel channel = NMSUtils.getChannel(player);
            if (channel != null) {
                channel.close();
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

    public void punish(Player player, String violationName) {
        punish(player, violationName, null, null);
    }

    public class ViolationContext {

        private final Map<CheckKey, PacketLimitHandler> packetLimitMap = new ConcurrentHashMap<>();
        private final Player player;

        public ViolationContext(Player player) {
            this.player = player;
        }

        public synchronized PacketLimitHandler getLimits(CheckKey key, ConfigurationOptions.VlSetting setting) {
            if (!packetLimitMap.containsKey(key)) {
                packetLimitMap.put(key, new PacketLimitHandler(player, setting));
            }
            return packetLimitMap.get(key);
        }

        public Map<CheckKey, PacketLimitHandler> getPacketlimitMap() {
            return packetLimitMap;
        }

        public Player getPlayer() {
            return player;
        }
    }

    public static class CheckKey extends Tuple2<Object, Object> {

        public static CheckKey DEFAULT = new CheckKey(null, null);

        public CheckKey(Object object1, Object object2) {
            super(object1, object2);
        }

        public static CheckKey of(Object object1, Object object2) {
            return new CheckKey(object1, object2);
        }
    }

    public class PacketLimitHandler {

        private final Player player;
        private final ConfigurationOptions.VlSetting setting;
        private int maxPerInterval;
        private int interval;
        private int vlsAmount;

        private volatile long intervalStart;
        private volatile long lastViolationClear;
        private volatile int flagged;
        private volatile int violations;
        private volatile boolean violatedForInterval;

        public PacketLimitHandler(Player player, ConfigurationOptions.VlSetting setting) {
            this.setting = setting;
            this.player = player;
            maxPerInterval = setting.getMaxPerInterval();
            interval = setting.getInterval();
            vlsAmount = setting.getVlsAmount();
            intervalStart = System.currentTimeMillis();
            lastViolationClear = System.currentTimeMillis();
        }

        public int getFlagged() {
            return flagged;
        }

        public int getViolations() {
            return violations;
        }

        public void reload() {
            this.maxPerInterval = setting.getMaxPerInterval();
            this.interval = setting.getInterval();
            this.vlsAmount = setting.getVlsAmount();
        }

        public ConfigurationOptions.VlSetting getSetting() {
            return setting;
        }

        public synchronized void flag(Object handle, String violationName, Runnable onViolation) {
            if (System.currentTimeMillis() - intervalStart >= interval) {
                violatedForInterval = false;
                flagged = 0;
                intervalStart = System.currentTimeMillis();
            }
            flagged++;
            if (loggerService.isDebug()) {
                HoneyPlugin.get().getLoggerService().logDebug(LoggerService.LoggingStrategy.FLAG, handle, player.getName(), violationName, this, setting);
            }
            if (flagged < maxPerInterval) {
                return;
            }
            violate(handle, violationName, onViolation);
        }

        public void flag(Object handle, String violationName) {
            flag(handle, violationName, null);
        }

        public synchronized void violate(Object handle, String violationName, Runnable onViolation) {
            if (onViolation != null) {
                onViolation.run();
            }
            if (System.currentTimeMillis() - lastViolationClear >= ConfigurationOptions.General.CLEAR_OLD_VIOLATIONS_TIMESTAMP) {
                violatedForInterval = false;
                violations = 0;
                lastViolationClear = System.currentTimeMillis();
            }
            if (violatedForInterval) {
                return;
            }
            if (loggerService.isDebug()) {
                HoneyPlugin.get().getLoggerService().logDebug(LoggerService.LoggingStrategy.VIOLATE, handle, player.getName(), violationName, this, setting);
            }
            violatedForInterval = true;
            violations++;
            HoneyPlugin.get().getNotificationService().notify(player, violationName, setting, this);
            if (violations < vlsAmount) {
                return;
            }
            punish(player, violationName, setting, this);
        }

        public void violate(Object handle, String violationName) {
            violate(handle, violationName, null);
        }

        public Player getPlayer() {
            return player;
        }
    }
}
