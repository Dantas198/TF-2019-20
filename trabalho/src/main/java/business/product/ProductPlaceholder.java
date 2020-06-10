package business.product;

public class ProductPlaceholder implements Product {
    private String name;

    public ProductPlaceholder(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public float getPrice() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDescription() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getStock() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reduceStock(int quantity) {
        throw new UnsupportedOperationException();
    }
}
