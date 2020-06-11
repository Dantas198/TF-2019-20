package business.data.customer;

import business.customer.Customer;
import business.data.CertifierDAO;
import business.data.DAO;
import middleware.certifier.StateUpdater;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;

public class CustomerCertifierDAO extends CertifierDAO<Customer> {
    public CustomerCertifierDAO(DAO<String, Customer> dao, StateUpdater<String, Serializable> updater) {
        super(dao, "customer", updater, Customer::getId);
    }
}
