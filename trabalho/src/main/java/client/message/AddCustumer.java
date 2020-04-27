package client.message;

import middleware.message.Message;

public class AddCustumer extends Message {
    public AddCustumer(String custumer) {
        super(custumer);
    }
}
