package me.alek.honey.validation;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packetwrappers.ByteWrappedPacket;
import me.alek.honey.validation.types.PacketValidation;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.PacketListenerAdapter;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.util.Array;
import me.alek.honey.util.Callback;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.Optional;

public abstract class AbstractValidationPacketListener<WP extends WrappedPacket<WP>> extends PacketListenerAdapter.Common<WP> implements BaseValidationContainer, ValidationPipelineFactory<PacketValidation<WP>> {

    public void applyCommonDevelopmentData(ValidationPipelineContext.DevelopmentData data) {
        data.addValue(" Version", Bukkit.getServer().getVersion());
        data.removeLastInternalData();
        data.addEmpty();
        data.addHeader("Description", null);

        // temporary placeholders
        data.addEmpty(); // description
        data.addEmpty(); // STQ
        data.addEmpty(); // NTQ
    }

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
    public void onPacketStream(HoneyPlayer player, PacketBound bound, PacketContainer<WP> packet) throws Exception {
        if (!player.isStreamActive() || requiresPlayer() && !player.isBukkitReady()) {
            return;
        }

        ValidationPipelineContext context = new ValidationPipelineContext();
        Callback callback = new Callback();
        callback.addAction(Callback.Action.<ViolationKey, ValidationViolationContext>from((keyOuter, contextInner) -> {
            contextInner.builder().onViolation(keyOuter, (keyInner, onViolation) -> {
                context.getDevelopmentData(keyInner).addEmpty();
                context.getDevelopmentData(keyInner).addValue(" Packet", packet.getType() + " (id: " + packet.getType().getMinecraftID().get() + ")");
                context.getDevelopmentData(keyInner).addValue(" Buffer", Optional.ofNullable(packet.getInitialByteWrapped()).map(ByteWrappedPacket::getByteSize).orElse(-1) + " bytes");
                applyCommonDevelopmentData(context.getDevelopmentData(keyInner));

                if (onViolation != null) onViolation.run();
            });
        }));

        if (!onPrePacketReceive(player, packet, context, callback)) {
            return;
        }

        getCachedPipeline(this, player).doStream(
                player,
                (validation, key) -> Settings.getSetting(player, validation, key, packet.getAnyWrapped()),
                validation -> validation.validate(player, packet.getAnyWrapped(), context),
                packet, context, callback);

        hasCalledCallback.remove();

        onPostPacketReceive(player, packet, context);
    }

    public boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<WP> packet, ValidationPipelineContext container, Callback callback) {
        return true;
    }

    public void onPostPacketReceive(HoneyPlayer player, PacketContainer<WP> packet, ValidationPipelineContext container) {
        // to be overridden
    }
}
