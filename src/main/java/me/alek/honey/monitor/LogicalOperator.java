package me.alek.honey.monitor;

import java.util.HashMap;
import java.util.Map;

public enum LogicalOperator {

    AND("&"),

    OR("|"),

    NOT("!"),

    PARANTHESIS_START("("),

    PARANTHESIS_END(")");

    private final static Map<String, LogicalOperator> OPERATOR_REGISTRY = new HashMap<>();

    static {
        for (LogicalOperator operator : LogicalOperator.values()) {
            OPERATOR_REGISTRY.put(operator.getSymbol(), operator);
        }
    }

    public static LogicalOperator getBySymbol(String symbol) {
        return OPERATOR_REGISTRY.get(symbol);
    }

    private final String symbol;

    LogicalOperator(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
