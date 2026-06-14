package de._0x2b.repository;

import de._0x2b.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import jakarta.inject.Inject;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractRepository<T> {
    private static final Logger logger = LoggerFactory.getLogger(AbstractRepository.class);

    @Inject
    DataSource dataSource;

    public AbstractRepository() {
    }

    // Generic select method
    protected List<T> query(String sql, RowMapper<T> mapper, List<Object> params) {
        logger.debug("Executing Query: {}", sql);

        try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

            setParameters(stmt, params);

            try (ResultSet rs = stmt.executeQuery()) {
                List<T> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapper.map(rs));
                }
                return results;
            }
        } catch (SQLException e) {
            // Surface real database failures instead of silently turning them
            // into "no data" - the previous behaviour was impossible to debug
            // and indistinguishable from an empty table.
            throw new DataAccessException("Query failed: " + sql, e);
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

        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            setParameters(stmt, params);

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
     * @return The number of affected rows.
     * @throws SQLException This is thrown so the caller can handle specific errors
     *                      (like duplicates). Unwrapped so callers can map
     *                      specific SQLState codes (e.g. 23505) to domain
     *                      exceptions.
     */
    protected int update(String sql, List<Object> params) throws SQLException {
        logger.debug("Executing SQL: {}", sql);

        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            setParameters(stmt, params);
            return stmt.executeUpdate();
        }
    }

    /**
     * Bind parameters to a {@link PreparedStatement}.
     * <p>
     * This is deliberately not a thin wrapper around {@code setObject}:
     * the PostgreSQL JDBC driver refuses to infer the SQL type for
     * {@link Instant} (and a few other {@code java.time} types) and
     * throws "Can't infer the SQL type to use for an instance of
     * java.time.Instant". We translate those to the explicit
     * {@code setTimestamp} call with the right SQL type.
     * <p>
     * Any other type falls through to {@code setObject} unchanged.
     */
    private void setParameters(PreparedStatement stmt, List<Object> params) throws SQLException {
        if (params == null)
            return;
        for (int i = 0; i < params.size(); i++) {
            Object value = params.get(i);
            int idx = i + 1;
            if (value instanceof Instant instant) {
                // The column is TIMESTAMP WITH TIME ZONE; bind as such.
                stmt.setTimestamp(idx, Timestamp.from(instant));
            } else {
                stmt.setObject(idx, value);
            }
        }
    }

    @FunctionalInterface
    protected interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }
}
