package me.alek.honey.checks;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.response.Validation;
import me.alek.honey.checks.types.DecodeValidation;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.listener.FuzzyPacketAdapter;
import me.alek.honey.netty.packet.cancellables.CancellableByteBufImpl;
import me.alek.honey.util.Array;
import me.alek.honey.util.CallbackAction;
import org.bukkit.Bukkit;

import java.util.Optional;

public abstract class AbstractDecodeListener extends FuzzyPacketAdapter implements CheckVisitable<DecodeValidation>, IListener {

    private final Array<CheckPipeline<DecodeValidation>> visitors = new Array<>();
    private boolean initializedCheckInstances;

    @Override
    public void onByteBufDecode(HoneyPlayer player, CancellableByteBufImpl byteBuf) {
        CheckContextContainer checkContextContainer = new CheckContextContainer();

        // cancel the decoding if the player is not even online
        if (player != null
                && byteBuf.buf().refCnt() < 1) {
            byteBuf.cancel();
            return;
        }

        CallbackAction<ResponseContainer> callback = new CallbackAction<>();
        int bytes = byteBuf.buf().readableBytes();
        callback.addAction(response -> {
            response.builder().onViolation((check, onViolation) -> {

                CheckContextContainer.DevelopmentData defaultDevelopmentData = checkContextContainer.getDefaultDevelopmentData();
                if (defaultDevelopmentData.getDataSet().isEmpty()) {
                    defaultDevelopmentData.addEmpty();
                    defaultDevelopmentData.addValue(" Buffer", bytes + " bytes");
                    AbstractPacketListener.applyCommonDevelopmentData(defaultDevelopmentData);
                }

                if (onViolation != null) onViolation.run();
            });
        });

        getPipeline(player).doStream(player, (check, key) -> Settings.getSetting(check), validation -> {
            try {
                return validation.validate(player, byteBuf.buf(), checkContextContainer);
            } catch (Exception e) {
                e.printStackTrace();
                Bukkit.getLogger().severe("Failed to handle decode validation for packet sent by " + Optional.ofNullable(player).map(p -> p.name).orElse("Unknown"));
            }
            return Validation.PASS;
        }, byteBuf, checkContextContainer, callback);
    }

    @Override
    public void register(PacketLibrary library) {
        library.addDecodeListener(this, getPriority());
    }

    public synchronized boolean hasInitializedCheckInstances() {
        return initializedCheckInstances;
    }

    public synchronized CheckPipeline<DecodeValidation> getPipeline(HoneyPlayer player) {
        return AbstractPacketListener.getCachedPipeline(visitors, this, player);
    }

    public void setInitializedCheckInstances() {
        initializedCheckInstances = true;
    }
}
