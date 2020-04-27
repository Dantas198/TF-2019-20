package business;

import business.customer.Customer;
import business.customer.CustomerImpl;
import business.data.CustomerDAO;
import business.data.DAO;
import business.data.OrderDAO;
import business.data.ProductDAO;
import business.order.Order;
import business.product.Product;
import business.product.ProductImpl;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class SuperMarketImpl implements SuperMarket, Serializable {

	DAO<String, Order> orderDAO;
	DAO<String, Product> productDAO;
	DAO<String, Customer> customerDAO;

	public SuperMarketImpl(){
		this.orderDAO = new OrderDAO();
		this.productDAO = new ProductDAO();
		this.customerDAO = new CustomerDAO();
	}

	@Override
	public boolean addCustomer(String customer) {
		Customer c = new CustomerImpl(customer);
		customerDAO.put(c);
		return true;
	}


	@Override
	public boolean resetOrder(String customer) {
		Customer c = customerDAO.get(customer);
		c.newCurrentOrder();
		customerDAO.update(customer, c);
		return true;
	}

	@Override
	public boolean finishOrder(String customer) {
		Customer c = customerDAO.get(customer);
		c.getOldOrders().add(c.getCurrentOrder());
		c.newCurrentOrder();
		customerDAO.update(customer, c);
		return true;
	}

	@Override
	public boolean addProduct(String customer, Product product, int amount) {
		Customer c = customerDAO.get(customer);
		Order order = c.getCurrentOrder();
		Product p = productDAO.get(product.getName());
		order.addProduct(p, amount);
		orderDAO.update(order.getId(), order);
		return true;
	}

	@Override
	public Map<Product, Integer> getCurrentOrderProducts(String customer) {
		return customerDAO.get(customer).getCurrentOrder().getProducts();
	}

	@Override
	public List<Order> getHistory(String customer) {
		Customer c = customerDAO.get(customer);
		return c.getOldOrders();
	}

	@Override
	public Collection<Product> getCatalogProducts() {
		return productDAO.getAll().values();
	}
}