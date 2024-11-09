package com.zenith.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.postgresql.ds.PGSimpleDataSource;

import java.sql.Connection;
import java.time.Duration;

import static com.zenith.Shared.CONFIG;

public final class ConnectionPool {

    private final HikariDataSource writePool;

    public ConnectionPool() {
        writePool = createDataSource();
    }

    private static HikariDataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        config.setDataSourceClassName(PGSimpleDataSource.class.getName());
        config.addDataSourceProperty("serverName", CONFIG.database.host);
        config.addDataSourceProperty("portNumber", CONFIG.database.port);
        config.addDataSourceProperty("databaseName", "postgres");
        config.setUsername(CONFIG.database.username);
        config.setPassword(CONFIG.database.password);
        config.setMinimumIdle(0);
        config.setMaximumPoolSize(1);
        config.setConnectionTimeout(10000);
        config.addDataSourceProperty("loginTimeout", 60);
        config.addDataSourceProperty("tcpKeepAlive", true);
        config.addDataSourceProperty("socketTimeout", 60);
        config.setKeepaliveTime(Duration.ofMinutes(1).toMillis());
        config.setMaxLifetime(Duration.ofMinutes(20).toMillis());
        return new HikariDataSource(config);
    }

    public Connection getWriteConnection() {
        try {
            return writePool.getConnection();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
