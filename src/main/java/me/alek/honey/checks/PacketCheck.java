package me.alek.honey.checks;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Supplier;

public interface PacketCheck extends Check {

    Supplier<List<PacketTypeEnum>> getPacketTypes();

    default void flagButAccept(
            Player player,
            PacketContainer<?> packet,
            Check check,
            String violationName,
            ConfigurationOptions.IntervalSetting intervalSetting
    ) {
        flag(player, packet, check, violationName, intervalSetting, false);
    }

    default void flag(
            Player player,
            PacketContainer<?> packet,
            Check check,
            String violationName,
            ConfigurationOptions.IntervalSetting intervalSetting
    ) {
        flag(player, packet, check, violationName, intervalSetting, true);
    }

    default void flag(
            Player player,
            PacketContainer<?> packet,
            Check check,
            String violationName,
            ConfigurationOptions.IntervalSetting intervalSetting,
            boolean cancel
    ) {
        if (cancel) {
            packet.cancel();
        }
        HoneyPlugin.get().getViolationService().flag(
                player,
                packet,
                check,
                violationName,
                intervalSetting
        );
    }

    default void punish(
            Player player,
            PacketContainer<?> packet,
            String violationName
    ) {
        packet.cancel();
        HoneyPlugin.get().getViolationService().punish(
                player,
                packet,
                violationName
        );
    }

    default void punishAndConfiscate(
            Player player,
            PacketContainer<? extends WrappedPacket<?>> packet,
            String violationName,
            ItemStack item
    ) {
        confiscate(player, item);
        punish(player, packet, violationName);
    }

    default void notify(
            Player player,
            PacketContainer<? extends WrappedPacket<?>> packet,
            String notification
    ) {
        HoneyPlugin.get().getNotificationService().notify(player, packet, notification);
    }

    default void notifyAndCancel(
            Player player,
            PacketContainer<?> packet,
            String violationName
    ) {
        packet.cancel();
        notify(player, packet, violationName);
    }

    default void confiscate(
            Player player,
            ItemStack itemStack
    ) {
        HoneyPlugin.get().getConfiscationService().confiscate(player, itemStack);
    }


}
