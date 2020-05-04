package business.customer;

import business.order.Order;

import java.util.List;
import java.util.Set;

public class CustomerStubAtomix implements Customer {
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
    public Set<Order> getOldOrders() {
        return null;
    }
}