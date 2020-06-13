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

import java.io.Serializable;
import java.time.Duration;
import java.util.*;

public class SuperMarketImpl implements SuperMarket, Serializable {

	private DAO<String, Order> orderDAO;
	private DAO<String, Product> productDAO;
	private DAO<String, Customer> customerDAO;
	private StateUpdater<String, Serializable> updater;
	private Calendar calendar;
	private long tmax;

	public SuperMarketImpl(DAO<String, Order> orderDAO,
						   DAO<String, Product> productDAO,
						   DAO<String, Customer> customerDAO,
						   StateUpdater<String, Serializable> updater,
						   Duration tmax) {
		this.orderDAO = orderDAO;
		this.productDAO = productDAO;
		this.customerDAO = customerDAO;
		this.updater = updater;
		this.calendar = Calendar.getInstance();
		this.tmax = tmax.toMillis();
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
		if(order == null || isClosed(order)) return false;
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
		if(amount < 0) return false;
		Customer customer = customerDAO.get(customerName);
		if(customer == null) return false;
		if (!customer.hasCurrentOrder()) {
			customer.newCurrentOrder();
			orderDAO.put(customer.getCurrentOrder());
			// A nova order deve vir antes do customer por causa de restrições da Foreign Key
			customerDAO.put(new CustomerPlaceholder(customer));
		} else if(isClosed(customer.getCurrentOrder())) {
			orderDAO.delete(customer.getCurrentOrder().getId());
			customer.newCurrentOrder();
			orderDAO.put(customer.getCurrentOrder());
			// A nova order deve vir antes do customer por causa de restrições da Foreign Key
			customerDAO.put(new CustomerPlaceholder(customer));
		}
		Order order = customer.getCurrentOrder();
		Product p = productDAO.get(product);
		if(p.getStock() < amount) return false;
		updater.put("order_product",
				order.getId() + ";" + p.getName(),
				new OrderProductQuantity(order.getId(), p.getName(), amount));
		return true;
	}

	public Map<Product, Integer> getCurrentOrderProducts(String customerName) {
		Customer customer = customerDAO.get(customerName);
		if(customer == null || !customer.hasCurrentOrder()) return null;
		Order currentOrder = orderDAO.get(customer.getCurrentOrder().getId());
		if(currentOrder == null || isClosed(currentOrder)) return new HashMap<>(0);
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

	private boolean isClosed(Order order) {
		long now = Calendar.getInstance().getTimeInMillis();
		long threshold = now - tmax;
		System.out.println(new Date(now) + " " + threshold);
		System.out.println(order.getTimestamp() + " - " + new Date(threshold));
		long orderTime = order.getTimestamp().getTime();
		return orderTime < threshold;
	}
}