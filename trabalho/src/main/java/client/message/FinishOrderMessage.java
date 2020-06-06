package client.message;

import middleware.message.TransactionMessage;


/**
 * Used to make a finish order request. This request needs certification.
 */
public class FinishOrderMessage extends TransactionMessage<String> {
    public FinishOrderMessage(String customer){
        super(customer);
    }
}
