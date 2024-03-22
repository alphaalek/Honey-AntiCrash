package me.alek.honey.checks.response;

import me.alek.honey.services.ViolationService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ResponseContainer implements Cloneable {

    public List<ResponseContainer> subContainers = new ArrayList<>();
    public boolean cancelled;
    public boolean punish;
    public boolean success;
    public boolean terminate;
    public int skipIndices;
    public ViolationService.HoneyKey limitsKey;
    public ViolationService.HoneyKey violationKey;
    public Runnable onViolation;
    public Runnable onFlag;
    public Runnable onPunish;
    public double flag = -1;

    public ResponseBuilder builderCopy() {
        return new ResponseBuilder(this.clone(), new ArrayList<>(subContainers));
    }

    public ResponseBuilder builder() {
        return new ResponseBuilder(this);
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
