package me.alek.honey.storage.sql;

import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.storage.sql.exceptions.SQLFactoryException;
import me.alek.honey.storage.sql.exceptions.SQLPlayerNotFoundException;
import me.alek.honey.storage.sql.model.ViolationEntry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SQLManagerWrapper implements QueryFactory, ComponentBridge {

    private boolean isEnabled;
    private int port;
    private String host;
    private String username;
    private String password;
    private String database;

    private Connection connection;

    private final BeanRepositoryCollection repositoryCollection;
    private final ComponentBridge componentBridge;
    private final QueryFactory queryFactory;

    public SQLManagerWrapper(BeanRepositoryCollection repositoryCollection) {
        this.repositoryCollection = repositoryCollection;
        this.componentBridge = new SimpleComponentBridge(repositoryCollection, this);

        reload();

        tryConnect();
        this.queryFactory = new AsyncQueryFactoryHelper(connection);
    }

    public void tryConnect() {
        if (!isEnabled) return;

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

    public BeanRepositoryCollection getRepositoryCollection() {
        return this.repositoryCollection;
    }

    public void reload() {
        this.componentBridge.reload();

        this.isEnabled = ConfigurationOptions.General.SQL.ENABLED;

        this.port = ConfigurationOptions.General.SQL.PORT;
        this.host = ConfigurationOptions.General.SQL.HOST;
        this.username = ConfigurationOptions.General.SQL.USERNAME;
        this.password = ConfigurationOptions.General.SQL.PASSWORD;
        this.database = ConfigurationOptions.General.SQL.DATBASE;

        // try connecting again if there isn't any connection and mysql is enabled
        if (this.isEnabled && queryFactory.isConnectionClosed()) {
            tryConnect();
            queryFactory.setConnection(this.connection);
        }
    }

    public boolean isEnabled() {
        return this.isEnabled;
    }

    public void preparePlayer(Player player) throws SQLPlayerNotFoundException, SQLFactoryException {
        componentBridge.preparePlayer(player);
    }

    @Override
    public List<ViolationEntry> getViolations(String playerName) throws SQLPlayerNotFoundException, SQLFactoryException {
        return componentBridge.getViolations(playerName);
    }

    @Override
    public List<ViolationEntry> getViolationsOf(String playerName, String violationName) throws SQLPlayerNotFoundException, SQLFactoryException {
        return componentBridge.getViolationsOf(playerName, violationName);
    }

    @Override
    public List<ViolationEntry> getViolationsBefore(String playerName, int unixTime) throws SQLPlayerNotFoundException, SQLFactoryException {
        return componentBridge.getViolationsBefore(playerName, unixTime);
    }

    public void postViolation(Player player, String violationName, ConfigurationOptions.VlSetting setting, CheckContextContainer.DevelopmentData developmentData) throws SQLPlayerNotFoundException, SQLFactoryException {
        componentBridge.postViolation(player, violationName, setting, developmentData);
    }

    public int getPlayerId(String playerName) throws SQLPlayerNotFoundException, SQLFactoryException {
        return componentBridge.getPlayerId(playerName);
    }

    public int getPlayerId(Player player) throws SQLPlayerNotFoundException,SQLFactoryException {
        return componentBridge.getPlayerId(player);
    }

    @Override
    public UUID getPlayerUUID(String playerName) throws SQLPlayerNotFoundException, SQLFactoryException {
        return componentBridge.getPlayerUUID(playerName);
    }

    public Connection getConnection() {
        return queryFactory.getConnection();
    }

    public void setConnection(Connection connection) {
        queryFactory.setConnection(connection);
    }

    public boolean isConnectionClosed() {
        return queryFactory.isConnectionClosed();
    }

    public CompletableFuture<PreparedStatement> runStatement(String query, Object... values) {
        return queryFactory.runStatement(query, values);
    }

    public CompletableFuture<ResultSet> runQuery(String query, Object... values) {
        return queryFactory.runQuery(query, values);
    }

    public CompletableFuture<Boolean> runInsert(String query, Object... values) {
        return queryFactory.runInsert(query, values);
    }
}
