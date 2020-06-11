package business.data.order;

import business.customer.Customer;
import business.data.CertifierDAO;
import business.data.DAO;
import business.data.customer.CustomerSQLDAO;
import business.order.Order;
import middleware.certifier.StateUpdater;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;

public class OrderCertifierDAO extends CertifierDAO<Order> {
    public OrderCertifierDAO(DAO<String, Order> dao, StateUpdater<String, Serializable> updater) throws SQLException {
        super(dao, "customer", updater, Order::getId);
    }
}
