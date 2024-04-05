package me.alek.honey.checks;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.netty.listener.FuzzyPacketAdapter;
import me.alek.honey.netty.packet.cancellables.CancellableByteBufImpl;
import me.alek.honey.util.Array;
import me.alek.honey.util.CallbackAction;

public abstract class AbstractDecodeListener extends FuzzyPacketAdapter implements CheckVisitable {

    private final Array<CheckVisitor<?>> visitors = new Array<>();
    private boolean initializedCheckInstances;

    @Override
    public void onByteBufDecode(HoneyPlayer player, CancellableByteBufImpl byteBuf) {
        CheckContextContainer checkContextContainer = new CheckContextContainer();

        // cancel the decoding if the player is not even online
        if (player != null && (!player.isOnlineDirect() || byteBuf.buf().refCnt() < 1)) {
            byteBuf.cancel();
            return;
        }

        CallbackAction<ResponseContainer> callback = new CallbackAction<>();
        int bytes = byteBuf.buf().readableBytes();
        callback.addAction(response -> {
            response.builder().onViolation(onViolation -> {

                CheckContextContainer.DevelopmentData defaultDevelopmentData = checkContextContainer.getDefaultDevelopmentData();
                if (defaultDevelopmentData.getDataSet().isEmpty()) {
                    defaultDevelopmentData.addEmpty();
                    defaultDevelopmentData.addValue(" ByteBuf", bytes + " bytes");
                    AbstractPacketListener.applyCommonDevelopmentData(defaultDevelopmentData);
                }

                if (onViolation != null) onViolation.run();
            });
        });

        getVisitor(player).check(player, byteBuf, checkContextContainer, callback);
    }

    public synchronized boolean hasInitializedCheckInstances() {
        return initializedCheckInstances;
    }

    public synchronized CheckVisitor<?> getVisitor(HoneyPlayer player) {
        return AbstractPacketListener.getCachedVisitor(visitors, this, player);
    }

    public void setInitializedCheckInstances() {
        initializedCheckInstances = true;
    }

}
