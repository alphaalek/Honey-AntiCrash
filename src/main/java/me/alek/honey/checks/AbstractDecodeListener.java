package me.alek.honey.checks;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.response.Validation;
import me.alek.honey.checks.types.DecodeValidation;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.listener.FuzzyPacketAdapter;
import me.alek.honey.netty.packet.CancellableByteBuf;
import me.alek.honey.util.Array;
import me.alek.honey.util.CallbackAction;
import org.bukkit.Bukkit;

import java.util.Optional;

public abstract class AbstractDecodeListener extends FuzzyPacketAdapter implements CheckPipelineFactory<DecodeValidation>, BaseValidation {

    private final Array<CheckPipeline<DecodeValidation>> pipelines = new Array<>();

    @Override
    public void onByteBufDecode(HoneyPlayer player, CancellableByteBuf byteBuf) {
        if (requiresPlayer() && player == null) {
            return;
        }

        // cancel the decoding if the player is not even online
        if (player != null && byteBuf.buf().refCnt() < 1) {
            byteBuf.cancel();
            return;
        }

        CheckContextContainer checkContextContainer = new CheckContextContainer();
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

        getPipeline(player).doStream(
                player,
                (validation, key) -> Settings.getSetting(validation),
                validation -> validation.validate(player, byteBuf.buf(), checkContextContainer),
                byteBuf, checkContextContainer, callback);
    }

    @Override
    public void register(PacketLibrary library) {
        library.addDecodeListener(this, getPriority());
    }

    public synchronized CheckPipeline<DecodeValidation> getPipeline(HoneyPlayer player) {
        return AbstractPacketListener.getCachedPipeline(pipelines, this, player);
    }
}
