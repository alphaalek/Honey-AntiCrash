package me.alek.honey.validation;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.validation.types.HoneyValidation;
import me.alek.honey.validation.types.PacketValidation;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.PacketAdapter;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.util.Array;
import me.alek.honey.util.Callback;
import org.bukkit.Bukkit;

import java.util.List;

public abstract class AbstractPacketListener<WP extends WrappedPacket<WP>> extends PacketAdapter<WP> implements BaseValidationContainer, ValidationPipelineFactory<PacketValidation<WP>> {

    private final Array<ValidationPipeline<PacketValidation<WP>>> visitors = new Array<>();
    private final ThreadLocal<Boolean> hasCalledCallback = new ThreadLocal<>();

    public static void applyCommonDevelopmentData(ValidationPipelineContext.DevelopmentData developmentData) {
        developmentData.addValue(" Version", Bukkit.getServer().getVersion());
        developmentData.removeLastInternalData();
        developmentData.addEmpty();
        developmentData.addHeader("Description", null);

        // temporary placeholders
        developmentData.addEmpty(); // description
        developmentData.addEmpty(); // STQ
        developmentData.addEmpty(); // NTQ
    }

    public static synchronized <C extends HoneyValidation<?>> ValidationPipeline<C> getCachedPipeline(Array<ValidationPipeline<C>> pipelines, ValidationPipelineFactory<C> factory, HoneyPlayer player) {
        int id = player.registryID;
        if (!pipelines.has(id)) {
            if (!factory.requiresPlayer()
                    || player.isBukkitReady() && player.isStreamActive()) {
                pipelines.set(id, factory.getNewPipeline());

                player.leaveCallback.addAction(Callback.Action.from(p -> pipelines.remove(id)));
            }
        }

        return pipelines.get(id);
    }

    @Override
    public void register(PacketLibrary library) {
        library.addListeners(this, getPriority(), getPacketTypes());
    }

    private void call(HoneyPlayer player, PacketContainer<WP> packet) {
        if (requiresPlayer() && !player.isBukkitReady()) {
            return;
        }

        ValidationPipelineContext context = new ValidationPipelineContext();
        Callback callback = new Callback();
        callback.addAction(Callback.Action.<ViolationKey, ValidationServiceContext>from((keyOuter, contextInner) -> {
            contextInner.builder().onViolation(keyOuter, (keyInner, onViolation) -> {
                context.getDevelopmentData(keyInner).addEmpty();
                context.getDevelopmentData(keyInner).addValue(" Packet", packet.getType() + " (id: " + packet.getType().getMinecraftID().get() + ")");
                context.getDevelopmentData(keyInner).addValue(" Buffer", packet.getByteSize() + " bytes");
                AbstractPacketListener.applyCommonDevelopmentData(context.getDevelopmentData(keyInner));

                if (onViolation != null) onViolation.run();
            });
        }));

        if (!onPrePacketReceive(player, packet, context, callback)) {
            return;
        }

        getCachedPipeline(visitors, this, player).doStream(
                player,
                (validation, key) -> Settings.getSetting(player, validation, key, packet.getPacket()),
                validation -> validation.validate(player, packet.getPacket(), context),
                packet, context, callback);

        hasCalledCallback.remove();

        onPostPacketReceive(player, packet, context);
    }

    @Override
    public void onPacketReceive(HoneyPlayer player, PacketContainer<WP> packet) {
        call(player, packet);
    }

    @Override
    public void onPacketSend(HoneyPlayer player, PacketContainer<WP> packet) {
        call(player, packet);
    }

    public abstract List<PacketType> getPacketTypes();

    public boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<WP> packet, ValidationPipelineContext container, Callback callback) {
        return true;
    }

    public void onPostPacketReceive(HoneyPlayer player, PacketContainer<WP> packet, ValidationPipelineContext container) {

    }
}
