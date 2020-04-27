package business.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public abstract class DAOSet<T> implements Set<T> {
    private Connection c;
    private String tableName;

    abstract T fromResultSet(ResultSet resultSet) throws SQLException;
    abstract PreparedStatement getAllPS() throws SQLException;
    abstract PreparedStatement sizePS() throws SQLException;
    abstract PreparedStatement addPS(T o) throws SQLException;
    abstract PreparedStatement removePS(Object o) throws SQLException;
    abstract PreparedStatement clearPS() throws SQLException;
    abstract PreparedStatement emptyPS() throws SQLException;
    abstract PreparedStatement containsPS(Object o) throws SQLException;

    public DAOSet(Connection c) throws SQLException {
        this.c = c;
    }

    protected PreparedStatement prepareStatement(String sql) throws SQLException {
        return c.prepareStatement(sql);
    }

    @Override
    public int size() {
        try {
            ResultSet rs = sizePS().executeQuery();
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public boolean isEmpty() {
        try {
            return !emptyPS().executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } catch (NullPointerException e) {
            return size() == 0;
        }
    }

    @Override
    public Iterator<T> iterator() {
        try {
            return new Iterator<T>() {
                private ResultSet rs = getAllPS().executeQuery();
                private Boolean hasNext = null;

                @Override
                public boolean hasNext() {
                    if(hasNext == null) {
                        try {
                            hasNext = rs.next();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    } else {
                        return hasNext;
                    }
                    return false;
                }

                @Override
                public T next() {
                    if(hasNext == null) {
                        try {
                            rs.next();
                            hasNext = true;
                        } catch (SQLException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                    if(hasNext) {
                        try {
                            return fromResultSet(rs);
                        } catch (SQLException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                    return null;
                }
            };
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public Object[] toArray() {
        List<T> list = new ArrayList<>();
        this.iterator().forEachRemaining(list::add);
        return list.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] t1s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(Object o) {
        try {
            return containsPS(o).executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean add(T order) {
        try {
            return addPS(order).executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean remove(Object o) {
        try {
            return removePS(o).executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends T> collection) {
        boolean result = false;
        for (T object:
             collection) {
            result |= this.add(object);
        }
        return result;
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        boolean changed = false;
        for (Iterator<T> it = this.iterator(); it.hasNext(); ) {
            T object = it.next();
            if(collection.contains(object)) {
                changed |= this.remove(object);
            }
        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        boolean changed = false;
        for (Iterator<T> it = this.iterator(); it.hasNext(); ) {
            T object = it.next();
            if(!collection.contains(object)) {
                changed |= this.remove(object);
            }
        }
        return changed;
    }

    @Override
    public void clear() {
        try {
            clearPS().execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Spliterator<T> spliterator() {
        throw new UnsupportedOperationException();
    }
}
