package client.bodies;

import business.customer.Customer;
import business.order.Order;

import java.io.Serializable;

public class FinishOrderBody implements Serializable {
    private String customer;
    private Order order;

    public FinishOrderBody(String customer, Order order){
        this.customer = customer;
        this.order = order;
    }
}
