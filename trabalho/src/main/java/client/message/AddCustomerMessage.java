package client.message;

import middleware.message.WriteMessage;

public class AddCustomerMessage extends WriteMessage<String> {
    public AddCustomerMessage(String custumer) {
        super(custumer);
    }
}
