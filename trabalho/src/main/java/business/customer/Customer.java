package business.customer;

import business.order.Order;

import java.io.Serializable;
import java.util.Set;

public interface Customer extends Serializable {

	String getId();

	void newCurrentOrder();

	boolean hasCurrentOrder();

	Order getCurrentOrder();

	void deleteCurrentOrder();

	Set<Order> getOldOrders();
}