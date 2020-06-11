package business.product;

import java.io.Serializable;

public interface Product extends Serializable {

	String getName();

	float getPrice();

	String getDescription();

	int getStock();

	void reduceStock(int quantity);
}