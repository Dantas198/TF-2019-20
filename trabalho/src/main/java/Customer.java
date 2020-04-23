import java.util.ArrayList;
import java.util.List;

public class Customer {

    private String id;
    private Order currentOrder;
    private List<Order> oldOrders;

    public Customer(String id) {
        this.id = id;
        this.oldOrders = new ArrayList<Order>();
    }

}
