package me.alek.honey.checks.response;

import me.alek.honey.services.ViolationService;

import java.util.ArrayList;
import java.util.List;

public class ResponseContainer implements Cloneable {

    public List<ResponseContainer> subContainers = new ArrayList<>();
    public boolean cancelled;
    public boolean punish;
    public boolean success;
    public boolean terminate;
    public int skipIndices;
    public ViolationService.HoneyKey violationKey;
    public Runnable onViolation;
    public Runnable onFlag;
    public Runnable onPunish;
    public double flag = -1;

    public Validation copy() {
        return new Validation(this.clone(), new ArrayList<>(subContainers));
    }

    public ResponseContainer copyDirectly() {
        return copy().build();
    }

    public Validation copyWithoutPosts() {
        Validation builder = copy();
        builder.getContainer().onViolation = null;
        builder.getContainer().onFlag = null;
        builder.getContainer().onPunish = null;
        return builder;
    }

    public Validation builder() {
        return new Validation(this);
    }

    @Override
    public ResponseContainer clone() {
        try {
            return (ResponseContainer) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
