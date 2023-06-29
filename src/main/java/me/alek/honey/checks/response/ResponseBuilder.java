package me.alek.honey.checks.response;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.services.ViolationService;

import java.util.function.Function;

public class ResponseBuilder {

    public static ResponseContainer SUCCESS =
        ResponseBuilder
            .inst()
            .success()
            .build();

    public static Function<PacketContainer<? extends WrappedPacket<?>>, ResponseContainer> FLOOD_FLAG
             = (packet) -> ResponseBuilder
                          .inst()
                          .accept()
                          .flag()
                          .onViolation(packet::cancel)
                          .build();

    public static Function<PacketContainer<? extends WrappedPacket<?>>, ResponseContainer> FLOOD_FLAG_DEFAULT_KEY
             = (packet) -> FLOOD_FLAG
                          .apply(packet)
                          .builder()
                          .withKey(ViolationService.CheckKey.DEFAULT)
                          .build();

    public static ResponseBuilder inst() {
        return new ResponseBuilder();
    }

    private final ResponseContainer responseContainer;

    public ResponseBuilder() {
        responseContainer = new ResponseContainer();
    }

    public ResponseBuilder(ResponseContainer container) {
        this.responseContainer = container;
    }

    public ResponseBuilder success() {
        responseContainer.SUCCESS = true;
        return this;
    }

    public ResponseBuilder accept() {
        responseContainer.ACCEPTED = true;
        return this;
    }

    public ResponseBuilder cancel() {
        responseContainer.CANCELLED = true;
        return this;
    }

    public ResponseBuilder flag() {
        return setFlagged(1);
    }

    public ResponseBuilder setFlagged(int amount) {
        responseContainer.FLAGGED = amount;
        return this;
    }

    public ResponseBuilder punish() {
        responseContainer.PUNISH = true;
        return this;
    }

    public ResponseBuilder terminate() {
        responseContainer.TERMINATE = true;
        return this;
    }

    public ResponseBuilder skip(int indices) {
        responseContainer.SKIP_INDICES = indices;
        return this;
    }

    public ResponseBuilder withKey(ViolationService.CheckKey key) {
        responseContainer.CUSTOM_KEY = key;
        return this;
    }

    public ResponseBuilder onViolation(Runnable runnable) {
        responseContainer.ON_VIOLATION = runnable;
        return this;
    }

    public ResponseBuilder notify(String notification) {
        responseContainer.NOTIFY = true;
        responseContainer.NOTIFICATION_MESSAGE = notification;
        return this;
    }

    public ResponseContainer build() {
        return responseContainer;
    }
}
