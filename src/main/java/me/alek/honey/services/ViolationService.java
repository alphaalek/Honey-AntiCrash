package me.alek.honey.services;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.Check;
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
import java.util.Objects;
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

    public void flag(
            Player player,
            PacketContainer<? extends WrappedPacket<?>> packet,
            Check check,
            String violationName,
            ConfigurationOptions.IntervalSetting setting
    ) {
        ViolationContext context = getViolationContext(player);
        PacketLimitHandler details = context.getLimits(check, packet.getType(), setting);
        details.flag(packet, violationName);
    }

    public void punish(Player player, PacketContainer<?> packet, String violationName) {
        HoneyPlugin.get().getNotificationService().warn(player, packet, violationName);
        removePlayer(player);
        new BukkitRunnable() {
            @Override
            public void run() {
                player.kickPlayer(ConfigurationOptions.General.KICK_MESSAGE);
            }
        }.runTask(HoneyPlugin.get());
    }

    public class ViolationContext {

        private final Map<PacketKeyWrapper, PacketLimitHandler> packetLimitMap = new HashMap<>();
        private final Player player;

        public ViolationContext(Player player) {
            this.player = player;
        }

        public synchronized PacketLimitHandler getLimits(Check check, PacketTypeEnum packetType, ConfigurationOptions.IntervalSetting setting) {
            final PacketKeyWrapper key = new PacketKeyWrapper(check, packetType);
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

    public static class PacketKeyWrapper extends Tuple2<Check, PacketTypeEnum> {

        public PacketKeyWrapper(Check check, PacketTypeEnum packetType) {
            super(check, packetType);
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

        public PacketLimitHandler(Player player, ConfigurationOptions.IntervalSetting setting) {
            this.player = player;
            maxPerInterval = setting.getMaxPerInterval();
            interval = setting.getInterval();
            vlsAmount = setting.getVlsAmount();
            intervalStart = System.currentTimeMillis();
            lastViolationClear = System.currentTimeMillis();
        }

        public void clearFlags() {
            flagged = 0;
            intervalStart = System.currentTimeMillis();
        }

        public void flag(PacketContainer<? extends WrappedPacket<?>> packet, String violationName) {
            if (System.currentTimeMillis() - intervalStart > interval) {
                Bukkit.broadcastMessage("cleared flags");
                clearFlags();
            }
            flagged++;
            Bukkit.broadcastMessage("flagged " + player.getName() + " " + violationName + " " + flagged + "/" + maxPerInterval + ", interval: " + interval);
            if (flagged <= maxPerInterval) {
                return;
            }
            violate(packet, violationName);
        }

        public void violate(PacketContainer<? extends WrappedPacket<?>> packet, String violationName) {
            if (System.currentTimeMillis() - lastViolationClear > ConfigurationOptions.General.CLEAR_OLD_VIOLATIONS_TIMESTAMP) {
                Bukkit.broadcastMessage("cleard violations");
                violations = 0;
                lastViolationClear = System.currentTimeMillis();
            }
            Bukkit.broadcastMessage("violated " + player.getName() + " " + violationName + " " + violations + "/" + vlsAmount + ", interval: " + ConfigurationOptions.General.CLEAR_OLD_VIOLATIONS_TIMESTAMP + ", interval left: " + (System.currentTimeMillis() - ConfigurationOptions.General.CLEAR_OLD_VIOLATIONS_TIMESTAMP));
            violations++;
            clearFlags();
            HoneyPlugin.get().getNotificationService().notify(player, packet, violationName);
            if (violations < vlsAmount) {
                return;
            }
            punish(player, packet, violationName);
        }

        public Player getPlayer() {
            return player;
        }
    }
}
