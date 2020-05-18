package client.message;

import middleware.message.Message;
import middleware.message.WriteMessage;

public class ResetOrderMessage extends WriteMessage<String> {
    public ResetOrderMessage (String customer){
        super(customer);
    }
}
