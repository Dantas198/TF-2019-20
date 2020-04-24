package business.customer;

import business.order.Order;
import business.order.OrderImpl;

import java.util.ArrayList;
import java.util.List;

public class CustomerImpl implements Customer {

	private String id;
	private Order currentOrder;
	private List<Order> oldOrders;

	public CustomerImpl(String id){
		this.id = id;
		this.currentOrder = null;
		this.oldOrders = new ArrayList<>();
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
	public List<Order> getOldOrders() {
		return oldOrders;
	}
}