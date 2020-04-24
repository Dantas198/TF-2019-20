package business.customer;

import business.order.Order;

import java.util.List;

public interface Customer {

	String getId();

	void newCurrentOrder();

	boolean hasCurrentOrder();

	Order getCurrentOrder();

	List<Order> getOldOrders();

}