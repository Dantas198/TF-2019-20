package middleware.message;

import java.io.Serializable;

/**
 * Message that implies certification
 */
public class WriteMessage<T extends Serializable> extends ContentMessage<T> {
    public WriteMessage(T body) {
        super(body);
    }
}
