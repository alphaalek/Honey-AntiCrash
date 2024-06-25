package me.alek.honey.validation;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.validation.response.ResponseContainer;
import me.alek.honey.validation.types.HoneyValidation;
import me.alek.honey.validation.types.PacketValidation;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.listener.PacketAdapter;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.util.Array;
import me.alek.honey.util.CallbackAction;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.function.Supplier;

public abstract class AbstractPacketListener<WP extends WrappedPacket<WP>> extends PacketAdapter<WP> implements BaseValidation, CheckPipelineFactory<PacketValidation<WP>> {

    private final Array<CheckPipeline<PacketValidation<WP>>> visitors = new Array<>();

    public static void applyCommonDevelopmentData(CheckContextContainer.DevelopmentData developmentData) {
        developmentData.addValue(" Version", Bukkit.getServer().getVersion());
        developmentData.removeLastInternalData();
        developmentData.addEmpty();
        developmentData.addHeader("Description", null);

        // temporary placeholders
        developmentData.addEmpty(); // description
        developmentData.addEmpty(); // STQ
        developmentData.addEmpty(); // NTQ
    }

    public static synchronized <C extends HoneyValidation<?>> CheckPipeline<C> getCachedPipeline(Array<CheckPipeline<C>> pipelines, CheckPipelineFactory<C> factory, HoneyPlayer player) {
        return getCachedPipeline(pipelines, factory, player, player != null ? player.registryID : 0);
    }

    public static synchronized <C extends HoneyValidation<?>> CheckPipeline<C> getCachedPipeline(Array<CheckPipeline<C>> pipelines, CheckPipelineFactory<C> factory, HoneyPlayer player, int registryID) {
        if (!pipelines.has(registryID)) {
            if (!factory.requiresPlayer()
                    || player != null && player.isOnlineDirect() && !player.punished) {
                pipelines.set(registryID, factory.getNewPipeline());

                if (player != null) {
                    player.leaveCallback.addAction(p -> pipelines.remove(registryID));
                }
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
        if (requiresPlayer() && player == null) {
            return;
        }

        CheckContextContainer checkContextContainer = new CheckContextContainer();
        CallbackAction<ResponseContainer> callback = new CallbackAction<>();
        callback.addAction((response) -> {
            response.builder().onViolation((check, onViolation) -> {
                CheckContextContainer.DevelopmentData defaultDevelopmentData = checkContextContainer.getDefaultDevelopmentData();

                if (defaultDevelopmentData.getDataSet().isEmpty()) {
                    defaultDevelopmentData.addEmpty();
                    defaultDevelopmentData.addValue(" Packet", packetContainer.getType() + " (id: " + packetContainer.getType().getMinecraftID().get() + ")");
                    applyCommonDevelopmentData(defaultDevelopmentData);
                }

                if (onViolation != null) onViolation.run();
            });
        });

        if (!onPrePacketReceive(player, packetContainer, checkContextContainer, callback)) {
            return;
        }

        getPipeline(player).doStream(
                player,
                (validation, key) -> Settings.getSetting(player, validation, key, packetContainer.getPacket()),
                validation -> validation.validate(player, packetContainer.getPacket(), checkContextContainer),
                packetContainer, checkContextContainer, callback);

        onPostPacketReceive(player, packetContainer, checkContextContainer);
    }

    public abstract Supplier<List<PacketType>> getPacketTypes();

    public synchronized CheckPipeline<PacketValidation<WP>> getPipeline(HoneyPlayer player) {
        return getCachedPipeline(visitors, this, player);
    }

    public boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer, CheckContextContainer container, CallbackAction<ResponseContainer> callback) {
        return true;
    }

    public void onPostPacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer, CheckContextContainer container) {

    }
}
