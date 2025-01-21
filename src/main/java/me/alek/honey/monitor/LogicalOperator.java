package me.alek.honey.monitor;

import java.util.HashMap;
import java.util.Map;

public enum LogicalOperator {

    AND('&'),

    OR('|'),

    NOT('!'),

    PARANTHESIS_START('('),

    PARANTHESIS_END(')');

    private final static Map<Character, LogicalOperator> OPERATOR_REGISTRY = new HashMap<>();

    static {
        for (LogicalOperator operator : LogicalOperator.values()) {
            OPERATOR_REGISTRY.put(operator.getSymbol(), operator);
        }
    }

    public static LogicalOperator getBySymbol(char symbol) {
        return OPERATOR_REGISTRY.get(symbol);
    }

    private final char symbol;

    LogicalOperator(char symbol) {
        this.symbol = symbol;
    }

    public char getSymbol() {
        return symbol;
    }
}
