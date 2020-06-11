package business.data.product;

import business.data.CertifierDAO;
import business.data.DAO;
import business.order.Order;
import business.product.Product;
import middleware.certifier.StateUpdater;

import java.io.Serializable;
import java.sql.SQLException;

public class ProductCertifierDAO extends CertifierDAO<Product> {
    public ProductCertifierDAO(DAO<String, Product> dao, StateUpdater<String, Serializable> updater) {
        super(dao, "product", updater, Product::getName);
    }
}
