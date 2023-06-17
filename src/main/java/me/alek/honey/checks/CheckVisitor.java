package me.alek.honey.checks;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.*;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CheckVisitor<WP extends WrappedPacket<WP>> {

    private final List<PacketCheck<WP>> checks = new ArrayList<>();

    public void visit(PacketCheck<WP> check) {
        checks.add(check);
    }

    public List<PacketCheck<WP>> getChecks() {
        return checks;
    }

    public void check(AbstractPacketListener<WP> packetListener, Player player, PacketContainer<WP> packetContainer, CheckContextContainer container) {
        int skipping = 0;
        for (PacketCheck<WP> check : checks) {
            if (skipping-- > 0) {
                continue;
            }
            ResponseContainer response = check.accept(player, packetContainer.getPacket(), container);
            if (response.SUCCESS) {
                continue;
            }
            if (response.CANCELLED && !response.ACCEPTED) {
                packetContainer.cancel();
            }
            if (response.FLAGGED) {

                ConfigurationOptions.IntervalSetting setting = null;
                if (check instanceof IntervalCheck) {
                    setting = ((IntervalCheck)check).getSetting();
                }
                else if (check instanceof IntervalConsumerCheck) {
                    setting = ((IntervalConsumerCheck)check).getSetting().apply(packetContainer.getType());
                }

                if (setting == null) {
                    continue;
                }
                if (response.ON_VIOLATION != null) {
                    HoneyPlugin.get().getViolationService().flag(
                            player,
                            packetContainer,
                            check,
                            response.ON_VIOLATION,
                            check.getClass().getSimpleName(),
                            setting
                    );
                }
            }
            if (response.NOTIFY && response.NOTIFICATION_MESSAGE != null) {
                HoneyPlugin.get().getNotificationService().notify(player, packetContainer, response.NOTIFICATION_MESSAGE);
            }
            if (response.CONFISCATE && response.CONFISCATED_ITEM != null) {
                HoneyPlugin.get().getConfiscationService().confiscate(player, response.CONFISCATED_ITEM);
            }
            if (response.PUNISH) {
                HoneyPlugin.get().getViolationService().punish(
                        player,
                        packetContainer,
                        check.getClass().getSimpleName()
                );
            }
            if (response.TERMINATE) {
                return;
            }
            if (response.SKIP_INDICES != 0) {
                skipping = response.SKIP_INDICES;
            }
        }
    }
}
