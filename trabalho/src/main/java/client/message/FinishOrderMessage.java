package client.message;

import business.order.Order;
import client.bodies.FinishOrderBody;
import middleware.message.TransactionMessage;
import middleware.message.WriteMessage;


/**
 * Used to make a finish order request. This request needs certification.
 */
public class FinishOrderMessage extends TransactionMessage<String> {
    public FinishOrderMessage(String customer){
        super(customer);
    }
}
