package client.message;

import client.message.bodies.UpdateProductBody;
import middleware.message.WriteMessage;

public class UpdateProductMessage extends WriteMessage<UpdateProductBody> {
    public UpdateProductMessage(String name, float price, String description, int stock) {
        super(new UpdateProductBody(name, price, description, stock));
    }
}
