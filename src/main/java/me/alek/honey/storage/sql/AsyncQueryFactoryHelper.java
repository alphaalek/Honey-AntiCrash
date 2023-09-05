package me.alek.honey.storage.sql;

import me.alek.honey.storage.sql.exceptions.SQLFactoryException;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

public class AsyncQueryFactoryHelper implements QueryFactory {

    private Connection connection;
    private final ReentrantLock databaseQueryLock = new ReentrantLock();

    public AsyncQueryFactoryHelper(Connection connection) {
        this.connection = connection;
    }

    public void checkConnection() {
        if (isConnectionClosed()) throw new IllegalArgumentException();
    }

    public <T> T sync(Callable<T> callable) {
        T value = null;

        databaseQueryLock.lock();
        try {
            value = callable.call();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {
            databaseQueryLock.unlock();
        }
        return value;
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
        return sync(() -> CompletableFuture.supplyAsync(() -> {
            checkConnection();
            try {
                PreparedStatement statement = this.connection.prepareStatement(query);

                int i = 1;
                for (Object value : values) {
                    statement.setObject(i++, value);
                }
                return statement;

            } catch (Exception ex) {
                Bukkit.getLogger().severe("SQL ERROR: " + query + " " + Arrays.toString(values));
                ex.printStackTrace();
            }
            throw new SQLFactoryException();
        }));
    }

    public CompletableFuture<ResultSet> runQuery(String query, Object... values) {
        return runStatement(query, values).thenApply((statement) -> {
            try {
                return statement.executeQuery();

            } catch (Exception ex) {
                Bukkit.getLogger().severe("SQL ERROR: " + query + " " + Arrays.toString(values));
                ex.printStackTrace();
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
                Bukkit.getLogger().severe("SQL ERROR: " + query + " " + Arrays.toString(values));
                ex.printStackTrace();
            }
            throw new SQLFactoryException();
        });
    }
}
