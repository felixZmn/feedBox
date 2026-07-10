package de._0x2b.repository;

import de._0x2b.exception.DuplicateEntityException;
import de._0x2b.model.Folder;
import jakarta.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

@ApplicationScoped
public class FolderRepository extends AbstractRepository<Folder> {

    private static final Logger logger = LoggerFactory.getLogger(FolderRepository.class);
    private static final String SELECT_ALL = """
            SELECT id, name, color FROM folder ORDER BY name
            """;
    private static final String SELECT_BY_NAME = """
            SELECT id, name, color FROM folder WHERE name = ? ORDER BY name
            """;
    private static final String INSERT_ONE = """
            INSERT INTO folder (name, color) VALUES (?, ?) RETURNING id
            """;
    private static final String UPDATE = """
            UPDATE folder set name = ?, color = ? WHERE id = ?
            """;
    private static final String DELETE = """
            DELETE FROM folder WHERE id = ?
            """;

    private final RowMapper<Folder> folderMapper = rs -> new Folder(
            rs.getInt("id"),
            rs.getString("name"),
            null,
            rs.getString("color"));

    public FolderRepository() {
    }

    public List<Folder> findAll() {
        logger.debug("findAll");
        return super.query(SELECT_ALL, folderMapper, List.of());
    }

    public List<Folder> findByName(String name) {
        logger.debug("findByName");
        return super.query(SELECT_BY_NAME, folderMapper, List.of(name));
    }

    public int create(Folder folder) {
        try {
            List<Object> params = List.of(folder.getName(), folder.getColor());

            return super.insert(INSERT_ONE, params);

        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                throw new DuplicateEntityException("Folder with this Name already exists");
            }
            logger.error("Error creating folder", e);
            return -1;
        }
    }

    public int update(Folder folder) {
        try {
            List<Object> params = List.of(folder.getName(), folder.getColor(), folder.getId());

            int rows = super.update(UPDATE, params);
            return (rows > 0) ? folder.getId() : -1;
        } catch (SQLException e) {
            logger.error("Error updating folder", e);
            return -1;
        }
    }

    public int delete(int id) throws SQLException {
        return super.update(DELETE, List.of(id));
    }
}
