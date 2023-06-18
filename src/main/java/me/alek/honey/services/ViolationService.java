package me.alek.honey.services;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.InternalPacketContainer;
import me.alek.honey.netty.packet.PacketFactory;
import me.alek.honey.netty.packet.cache.PacketWrapperCache;
import me.alek.honey.netty.packet.cache.PacketWrapperFactory;
import me.alek.honey.netty.packet.structure.ReflectStructureCache;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.packetwrappers.play.server.WrappedPlayOutKickDisconnect;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import me.alek.honey.utility.Tuple2;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ViolationService {

    private volatile Map<UUID, ViolationContext> violatedMap = new ConcurrentHashMap<>();
    private volatile List<UUID> punishing = new CopyOnWriteArrayList<>();
    private final LoggerService loggerService = HoneyPlugin.get().getLoggerService();

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

    public boolean isPunishing(Player player) {
        if (player == null) {
            return false;
        }
        return punishing.contains(player.getUniqueId());
    }

    public void addPlayer(Player player) {
        if (violatedMap.containsKey(player.getUniqueId())) {
            return;
        }
        violatedMap.put(player.getUniqueId(), new ViolationContext(player));
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
            PacketCheck<? extends WrappedPacket<?>> check,
            PacketTypeEnum packetType,
            ConfigurationOptions.IntervalSetting setting
    ) {
        ViolationContext context = getViolationContext(player);
        return context.getLimits(new CheckKey(check, packetType), setting);
    }

    public void flag(
            Player player,
            PacketContainer<? extends WrappedPacket<?>> packet,
            PacketCheck<? extends WrappedPacket<?>> check,
            String violationName,
            ConfigurationOptions.IntervalSetting setting
    ) {
        getLimits(player, check, packet.getType(), setting).flag(packet.getHandle(), violationName);
    }

    public void flag(
            Player player,
            PacketContainer<? extends WrappedPacket<?>> packet,
            PacketCheck<? extends WrappedPacket<?>> check,
            Runnable onViolation,
            String violationName,
            ConfigurationOptions.IntervalSetting setting
    ) {
        getLimits(player, check, packet.getType(), setting).flag(packet.getHandle(), violationName, onViolation);
    }

    public void punish(Player player, String violationName, ConfigurationOptions.IntervalSetting setting, PacketLimitHandler packetLimitHandler) {
        punishing.add(player.getUniqueId());

        Channel channel = NMSUtils.getChannel(player);
        if (channel != null) {
            channel.close();
        }

        /*Object handle = PacketFactory.createPacket(PacketType.Play.Server.KICK_DISCONNECT);
        PacketContainer<WrappedPlayOutKickDisconnect> packetContainer =
                new InternalPacketContainer<>(handle, player, NMSUtils.getChannel(player), null, PacketType.Play.Server.KICK_DISCONNECT);
        WrappedPlayOutKickDisconnect packet = PacketWrapperFactory.playOutKickDisconnect(handle, packetContainer);
        packet.getContainer() // chat base components write

        PacketLibrary.get().getProxy().writePacket(player, PacketFactory.createPacket(PacketType.Play.Server.KICK_DISCONNECT));*/

        new BukkitRunnable() {
            @Override
            public void run() {
                player.kickPlayer(ConfigurationOptions.General.KICK_MESSAGE);
            }
        }.runTask(HoneyPlugin.get());

        HoneyPlugin.get().getNotificationService().warn(player, violationName, setting, packetLimitHandler);
        removePlayer(player);
        punishing.remove(player.getUniqueId());
    }

    public void punish(Player player, String violationName) {
        punish(player, violationName, null, null);
    }

    public class ViolationContext {

        private volatile Map<CheckKey, PacketLimitHandler> packetLimitMap = new ConcurrentHashMap<>();
        private final Player player;

        public ViolationContext(Player player) {
            this.player = player;
        }

        public synchronized PacketLimitHandler getLimits(CheckKey key, ConfigurationOptions.IntervalSetting setting) {
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

    private static class CheckKey extends Tuple2<PacketCheck<? extends WrappedPacket<?>>, PacketTypeEnum> {

        protected CheckKey(PacketCheck<? extends WrappedPacket<?>> check, PacketTypeEnum packetTypeEnum) {
            super(check, packetTypeEnum);
        }
    }

    public class PacketLimitHandler {

        private final Player player;
        private final ConfigurationOptions.IntervalSetting setting;
        private int maxPerInterval;
        private int interval;
        private int vlsAmount;

        private volatile long intervalStart;
        private volatile long lastViolationClear;
        private volatile int flagged;
        private volatile int violations;
        private volatile boolean violatedForInterval;

        public PacketLimitHandler(Player player, ConfigurationOptions.IntervalSetting setting) {
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

        public ConfigurationOptions.IntervalSetting getSetting() {
            return setting;
        }

        public synchronized void flag(Object handle, String violationName, Runnable onViolation) {
            if (System.currentTimeMillis() - intervalStart > interval) {
                violatedForInterval = false;
                flagged = 0;
                intervalStart = System.currentTimeMillis();
            }
            flagged++;
            if (loggerService.isEnabled()) {
                HoneyPlugin.get().getLoggerService().log(
                        LoggerService.LoggingStrategy.FLAG,
                        handle,
                        player.getName(),
                        violationName,
                        this,
                        setting
                );
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
            if (System.currentTimeMillis() - lastViolationClear > ConfigurationOptions.General.CLEAR_OLD_VIOLATIONS_TIMESTAMP) {
                violatedForInterval = false;
                violations = 0;
                lastViolationClear = System.currentTimeMillis();
            }
            if (violatedForInterval) {
                return;
            }
            if (loggerService.isEnabled()) {
                HoneyPlugin.get().getLoggerService().log(
                        LoggerService.LoggingStrategy.VIOLATE,
                        handle,
                        player.getName(),
                        violationName,
                        this,
                        setting
                );
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
