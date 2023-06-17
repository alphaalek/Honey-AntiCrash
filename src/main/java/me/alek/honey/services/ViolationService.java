package me.alek.honey.services;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.utility.Tuple2;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ViolationService {

    private final Map<UUID, ViolationContext> violatedMap = new ConcurrentHashMap<>();

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
        getLimits(player, check, packet.getType(), setting).flag(packet, violationName);
    }

    public void flag(
            Player player,
            PacketContainer<? extends WrappedPacket<?>> packet,
            PacketCheck<? extends WrappedPacket<?>> check,
            Runnable onViolation,
            String violationName,
            ConfigurationOptions.IntervalSetting setting
    ) {
        getLimits(player, check, packet.getType(), setting).flag(packet, violationName, onViolation);
    }

    public void punish(Player player, PacketContainer<? extends WrappedPacket<?>> packet, String violationName) {
        HoneyPlugin.get().getNotificationService()
                .warn(player, packet, "punished " + player + " for " + violationName);
        removePlayer(player);
        new BukkitRunnable() {
            @Override
            public void run() {
                player.kickPlayer(ConfigurationOptions.General.KICK_MESSAGE);
            }
        }.runTask(HoneyPlugin.get());
    }

    public class ViolationContext {

        private final Map<CheckKey, PacketLimitHandler> packetLimitMap = new HashMap<>();
        private final Player player;

        public ViolationContext(Player player) {
            this.player = player;
        }

        public synchronized PacketLimitHandler getLimits(CheckKey key, ConfigurationOptions.IntervalSetting setting) {
            if (!packetLimitMap.containsKey(key)) {
                Bukkit.broadcastMessage("creating new entry");
                packetLimitMap.put(key, new PacketLimitHandler(player, setting));
            }
            return packetLimitMap.get(key);
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
        private final int maxPerInterval;
        private final int interval;
        private final int vlsAmount;

        private long intervalStart;
        private long lastViolationClear;
        private int flagged;
        private int violations;
        private boolean violatedForInterval;

        public PacketLimitHandler(Player player, ConfigurationOptions.IntervalSetting setting) {
            this.player = player;
            maxPerInterval = setting.getMaxPerInterval();
            interval = setting.getInterval();
            vlsAmount = setting.getVlsAmount();
            intervalStart = System.currentTimeMillis();
            lastViolationClear = System.currentTimeMillis();
        }

        public void flag(PacketContainer<? extends WrappedPacket<?>> packet, String violationName, Runnable onViolation) {
            if (System.currentTimeMillis() - intervalStart > interval) {
                Bukkit.broadcastMessage("cleared flags");
                violatedForInterval = false;
                flagged = 0;
                intervalStart = System.currentTimeMillis();
            }
            flagged++;
            Bukkit.broadcastMessage("flagged " + player.getName() + " " + violationName + " " + flagged + "/" + maxPerInterval + ", interval: " + interval);
            if (flagged <= maxPerInterval) {
                return;
            }
            violate(packet, violationName, onViolation);
        }

        public void flag(PacketContainer<? extends WrappedPacket<?>> packet, String violationName) {
            violate(packet, violationName, null);
        }

        public void violate(PacketContainer<? extends WrappedPacket<?>> packet, String violationName, Runnable onViolation) {
            if (onViolation != null) {
                Bukkit.broadcastMessage("run on violation");
                onViolation.run();
            }
            if (System.currentTimeMillis() - lastViolationClear > ConfigurationOptions.General.CLEAR_OLD_VIOLATIONS_TIMESTAMP) {
                Bukkit.broadcastMessage("cleared violations");
                violatedForInterval = false;
                violations = 0;
                lastViolationClear = System.currentTimeMillis();
            }
            if (violatedForInterval) {
                return;
            }
            Bukkit.broadcastMessage("violated " + player.getName() + " " + violationName + " " + violations + "/" + vlsAmount + ", interval: " + ConfigurationOptions.General.CLEAR_OLD_VIOLATIONS_TIMESTAMP + ", interval left: " + (System.currentTimeMillis() - ConfigurationOptions.General.CLEAR_OLD_VIOLATIONS_TIMESTAMP));
            violatedForInterval = true;
            violations++;
            HoneyPlugin.get().getNotificationService()
                    .notify(player, packet, violationName);
            if (violations < vlsAmount) {
                return;
            }
            punish(player, packet, violationName);
        }

        public void violate(PacketContainer<? extends WrappedPacket<?>> packet, String violationName) {
            violate(packet, violationName, null);
        }

        public Player getPlayer() {
            return player;
        }
    }
}
