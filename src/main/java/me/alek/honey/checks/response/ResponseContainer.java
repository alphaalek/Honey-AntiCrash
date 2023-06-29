package me.alek.honey.checks.response;

import me.alek.honey.services.ViolationService;

public class ResponseContainer {
    public boolean ACCEPTED;
    public boolean CANCELLED;
    public boolean PUNISH;
    public boolean NOTIFY;
    public boolean SUCCESS;
    public boolean TERMINATE;
    public int SKIP_INDICES;
    public ViolationService.CheckKey CUSTOM_KEY;
    public Runnable ON_VIOLATION;
    public String NOTIFICATION_MESSAGE;
    public int FLAGGED;

    public ResponseBuilder builder() {
        return new ResponseBuilder(this);
    }
}
