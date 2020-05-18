package client.bodies;

import java.io.Serializable;

public class AddProductBody implements Serializable {

    private final String customer;
    private final String product;
    private final int amount;

    public AddProductBody(String customer, String product, int amount) {
        this.customer = customer;
        this.product = product;
        this.amount = amount;
    }

    public String getCustomer() {
        return customer;
    }

    public String getProduct() {
        return product;
    }

    public int getAmount() {
        return amount;
    }
}
