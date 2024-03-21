package me.alek.honey.checks;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.*;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.CancellablePacketObserver;
import me.alek.honey.netty.packet.cancellables.CancellableByteBufImpl;
import me.alek.honey.netty.packet.cancellables.CancellablePacketImpl;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.services.ViolationService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class CheckVisitor<WP extends WrappedPacket<WP>> {

    private final List<HoneyCheck> checks = new ArrayList<>();
    private final HoneyPlayer player;

    public CheckVisitor(HoneyPlayer player) {
        this.player = player;
    }

    public HoneyPlayer getPlayer() {
        return player;
    }

    public void visit(HoneyCheck check) {
        checks.add(check);
    }

    public List<HoneyCheck> getChecks() {
        return checks;
    }

    private void handleCommonCheck(HoneyPlayer player, Object handle, Function<HoneyCheck, ResponseContainer> getResponse,
            WP packet, CancellablePacketObserver cancellablePacketObserver, CheckContextContainer checkContextContainer, CallbackAction<ResponseContainer> callback) {
        AtomicInteger skipping = null;

        for (HoneyCheck check : checks) {
            if (skipping != null && skipping.getAndDecrement() > 0) continue;

            ConfigurationOptions.VlSetting setting = getSetting(check, packet);
            long registryKey = -1;
            if (setting != null && setting.isEnabled())
                registryKey = HoneyPlugin.get().getCheckHandleTracker().registerTimeStart(check.getName());

            final ResponseContainer defaultResponse = getResponse.apply(check);

            if (setting != null && setting.isEnabled() && registryKey != -1)
                HoneyPlugin.get().getCheckHandleTracker().registerTimeStop(check.getName(), registryKey);

            if (player != null && player.punished) {
                cancellablePacketObserver.cancel();
                return;
            }
            final List<ResponseContainer> responsesList = new ArrayList<>(defaultResponse.SUB_CONTAINERS);
            responsesList.add(defaultResponse);

            boolean terminated = false;
            CheckContextContainer.DevelopmentData developmentData = checkContextContainer.getDevelopmentData(check);
            for (ResponseContainer response : responsesList) {
                if (response == null) continue;

                if (check instanceof KeyedViolationCheck) {
                    KeyedViolationCheck<WP, Object> keyedViolationCheck = (KeyedViolationCheck<WP, Object>) check;
                    if (response.LIMITS_KEY == null) response.LIMITS_KEY = new ViolationService.CheckKey(check, check.getClass(), keyedViolationCheck.getKey(packet));
                }

                if (response.SKIP_INDICES != null) skipping = response.SKIP_INDICES;

                if (response.SUCCESS) {
                    if (response.TERMINATE) terminated = true;
                    continue;
                }
                if (response.CANCELLED) cancellablePacketObserver.cancel();

                final ViolationService.CheckKey key = (response.LIMITS_KEY != null) ? response.LIMITS_KEY : ViolationService.CheckKey.of(check);
                if (response.FLAG != -1 && player != null) {
                    if (setting == null) {
                        if (response.TERMINATE) terminated = true;
                        continue;
                    }

                    response.builder().onViolation(onViolation -> {
                        developmentData.setText(0, "§8* §7Player " + player.name + " flagged " + check.getName() + " §8*");
                        developmentData.setLine(5, check.getDescription());

                        if (onViolation != null) onViolation.run();
                    });
                    callback.call(response);

                    HoneyPlugin.get().getViolationService().flag(player, handle, cancellablePacketObserver, key, response.VIOLATION_KEY, response.FLAG, response.ON_PUNISH, response.ON_VIOLATION, response.ON_FLAG, check, setting, developmentData);
                }
                if (response.PUNISH && player != null)
                    HoneyPlugin.get().getViolationService().punish(null, player, check.getName(), key);

                if (response.TERMINATE) terminated = true;
            }
            if (terminated) return;
        }
    }

    private ConfigurationOptions.VlSetting getSetting(HoneyCheck check, WP packet) {
        if (!check.isViolation()) return null;

        ViolationCheck violationCheck = (ViolationCheck) check;
        if (violationCheck.isKeyed()) {
            KeyedViolationCheck<WP, Object> keyedViolationCheck = (KeyedViolationCheck<WP, Object>) check;
            return keyedViolationCheck.getSetting(packet);
        }
        return violationCheck.getSetting();
    }

    public void check(HoneyPlayer player, CancellableByteBufImpl byteBuf, CheckContextContainer checkContextContainer, CallbackAction<ResponseContainer> callback) {
        handleCommonCheck(player, null, (check) -> {
            try {
                return ((DecodeCheck)check).check(player, byteBuf.buf(), checkContextContainer);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return ResponseBuilder.SUCCESS;
        }, null, byteBuf, checkContextContainer, callback);
    }

    @SuppressWarnings("unchecked")
    public void check(HoneyPlayer player, PacketContainer<WP> packetContainer, CheckContextContainer checkContextContainer, CallbackAction<ResponseContainer> callback) throws Exception {
        final CancellablePacketObserver cancellablePacketObserver = new CancellablePacketImpl(packetContainer);
        handleCommonCheck(player, packetContainer.getHandle(), (check) -> {
            try {
                return ((PacketCheck<WP>)check).check(player, packetContainer.getPacket(), checkContextContainer);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return ResponseBuilder.SUCCESS;
        }, packetContainer.getPacket(), cancellablePacketObserver, checkContextContainer, callback);
    }
}
