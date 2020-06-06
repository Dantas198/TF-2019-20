package client.message.bodies;

import business.order.Order;

import java.io.Serializable;

public class FinishOrderBody implements Serializable {
    private String customer;
    private Order order;

    public FinishOrderBody(String customer, Order order){
        this.customer = customer;
        this.order = order;
    }

    public String getCustomer() {
        return customer;
    }

    public Order getOrder() {
        return order;
    }
}
