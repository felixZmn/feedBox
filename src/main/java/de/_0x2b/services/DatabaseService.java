package de._0x2b.services;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);
    private final HikariDataSource dataSource;

    // Constructor initiates connection pool immediately
    public DatabaseService(String host, int port, String dbName, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format("jdbc:postgresql://%s:%d/%s", host, port, dbName));
        config.setUsername(username);
        config.setPassword(password);

        config.setInitializationFailTimeout(10000);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setPoolName("FeedBoxPool");

        this.dataSource = new HikariDataSource(config);
        logger.info("Database pool initialized connected to {}", host);
    }

    /**
     * Get a connection from the pool
     * @return
     * @throws SQLException
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Get the DataSource
     * @return
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Close the DataSource pool
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database pool closed");
        }
    }

    /**
     * Perform database schema migration
     * @throws SQLException
     */
    public void migrate() throws SQLException {
        logger.info("Checking database schema...");

        // Split SQL to ensure we can identify which statement fails and wrap in transaction for safety.
        String[] migrationStatements = { """
                CREATE TABLE IF NOT EXISTS folder (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(255) UNIQUE NOT NULL,
                    color VARCHAR(255) DEFAULT 'f-base'
                )
                """, """
                CREATE TABLE IF NOT EXISTS feed (
                    id SERIAL PRIMARY KEY,
                    folder_id INT NOT NULL,
                    name VARCHAR(255) NOT NULL,
                    url VARCHAR(2048) NOT NULL,
                    feed_url VARCHAR(2048) NOT NULL UNIQUE,
                    FOREIGN KEY (folder_id) REFERENCES folder(id) ON DELETE CASCADE
                )
                """, """
                CREATE TABLE IF NOT EXISTS icon (
                    id SERIAL PRIMARY KEY,
                    feed_id integer,
                    image BYTEA NOT NULL,
                    mime_type VARCHAR(255) NOT NULL,
                    file_name VARCHAR(255) NOT NULL,
                    FOREIGN KEY (feed_id) REFERENCES feed(id) ON DELETE SET NULL
                )
                """, """
                CREATE TABLE IF NOT EXISTS article (
                    id SERIAL PRIMARY KEY,
                    feed_id INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    content TEXT NOT NULL,
                    link TEXT NULL unique,
                    published VARCHAR(255) NULL,
                    authors TEXT NOT NULL,
                    image_url TEXT NULL,
                    categories TEXT NULL,
                    FOREIGN KEY (feed_id) REFERENCES feed(id) ON DELETE CASCADE
                )
                """, "INSERT INTO folder (id, name) VALUES (0,'root') ON CONFLICT DO NOTHING" };

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); // Start Transaction
            try (Statement stmt = conn.createStatement()) {
                for (String sql : migrationStatements) {
                    stmt.execute(sql);
                }
                conn.commit(); // Commit Transaction
                logger.info("Schema migration successful/verified");
            } catch (SQLException e) {
                conn.rollback(); // Rollback if anything fails
                logger.error("Migration failed, rolling back", e);
                throw e;
            }
        }
    }
}
