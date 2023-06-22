package me.alek.honey.checks.response;

import org.bukkit.inventory.ItemStack;

public class ResponseContainer {
    public boolean ACCEPTED;
    public boolean CANCELLED;
    public boolean FLAGGED;
    public boolean FLAG_DEFAULT_CHECK_KEY;
    public boolean PUNISH;
    public boolean CONFISCATE;
    public boolean NOTIFY;
    public boolean SUCCESS;
    public boolean TERMINATE;
    public int SKIP_INDICES;

    public Runnable ON_VIOLATION;
    public ItemStack CONFISCATED_ITEM;
    public String NOTIFICATION_MESSAGE;

    public ResponseBuilder builder() {
        return new ResponseBuilder(this);
    }
}
