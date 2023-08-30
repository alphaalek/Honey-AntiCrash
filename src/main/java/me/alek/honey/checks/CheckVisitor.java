package me.alek.honey.checks;

import io.netty.buffer.ByteBuf;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.*;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.CancellablePacketObserver;
import me.alek.honey.netty.packet.cancellables.CancellableByteBufImpl;
import me.alek.honey.netty.packet.cancellables.CancellablePacketDelegateImpl;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.services.ViolationService;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class CheckVisitor<WP extends WrappedPacket<WP>> {

    private final List<HoneyCheck> checks = new ArrayList<>();

    public void visit(HoneyCheck check) {
        checks.add(check);
    }

    public List<HoneyCheck> getChecks() {
        return checks;
    }

    private void handleCommonCheck(
            Player player,
            Object handle,
            Function<HoneyCheck, ResponseContainer> responseGeneratorFunction,
            WP packet,
            CancellablePacketObserver cancellablePacketObserver,
            CheckContextContainer checkContextContainer
    ) {
        AtomicInteger skipping = null;
        for (HoneyCheck check : checks) {

            CheckContextContainer.DevelopmentData developmentData = checkContextContainer.getDevelopmentData(check);

            if (player != null)
                developmentData.setText(0, "§8* §7Player " + player.getName() + " flagged " + check.getViolationName() + " §8*");

            // add information to the development data
            developmentData.setLine(5, check.getDescription());

            if (HoneyPlugin.get().getViolationService().isPunishing(player)) {
                cancellablePacketObserver.cancel();
                return;
            }
            if (skipping != null && skipping.getAndDecrement() > 0) continue;

            // do the check and get the response
            ResponseContainer response = responseGeneratorFunction.apply(check);

            // get the setting for this check
            ConfigurationOptions.VlSetting setting = null;
            if (check instanceof ViolationCheck) {
                setting = ((ViolationCheck)check).getSetting();
            }
            else if (check instanceof KeyedViolationCheck) {
                KeyedViolationCheck<WP, Object> keyedViolationCheck = (KeyedViolationCheck<WP, Object>) check;
                setting = keyedViolationCheck.getSetting(packet);
                response.CUSTOM_KEY = new ViolationService.CheckKey(check.getClass(), keyedViolationCheck.getKey(packet));
            }

            // checks the contents of the response container and do whatever action it represents
            if (response.SKIP_INDICES != null) skipping = response.SKIP_INDICES;

            if (response.SUCCESS) {

                if (response.TERMINATE) return;
                continue;
            }
            if (response.CANCELLED) cancellablePacketObserver.cancel();

            if (response.FLAGGED != 0) {
                if (setting == null) {

                    if (response.TERMINATE) return;
                    continue;
                }
                ViolationService.CheckKey key = (response.CUSTOM_KEY != null) ? response.CUSTOM_KEY : new ViolationService.CheckKey(check.getClass(), check.getViolationName());

                HoneyPlugin.get().getViolationService().flag(player, handle, cancellablePacketObserver, key, response.FLAGGED, response.ON_VIOLATION, check.getViolationName(), setting, developmentData);
            }
            if (response.NOTIFY && response.NOTIFICATION_MESSAGE != null)
                HoneyPlugin.get().getNotificationService().notify(response.NOTIFICATION_MESSAGE);

            if (response.PUNISH && player != null)
                HoneyPlugin.get().getViolationService().punish(player, check.getViolationName());

            if (response.TERMINATE) return;
        }
    }

    public void check(Player player, ByteBuf byteBuf, CheckContextContainer checkContextContainer) {
        CancellablePacketObserver cancellablePacketObserver = new CancellableByteBufImpl(byteBuf);
        handleCommonCheck(player, null, (check) -> {
            try {
                return ((DecodeCheck)check).check(player, byteBuf, checkContextContainer);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return ResponseBuilder.SUCCESS;
        }, null, cancellablePacketObserver, checkContextContainer);
    }

    @SuppressWarnings("unchecked")
    public void check(Player player, PacketContainer<WP> packetContainer, CheckContextContainer checkContextContainer) throws Exception {
        CancellablePacketObserver cancellablePacketObserver = new CancellablePacketDelegateImpl(packetContainer);
        handleCommonCheck(player, packetContainer.getHandle(), (check) -> {
            try {
                return ((PacketCheck<WP>)check).check(player, packetContainer.getPacket(), checkContextContainer);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return ResponseBuilder.SUCCESS;
        }, packetContainer.getPacket(), cancellablePacketObserver, checkContextContainer);
    }
}
