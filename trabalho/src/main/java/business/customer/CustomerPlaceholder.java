package business.customer;

import business.order.Order;

import java.util.Set;

public class CustomerPlaceholder implements Customer {
    private String id;
    private Order currentOrder;


    public CustomerPlaceholder(String id, Order currentOrder) {
        this.id = id;
        this.currentOrder = currentOrder;
    }

    public CustomerPlaceholder(Customer customer) {
        this.id = customer.getId();
        this.currentOrder = customer.getCurrentOrder();
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void newCurrentOrder() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasCurrentOrder() {
        return this.currentOrder != null;
    }

    @Override
    public Order getCurrentOrder() {
        return currentOrder;
    }

    @Override
    public void deleteCurrentOrder() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Order> getOldOrders() {
        throw new UnsupportedOperationException();
    }
}
