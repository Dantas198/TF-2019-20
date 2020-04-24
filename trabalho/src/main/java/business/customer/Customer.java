package business.customer;

import business.order.Order;

public interface Customer {

	String getId();

	void newCurrentOrder();

	boolean hasCurrentOrder();

	Order getCurrentOrder();

}