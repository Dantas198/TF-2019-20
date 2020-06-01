package business.customer;

import business.data.customer.CustomerDAO;
import business.data.customer.CustomerSQLDAO;
import business.data.order.OrderDAO;
import business.data.order.OrderSQLDAO;
import business.order.Order;
import business.order.OrderImpl;

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
        this.customerDAO = new CustomerSQLDAO(c, orderDAO);
    }

    // Implementa este método em modo eager para manter a base de dados consistente
    @Override
    public void newCurrentOrder() {
        this.removeCurrentOrder();
        super.newCurrentOrder();
        // Transaction ??
        this.orderDAO.put(this.getCurrentOrder());
        customerDAO.update(this.getId(), this);
        System.out.println(this.getId());
        System.out.println(this);
    }

    @Override
    public void deleteCurrentOrder() {
        super.deleteCurrentOrder();
        customerDAO.update(this.getId(), this);
        this.removeCurrentOrder();
        System.out.println("Tem order?? : " + this.hasCurrentOrder());
        System.out.println("Order : " + this.getId() + " " + this.getCurrentOrder());
    }

    private void removeCurrentOrder() {
        if(this.hasCurrentOrder()) {
            this.orderDAO.getAll().values().forEach(order -> System.out.println(order.getId()));
            System.out.println(this.getCurrentOrder().getId() + " " + this.hasCurrentOrder());
            System.out.println("Apagou linha? " + this.orderDAO.delete(this.getCurrentOrder().getId()));
        }
    }
}
