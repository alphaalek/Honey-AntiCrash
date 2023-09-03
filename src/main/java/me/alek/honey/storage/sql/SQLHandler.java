package me.alek.honey.storage.sql;

import me.alek.honey.configuration.ConfigurationOptions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

public class SQLHandler implements QueryFactory {

    private boolean isEnabled;
    private int port;
    private String host;
    private String username;
    private String password;
    private String database;

    private Connection connection;

    private final ReentrantLock databaseQueryLock = new ReentrantLock();
    private final BeanRepositoryCollection repositoryCollection;

    public SQLHandler(BeanRepositoryCollection repositoryCollection) {
        reload();

        this.repositoryCollection = repositoryCollection;
        if (isEnabled) {

            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (Exception ex) {
                ex.printStackTrace();

                Bukkit.getLogger().severe("No Driver found!");
                return;
            }

            try {
                final String url = "jdbc:mysql://" + host + ":" + port + "/" + database;

                this.connection = DriverManager.getConnection(url, username, password);
                Bukkit.getLogger().info("Successfully connected to database");
                Bukkit.getLogger().info(host + ":" + port + "/" + database);

                // prepare repositories
                for (RepositoryComponent repository : repositoryCollection) {
                    repository.prepare(this);
                }

            } catch (Exception ex) {
                ex.printStackTrace();

                Bukkit.getLogger().severe("Error occurred when making connection to database!");
                Bukkit.getLogger().severe(host + ":" + port + "/" + database + ", username: " + username + ", password: " + password);
            }
        }
    }

    public boolean isEnabled() {
        return this.isEnabled;
    }

    public void  preparePlayer(Player player) {
        for (RepositoryComponent repositoryComponent : repositoryCollection) {
            repositoryComponent.preparePlayer(this, player);
        }
    }

    public Connection getConnection() {
        return this.connection;
    }

    public boolean isConnectionEstablished() {
        try {
            return this.connection != null && !this.connection.isClosed();
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public void checkConnection() {
        if (!isConnectionEstablished()) throw new IllegalArgumentException();
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

    public void reload() {
        this.isEnabled = ConfigurationOptions.General.SQL.ENABLED;
        this.port = ConfigurationOptions.General.SQL.PORT;
        this.host = ConfigurationOptions.General.SQL.HOST;
        this.username = ConfigurationOptions.General.SQL.USERNAME;
        this.password = ConfigurationOptions.General.SQL.PASSWORD;
        this.database = ConfigurationOptions.General.SQL.DATBASE;
    }

    public CompletableFuture<PreparedStatement> runStatement(String query, Object... values) {
        return sync(() -> CompletableFuture.supplyAsync(() -> {
            try {
                PreparedStatement statement = this.connection.prepareStatement(query);

                int i = 1;
                for (Object value : values) {
                    statement.setObject(i++, value);
                }
                return statement;

            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return null;
        }));
    }

    public CompletableFuture<ResultSet> runQuery(String query, Object... values) {
        return runStatement(query, values).thenApply((statement) -> {
            checkConnection();

            try {
                if (statement == null) return null;

                return statement.executeQuery();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return null;
        });
    }

    public CompletableFuture<Boolean> runInsert(String query, Object... values) {
        return runStatement(query, values).thenApply((statement) -> {
            checkConnection();

            try {
                if (statement == null) return false;

                int rows = statement.executeUpdate();
                return rows > 0;

            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return false;
        });
    }
}
