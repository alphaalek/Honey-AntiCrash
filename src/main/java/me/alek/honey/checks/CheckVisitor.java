package me.alek.honey.checks;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.types.ViolationCheck;
import me.alek.honey.checks.types.ViolationKeyedCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.services.ViolationService;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
        AtomicInteger skipping = null;
        for (PacketCheck<WP> check : checks) {
            if (HoneyPlugin.get().getViolationService().isPunishing(player)) {
                packetContainer.cancel();
                return;
            }
            if (skipping != null && skipping.getAndDecrement() > 0) {
                continue;
            }
            ResponseContainer response = check.accept(player, packetContainer.getPacket(), container);
            ConfigurationOptions.VlSetting setting = null;
            if (check instanceof ViolationCheck) {
                setting = ((ViolationCheck)check).getSetting();
            }
            else if (check instanceof ViolationKeyedCheck) {
                ViolationKeyedCheck<WP, Object> violationCheck = (ViolationKeyedCheck<WP, Object>) check;
                setting = violationCheck.getSetting(violationCheck.getKey(packetContainer.getPacket()));
            }

            if (response.SKIP_INDICES != null) {
                skipping = response.SKIP_INDICES;
            }
            if (response.SUCCESS) {

                if (response.TERMINATE) {
                    return;
                }
                continue;
            }
            if (response.CANCELLED && !response.ACCEPTED) {
                packetContainer.cancel();
            }
            if (response.FLAGGED != 0) {
                if (setting == null) {

                    if (response.TERMINATE) {
                        return;
                    }
                    continue;
                }
                ViolationService.CheckKey key = (response.CUSTOM_KEY != null) ? response.CUSTOM_KEY : new ViolationService.CheckKey(check, packetContainer.getType());
                HoneyPlugin.get().getViolationService().flag(player, packetContainer, key, response.FLAGGED, response.ON_VIOLATION, check.getViolationName(), setting);
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
        }
    }
}
