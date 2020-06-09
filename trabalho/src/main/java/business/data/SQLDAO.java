package business.data;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class SQLDAO<K, T> implements DAO<K, T> {
    private DAOPS<K, T> ps;
    private Connection connection;

    public SQLDAO(Connection connection, DAOPS<K, T> ps) throws SQLException {
        this.ps = ps;
        this.connection = connection;
    }

    @Override
    public T get(K key) {
        try {
            ResultSet rs = ps.get(key).executeQuery();
            if(!rs.next()) return null;
            return ps.fromResultSet(rs);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean put(T obj) {
        try {
            return ps.put(obj).executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean delete(K key) {
        try {
            return ps.delete(key).executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean update(K key, T obj) {
        try {
            return ps.update(key, obj).executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Map<K, T> getAll() {
        try {
            Map<K, T> map = new HashMap<>();
            ResultSet rs = ps.getAll().executeQuery();
            while (rs.next()) {
                map.put(ps.getKey(rs), ps.fromResultSet(rs));
            }
            return map;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
