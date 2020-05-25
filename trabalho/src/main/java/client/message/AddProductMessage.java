package client.message;

import client.bodies.AddProductBody;
import middleware.message.Message;
import middleware.message.WriteMessage;

public class AddProductMessage extends WriteMessage<AddProductBody> {
    public AddProductMessage(String customer, String product, int amount) {
        super(new AddProductBody(customer, product, amount));
    }
}
