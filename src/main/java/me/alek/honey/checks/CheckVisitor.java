package me.alek.honey.checks;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.ViolationCheck;
import me.alek.honey.checks.types.ViolationConsumerCheck;
import me.alek.honey.checks.types.PacketCheck;
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

    @SuppressWarnings("unchecked")
    public void check(AbstractPacketListener<WP> packetListener, Player player, PacketContainer<WP> packetContainer, CheckContextContainer container) throws Exception {
        int skipping = 0;
        for (PacketCheck<WP> check : checks) {
            if (HoneyPlugin.get().getViolationService().isPunishing(player)) {
                packetContainer.cancel();
                return;
            }
            if (skipping-- > 0) continue;

            ResponseContainer response = check.accept(player, packetContainer.getPacket(), container);
            ConfigurationOptions.VlSetting setting = null;
            if (check instanceof ViolationCheck) {
                setting = ((ViolationCheck)check).getSetting();
            }
            else if (check instanceof ViolationConsumerCheck) {
                ViolationConsumerCheck<WP, Object> violationCheck = (ViolationConsumerCheck<WP, Object>) check;
                setting = violationCheck.getSetting(violationCheck.getKey(packetContainer.getPacket()));
            }

            if (response.SUCCESS) {
                continue;
            }
            if (response.CANCELLED && !response.ACCEPTED) {
                packetContainer.cancel();
            }
            if (response.FLAGGED) {
                if (setting == null) {
                    continue;
                }
                if (response.CUSTOM_KEY != null) {
                    HoneyPlugin.get().getViolationService().flag(player, packetContainer, response.CUSTOM_KEY, response.ON_VIOLATION, check.getViolationName(), setting);
                }
                else {
                    HoneyPlugin.get().getViolationService().flag(player, packetContainer, check, response.ON_VIOLATION, check.getViolationName(), setting);
                }
            }
            if (response.NOTIFY && response.NOTIFICATION_MESSAGE != null) {
                HoneyPlugin.get().getNotificationService().notify(response.NOTIFICATION_MESSAGE);
            }
            if (response.PUNISH) {
                HoneyPlugin.get().getViolationService().punish(player, check.getViolationName());
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
