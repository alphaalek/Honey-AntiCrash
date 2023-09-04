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

            // add information to the development data
            if (player != null) {
                developmentData.setText(0, "§8* §7Player " + player.getName() + " flagged " + check.getCheckName() + " §8*");
                developmentData.setLine(5, check.getDescription());

                if (HoneyPlugin.get().getViolationService().isPunishing(player)) {
                    cancellablePacketObserver.cancel();
                    return;
                }
            }
            if (skipping != null && skipping.getAndDecrement() > 0) continue;

            // do the check and get the response, and register the time taken to the check handle tracker
            HoneyPlugin.get().getCheckHandleTracker().registerTimeStart(check.getCheckName());
            ResponseContainer response = responseGeneratorFunction.apply(check);
            HoneyPlugin.get().getCheckHandleTracker().registerTimeStop(check.getCheckName());

            // get the setting for this check
            ConfigurationOptions.VlSetting setting = null;
            if (check instanceof ViolationCheck) {
                setting = ((ViolationCheck)check).getSetting();
            }
            else if (check instanceof KeyedViolationCheck) {
                KeyedViolationCheck<WP, Object> keyedViolationCheck = (KeyedViolationCheck<WP, Object>) check;
                setting = keyedViolationCheck.getSetting(packet);
                if (response.KEY == null) response.KEY = new ViolationService.CheckKey(check.getClass(), keyedViolationCheck.getKey(packet));
            }

            // checks the contents of the response container and do whatever action it represents
            if (response.SKIP_INDICES != null) skipping = response.SKIP_INDICES;

            if (response.SUCCESS) {

                if (response.TERMINATE) return;
                continue;
            }
            if (response.CANCELLED) cancellablePacketObserver.cancel();

            if (response.FLAG != -1) {
                if (setting == null) {

                    if (response.ON_BLOCK != null) response.ON_BLOCK.run();

                    if (response.TERMINATE) return;
                    continue;
                }
                ViolationService.CheckKey key = (response.KEY != null) ? response.KEY : new ViolationService.CheckKey(check.getClass(), check.getCheckName());

                HoneyPlugin.get().getViolationService().flag(player, handle, cancellablePacketObserver, key, response.FLAG, response.ON_VIOLATION, response.ON_FLAG, check.getCheckName(), setting, developmentData, response.IGNORE_SPIKE);
            }
            if (response.NOTIFY && response.NOTIFICATION_MESSAGE != null)
                HoneyPlugin.get().getNotificationService().notify(response.NOTIFICATION_MESSAGE);

            if (response.PUNISH && player != null)
                HoneyPlugin.get().getViolationService().punish(player, check.getCheckName());

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
