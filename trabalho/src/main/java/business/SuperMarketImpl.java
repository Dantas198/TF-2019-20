package business;

import business.customer.Customer;
import business.customer.CustomerImpl;
import business.data.customer.CustomerDAO;
import business.data.DAO;
import business.data.customer.CustomerSQLDAO;
import business.data.order.OrderDAO;
import business.data.order.OrderSQLDAO;
import business.data.product.ProductDAO;
import business.data.product.ProductSQLDAO;
import business.order.Order;
import business.product.Product;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

public class SuperMarketImpl implements SuperMarket, Serializable {

	private DAO<String, Order> orderDAO;
	private DAO<String, Product> productDAO;
	private DAO<String, Customer> customerDAO;

	public SuperMarketImpl(String privateName) throws SQLException {
		Connection c = DriverManager.getConnection("jdbc:hsqldb:file:" + privateName + ";shutdown=true;hsqldb.sqllog=2", "", "");
		OrderSQLDAO orderSQLDAO = new OrderSQLDAO(c);
		this.orderDAO = orderSQLDAO;
		this.productDAO = new ProductSQLDAO(c);
		this.customerDAO = new CustomerSQLDAO(c, orderSQLDAO);
	}

	@Override
	public boolean addCustomer(String customer) {
		Customer c = new CustomerImpl(customer);
		return customerDAO.put(c);
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
		// TODO tmax
		Customer c = customerDAO.get(customer);
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
			productDAO.update(p.getName(), p);
		}

		c.getOldOrders().add(order);
		c.newCurrentOrder();
		customerDAO.update(customer, c);
		return true;
	}



	@Override
	public boolean addProduct(String customer, String product, int amount) {
		Customer c = customerDAO.get(customer);
		if (!c.hasCurrentOrder()) {
			/* customerDAO retorna uma instância de CustomerSQLImpl
			// CustomerSQLImpl subrepõe o método newCurrentOrder
			// e ao fazer newCurrentOrder ele remove a current order antiga da tabela Order
			// e adiciona a current order à tabela e no cliente
			*/
			c.newCurrentOrder();
		}
		Order order = c.getCurrentOrder();
		Product p = productDAO.get(product);
		order.addProduct(p, amount);
		orderDAO.update(order.getId(), order);
		return true;
	}

	@Override
	public Map<Product, Integer> getCurrentOrderProducts(String customer) {
		return customerDAO.get(customer).getCurrentOrder().getProducts();
	}

	@Override
	public Collection<Order> getHistory(String customer) {
		Customer c = customerDAO.get(customer);
		return c.getOldOrders();
	}

	@Override
	public Collection<Product> getCatalogProducts() {
		return productDAO.getAll().values();
	}
}