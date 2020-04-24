package business;

import business.customer.Customer;
import business.data.CustomerDAO;
import business.data.DAO;
import business.data.OrderDAO;
import business.data.ProductDAO;
import business.order.Order;
import business.product.Product;

import java.io.Serializable;
import java.util.Collection;

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
	public boolean addCustomer(Customer customer) {
		customerDAO.put(customer);
		return true;
	}

	@Override
	public void addToOrder(Customer client, Product prod, int quantity) {
		Customer customer = customerDAO.get(client.getId());
		Order order = customer.getCurrentOrder();
		order.addProduct(prod, quantity);
		orderDAO.update(order.getId(), order);
	}

	@Override
	public void buyOrder(Customer client) {
		Customer customer = customerDAO.get(client.getId());
		customer.newCurrentOrder();
		customerDAO.update(customer.getId(), customer);
	}

	@Override
	public Order getOrder(Customer client) {
		return customerDAO.get(client.getId()).getCurrentOrder();
	}

	@Override
	public Product getProduct(String name) {
		return productDAO.get(name);
	}

	@Override
	public Collection<Product> getProducts() {
		return productDAO.getAll().values();
	}
}