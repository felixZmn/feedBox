package de._0x2b.repositories;

import de._0x2b.exceptions.DuplicateEntityException;
import de._0x2b.models.Icon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class IconRepository extends AbstractRepository<Icon> {
    private static final Logger logger = LoggerFactory.getLogger(IconRepository.class);

    private static final String SELECT_COLS = """
            SELECT id, feed_id, image, mime_type, file_name
            """;
    private static final String FROM = """
            from icon
            """;

    private static final String INSERT_ONE = """
            INSERT INTO icon (feed_id, mime_type, file_name, image) VALUES (?, ?, ?, ?) RETURNING id
            """;

    private final RowMapper<Icon> iconMapper = rs -> new Icon(rs.getInt("id"), rs.getInt("feed_id"), rs.getBytes("image"), rs.getString("mime_type"), rs.getString("file_name"), "");

    public List<Icon> findByFeed(int feedId) {
        return findInternal("feed_id = ?", List.of(feedId));
    }

    public int create(Icon icon) {
        try {
            List<Object> params = List.of(icon.getFeedId(), icon.getMimeType(), icon.getFileName(), icon.getImage());

            return super.insert(INSERT_ONE, params);

        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                throw new DuplicateEntityException("Icon already exists");
            }
            logger.error("Error creating Icon", e);
            return -1;
        }
    }

    private List<Icon> findInternal(String whereClause, List<Object> initialParams) {

        StringBuilder sql = new StringBuilder(SELECT_COLS).append(FROM);
        List<Object> params = new ArrayList<>();

        if (initialParams != null) {
            params.addAll(initialParams);
        }

        // Add where
        sql.append(" WHERE 1=1 ");
        if (whereClause != null) {
            sql.append(" AND ").append(whereClause);
        }

        return super.query(sql.toString(), iconMapper, params);
    }
}
