package me.alek.honey.tickloop.postpone;

public class PostponeTerminationException extends Exception {

    public static PostponeTerminationException TERMINATOR = new PostponeTerminationException();

    private PostponeTerminationException() {

    }
}
