package me.alek.honey.validation.print;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.message.Message;

public class Log4jFilter extends AbstractLifeCycle implements Filter {

    public static int getLength(String msg) {
        return msg.length();
    }

    public static int getLength(Throwable throwable) {
        int length = 0;

        for (StackTraceElement trace : throwable.getStackTrace()) {
            length += trace.toString().length();
        }
        if (throwable.getCause() != null) {
            length += getLength(throwable.getCause());
        }
        if (throwable.getMessage() != null) {
            length += throwable.getMessage().length();
        }

        return length;
    }

    @Override
    public Result getOnMismatch() {
        return Result.DENY;
    }

    @Override
    public Result getOnMatch() {
        return Result.NEUTRAL;
    }

    public Result filter(String msg) {
        return PrintValidationBridge.handle(null, () -> getLength(msg));
    }

    public Result filter(String msg, Throwable thrown) {
        return PrintValidationBridge.handle(thrown, () -> getLength(msg) + getLength(thrown));
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object... params) {
        return filter(msg);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0) {
        return filter(message);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1) {
        return filter(message);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2) {
        return filter(message);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3) {
        return filter(message);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
        return filter(message);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
        return filter(message);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
        return filter(message);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {
        return filter(message);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8) {
        return filter(message);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {
        return filter(message);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t) {
        return filter(msg.toString(), t);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
        return filter(msg.getFormattedMessage(), t);
    }

    @Override
    public Result filter(LogEvent event) {
        if (event.getThrown() != null) {
            return filter(event.getMessage().getFormattedMessage(), event.getThrown());
        } else {
            return filter(event.getMessage().getFormattedMessage());
        }
    }
}
