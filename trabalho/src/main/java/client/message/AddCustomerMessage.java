package client.message;

import middleware.message.ContentMessage;

public class AddCustomerMessage extends ContentMessage<String> {
    public AddCustomerMessage(String custumer) {
        super(custumer);
    }
}
