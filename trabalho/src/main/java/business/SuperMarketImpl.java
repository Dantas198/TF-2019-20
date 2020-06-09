package business;

import business.customer.Customer;
import business.customer.CustomerImpl;
import business.data.DAO;
import business.data.customer.CustomerSQLDAO;
import business.data.order.OrderSQLDAO;
import business.data.product.ProductSQLDAO;
import business.order.Order;
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
						   DAO<String, Customer> customerDAO)
			throws SQLException {
		this.orderDAO = orderDAO;
		this.productDAO = productDAO;
		this.customerDAO = customerDAO;
	}

	public boolean addCustomer(String customer, StateUpdater<String, Serializable> updater) {
		Customer c = new CustomerImpl(customer);
		updater.put("customer", customer, c);
		return true;
	}

	public boolean resetOrder(String customer) {
		Customer c = customerDAO.get(customer);
		c.newCurrentOrder();
		customerDAO.update(customer, c);
		return true;
	}

	public boolean finishOrder(String customer, StateUpdater<String, Serializable> updater) {
		// TODO tmax
		Customer c = customerDAO.get(customer);
		if(!c.hasCurrentOrder())
			return false;
		Order order = c.getCurrentOrder();
		Map<Product,Integer> products = order.getProducts();

		// TODO trocar order por lista de Strings?
		// verifica se existe stock e substitui keySet por produto completo do stock
		for (Product p : products.keySet()) {
			String name = p.getName();
			int quantity = products.get(p);
			Product product = productDAO.get(name);
			products.put(product, quantity);
			int stock = product.getStock();
			if (!(stock - quantity < 0)) return false;
		}

		// atualiza stock
		for (Product p : products.keySet()) {
			int quantity = products.get(p);
			p.reduceStock(quantity);
			//productDAO.update(p.getName(), p);
			updater.put("product", p.getName(), p);
		}
		c.getOldOrders().add(order); // pode ser removido
		c.newCurrentOrder();
		updater.put("customer", c.getId(), c);
		//customerDAO.update(customer, c);
		return true;
	}

	public boolean addProduct(String customerName, String product, int amount) {
		System.out.println("addProduct");
		Customer customer = customerDAO.get(customerName);
		if(customer == null) return false;
		if (!customer.hasCurrentOrder()) {
			/* customerDAO retorna uma instância de CustomerSQLImpl
			// CustomerSQLImpl subrepõe o método newCurrentOrder
			// e ao fazer newCurrentOrder ele remove a current order antiga da tabela Order
			// e adiciona a current order à tabela e no cliente
			*/
			customer.newCurrentOrder();
		}
		Order order = customer.getCurrentOrder();
		System.out.println(order);
		Product p = productDAO.get(product);
		System.out.println(p.getName());
		order.addProduct(p, amount);
		return true;
	}

	public Map<Product, Integer> getCurrentOrderProducts(String customerName) {
		Customer customer = customerDAO.get(customerName);
		if(customer == null || !customer.hasCurrentOrder()) return null;
		return customer.getCurrentOrder().getProducts();
	}

	public Collection<Order> getHistory(String customerName) {
		Customer customer = customerDAO.get(customerName);
		if(customer == null) return new ArrayList<>(0);
		// TODO: implementar método clone
		return customer.getOldOrders();
	}

	public Collection<Product> getCatalogProducts() {
		return productDAO.getAll().values();
	}
}