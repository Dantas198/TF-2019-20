package business.data;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface DAOPS<K, T> {
    /**
     * Converts a resultSet row to an object
     *
     * @param resultSet
     * @return Object from result
     * @throws SQLException
     */
    T fromResultSet(ResultSet resultSet) throws SQLException;

    K getKey(ResultSet resultSet) throws SQLException;

    PreparedStatement get(K key) throws SQLException;

    PreparedStatement put(T o) throws SQLException;

    PreparedStatement delete(K key) throws SQLException;

    PreparedStatement update(K key, T o) throws SQLException;

    PreparedStatement getAll() throws SQLException;
}
