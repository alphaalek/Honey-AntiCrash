package me.alek.honey.checks.response;

import me.alek.honey.services.ViolationService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ResponseBuilder {

    public static ResponseContainer SUCCESS = ResponseBuilder
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

    public ResponseBuilder(ResponseContainer container, List<ResponseContainer> subContainers) {
        this(container);
        this.responseContainer.SUB_CONTAINERS = subContainers;
    }

    public ResponseBuilder add(ResponseContainer... containers) {
        List<ResponseContainer> responses = Arrays.asList(containers);
        responses.removeAll(Collections.singleton(null));
        responseContainer.SUB_CONTAINERS.addAll(responses);
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

    public ResponseBuilder onViolation(Consumer<Runnable> runnable) {
        Runnable priorOnViolation = responseContainer.ON_VIOLATION;
        responseContainer.ON_VIOLATION = () -> runnable.accept(priorOnViolation);
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
