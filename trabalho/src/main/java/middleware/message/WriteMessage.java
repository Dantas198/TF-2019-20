package middleware.message;

import java.io.Serializable;

/**
 * Message that implies certification
 */
public class WriteMessage<T extends Serializable> extends Message {
    private T body;

    public WriteMessage(T body){
        super();
        this.body = body;
    }

    public T getBody() {
        return body;
    }

    public String getId() {
        return super.getId();
    }
}
