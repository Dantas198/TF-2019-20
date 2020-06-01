package business.customer;

import business.order.Order;

import java.util.Set;

public interface Customer {

	String getId();

	void newCurrentOrder();

	boolean hasCurrentOrder();

	Order getCurrentOrder();

	void deleteCurrentOrder();

	Set<Order> getOldOrders();
}