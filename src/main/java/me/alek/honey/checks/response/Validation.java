package me.alek.honey.checks.response;

import me.alek.honey.services.ViolationService;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class Validation {

    public static ResponseContainer PASS = Validation
            .inst()
            .pass()
            .build();

    public static Validation inst() {
        return new Validation();
    }

    private final ResponseContainer responseContainer;

    public Validation() {
        responseContainer = new ResponseContainer();
    }

    public Validation(ResponseContainer container) {
        this.responseContainer = container;
    }

    public ResponseContainer getContainer() {
        return responseContainer;
    }

    public Validation(ResponseContainer container, List<ResponseContainer> subContainers) {
        this(container);
        this.responseContainer.subContainers = subContainers;
    }

    public Validation add(ResponseContainer... containers) {
        responseContainer.subContainers.addAll(Arrays.asList(containers));
        return this;
    }

    public Validation pass() {
        responseContainer.success = true;
        return this;
    }

    public Validation cancel() {
        responseContainer.cancelled = true;
        return this;
    }

    public Validation flag() {
        return flag(1);
    }

    public Validation flag(double amount) {
        responseContainer.flag = amount;
        return this;
    }

    public Validation punish() {
        responseContainer.punish = true;
        return this;
    }

    public Validation terminate() {
        responseContainer.terminate = true;
        return this;
    }

    public Validation skip(int indices) {
        responseContainer.skipIndices = indices;
        return this;
    }

    public Validation withViolationKey(ViolationService.HoneyKey key) {
        responseContainer.violationKey = key;
        return this;
    }

    public Validation onPunish(Runnable runnable) {
        responseContainer.onPunish = runnable;
        return this;
    }

    public Validation onViolation(Consumer<Runnable> runnable) {
        Runnable priorOnViolation = responseContainer.onViolation;
        responseContainer.onViolation = () -> runnable.accept(priorOnViolation);
        return this;
    }

    public Validation onFlag(Runnable runnable) {
        responseContainer.onFlag = runnable;
        return this;
    }

    public ResponseContainer build() {
        return responseContainer;
    }
}
