package main;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class ReadReplicaDataSource {
    private static final int DB_CONNECTION_POOL_SIZE_PER_INSTANCE = 10;
    private static HikariConfig config = new HikariConfig();
    private static HikariDataSource dataSource;

    static {
        String dbName = System.getenv("READ_REPLICA_DB_NAME");
        String userName = System.getenv("READ_REPLICA_USERNAME");
        String password = System.getenv("READ_REPLICA_PASSWORD");
        String hostname = System.getenv("READ_REPLICA_HOSTNAME");;
        String port = System.getenv("READ_REPLICA_PORT");
        String jdbcUrl = "jdbc:mysql://" + hostname + ":" + port + "/" + dbName + "?user=" + userName + "&password=" + password;
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(userName);
        config.setPassword(password);
        config.setIdleTimeout(5000);
        config.setMaximumPoolSize(DB_CONNECTION_POOL_SIZE_PER_INSTANCE);
        config.setConnectionTimeout(10000);
        dataSource = new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
