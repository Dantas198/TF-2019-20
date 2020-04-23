import java.util.HashMap;
import java.util.Map;

public class SuperMarket {

    private DAO<String,Customer> customerDAO;


    public SuperMarket() {
        customerDAO = new CustomerDAO();
    }

    public void buyOrder(Customer customer) {

    }
}
