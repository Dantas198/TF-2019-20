package middleware.message;

import java.io.Serializable;
import java.util.UUID;

public class Message<T extends Serializable> implements Serializable {

    private String id;
    private Serializable body;

    public Message(){
        this.id = UUID.randomUUID().toString();
    }

    public Message(Serializable body){
        this.id = UUID.randomUUID().toString();
        this.body = body;
    }

    public Serializable getBody() {
        return body;
    }

    public String getId() {
        return id;
    }
}
