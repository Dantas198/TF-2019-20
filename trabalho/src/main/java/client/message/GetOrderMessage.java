package client.message;

import middleware.message.ContentMessage;
import middleware.message.Message;
import middleware.message.WriteMessage;

public class GetOrderMessage extends ContentMessage<String> {
    public GetOrderMessage(String customer) {
        super(customer);
    }
}
