package me.alek.honey.validation;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.packet.PacketAdapter;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.validation.types.DecodeValidation;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.packet.CancellableByteBuf;
import me.alek.honey.util.Array;
import me.alek.honey.util.CallbackAction;

public abstract class AbstractDecodeListener<WP extends WrappedPacket<WP>> extends PacketAdapter<WP> implements ValidationPipelineFactory<DecodeValidation>, BaseValidation {

    private final Array<ValidationPipeline<DecodeValidation>> pipelines = new Array<>();

    @Override
    public void onByteBufDecode(HoneyPlayer player, CancellableByteBuf byteBuf) {
        if (requiresPlayer() && player == null) {
            return;
        }

        ValidationContextContainer validationContext = new ValidationContextContainer();
        CallbackAction<ResponseContainer> callback = new CallbackAction<>();
        int bytes = byteBuf.buf().readableBytes();
        callback.addAction(response -> {
            response.builder().onViolation((check, onViolation) -> {

                ValidationContextContainer.DevelopmentData defaultDevelopmentData = validationContext.getDefaultDevelopmentData();
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
                validation -> validation.validate(player, byteBuf.buf(), validationContext),
                byteBuf, validationContext, callback);
    }

    @Override
    public void register(PacketLibrary library) {
        library.addDecodeListener(this, getPriority());
    }

    public synchronized ValidationPipeline<DecodeValidation> getPipeline(HoneyPlayer player) {
        return AbstractPacketListener.getCachedPipeline(pipelines, this, player);
    }
}
