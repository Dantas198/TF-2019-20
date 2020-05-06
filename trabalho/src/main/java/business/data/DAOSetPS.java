package business.data;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface DAOSetPS<T> {
    /**
     * Converts a resultSet row to an object
     * @param resultSet
     * @return Object from result
     * @throws SQLException
     */
    T fromResultSet(ResultSet resultSet) throws SQLException;
    PreparedStatement getAll() throws SQLException;
    PreparedStatement size() throws SQLException;
    PreparedStatement add(T o) throws SQLException;
    PreparedStatement remove(Object o) throws SQLException;
    PreparedStatement clear() throws SQLException;
    PreparedStatement empty() throws SQLException;
    PreparedStatement contains(Object o) throws SQLException;
}
