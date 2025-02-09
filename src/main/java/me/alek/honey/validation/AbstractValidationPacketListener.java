package me.alek.honey.validation;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.PacketListenerAdapter;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packetwrappers.SourcedWrappedPacket;
import me.alek.honey.netty.processor.ListenerPriority;
import me.alek.honey.util.Array;
import me.alek.honey.util.Callback;

import java.util.List;

public abstract class AbstractValidationPacketListener<WP extends SourcedWrappedPacket<WP>> extends PacketListenerAdapter.Common<WP> implements BaseValidationContainer, ValidationPipelineFactory<PacketValidation<WP>> {

    public synchronized ValidationPipeline<PacketValidation<WP>> getCachedPipeline(ValidationPipelineFactory<PacketValidation<WP>> factory, HoneyPlayer player) {
        if (!this.pipelines.has(player.registryID)) {
            this.pipelines.set(player.registryID, factory.createValidationPipeline());

            player.leaveCallback.addAction(Callback.Action.from(p -> this.pipelines.remove(player.registryID)));
        }

        return this.pipelines.get(player.registryID);
    }

    private final Array<ValidationPipeline<PacketValidation<WP>>> pipelines = new Array<>();

    public AbstractValidationPacketListener(PacketLibrary library, PacketContainer.PipelineState state, ListenerPriority priority, List<PacketType> types) {
        library.addListener(this, state, priority, types);
    }

    @Override
    public void onPacketStream(HoneyPlayer player, PacketBound bound, PacketContainer<WP> packet) {
        if (!player.isStreamActive() || requiresPlayer() && !player.linkedBukkit()) {
            return;
        }

        ValidationPipelineContext context = new ValidationPipelineContext(true);
        ValidationPipeline.applyCallbackDevelopmentData(packet, context);
        if (!onPrePacketReceive(player, packet, context)) {
            return;
        }

        getCachedPipeline(this, player).doStream(
                player,
                (validation, key) -> Settings.getSetting(player, validation, key, packet.getAnyWrapped()),
                validation -> validation.validate(player, packet.getAnyWrapped(), context),
                packet, context);

        onPostPacketReceive(player, packet, context);
    }

    public boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<WP> packet, ValidationPipelineContext context) {
        // to be overridden
        return true;
    }

    public void onPostPacketReceive(HoneyPlayer player, PacketContainer<WP> packet, ValidationPipelineContext context) {
        // to be overridden
    }
}
