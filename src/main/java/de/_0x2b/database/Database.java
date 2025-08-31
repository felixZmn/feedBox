package de._0x2b.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class Database {
    private static HikariDataSource dataSource;

    public static void connect() {
        if (dataSource != null)
            return; // already connected/started

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://postgres:5432/postgres");
        config.setUsername("user");
        config.setPassword("password");
        config.setMaximumPoolSize(10); // adjust as needed
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);

        dataSource = new HikariDataSource(config);

        System.out.println("Database pool initialized!"); // ToDo: Logger
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null)
            throw new IllegalStateException("Call Database.connect() first!");
        return dataSource.getConnection(); // this gets a pooled connection. Always close it after use!
    }

    public static void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("Database pool closed"); // ToDo: Logger
        }
    }

    public static void migrate() {
        var foo = """
                CREATE TABLE IF NOT EXISTS folder (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(255) UNIQUE NOT NULL,
                    color VARCHAR(255) DEFAULT 'f-base'
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
                PreparedStatement stmt = conn.prepareStatement(foo)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
}
