package me.alek.honey.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.CompletableFuture;

public interface QueryFactory {

    boolean isConnectionEstablished();

    Connection getConnection();

    CompletableFuture<PreparedStatement> runStatement(String query, Object... values);

    CompletableFuture<ResultSet> runQuery(String query, Object... values);

    CompletableFuture<Boolean> runInsert(String query, Object... values);
}
