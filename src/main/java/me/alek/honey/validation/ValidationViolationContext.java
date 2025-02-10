package me.alek.honey.validation;

import me.alek.honey.HoneyKey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ValidationViolationContext implements Cloneable {

    public static ValidationViolationContext PASS = Builder.validate()
            .pass()
            .build();

    public static ValidationViolationContext FLAG = Builder.validate()
            .cancel()
            .flag()
            .build();

    public static ValidationViolationContext TERMINATE = Builder.validate()
            .pass()
            .terminate()
            .build();

    public List<ValidationViolationContext> subContainers = new ArrayList<>();
    public boolean cancelled;
    public boolean success;
    public boolean terminate;
    public int skipIndices;
    public HoneyKey violationKey;
    public Consumer<ViolationKey> onViolation;
    public Runnable onPunish;
    public double flag = -1;

    public Builder copy() {
        return new Builder(this.clone(), new ArrayList<>(subContainers));
    }

    public Builder builder() {
        return new Builder(this);
    }

    @Override
    public ValidationViolationContext clone() {
        try {
            return (ValidationViolationContext) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public static class Builder {

        private final ValidationViolationContext responseContainer;

        public static Builder validate() {
            return new Builder();
        }

        public Builder() {
            this.responseContainer = new ValidationViolationContext();
        }

        public Builder(ValidationViolationContext container) {
            this.responseContainer = container;
        }

        public Builder(ValidationViolationContext container, List<ValidationViolationContext> subContainers) {
            this(container);
            this.responseContainer.subContainers = subContainers;
        }

        public Builder add(ValidationViolationContext... containers) {
            this.responseContainer.subContainers.addAll(Arrays.asList(containers));
            return this;
        }

        public Builder pass() {
            this.responseContainer.success = true;
            return this;
        }

        public Builder cancel() {
            this.responseContainer.cancelled = true;
            return this;
        }

        public Builder flag() {
            return flag(1);
        }

        public Builder flag(double amount) {
            this.responseContainer.flag = amount;
            return this;
        }

        public Builder terminate() {
            this.responseContainer.terminate = true;
            return this;
        }

        public Builder skip(int indices) {
            this.responseContainer.skipIndices = indices;
            return this;
        }

        public Builder withViolationKey(HoneyKey key) {
            this.responseContainer.violationKey = key;
            return this;
        }

        public Builder onPunish(Runnable runnable) {
            this.responseContainer.onPunish = runnable;
            return this;
        }

        public synchronized Builder onViolation(ViolationKey key, BiConsumer<ViolationKey, Runnable> runnable) {
            Consumer<ViolationKey> prior = this.responseContainer.onViolation;
            Consumer<ViolationKey> next = v -> {
                if (prior != null) prior.accept(v);
            };
            this.responseContainer.onViolation = v -> {
                if (v.equals(key)) {
                    runnable.accept(v, () -> next.accept(v));
                } else {
                    next.accept(v);
                }
            };

            return this;
        }

        public ValidationViolationContext build() {
            return this.responseContainer;
        }
    }
}
