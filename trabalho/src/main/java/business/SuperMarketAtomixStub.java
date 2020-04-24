package business;

import business.customer.Customer;
import business.order.Order;
import business.product.Product;

import java.util.Collection;

public class SuperMarketAtomixStub implements SuperMakert {
    @Override
    public boolean addCustomer(Customer customer) {
        return false;
    }

    @Override
    public void addToOrder(Customer client, Product prod, int quantity) {

    }

    @Override
    public void buyOrder(Customer client) {

    }

    @Override
    public Order getOrder(Customer client) {
        return null;
    }

    @Override
    public Product getProduct(String name) {
        return null;
    }

    @Override
    public Collection<Product> getProducts() {
        return null;
    }
}