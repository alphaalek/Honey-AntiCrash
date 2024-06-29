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
import me.alek.honey.util.Callback;

import java.util.stream.Collectors;

public abstract class AbstractDecodeListener<WP extends WrappedPacket<WP>> extends PacketAdapter<WP> implements ValidationPipelineFactory<DecodeValidation>, BaseValidation {

    private final Array<ValidationPipeline<DecodeValidation>> pipelines = new Array<>();

    @Override
    public void onPacketDecode(HoneyPlayer player, PacketType type, CancellableByteBuf byteBuf) {
        if (requiresPlayer() && !player.isBukkitReady()) {
            return;
        }

        ValidationPipelineContext context = new ValidationPipelineContext();
        Callback callback = new Callback();
        int bytes = byteBuf.buf().readableBytes();

        callback.addAction(Callback.Action.<ViolationKey, ValidationServiceContext>from((keyOuter, contextInner) -> {
            contextInner.builder().onViolation(keyOuter, (keyInner, onViolation) -> {
                context.getDevelopmentData(keyInner).addEmpty();
                context.getDevelopmentData(keyInner).addValue(" Packet", type + " (id: " + type.getMinecraftID().get() + ")");
                context.getDevelopmentData(keyInner).addValue(" Buffer", bytes + " bytes");
                AbstractPacketListener.applyCommonDevelopmentData(context.getDevelopmentData(keyInner));

                if (onViolation != null) onViolation.run();
            });
        }));

        getPipeline(player).doStream(
                player,
                (validation, key) -> Settings.getSetting(validation),
                validation -> validation.validate(player, byteBuf.buf(), context),
                byteBuf, context, callback);
    }

    @Override
    public void register(PacketLibrary library) {
        library.addListeners(this, getPriority(), PacketTypes.getTypesAvailable().stream().collect(Collectors.toList()));
    }

    public synchronized ValidationPipeline<DecodeValidation> getPipeline(HoneyPlayer player) {
        return AbstractPacketListener.getCachedPipeline(pipelines, this, player);
    }
}
