package me.alek.honey.checks.response;

import me.alek.honey.services.ViolationService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ResponseContainer implements Cloneable {

    public List<ResponseContainer> SUB_CONTAINERS = new ArrayList<>();
    public boolean CANCELLED;
    public boolean PUNISH;
    public boolean IGNORE_SPIKE;
    public boolean NOTIFY;
    public boolean SUCCESS;
    public boolean TERMINATE;
    public AtomicInteger SKIP_INDICES;
    public ViolationService.CheckKey LIMITS_KEY;
    public ViolationService.CheckKey VIOLATION_KEY;
    public Runnable ON_VIOLATION;
    public Runnable ON_FLAG;
    public Runnable ON_PUNISH;
    public double FLAG = -1;

    public ResponseBuilder builderCopy() {
        return new ResponseBuilder(this.clone());
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
