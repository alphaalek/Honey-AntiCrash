package me.alek.honey.validation;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.packet.PacketAdapter;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.validation.types.DecodeValidation;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.packet.CancellableByteBuf;
import me.alek.honey.util.Array;
import me.alek.honey.util.CallbackAction;
import me.alek.honey.validation.types.HoneyValidation;

import java.util.stream.Collectors;

public abstract class AbstractDecodeListener<WP extends WrappedPacket<WP>> extends PacketAdapter<WP> implements ValidationPipelineFactory<DecodeValidation>, BaseValidation {

    private final Array<ValidationPipeline<DecodeValidation>> pipelines = new Array<>();

    @Override
    public void onPacketDecode(HoneyPlayer player, PacketType type, CancellableByteBuf byteBuf) {
        if (requiresPlayer() && !player.isBukkitReady()) {
            return;
        }

        ValidationPipelineContext validationContext = new ValidationPipelineContext();
        CallbackAction callback = new CallbackAction();
        int bytes = byteBuf.buf().readableBytes();

        callback.addAction(CallbackAction.Action.<ViolationKey, ValidationServiceContext>as((keyOuter, context) -> {
            context.builder().onViolation(keyOuter, (keyInner, onViolation) -> {
                ValidationPipelineContext.DevelopmentData developmentData = validationContext.getDevelopmentData(keyInner);

                developmentData.addEmpty();
                developmentData.addValue(" Packet", type + " (id: " + type.getMinecraftID().get() + ")");
                developmentData.addValue(" Buffer", bytes + " bytes");
                AbstractPacketListener.applyCommonDevelopmentData(developmentData);

                if (onViolation != null) onViolation.run();
            });
        }));

        getPipeline(player).doStream(
                player,
                (validation, key) -> Settings.getSetting(validation),
                validation -> validation.validate(player, byteBuf.buf(), validationContext),
                byteBuf, validationContext, callback);
    }

    @Override
    public void register(PacketLibrary library) {
        library.addListeners(this, getPriority(), PacketTypes.getTypesAvailable().stream().collect(Collectors.toList()));
    }

    public synchronized ValidationPipeline<DecodeValidation> getPipeline(HoneyPlayer player) {
        return AbstractPacketListener.getCachedPipeline(pipelines, this, player);
    }
}
