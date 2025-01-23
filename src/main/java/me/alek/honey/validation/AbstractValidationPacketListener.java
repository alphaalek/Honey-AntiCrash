package me.alek.honey.validation;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.PacketListenerAdapter;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packetwrappers.ByteWrappedPacket;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.processor.ListenerPriority;
import me.alek.honey.util.Array;
import me.alek.honey.util.Callback;
import me.alek.honey.validation.types.PacketValidation;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.Optional;

public abstract class AbstractValidationPacketListener<WP extends WrappedPacket<WP>> extends PacketListenerAdapter.Common<WP> implements BaseValidationContainer, ValidationPipelineFactory<PacketValidation<WP>> {

    public synchronized ValidationPipeline<PacketValidation<WP>> getCachedPipeline(ValidationPipelineFactory<PacketValidation<WP>> factory, HoneyPlayer player) {
        if (!pipelines.has(player.registryID)) {
            pipelines.set(player.registryID, factory.createValidationPipeline());

            player.leaveCallback.addAction(Callback.Action.from(p -> pipelines.remove(player.registryID)));
        }

        return pipelines.get(player.registryID);
    }

    private final Array<ValidationPipeline<PacketValidation<WP>>> pipelines = new Array<>();
    private final ThreadLocal<Boolean> hasCalledCallback = new ThreadLocal<>();

    public AbstractValidationPacketListener(PacketLibrary library, PacketContainer.PipelineState state, ListenerPriority priority, List<PacketType> types) {
        library.addListeners(this, state, priority, types);
    }

    @Override
    public void onPacketStream(HoneyPlayer player, PacketBound bound, PacketContainer<WP> packet) {
        if (!player.isStreamActive() || requiresPlayer() && !player.isBukkitReady()) {
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

        hasCalledCallback.remove();

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
