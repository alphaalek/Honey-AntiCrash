package me.alek.honey.checks.response;

import me.alek.honey.services.ViolationService;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class ResponseBuilder {

    public static ResponseContainer PASS = ResponseBuilder
            .inst()
            .pass()
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

    public ResponseContainer getContainer() {
        return responseContainer;
    }

    public ResponseBuilder(ResponseContainer container, List<ResponseContainer> subContainers) {
        this(container);
        this.responseContainer.subContainers = subContainers;
    }

    public ResponseBuilder add(ResponseContainer... containers) {
        responseContainer.subContainers.addAll(Arrays.asList(containers));
        return this;
    }

    public ResponseBuilder pass() {
        responseContainer.success = true;
        return this;
    }

    public ResponseBuilder cancel() {
        responseContainer.cancelled = true;
        return this;
    }

    public ResponseBuilder flag() {
        return flag(1);
    }

    public ResponseBuilder flag(double amount) {
        responseContainer.flag = amount;
        return this;
    }

    public ResponseBuilder punish() {
        responseContainer.punish = true;
        return this;
    }

    public ResponseBuilder terminate() {
        responseContainer.terminate = true;
        return this;
    }

    public ResponseBuilder skip(int indices) {
        responseContainer.skipIndices = indices;
        return this;
    }

    public ResponseBuilder withViolationKey(ViolationService.HoneyKey key) {
        responseContainer.violationKey = key;
        return this;
    }

    public ResponseBuilder onPunish(Runnable runnable) {
        responseContainer.onPunish = runnable;
        return this;
    }

    public ResponseBuilder onViolation(Consumer<Runnable> runnable) {
        Runnable priorOnViolation = responseContainer.onViolation;
        responseContainer.onViolation = () -> runnable.accept(priorOnViolation);
        return this;
    }

    public ResponseBuilder onFlag(Runnable runnable) {
        responseContainer.onFlag = runnable;
        return this;
    }

    public ResponseContainer build() {
        return responseContainer;
    }
}
