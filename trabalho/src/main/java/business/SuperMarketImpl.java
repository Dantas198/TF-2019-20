package business;

import business.customer.Customer;
import business.customer.CustomerImpl;
import business.customer.CustomerPlaceholder;
import business.data.DAO;
import business.order.Order;
import business.product.OrderProductQuantity;
import business.product.Product;
import business.product.ProductImpl;
import middleware.certifier.StateUpdater;
import server.CurrentOrderCleaner;

import java.io.Serializable;
import java.util.*;

public class SuperMarketImpl implements SuperMarket, Serializable {

	private DAO<String, Order> orderDAO;
	private DAO<String, Product> productDAO;
	private DAO<String, Customer> customerDAO;
	private StateUpdater<String, Serializable> updater;

	public SuperMarketImpl(DAO<String, Order> orderDAO,
						   DAO<String, Product> productDAO,
						   DAO<String, Customer> customerDAO,
						   StateUpdater<String, Serializable> updater) {
		this.orderDAO = orderDAO;
		this.productDAO = productDAO;
		this.customerDAO = customerDAO;
		this.updater = updater;
	}

	public boolean addCustomer(String customer) {
		if(customerDAO.get(customer) != null) return false;
		Customer c = new CustomerImpl(customer);
		customerDAO.put(c);
		return true;
	}

	public boolean resetOrder(String customer) {
		Customer c = customerDAO.get(customer);
		if(c == null || !c.hasCurrentOrder())
			return false;
		String oldCurrentOrder = c.getCurrentOrder().getId();
		c.deleteCurrentOrder();
		customerDAO.put(new CustomerPlaceholder(c));
		orderDAO.delete(oldCurrentOrder); // Apaga encomenda atual
		return true;
	}

	public boolean finishOrder(String customer) {
		Customer c = customerDAO.get(customer);
		if(!c.hasCurrentOrder())
			return false;
		Order order = orderDAO.get(c.getCurrentOrder().getId());
		Map<Product,Integer> products = order.getProducts();

		// verifica se existe stock e substitui keySet por produto completo do stock
		for (Product p : products.keySet()) {
			int quantity = products.get(p);
			int stock = p.getStock();
			if (quantity > stock) return false;
		}

		// atualiza stock
		for (Product p : products.keySet()) {
			int quantity = products.get(p);
			p.reduceStock(quantity);
			productDAO.update(p.getName(), p);
		}
		c.deleteCurrentOrder();
		customerDAO.put(new CustomerPlaceholder(c));
		return true;
	}

	public boolean addProductToOrder(String customerName, String product, int amount) {
		Customer customer = customerDAO.get(customerName);
		if(customer == null) return false;
		Product p = productDAO.get(product);
		if(p.getStock() < amount) return false;
		if (!customer.hasCurrentOrder()) {
			customer.newCurrentOrder();
			orderDAO.put(customer.getCurrentOrder());
			// A nova order deve vir antes do customer por causa de restrições da Foreign Key
			customerDAO.put(new CustomerPlaceholder(customer));
		}
		Order order = customer.getCurrentOrder();
		updater.put("order_product",
				order.getId() + ";" + p.getName(),
				new OrderProductQuantity(order.getId(), p.getName(), amount));
		return true;
	}

	public Map<Product, Integer> getCurrentOrderProducts(String customerName) {
		Customer customer = customerDAO.get(customerName);
		if(customer == null || !customer.hasCurrentOrder()) return null;
		Order currentOrder = orderDAO.get(customer.getCurrentOrder().getId());
		if(currentOrder == null) return new HashMap<>(0);
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

	public boolean updateProduct(String name, float price, String description, int stock) {
		Product product = new ProductImpl(name, price, description, stock);
		productDAO.put(product);
		return true;
	}
}