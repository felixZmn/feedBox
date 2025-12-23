package de._0x2b.repositories;

import de._0x2b.database.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractRepository<T> {
    private static final Logger logger = LoggerFactory.getLogger(AbstractRepository.class);

    // Generic select method
    protected List<T> query(String sql, RowMapper<T> mapper, List<Object> params) {
        logger.debug("Executing Query: {}", sql);

        try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

            setParameters(stmt, params);

            try (ResultSet rs = stmt.executeQuery()) {
                List<T> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapper.map(rs));
                }
                return results;
            }
        } catch (SQLException e) {
            logger.error("Query failed: {}", sql, e);
            return List.of();
        }
    }

    /**
     * Abstract insert method
     *
     * @param sql    The sql statement to execute
     * @param params The parameters for the PreparedStatement
     * @return The new ID (if returnGeneratedKeys is true) OR the row count.
     * @throws SQLException This is thrown so the caller can handle specific errors
     *                      (like duplicates).
     */
    protected int insert(String sql, List<Object> params) throws SQLException {
        logger.debug("Executing SQL: {}", sql);

        try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (params != null) {
                for (int i = 0; i < params.size(); i++) {
                    stmt.setObject(i + 1, params.get(i));
                }
            }

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Insert failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Insert failed, no ID obtained.");
                }
            }
        }
    }

    /**
     * Abstract update/delete method
     *
     * @param sql    The sql statement to execute
     * @param params The parameters for the PreparedStatement
     * @return The new ID (if returnGeneratedKeys is true) OR the row count.
     * @throws SQLException This is thrown so the caller can handle specific errors
     *                      (like duplicates).
     */
    protected int update(String sql, List<Object> params) throws SQLException {
        logger.debug("Executing SQL: {}", sql);

        try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (params != null) {
                for (int i = 0; i < params.size(); i++) {
                    stmt.setObject(i + 1, params.get(i));
                }
            }
            return stmt.executeUpdate();
        }
    }

    private void setParameters(PreparedStatement stmt, List<Object> params) throws SQLException {
        if (params == null) return;
        for (int i = 0; i < params.size(); i++) {
            stmt.setObject(i + 1, params.get(i));
        }
    }

    @FunctionalInterface
    protected interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }
}