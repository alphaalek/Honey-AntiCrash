package me.alek.honey.checks.response;

import me.alek.honey.services.ViolationService;

import java.util.concurrent.atomic.AtomicInteger;

public class ResponseBuilder {

    public static ResponseContainer SUCCESS =
        ResponseBuilder
            .inst()
            .success()
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

    public ResponseBuilder add(ResponseContainer container) {
        responseContainer.SUB_CONTAINERS.add(container);
        return this;
    }

    public ResponseBuilder success() {
        responseContainer.SUCCESS = true;
        return this;
    }

    public ResponseBuilder cancel() {
        responseContainer.CANCELLED = true;
        return this;
    }

    public ResponseBuilder flag() {
        return flag(1);
    }

    public ResponseBuilder flag(double amount) {
        responseContainer.FLAG = amount;
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

    public ResponseBuilder ignoreSpike() {
        responseContainer.IGNORE_SPIKE = true;
        return this;
    }

    public ResponseBuilder skip(int indices) {
        responseContainer.SKIP_INDICES = new AtomicInteger(indices);
        return this;
    }

    public ResponseBuilder withLimitsKey(ViolationService.CheckKey key) {
        responseContainer.LIMITS_KEY = key;
        return this;
    }

    public ResponseBuilder withViolationKey(ViolationService.CheckKey key) {
        responseContainer.VIOLATION_KEY = key;
        return this;
    }

    public ResponseBuilder onPunish(Runnable runnable) {
        responseContainer.ON_PUNISH = runnable;
        return this;
    }

    public ResponseBuilder onViolation(Runnable runnable) {
        responseContainer.ON_VIOLATION = runnable;
        return this;
    }

    public ResponseBuilder onFlag(Runnable runnable) {
        responseContainer.ON_FLAG = runnable;
        return this;
    }

    public ResponseContainer build() {
        return responseContainer;
    }
}
