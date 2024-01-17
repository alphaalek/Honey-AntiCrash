package me.alek.honey.checks;

import io.netty.buffer.ByteBuf;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.*;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.injector.DuplexChannelHandler;
import me.alek.honey.netty.packet.CancellablePacketObserver;
import me.alek.honey.netty.packet.cancellables.CancellableByteBufImpl;
import me.alek.honey.netty.packet.cancellables.CancellablePacketDelegateImpl;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.services.ViolationService;
import org.bukkit.Bukkit;
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

    private void handleCommonCheck(Player player, Object handle, Function<HoneyCheck, ResponseContainer> responseGeneratorFunction,
            WP packet, CancellablePacketObserver cancellablePacketObserver, CheckContextContainer checkContextContainer) {
        AtomicInteger skipping = null;

        for (HoneyCheck check : checks) {
            final CheckContextContainer.DevelopmentData developmentData = checkContextContainer.getDevelopmentData(check);

            // add information to the development data
            if (player != null) {
                developmentData.setText(0, "§8* §7Player " + player.getName() + " flagged " + check.getName() + " §8*");
                developmentData.setLine(5, check.getDescription());

                if (HoneyPlugin.get().getViolationService().isPunishing(player)) {
                    cancellablePacketObserver.cancel();
                    return;
                }
            }
            if (skipping != null && skipping.getAndDecrement() > 0) continue;

            ConfigurationOptions.VlSetting setting = getSetting(check, packet);

            // do the check and get the response, and register the time taken to the check handle tracker
            if (setting != null && setting.isEnabled())
                HoneyPlugin.get().getCheckHandleTracker().registerTimeStart(check.getName());
            final ResponseContainer responses = responseGeneratorFunction.apply(check);
            if (setting != null && setting.isEnabled())
                HoneyPlugin.get().getCheckHandleTracker().registerTimeStop(check.getName());

            final List<ResponseContainer> responsesList = new ArrayList<>(responses.SUB_CONTAINERS);
            responsesList.add(responses);

            boolean terminated = false;
            for (ResponseContainer response : responsesList) {
                // get the setting for this check
                if (check instanceof KeyedViolationCheck) {
                    KeyedViolationCheck<WP, Object> keyedViolationCheck = (KeyedViolationCheck<WP, Object>) check;
                    if (response.LIMITS_KEY == null) response.LIMITS_KEY = new ViolationService.CheckKey(check.getClass(), keyedViolationCheck.getKey(packet));
                }

                // checks the contents of the response container and do whatever action it represents
                if (response.SKIP_INDICES != null) skipping = response.SKIP_INDICES;

                if (response.SUCCESS) {
                    if (response.TERMINATE) terminated = true;
                    continue;
                }
                if (response.CANCELLED) cancellablePacketObserver.cancel();

                final ViolationService.CheckKey key = (response.LIMITS_KEY != null) ? response.LIMITS_KEY : new ViolationService.CheckKey(check.getClass(), check.getName());
                if (response.FLAG != -1) {
                    if (setting == null) {

                        if (response.TERMINATE) terminated = true;
                        continue;
                    }
                    HoneyPlugin.get().getViolationService().flag(player, handle, cancellablePacketObserver, key, response.VIOLATION_KEY, response.FLAG, response.ON_PUNISH, response.ON_VIOLATION, response.ON_FLAG, check.getName(), setting, developmentData, response.IGNORE_SPIKE);
                }
                if (response.PUNISH && player != null)
                    HoneyPlugin.get().getViolationService().punish(null, player, check.getName(), key);

                if (response.TERMINATE) terminated = true;
            }
            if (terminated) return;
        }
    }

    private ConfigurationOptions.VlSetting getSetting(HoneyCheck check, WP packet) {
        if (check instanceof KeyedViolationCheck) {
            KeyedViolationCheck<WP, Object> keyedViolationCheck = (KeyedViolationCheck<WP, Object>) check;
            return keyedViolationCheck.getSetting(packet);
        }
        else if (check instanceof ViolationCheck) {
            return ((ViolationCheck)check).getSetting();
        }
        return null;
    }

    public void check(Player player, ByteBuf byteBuf, CheckContextContainer checkContextContainer) {
        final CancellablePacketObserver cancellablePacketObserver = new CancellableByteBufImpl(player, byteBuf);
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
        final CancellablePacketObserver cancellablePacketObserver = new CancellablePacketDelegateImpl(packetContainer);
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
