package client.message.bodies;

import java.io.Serializable;

public class UpdateProductBody implements Serializable {

    private final String name;
    private final float price;
    private final String description;
    private final int stock;

    public UpdateProductBody(String name, float price, String description, int stock) {
        this.name = name;
        this.price = price;
        this.description = description;
        this.stock = stock;
    }

    public String getName() {
        return name;
    }

    public float getPrice() {
        return price;
    }

    public String getDescription() {
        return description;
    }

    public int getStock() {
        return stock;
    }
}
