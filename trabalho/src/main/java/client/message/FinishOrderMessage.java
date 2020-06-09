package client.message;

import middleware.message.WriteMessage;


/**
 * Used to make a finish order request. This request needs certification.
 */
public class FinishOrderMessage extends WriteMessage<String> {
    public FinishOrderMessage(String customer){
        super(customer);
    }
}
