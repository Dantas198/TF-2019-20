package client.message;

import middleware.message.Message;
import middleware.message.WriteMessage;

public class GetOrderMessage extends WriteMessage<String> {
    public GetOrderMessage(String customer) {
        super(customer);
    }
}
