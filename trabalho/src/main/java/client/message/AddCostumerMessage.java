package client.message;

import middleware.message.ContentMessage;

public class AddCostumerMessage extends ContentMessage<String> {
    public AddCostumerMessage(String custumer) {
        super(custumer);
    }
}
