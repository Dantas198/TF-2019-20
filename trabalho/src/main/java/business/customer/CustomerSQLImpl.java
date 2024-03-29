package business.customer;

import business.data.customer.CustomerDAO;
import business.data.customer.CustomerSQLDAO;
import business.data.order.OrderDAO;
import business.data.order.OrderSQLDAO;
import business.order.Order;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Set;

public class CustomerSQLImpl extends CustomerImpl {

    private CustomerSQLDAO customerDAO;
    private OrderSQLDAO orderDAO;

    public CustomerSQLImpl(String id, Order currentOrder, Set<Order> oldOrder, OrderSQLDAO orderDAO, Connection c) throws SQLException {
        super(id, currentOrder, oldOrder);
        this.orderDAO = orderDAO;
        this.customerDAO = new CustomerSQLDAO(c);
    }

    @Override
    public Order getCurrentOrder() {
        if(!super.hasCurrentOrder()) return null;
        return orderDAO.get(super.getCurrentOrder().getId());
    }

    // Implementa este método em modo eager para manter a base de dados consistente
    @Override
    public void newCurrentOrder() {
        // Transaction ??
        super.newCurrentOrder();
        this.getOldOrders().add(super.getCurrentOrder());
        customerDAO.update(this.getId(), this);
        System.out.println(this.getId());
        System.out.println(this);
    }

    @Override
    public void deleteCurrentOrder() {
        super.deleteCurrentOrder();
        customerDAO.update(this.getId(), this);
    }
}
