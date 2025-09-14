package util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public final class DB {
    private static HikariDataSource ds;

    private DB() {}

    public static DataSource getDataSource() {
        if (ds == null) {
            synchronized (DB.class) {
                if (ds == null) {
                    HikariConfig cfg = new HikariConfig();
                    cfg.setJdbcUrl("jdbc:mysql://localhost:3006/recallloop?serverTimezone=Europe/Berlin&useSSL=false&allowPublicKeyRetrieval=true");
                    cfg.setUsername("root");
                    cfg.setPassword("root");
                    cfg.setMaximumPoolSize(5);
                    cfg.setMinimumIdle(1);
                    cfg.setPoolName("SRSFX-Pool");
                    ds = new HikariDataSource(cfg);
                }
            }
        }
        return ds;
    }
}