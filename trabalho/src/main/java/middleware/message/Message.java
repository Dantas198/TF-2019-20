package middleware.message;

import java.io.Serializable;
import java.util.UUID;

public abstract class Message implements Serializable {

    private String id;
    private Serializable body;

    public Message(Serializable body){
        this.id = UUID.randomUUID().toString();
        this.body = body;
    }
}
