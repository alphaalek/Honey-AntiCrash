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
import me.alek.honey.util.CallbackAction;
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
        return getCachedPipeline(pipelines, factory, player, player.registryID);
    }

    public static synchronized <C extends HoneyValidation<?>> ValidationPipeline<C> getCachedPipeline(Array<ValidationPipeline<C>> pipelines, ValidationPipelineFactory<C> factory, HoneyPlayer player, int registryID) {
        if (!pipelines.has(registryID)) {
            if (!factory.requiresPlayer()
                    || player.isBukkitReady() && player.isOnlineDirect() && !player.punished) {
                pipelines.set(registryID, factory.getNewPipeline());

                player.leaveCallback.addAction(CallbackAction.Action.as(p -> pipelines.remove(registryID)));
            }
        }

        return pipelines.get(registryID);
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

        ValidationPipelineContext validationContext = new ValidationPipelineContext();
        CallbackAction callback = new CallbackAction();
        callback.addAction(CallbackAction.Action.<ViolationKey, ValidationServiceContext>as((keyOuter, context) -> {
            context.builder().onViolation(keyOuter, (keyInner, onViolation) -> {
                ValidationPipelineContext.DevelopmentData developmentData = validationContext.getDevelopmentData(keyInner);

                developmentData.addEmpty();
                developmentData.addValue(" Packet", packetContainer.getType() + " (id: " + packetContainer.getType().getMinecraftID().get() + ")");
                developmentData.addValue(" Buffer", packetContainer.getByteSize() + " bytes");
                AbstractPacketListener.applyCommonDevelopmentData(developmentData);

                if (onViolation != null) onViolation.run();
            });
        }));

        if (!onPrePacketReceive(player, packetContainer, validationContext, callback)) {
            return;
        }

        getPipeline(player).doStream(
                player,
                (validation, key) -> Settings.getSetting(player, validation, key, packetContainer.getPacket()),
                validation -> validation.validate(player, packetContainer.getPacket(), validationContext),
                packetContainer, validationContext, callback);

        hasCalledCallback.remove();

        onPostPacketReceive(player, packetContainer, validationContext);
    }

    public abstract List<PacketType> getPacketTypes();

    public synchronized ValidationPipeline<PacketValidation<WP>> getPipeline(HoneyPlayer player) {
        return getCachedPipeline(visitors, this, player);
    }

    public boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer, ValidationPipelineContext container, CallbackAction callback) {
        return true;
    }

    public void onPostPacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer, ValidationPipelineContext container) {

    }
}
