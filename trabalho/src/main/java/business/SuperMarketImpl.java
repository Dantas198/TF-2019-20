package business;

import business.customer.Customer;
import business.customer.CustomerImpl;
import business.customer.CustomerPlaceholder;
import business.data.DAO;
import business.data.customer.CustomerSQLDAO;
import business.data.order.OrderSQLDAO;
import business.data.product.ProductSQLDAO;
import business.order.Order;
import business.product.OrderProductQuantity;
import business.product.Product;
import middleware.certifier.StateUpdater;
import server.CurrentOrderCleaner;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.*;

public class SuperMarketImpl implements Serializable { // Implement SuperMarket

	private DAO<String, Order> orderDAO;
	private DAO<String, Product> productDAO;
	private DAO<String, Customer> customerDAO;
	private CurrentOrderCleaner cleaner;

	public SuperMarketImpl(DAO<String, Order> orderDAO,
						   DAO<String, Product> productDAO,
						   DAO<String, Customer> customerDAO) {
		this.orderDAO = orderDAO;
		this.productDAO = productDAO;
		this.customerDAO = customerDAO;
	}

	public boolean addCustomer(String customer, StateUpdater<String, Serializable> updater) {
		if(customerDAO.get(customer) != null) return false;
		Customer c = new CustomerImpl(customer);
		updater.put("customer", customer, c);
		return true;
	}

	public boolean resetOrder(String customer, StateUpdater<String, Serializable> updater) {
		Customer c = customerDAO.get(customer);
		if(c == null) return false;
		String oldCurrentOrder = c.getCurrentOrder().getId();
		c.deleteCurrentOrder();
		updater.put("customer", customer, c); // Atualiza o cliente por causa da Foreign Key
		updater.put("order", oldCurrentOrder, null); // Apaga ordem atual
		c.newCurrentOrder();
		updater.put("order", c.getCurrentOrder().getId(), c.getCurrentOrder()); // Cria a order
		updater.put("customer", c.getId(), c); // Atualiza o customer com a nova order
		return true;
	}

	public boolean finishOrder(String customer, StateUpdater<String, Serializable> updater) {
		// TODO tmax
		Customer c = customerDAO.get(customer);
		if(!c.hasCurrentOrder())
			return false;
		Order order = orderDAO.get(c.getCurrentOrder().getId());
		Map<Product,Integer> products = order.getProducts();

		// TODO trocar order por lista de Strings?
		// verifica se existe stock e substitui keySet por produto completo do stock
		for (Product p : products.keySet()) {
			String name = p.getName();
			int quantity = products.get(p);
			Product product = productDAO.get(name);
			products.put(product, quantity);
			int stock = product.getStock();
			if (quantity > stock) return false;
		}

		// atualiza stock
		for (Product p : products.keySet()) {
			int quantity = products.get(p);
			p.reduceStock(quantity);
			//productDAO.update(p.getName(), p);
			updater.put("product", p.getName(), p);
		}
		c.deleteCurrentOrder();
		updater.put("customer", c.getId(), new CustomerPlaceholder(c));
		return true;
	}

	public boolean addProduct(String customerName, String product, int amount, StateUpdater<String, Serializable> updater) {
		// TODO usar updater
		System.out.println("addProduct");
		Customer customer = customerDAO.get(customerName);
		if(customer == null) return false;
		if (!customer.hasCurrentOrder()) {
			customer.newCurrentOrder();
			updater.put("order", customer.getCurrentOrder().getId(), customer.getCurrentOrder());
			// A nova order deve vir antes do customer por causa de restrições da Foreign Key
			updater.put("customer", customerName, new CustomerPlaceholder(customer));
		}
		Order order = customer.getCurrentOrder();
		System.out.println(order);
		Product p = productDAO.get(product);
		System.out.println(p.getName());
		updater.put("order_product",
				order.getId() + ";" + p.getName(),
				new OrderProductQuantity(order.getId(), p.getName(), amount));
		return true;
	}

	public Map<Product, Integer> getCurrentOrderProducts(String customerName) {
		Customer customer = customerDAO.get(customerName);
		if(customer == null || !customer.hasCurrentOrder()) return null;
		Order currentOrder = orderDAO.get(customer.getCurrentOrder().getId());
		if(currentOrder == null) return null;
		return currentOrder.getProducts();
	}

	public Collection<Order> getHistory(String customerName) {
		Customer customer = customerDAO.get(customerName);
		if(customer == null) return new ArrayList<>(0);
		return customer.getOldOrders();
	}

	public Collection<Product> getCatalogProducts() {
		return productDAO.getAll().values();
	}
}