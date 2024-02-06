package me.alek.honey.storage;

import com.google.common.base.Strings;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.storage.exceptions.SQLFactoryException;
import me.alek.honey.storage.exceptions.SQLPlayerNotFoundException;
import me.alek.honey.storage.model.ViolationEntry;
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

        reload(true);

        tryConnect();
        this.queryFactory = new AsyncQueryFactoryHelper(this.connection);

        // prepare repositories
        prepareRepositories();
    }

    public void prepareRepositories() {
        for (RepositoryComponent repository : repositoryCollection) {
            repository.prepare(this);
        }
    }

    public void tryConnect() {
        if (!isEnabled) return;

        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (Exception ex) {
            ex.printStackTrace();

            HoneyPlugin.get().getLoggerService().getLogger().severe("No Driver found!");
            return;
        }

        final String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?characterEncoding=utf8";
        try {
            this.connection = DriverManager.getConnection(url, username, password);

            HoneyPlugin.get().getLoggerService().getLogger().info("[Honey] Successfully connected to database");
            HoneyPlugin.get().getLoggerService().getLogger().info("[Honey] " + url + ", username: " + username + ", password: " + Strings.repeat("*", password.length()));
        } catch (Exception ex) {
            ex.printStackTrace();

            HoneyPlugin.get().getLoggerService().getLogger().severe("[Honey] Error occurred when making connection to database!");
            HoneyPlugin.get().getLoggerService().getLogger().severe("[Honey] " + url + ", username: " + username + ", password: " + Strings.repeat("*", password.length()));
        }
    }

    public BeanRepositoryCollection getRepositoryCollection() {
        return this.repositoryCollection;
    }

    public void reload() {
        reload(false);
    }

    public void reload(boolean initiallyReload) {
        this.componentBridge.reload();

        this.isEnabled = ConfigurationOptions.SQL.ENABLED;

        this.port = ConfigurationOptions.SQL.PORT;
        this.host = ConfigurationOptions.SQL.HOST;
        this.username = ConfigurationOptions.SQL.USERNAME;
        this.password = ConfigurationOptions.SQL.PASSWORD;
        this.database = ConfigurationOptions.SQL.DATBASE;

        // try connecting again if there isn't any connection and mysql is enabled
        if (!initiallyReload && this.isEnabled && queryFactory.isConnectionClosed()) {
            tryConnect();
            queryFactory.setConnection(this.connection);

            // prepare repositories
            if (!queryFactory.isConnectionClosed()) prepareRepositories();
        }
    }

    public boolean isEnabled() {
        return this.isEnabled;
    }

    public void preparePlayer(Player player) throws SQLPlayerNotFoundException, SQLFactoryException {
        componentBridge.preparePlayer(player);
    }

    @Override
    public void updatePlayer(Player player) throws SQLPlayerNotFoundException, SQLFactoryException {
        componentBridge.updatePlayer(player);
    }

    @Override
    public void setupPlayer(Player player) throws SQLFactoryException {
        componentBridge.setupPlayer(player);
    }

    @Override
    public boolean existsPlayer(String playerName) {
        return componentBridge.existsPlayer(playerName);
    }

    @Override
    public boolean containsUUID(String UUID) throws SQLFactoryException {
        return componentBridge.containsUUID(UUID);
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

    public void postViolation(Player player, String violationName, String context, CheckContextContainer.DevelopmentData developmentData) throws SQLPlayerNotFoundException, SQLFactoryException {
        componentBridge.postViolation(player, violationName, context, developmentData);
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
