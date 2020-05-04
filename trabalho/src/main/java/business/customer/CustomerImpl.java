package business.customer;

import business.order.Order;
import business.order.OrderImpl;

import java.io.Serializable;
import java.util.*;

public class CustomerImpl implements Customer, Serializable {

	private String id;
	private Order currentOrder;
	private Set<Order> oldOrders;

	public CustomerImpl(String id){
		this.id = id;
		this.currentOrder = null;
		this.oldOrders = new HashSet<>();
	}

	public CustomerImpl(String id, Order currentOrder, Set<Order> oldOrders){
		this.id = id;
		this.currentOrder = currentOrder;
		this.oldOrders = oldOrders;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void newCurrentOrder() {
		this.currentOrder = new OrderImpl();
	}

	@Override
	public boolean hasCurrentOrder() {
		return currentOrder != null;
	}

	@Override
	public Order getCurrentOrder() {
		return currentOrder;
	}

	@Override
	public Set<Order> getOldOrders() {
		return oldOrders;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CustomerImpl customer = (CustomerImpl) o;
		return Objects.equals(id, customer.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}