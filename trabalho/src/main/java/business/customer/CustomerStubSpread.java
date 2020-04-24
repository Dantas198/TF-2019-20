package business.customer;

import business.order.Order;

import java.util.List;

public class CustomerStubSpread  implements Customer {
    @Override
    public String getId() {
        return null;
    }

    @Override
    public void newCurrentOrder() {

    }

    @Override
    public boolean hasCurrentOrder() {
        return false;
    }

    @Override
    public Order getCurrentOrder() {
        return null;
    }

    @Override
    public List<Order> getOldOrders() {
        return null;
    }
}