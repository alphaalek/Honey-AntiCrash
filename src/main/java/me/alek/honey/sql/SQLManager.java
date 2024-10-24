package me.alek.honey.sql;

import com.google.common.base.Strings;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.sql.exceptions.SQLFactoryException;
import me.alek.honey.sql.exceptions.SQLPlayerNotFoundException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.CompletableFuture;

public class SQLManager implements QueryFactory {

    private boolean isEnabled;
    private int port;
    private String host;
    private String username;
    private String password;
    private String database;

    private Connection connection;

    private final RepositoryCollection repositoryCollection;
    private final ComponentBridge componentBridge;
    private final QueryFactory queryFactory;

    public SQLManager(RepositoryCollection repositoryCollection) {
        this.repositoryCollection = repositoryCollection;
        this.componentBridge = new InternalComponentBridge(repositoryCollection, this);

        reload(true);

        tryConnect();
        this.queryFactory = new AsyncQueryFactory(this.connection);

        // prepare repositories
        prepareRepositories();
    }

    public void prepareRepositories() {
        for (Repository repository : repositoryCollection) {
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

            HoneyPlugin.get().getLoggerService().getLogger().info("Successfully connected to database");
        } catch (Exception ex) {
            ex.printStackTrace();

            HoneyPlugin.get().getLoggerService().getLogger().severe("Error occurred when making connection to database!");
        }
        HoneyPlugin.get().getLoggerService().getLogger().info(url + ", username: " + username + ", password: " + Strings.repeat("*", password.length()));
    }

    public RepositoryCollection getRepositoryCollection() {
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
        this.database = ConfigurationOptions.SQL.DATABASE;

        // try connecting again if there isn't any connection and mysql is enabled
        if (!initiallyReload && this.isEnabled && queryFactory.isConnectionClosed()) {
            tryConnect();
            queryFactory.setConnection(this.connection);

            // prepare repositories
            if (!queryFactory.isConnectionClosed()) {
                prepareRepositories();
            }
        }
    }

    public boolean isEnabled() {
        return this.isEnabled;
    }

    public void preparePlayer(HoneyPlayer player) throws SQLPlayerNotFoundException, SQLFactoryException {
        componentBridge.preparePlayer(player);
    }

    public ComponentBridge getComponentBridge() {
        return componentBridge;
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
