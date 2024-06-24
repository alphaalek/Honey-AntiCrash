package me.alek.honey.checks;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.response.Validation;
import me.alek.honey.checks.types.DecodeValidation;
import me.alek.honey.checks.types.HoneyValidation;
import me.alek.honey.checks.types.PacketValidation;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.listener.PacketAdapter;
import me.alek.honey.netty.packet.cancellables.CancellablePacketImpl;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.util.Array;
import me.alek.honey.util.CallbackAction;
import org.bukkit.Bukkit;

public abstract class AbstractPacketListener<WP extends WrappedPacket<WP>> extends PacketAdapter<WP> implements IPacketListener, CheckVisitable<PacketValidation<WP>> {

    private final Array<CheckPipeline<PacketValidation<WP>>> visitors = new Array<>();
    private boolean initializedCheckInstances;

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

    @Override
    public void onPacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer) throws Exception {
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

        if (requiresPlayer() && player == null
                || !onPrePacketReceive(player, packetContainer, checkContextContainer, callback)) {
            return;
        }

        getPipeline(player).doStream(player, packetContainer.getHandle(), (check, key) -> Settings.getSetting(player, check, key, packetContainer.getPacket()), check -> {
            try {
                return check.check(player, packetContainer.getPacket(), checkContextContainer);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return Validation.PASS;
        }, new CancellablePacketImpl(packetContainer), checkContextContainer, callback);

        onPostPacketReceive(player, packetContainer, checkContextContainer);
    }

    public synchronized CheckPipeline<PacketValidation<WP>> getPipeline(HoneyPlayer player) {
        return getCachedPipeline(visitors, this, player);
    }

    public static synchronized <C extends HoneyValidation<?>> CheckPipeline<C> getCachedPipeline(Array<CheckPipeline<C>> visitors, CheckVisitable<C> visitable, HoneyPlayer player) {
        return getCachedPipeline(visitors, visitable, player, player != null ? player.registryID : 0);
    }

    public static synchronized <C extends HoneyValidation<?>> CheckPipeline<C> getCachedPipeline(Array<CheckPipeline<C>> visitors, CheckVisitable<C> visitable, HoneyPlayer player, int registryID) {
        if (!visitable.requiresPlayer() && !visitors.has(registryID)
                || visitable.requiresPlayer() && player != null && player.isOnlineWeak() && !player.punished && !player.equals(visitors.get(registryID, () -> visitable.getPipeline(player)).getPlayer())) {
            visitors.set(registryID, visitable.getPipeline(player));

            if (player != null) {
                player.leaveCallback.addAction(p -> visitors.remove(registryID));
            }
        }
        visitable.setInitializedCheckInstances();

        return visitors.get(registryID);
    }

    public void setInitializedCheckInstances() {
        initializedCheckInstances = true;
    }

    public synchronized boolean hasInitializedCheckInstances() {
        return initializedCheckInstances;
    }

    public boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer, CheckContextContainer container, CallbackAction<ResponseContainer> callback) {
        return true;
    }

    public void onPostPacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer, CheckContextContainer container) {

    }
}
