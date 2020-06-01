package business.customer;

import business.data.order.OrderSQLDAO;
import business.order.Order;

import java.util.Set;

public class CustomerSQLImpl extends CustomerImpl {

    private OrderSQLDAO orderDAO;

    public CustomerSQLImpl(String id, Order currentOrder, Set<Order> oldOrder, OrderSQLDAO orderDAO) {
        super(id, currentOrder, oldOrder);
        this.orderDAO = orderDAO;
    }

    // Implementa este método em modo eager para manter a base de dados consistente
    @Override
    public void newCurrentOrder() {
        if(this.hasCurrentOrder()) {
            this.orderDAO.delete(this.getCurrentOrder().getId());
        }
        super.newCurrentOrder();
        this.orderDAO.put(getCurrentOrder());
    }
}
