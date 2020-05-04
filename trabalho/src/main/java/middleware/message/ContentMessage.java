package middleware.message;

import java.io.Serializable;
import java.util.UUID;

public class ContentMessage<T extends Serializable> extends Message implements Serializable{

    private T body;

    public ContentMessage(T body){
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
