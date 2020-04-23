import java.util.HashMap;
import java.util.Map;

public class Order {

    private String id;
    private Map<String,Product> products;

    public Order(String id) {
        this.id = id;
        products = new HashMap<String, Product>();
    }
}
