import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DataSource {

    private static final int DB_CONNECTION_POOL_SIZE_PER_INSTANCE = 25;
    private static HikariConfig config = new HikariConfig();
    private static HikariDataSource dataSource;

    static {
        String dbName = System.getenv("RDS_DB_NAME");
        String userName = System.getenv("RDS_USERNAME");
        String password = System.getenv("RDS_PASSWORD");
        String hostname = System.getenv("RDS_HOSTNAME");;
        String port = System.getenv("RDS_PORT");
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
