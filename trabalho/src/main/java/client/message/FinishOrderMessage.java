package client.message;

import business.order.Order;
import client.bodies.FinishOrderBody;
import middleware.message.ContentMessage;

public class FinishOrderMessage extends ContentMessage<FinishOrderBody> {
    public FinishOrderMessage(String customer, Order order){
        super(new FinishOrderBody(customer, order));
    }
}
