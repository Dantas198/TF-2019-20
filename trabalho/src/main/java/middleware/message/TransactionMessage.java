package middleware.message;

import java.io.Serializable;

public class TransactionMessage<T extends Serializable> extends ContentMessage<T> {
    public TransactionMessage(T body) {
        super(body);
    }
}
