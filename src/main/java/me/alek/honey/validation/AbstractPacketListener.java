package me.alek.honey.validation;

import me.alek.honey.HoneyPlayer;
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

public abstract class AbstractPacketListener<WP extends WrappedPacket<WP>> extends PacketAdapter<WP> implements BaseValidation, ValidationPipelineFactory<PacketValidation<WP>> {

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
                    || player.isBukkitReady() && player.isOnlineDirect() && !player.punished) {
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

    @Override
    public void onPacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer) {
        if (requiresPlayer() && !player.isBukkitReady()) {
            return;
        }

        ValidationPipelineContext context = new ValidationPipelineContext();
        Callback callback = new Callback();
        callback.addAction(Callback.Action.<ViolationKey, ValidationServiceContext>from((keyOuter, contextInner) -> {
            contextInner.builder().onViolation(keyOuter, (keyInner, onViolation) -> {
                context.getDevelopmentData(keyInner).addEmpty();
                context.getDevelopmentData(keyInner).addValue(" Packet", packetContainer.getType() + " (id: " + packetContainer.getType().getMinecraftID().get() + ")");
                context.getDevelopmentData(keyInner).addValue(" Buffer", packetContainer.getByteSize() + " bytes");
                AbstractPacketListener.applyCommonDevelopmentData(context.getDevelopmentData(keyInner));

                if (onViolation != null) onViolation.run();
            });
        }));

        if (!onPrePacketReceive(player, packetContainer, context, callback)) {
            return;
        }

        getPipeline(player).doStream(
                player,
                (validation, key) -> Settings.getSetting(player, validation, key, packetContainer.getPacket()),
                validation -> validation.validate(player, packetContainer.getPacket(), context),
                packetContainer, context, callback);

        hasCalledCallback.remove();

        onPostPacketReceive(player, packetContainer, context);
    }

    public abstract List<PacketType> getPacketTypes();

    public synchronized ValidationPipeline<PacketValidation<WP>> getPipeline(HoneyPlayer player) {
        return getCachedPipeline(visitors, this, player);
    }

    public boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer, ValidationPipelineContext container, Callback callback) {
        return true;
    }

    public void onPostPacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer, ValidationPipelineContext container) {

    }
}
