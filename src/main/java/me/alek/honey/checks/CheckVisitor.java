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
import me.alek.honey.utility.CallbackAction;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;
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

    private void handleCommonCheck(HoneyPlayer player, Object handle,
                                   Function<HoneyCheck, ConfigurationOptions.VlSetting> getSetting,
                                   Function<HoneyCheck, ResponseContainer> getResponse,
                                   CancellablePacketObserver cancellablePacketObserver, CheckContextContainer checkContextContainer, CallbackAction<ResponseContainer> callback) {
        int skipping = 0;
        for (HoneyCheck check : checks) {
            if (--skipping >= 0) continue;

            ConfigurationOptions.VlSetting setting = getSetting.apply(check);
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

            final List<ResponseContainer> responsesList = new ArrayList<>(defaultResponse.subContainers);
            responsesList.add(defaultResponse);

            boolean terminated = false;
            CheckContextContainer.DevelopmentData developmentData = checkContextContainer.getDevelopmentData(check);
            for (ResponseContainer response : responsesList) {
                if (response == null) continue;
                if (response.skipIndices != 0) skipping = response.skipIndices;
                if (response.success) {
                    if (response.terminate) terminated = true;
                    continue;
                }
                if (response.cancelled) cancellablePacketObserver.cancel();

                final ViolationService.HoneyKey key = (response.limitsKey != null)
                        ? response.limitsKey
                        : ViolationService.HoneyKey.of(check);
                if (response.flag != -1 && player != null) {
                    if (setting == null) {
                        if (response.terminate) terminated = true;
                        continue;
                    }

                    if (response.onViolation == null && response.onPunish == null && response.onFlag == null) {
                        response = response.copyWithoutPosts().build();
                    }
                    callback.call(response);
                    response.builder().onViolation(onViolation -> {
                        if (onViolation != null) onViolation.run();

                        developmentData.setText(0, "§8* §7Player " + player.name + " flagged " + check.getName() + " §8*");
                        developmentData.setLine(5, check.getDescription());
                    });

                    HoneyPlugin.get().getViolationService().flag(player, handle, cancellablePacketObserver, key, response.violationKey, response.flag, response.onPunish, response.onViolation, response.onFlag, check, setting, developmentData);
                }
                if (response.punish && player != null)
                    HoneyPlugin.get().getViolationService().punish(null, player, check, key);

                if (response.terminate) terminated = true;
            }
            if (terminated) return;
        }
    }

    private ConfigurationOptions.VlSetting getSetting(HoneyCheck check, WP packet) {
        if (!check.isViolation()) return null;

        ViolationCheck violationCheck = (ViolationCheck) check;
        if (packet != null && violationCheck.isKeyed()) {
            return ((KeyedViolationCheck<WP>)violationCheck).getSetting(packet);
        }
        return violationCheck.getSetting();
    }

    public void check(HoneyPlayer player, CancellableByteBufImpl byteBuf, CheckContextContainer checkContextContainer, CallbackAction<ResponseContainer> callback) {
        handleCommonCheck(player, null, (check) -> getSetting(check, null), (check) -> {
            try {
                return ((DecodeCheck)check).check(player, byteBuf.buf(), checkContextContainer);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return ResponseBuilder.SUCCESS;
        }, byteBuf, checkContextContainer, callback);
    }

    @SuppressWarnings("unchecked")
    public void check(HoneyPlayer player, PacketContainer<WP> packetContainer, CheckContextContainer checkContextContainer, CallbackAction<ResponseContainer> callback) throws Exception {
        final CancellablePacketObserver cancellablePacketObserver = new CancellablePacketImpl(packetContainer);
        handleCommonCheck(player, packetContainer.getHandle(), (check) -> getSetting(check, packetContainer.getPacket()), (check) -> {
            try {
                return ((PacketCheck<WP>)check).check(player, packetContainer.getPacket(), checkContextContainer);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return ResponseBuilder.SUCCESS;
        }, cancellablePacketObserver, checkContextContainer, callback);
    }
}
