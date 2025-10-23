package de._0x2b.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Database {
    private static final Logger logger = LoggerFactory.getLogger(Database.class);
    private static HikariDataSource dataSource;

    public static void connect(String host, String port, String dbName, String username, String password) {
        logger.debug("connect");
        if (dataSource != null)
            return; // already connected/started

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format("jdbc:postgresql://%s:%s/%s", host, port, dbName));
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);

        dataSource = new HikariDataSource(config);
        logger.info("Database pool initialized!");
    }

    public static Connection getConnection() throws SQLException {
        logger.debug("getConnection");
        if (dataSource == null)
            throw new IllegalStateException("Call Database.connect() first!");
        return dataSource.getConnection(); // this gets a pooled connection. Always close it after use!
    }

    public static void disconnect() {
        logger.debug("disconnect");
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database pool closed");
        }
    }

    public static void migrate() throws SQLException {
        logger.info("Create database schema...");
        var sql = """
                CREATE TABLE IF NOT EXISTS folder (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(255) UNIQUE NOT NULL,
                    color VARCHAR(255) DEFAULT 'f-base'
                );
                
                CREATE TABLE IF NOT EXISTS icon (
                    id SERIAL PRIMARY KEY,
                    feed_id integer,
                    image BYTEA NOT NULL,
                    mime_type VARCHAR(255) NOT NULL,
                    file_name VARCHAR(255) NOT NULL
                );
                
                CREATE TABLE IF NOT EXISTS feed (
                    id SERIAL PRIMARY KEY,
                    folder_id INT NOT NULL,
                    name VARCHAR(255) NOT NULL,
                    url VARCHAR(2048) NOT NULL UNIQUE,
                    feed_url VARCHAR(2048) NOT NULL UNIQUE,
                    FOREIGN KEY (folder_id) REFERENCES folder(id) ON DELETE CASCADE
                );
                
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
                );
                
                INSERT INTO folder (id, name) VALUES (0,'') ON CONFLICT DO NOTHING;
                """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
            logger.info("... schema migration successful");
        }
    }
}
