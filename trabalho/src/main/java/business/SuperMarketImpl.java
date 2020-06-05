package business;

import business.customer.Customer;
import business.customer.CustomerImpl;
import business.data.DBInitialization;
import business.data.DAO;
import business.data.customer.CustomerSQLDAO;
import business.data.order.OrderSQLDAO;
import business.data.product.ProductSQLDAO;
import business.order.Order;
import business.order.OrderImpl;
import business.product.Product;
import middleware.certifier.BitWriteSet;
import server.CurrentOrderCleaner;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.*;

public class SuperMarketImpl implements Serializable { // Implement SuperMarket

	private DAO<String, Order> orderDAO;
	private DAO<String, Product> productDAO;
	private DAO<String, Customer> customerDAO;
	private CurrentOrderCleaner cleaner;
	private Connection connection;

	public SuperMarketImpl(String privateName, Connection connection) throws SQLException {
		this.connection = connection;
		DBInitialization dbInit = new DBInitialization(this.connection);
		if(!dbInit.exists()){
			dbInit.init();
			System.out.println("Database initialized");
			if(privateName.equals("Server0")) {
				dbInit.populateProduct();
				System.out.println("Populated database");
			} else {
				// TODO: pedir os dados
				/*
				try {
					Files.copy(new File("db/Server0.sql").toPath(), new File("db/" + privateName + ".script").toPath());
				} catch (IOException e) {
					e.printStackTrace();
				}
				 */
			}
			//this.connection.prepareCall("SCRIPT 'db/" + privateName  + ".sql'").execute();
		}
		OrderSQLDAO orderSQLDAO = new OrderSQLDAO(this.connection);
		this.orderDAO = orderSQLDAO;
		this.productDAO = new ProductSQLDAO(this.connection);
		CustomerSQLDAO customerSQLDAO = new CustomerSQLDAO(this.connection, orderSQLDAO);
		this.customerDAO = customerSQLDAO;
		long tmax = 100;
		cleaner = new CurrentOrderCleaner(customerSQLDAO, Duration.ofDays(tmax));
	}

	public boolean addCustomer(String customer) {
		Customer c = new CustomerImpl(customer);
		return customerDAO.put(c);
	}

	public boolean resetOrder(String customer) {
		Customer c = customerDAO.get(customer);
		c.newCurrentOrder();
		customerDAO.update(customer, c);
		return true;
	}

	public boolean finishOrder(String customer, Map<String, BitWriteSet> writeSets) {
		// TODO: Resolver problemas de transação
		try {
			connection.setAutoCommit(false);
		} catch (SQLException throwables) {
			return false;
		}
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
		BitWriteSet productBws = new BitWriteSet();
		for (Product p : products.keySet()) {
			int quantity = products.get(p);
			p.reduceStock(quantity);
			productDAO.update(p.getName(), p);
			productBws.add(p.getName());
		}
		writeSets.put("product", productBws);
		BitWriteSet customerBws = new BitWriteSet();
		customerBws.add(c.getId());
		writeSets.put("customer", customerBws);

		c.getOldOrders().add(order); // pode ser removido
		c.newCurrentOrder();
		customerDAO.update(customer, c);
		/*
		try {
			this.connection.commit();
		} catch (SQLException throwables) {
			try {
				this.connection.rollback();
				this.connection.setAutoCommit(true);
			} catch (SQLException throwables1) {
				return false;
			}
			return false;
		}
		 */
		return true;
	}

	public boolean addProduct(String customerName, String product, int amount) {
		System.out.println("addProduct");
		Customer customer = customerDAO.get(customerName);
		if(customer == null) return false;
		try {
			cleaner.clean();
		} catch (SQLException throwables) {
			throwables.printStackTrace();
		}
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