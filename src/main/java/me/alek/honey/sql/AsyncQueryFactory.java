package me.alek.honey.sql;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.sql.exceptions.SQLFactoryException;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class AsyncQueryFactory implements QueryFactory {

    private Connection connection;

    public AsyncQueryFactory(Connection connection) {
        this.connection = connection;
    }

    public void checkConnection() {
        if (isConnectionClosed()) throw new IllegalArgumentException();
    }

    public Connection getConnection() {
        return this.connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public boolean isConnectionClosed() {
        try {
            return this.connection == null || this.connection.isClosed();
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public CompletableFuture<PreparedStatement> runStatement(String query, Object... values) {
        return CompletableFuture.supplyAsync(() -> {
            checkConnection();
            try {
                PreparedStatement statement = this.connection.prepareStatement(query);

                int i = 1;
                for (Object value : values) {
                    statement.setObject(i++, value);
                }
                return statement;

            } catch (Exception ex) {
                Bukkit.getLogger().log(Level.SEVERE, "SQL ERROR: (STATEMENT) " + query + " " + Arrays.toString(values), ex);
            }
            throw new SQLFactoryException();
        });
    }

    public CompletableFuture<ResultSet> runQuery(String query, Object... values) {
        return runStatement(query, values).thenApply((statement) -> {
            try {
                return statement.executeQuery();

            } catch (Exception ex) {
                Bukkit.getLogger().log(Level.SEVERE, "SQL ERROR: (QUERY) " + query + " " + Arrays.toString(values), ex);
            }
            throw new SQLFactoryException();
        });
    }

    public CompletableFuture<Boolean> runInsert(String query, Object... values) {
        return runStatement(query, values).thenApply((statement) -> {
            try {
                int rows = statement.executeUpdate();

                return rows > 0;
            } catch (Exception ex) {
                Bukkit.getLogger().log(Level.SEVERE, "SQL ERROR: (INSERT) " + query + " " + Arrays.toString(values), ex);
            }
            throw new SQLFactoryException();
        });
    }
}
